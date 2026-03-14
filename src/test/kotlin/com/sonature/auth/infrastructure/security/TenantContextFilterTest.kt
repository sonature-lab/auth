package com.sonature.auth.infrastructure.security

import com.sonature.auth.application.service.JwtService
import com.sonature.auth.domain.tenant.context.TenantContextHolder
import com.sonature.auth.domain.tenant.model.TenantRole
import com.sonature.auth.domain.token.model.TokenClaims
import com.sonature.auth.domain.token.model.TokenType
import com.sonature.auth.domain.token.model.Algorithm
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import jakarta.servlet.FilterChain
import java.time.Instant
import java.util.UUID

class TenantContextFilterTest {

    private val jwtService = mockk<JwtService>()
    private val filter = TenantContextFilter(jwtService)
    private val filterChain = mockk<FilterChain>(relaxed = true)

    @AfterEach
    fun cleanup() {
        TenantContextHolder.clear()
    }

    @Test
    fun `sets tenant context when valid token and tenant header provided`() {
        val userId = UUID.randomUUID()
        val request = MockHttpServletRequest().apply {
            addHeader("X-Tenant-Slug", "my-org")
            addHeader("Authorization", "Bearer valid-token")
        }
        val response = MockHttpServletResponse()

        every { jwtService.verifyToken("valid-token", Algorithm.HS256) } returns TokenClaims(
            subject = userId.toString(),
            issuer = "sonature-auth",
            issuedAt = Instant.now(),
            expiresAt = Instant.now().plusSeconds(900),
            audience = null,
            tokenType = TokenType.ACCESS,
            tokenId = UUID.randomUUID().toString(),
            customClaims = mapOf(
                "tenants" to listOf(
                    mapOf("slug" to "my-org", "role" to "OWNER")
                )
            )
        )

        var capturedContext: com.sonature.auth.domain.tenant.context.TenantContext? = null
        every { filterChain.doFilter(any(), any()) } answers {
            capturedContext = TenantContextHolder.get()
        }

        filter.doFilter(request, response, filterChain)

        assertNotNull(capturedContext)
        assertEquals("my-org", capturedContext!!.tenantSlug)
        assertEquals(userId, capturedContext!!.userId)
        assertEquals(TenantRole.OWNER, capturedContext!!.role)
    }

    @Test
    fun `does not set context when tenant header missing`() {
        val request = MockHttpServletRequest().apply {
            addHeader("Authorization", "Bearer valid-token")
        }
        val response = MockHttpServletResponse()

        var capturedContext: com.sonature.auth.domain.tenant.context.TenantContext? = null
        every { filterChain.doFilter(any(), any()) } answers {
            capturedContext = TenantContextHolder.get()
        }

        filter.doFilter(request, response, filterChain)

        assertNull(capturedContext)
    }

    @Test
    fun `does not set context when authorization header missing`() {
        val request = MockHttpServletRequest().apply {
            addHeader("X-Tenant-Slug", "my-org")
        }
        val response = MockHttpServletResponse()

        var capturedContext: com.sonature.auth.domain.tenant.context.TenantContext? = null
        every { filterChain.doFilter(any(), any()) } answers {
            capturedContext = TenantContextHolder.get()
        }

        filter.doFilter(request, response, filterChain)

        assertNull(capturedContext)
    }

    @Test
    fun `does not set context when user not member of requested tenant`() {
        val userId = UUID.randomUUID()
        val request = MockHttpServletRequest().apply {
            addHeader("X-Tenant-Slug", "other-org")
            addHeader("Authorization", "Bearer valid-token")
        }
        val response = MockHttpServletResponse()

        every { jwtService.verifyToken("valid-token", Algorithm.HS256) } returns TokenClaims(
            subject = userId.toString(),
            issuer = "sonature-auth",
            issuedAt = Instant.now(),
            expiresAt = Instant.now().plusSeconds(900),
            audience = null,
            tokenType = TokenType.ACCESS,
            tokenId = UUID.randomUUID().toString(),
            customClaims = mapOf(
                "tenants" to listOf(
                    mapOf("slug" to "my-org", "role" to "OWNER")
                )
            )
        )

        var capturedContext: com.sonature.auth.domain.tenant.context.TenantContext? = null
        every { filterChain.doFilter(any(), any()) } answers {
            capturedContext = TenantContextHolder.get()
        }

        filter.doFilter(request, response, filterChain)

        assertNull(capturedContext)
    }

    @Test
    fun `clears context after request completes`() {
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, filterChain)

        assertNull(TenantContextHolder.get())
    }

    @Test
    fun `clears context even when exception occurs`() {
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()

        every { filterChain.doFilter(any(), any()) } throws RuntimeException("test")

        try {
            filter.doFilter(request, response, filterChain)
        } catch (_: RuntimeException) {}

        assertNull(TenantContextHolder.get())
    }

    @Test
    fun `handles invalid token gracefully`() {
        val request = MockHttpServletRequest().apply {
            addHeader("X-Tenant-Slug", "my-org")
            addHeader("Authorization", "Bearer invalid-token")
        }
        val response = MockHttpServletResponse()

        every { jwtService.verifyToken("invalid-token", Algorithm.HS256) } throws RuntimeException("Invalid token")

        var capturedContext: com.sonature.auth.domain.tenant.context.TenantContext? = null
        every { filterChain.doFilter(any(), any()) } answers {
            capturedContext = TenantContextHolder.get()
        }

        filter.doFilter(request, response, filterChain)

        assertNull(capturedContext)
        verify { filterChain.doFilter(any(), any()) }
    }

    @Test
    fun `extracts tenant roles from custom claims`() {
        val claims = mapOf<String, Any>(
            "tenants" to listOf(
                mapOf("slug" to "org-a", "role" to "OWNER"),
                mapOf("slug" to "org-b", "role" to "MEMBER"),
                mapOf("slug" to "org-c", "role" to "VIEWER")
            )
        )

        val roles = filter.extractTenantRoles(claims)

        assertEquals(3, roles.size)
        assertEquals(TenantRole.OWNER, roles["org-a"])
        assertEquals(TenantRole.MEMBER, roles["org-b"])
        assertEquals(TenantRole.VIEWER, roles["org-c"])
    }

    @Test
    fun `returns empty map when no tenants claim`() {
        val roles = filter.extractTenantRoles(emptyMap())
        assertTrue(roles.isEmpty())
    }
}
