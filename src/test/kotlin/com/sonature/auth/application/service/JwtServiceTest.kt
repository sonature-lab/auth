package com.sonature.auth.application.service

import com.sonature.auth.application.port.output.TokenProvider
import com.sonature.auth.common.util.IdGenerator
import com.sonature.auth.common.util.TimeProvider
import com.sonature.auth.domain.token.exception.UnsupportedAlgorithmException
import com.sonature.auth.domain.token.model.Algorithm
import com.sonature.auth.domain.token.model.TokenClaims
import com.sonature.auth.domain.token.model.TokenConfig
import com.sonature.auth.domain.token.model.TokenType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class JwtServiceTest {

    private lateinit var tokenProvider: TokenProvider
    private lateinit var timeProvider: TimeProvider
    private lateinit var idGenerator: IdGenerator
    private lateinit var jwtService: JwtService

    private val fixedTime = Instant.parse("2026-01-29T10:00:00Z")
    private val fixedId = "test-jti-123"

    @BeforeEach
    fun setUp() {
        tokenProvider = mockk()
        timeProvider = mockk()
        idGenerator = mockk()

        every { tokenProvider.supportedAlgorithm() } returns Algorithm.HS256
        every { timeProvider.now() } returns fixedTime
        every { idGenerator.generateId() } returns fixedId

        jwtService = JwtService(
            tokenProviders = listOf(tokenProvider),
            timeProvider = timeProvider,
            idGenerator = idGenerator
        )
    }

    @Test
    fun `issueAccessToken should create token with correct claims`() {
        val expectedToken = "eyJhbGciOiJIUzI1NiJ9.xxx.xxx"
        every { tokenProvider.issue(any(), any()) } returns expectedToken

        val token = jwtService.issueAccessToken(
            subject = "user-123",
            audience = "client-app"
        )

        assertNotNull(token)
        assertEquals(expectedToken, token.value)
        assertEquals(TokenType.ACCESS, token.type)
        assertEquals(Algorithm.HS256, token.algorithm)

        verify {
            tokenProvider.issue(match {
                it.subject == "user-123" &&
                it.audience == "client-app" &&
                it.tokenId == fixedId &&
                it.tokenType == TokenType.ACCESS &&
                it.issuer == "sonature-auth"
            }, any())
        }
    }

    @Test
    fun `issueAccessToken should use default expiration when not specified`() {
        every { tokenProvider.issue(any(), any()) } returns "token"

        val token = jwtService.issueAccessToken(subject = "user-123")

        assertEquals(fixedTime.plus(TokenConfig.DEFAULT_ACCESS_EXPIRATION), token.expiresAt)
        assertEquals(fixedTime, token.issuedAt)
    }

    @Test
    fun `issueAccessToken should use custom expiration when specified`() {
        every { tokenProvider.issue(any(), any()) } returns "token"

        val customExpiration = Duration.ofMinutes(30)
        val token = jwtService.issueAccessToken(
            subject = "user-123",
            expiration = customExpiration
        )

        assertEquals(fixedTime.plus(customExpiration), token.expiresAt)
    }

    @Test
    fun `issueAccessToken should include custom claims`() {
        every { tokenProvider.issue(any(), any()) } returns "token"

        val customClaims = mapOf("role" to "admin", "orgId" to "org-456")
        jwtService.issueAccessToken(
            subject = "user-123",
            customClaims = customClaims
        )

        verify {
            tokenProvider.issue(match {
                it.customClaims["role"] == "admin" &&
                it.customClaims["orgId"] == "org-456"
            }, any())
        }
    }

    @Test
    fun `verifyToken should delegate to provider`() {
        val expectedClaims = TokenClaims(
            issuer = "sonature-auth",
            subject = "user-123",
            audience = null,
            expiresAt = fixedTime.plusSeconds(900),
            issuedAt = fixedTime,
            tokenId = fixedId,
            tokenType = TokenType.ACCESS
        )

        every { tokenProvider.verify("valid-token") } returns expectedClaims

        val claims = jwtService.verifyToken("valid-token")

        assertEquals(expectedClaims.subject, claims.subject)
        assertEquals(expectedClaims.issuer, claims.issuer)
        verify { tokenProvider.verify("valid-token") }
    }

    @Test
    fun `should throw UnsupportedAlgorithmException for unknown algorithm`() {
        assertThrows<UnsupportedAlgorithmException> {
            jwtService.issueAccessToken(
                subject = "user-123",
                algorithm = Algorithm.RS256
            )
        }
    }
}
