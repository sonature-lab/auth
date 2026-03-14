package com.sonature.auth.infrastructure.security

import com.sonature.auth.application.service.JwtService
import com.sonature.auth.domain.token.model.Algorithm
import com.sonature.auth.domain.token.model.TokenClaims
import com.sonature.auth.domain.token.model.TokenType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.servlet.FilterChain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import java.time.Instant
import java.util.UUID

class JwtBearerAuthenticationFilterTest {

    private val jwtService = mockk<JwtService>()
    private val filter = JwtBearerAuthenticationFilter(jwtService)
    private val filterChain = mockk<FilterChain>(relaxed = true)

    @AfterEach
    fun cleanup() {
        SecurityContextHolder.clearContext()
    }

    private fun stubValidToken(token: String, subject: String = UUID.randomUUID().toString()) {
        val claims = TokenClaims(
            issuer = "sonature-auth",
            subject = subject,
            audience = null,
            expiresAt = Instant.now().plusSeconds(900),
            issuedAt = Instant.now(),
            tokenId = UUID.randomUUID().toString(),
            tokenType = TokenType.ACCESS,
            customClaims = emptyMap()
        )
        every { jwtService.verifyToken(token, Algorithm.HS256) } returns claims
    }

    // --- Happy Path ---

    @Test
    fun `should set authentication when valid Bearer token is provided`() {
        val userId = UUID.randomUUID().toString()
        val token = "valid-jwt-token"
        stubValidToken(token, userId)

        val request = MockHttpServletRequest().apply {
            addHeader("Authorization", "Bearer $token")
        }
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, filterChain)

        val authentication = SecurityContextHolder.getContext().authentication
        assertNotNull(authentication)
        assertEquals(userId, authentication.principal)
        verify { filterChain.doFilter(request, response) }
    }

    @Test
    fun `should set ROLE_USER authority for authenticated user`() {
        stubValidToken("some-token", UUID.randomUUID().toString())

        val request = MockHttpServletRequest().apply {
            addHeader("Authorization", "Bearer some-token")
        }

        filter.doFilter(request, MockHttpServletResponse(), filterChain)

        val authorities = SecurityContextHolder.getContext().authentication?.authorities
        assertNotNull(authorities)
        assertEquals("ROLE_USER", authorities!!.first().authority)
    }

    // --- Edge Cases ---

    @Test
    fun `should not set authentication when no Authorization header`() {
        val request = MockHttpServletRequest()

        filter.doFilter(request, MockHttpServletResponse(), filterChain)

        assertNull(SecurityContextHolder.getContext().authentication)
        verify { filterChain.doFilter(any(), any()) }
    }

    @Test
    fun `should not set authentication when Authorization header is not Bearer`() {
        val request = MockHttpServletRequest().apply {
            addHeader("Authorization", "Basic dXNlcjpwYXNz")
        }

        filter.doFilter(request, MockHttpServletResponse(), filterChain)

        assertNull(SecurityContextHolder.getContext().authentication)
    }

    @Test
    fun `should not replace existing authentication when already set`() {
        stubValidToken("new-token", "new-user")

        // Pre-set authentication
        val existingAuth = org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
            "existing-user", null, emptyList()
        )
        SecurityContextHolder.getContext().authentication = existingAuth

        val request = MockHttpServletRequest().apply {
            addHeader("Authorization", "Bearer new-token")
        }

        filter.doFilter(request, MockHttpServletResponse(), filterChain)

        // Existing authentication should remain
        assertEquals("existing-user", SecurityContextHolder.getContext().authentication.principal)
        verify(exactly = 0) { jwtService.verifyToken(any(), any()) }
    }

    // --- Error Cases ---

    @Test
    fun `should not set authentication when token verification fails`() {
        every { jwtService.verifyToken("invalid-token", Algorithm.HS256) } throws RuntimeException("Invalid token")

        val request = MockHttpServletRequest().apply {
            addHeader("Authorization", "Bearer invalid-token")
        }

        filter.doFilter(request, MockHttpServletResponse(), filterChain)

        assertNull(SecurityContextHolder.getContext().authentication)
        verify { filterChain.doFilter(any(), any()) }
    }

    @Test
    fun `should continue filter chain even when token verification fails`() {
        every { jwtService.verifyToken(any(), any()) } throws RuntimeException("Expired token")

        val request = MockHttpServletRequest().apply {
            addHeader("Authorization", "Bearer expired-token")
        }
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, filterChain)

        verify { filterChain.doFilter(request, response) }
    }
}
