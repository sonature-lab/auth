package com.sonature.auth.infrastructure.oauth2

import com.sonature.auth.application.usecase.TokenRefreshUseCase
import com.sonature.auth.domain.token.model.Algorithm
import com.sonature.auth.domain.token.model.Token
import com.sonature.auth.domain.token.model.TokenPair
import com.sonature.auth.domain.token.model.TokenType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.core.user.DefaultOAuth2User
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import java.time.Instant
import java.util.UUID

class OAuth2LoginSuccessHandlerTest {

    private lateinit var tokenRefreshUseCase: TokenRefreshUseCase
    private lateinit var handler: OAuth2LoginSuccessHandler

    private val now = Instant.parse("2026-03-14T10:00:00Z")

    @BeforeEach
    fun setUp() {
        tokenRefreshUseCase = mockk()
        handler = OAuth2LoginSuccessHandler(tokenRefreshUseCase)
    }

    private fun mockTokenPair(): TokenPair {
        val accessToken = Token(
            value = "test-access-token",
            type = TokenType.ACCESS,
            algorithm = Algorithm.HS256,
            expiresAt = now.plusSeconds(900),
            issuedAt = now
        )
        val refreshToken = Token(
            value = "test-refresh-token",
            type = TokenType.REFRESH,
            algorithm = Algorithm.HS256,
            expiresAt = now.plusSeconds(604800),
            issuedAt = now
        )
        return TokenPair(accessToken, refreshToken)
    }

    @Test
    fun `should issue JWT tokens on successful OAuth2 login`() {
        val userId = UUID.randomUUID().toString()
        val attributes = mapOf<String, Any>(
            "sub" to "google-123",
            "email" to "user@gmail.com",
            "name" to "Test User",
            "userId" to userId
        )
        val oAuth2User = DefaultOAuth2User(
            listOf(SimpleGrantedAuthority("ROLE_USER")),
            attributes,
            "sub"
        )
        val authentication = OAuth2AuthenticationToken(
            oAuth2User,
            oAuth2User.authorities,
            "google"
        )

        every { tokenRefreshUseCase.issueTokenPair(any(), any(), any(), any(), any(), any(), any(), any()) } returns mockTokenPair()

        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()

        handler.onAuthenticationSuccess(request, response, authentication)

        verify { tokenRefreshUseCase.issueTokenPair(any(), any(), any(), any(), any(), any(), any(), any()) }
        assertEquals(HttpServletResponse.SC_OK, response.status)
    }

    @Test
    fun `should include tokens in response body as JSON`() {
        val userId = UUID.randomUUID().toString()
        val attributes = mapOf<String, Any>(
            "sub" to "google-123",
            "email" to "user@gmail.com",
            "name" to "Test User",
            "userId" to userId
        )
        val oAuth2User = DefaultOAuth2User(
            listOf(SimpleGrantedAuthority("ROLE_USER")),
            attributes,
            "sub"
        )
        val authentication = OAuth2AuthenticationToken(
            oAuth2User,
            oAuth2User.authorities,
            "google"
        )

        every { tokenRefreshUseCase.issueTokenPair(any(), any(), any(), any(), any(), any(), any(), any()) } returns mockTokenPair()

        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()

        handler.onAuthenticationSuccess(request, response, authentication)

        val responseBody = response.contentAsString
        assertTrue(responseBody.contains("test-access-token"))
        assertTrue(responseBody.contains("test-refresh-token"))
        assertEquals("application/json;charset=UTF-8", response.contentType)
    }

    private fun assertEquals(expected: Any, actual: Any?) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual)
    }
}
