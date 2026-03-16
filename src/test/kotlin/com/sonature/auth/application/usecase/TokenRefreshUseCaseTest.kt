package com.sonature.auth.application.usecase

import com.sonature.auth.application.service.JwtService
import com.sonature.auth.application.service.RefreshTokenService
import com.sonature.auth.domain.refresh.entity.RefreshTokenEntity
import com.sonature.auth.domain.tenant.context.TenantContext
import com.sonature.auth.domain.tenant.context.TenantContextHolder
import com.sonature.auth.domain.tenant.exception.TenantMismatchException
import com.sonature.auth.domain.tenant.model.TenantRole
import com.sonature.auth.domain.token.model.Algorithm
import com.sonature.auth.domain.token.model.Token
import com.sonature.auth.domain.token.model.TokenClaims
import com.sonature.auth.domain.token.model.TokenPair
import com.sonature.auth.domain.token.model.TokenType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration
import java.time.Instant
import java.util.UUID

class TokenRefreshUseCaseTest {

    private lateinit var jwtService: JwtService
    private lateinit var refreshTokenService: RefreshTokenService
    private lateinit var useCase: TokenRefreshUseCase

    private val now = Instant.parse("2026-03-13T10:00:00Z")
    private val tenantId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
    private val otherTenantId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")
    private val userId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc")
    private val subject = "user-abc"

    @BeforeEach
    fun setUp() {
        jwtService = mockk()
        refreshTokenService = mockk()
        useCase = TokenRefreshUseCase(jwtService, refreshTokenService)
        TenantContextHolder.clear()
    }

    @AfterEach
    fun tearDown() {
        TenantContextHolder.clear()
    }

    @Test
    fun `refreshTokens should return new token pair on valid refresh token`() {
        val refreshTokenValue = "valid-refresh-token"
        val claims = buildRefreshClaims(subject)
        val oldEntity = buildEntity(subject, tenantId = null)
        val newPair = buildTokenPair(subject)

        every { jwtService.verifyTokenWithType(refreshTokenValue, TokenType.REFRESH, any()) } returns claims
        every { refreshTokenService.validateAndConsume(refreshTokenValue) } returns oldEntity
        every { jwtService.issueTokenPair(any(), any(), any(), any(), any(), any(), any()) } returns newPair
        every { refreshTokenService.rotateToken(any(), any(), any(), any()) } returns oldEntity

        val result = useCase.refreshTokens(refreshTokenValue)

        assertNotNull(result)
        verify { refreshTokenService.rotateToken(any(), any(), any(), any()) }
    }

    @Test
    fun `refreshTokens should throw TenantMismatchException when token tenant differs from context`() {
        val refreshTokenValue = "tenant-token"
        val claims = buildRefreshClaims(subject)
        val oldEntity = buildEntity(subject, tenantId = tenantId)

        TenantContextHolder.set(TenantContext(
            tenantSlug = "other-tenant",
            tenantId = otherTenantId,
            userId = userId,
            role = TenantRole.MEMBER
        ))

        every { jwtService.verifyTokenWithType(refreshTokenValue, TokenType.REFRESH, any()) } returns claims
        every { refreshTokenService.validateAndConsume(refreshTokenValue) } returns oldEntity

        assertThrows<TenantMismatchException> {
            useCase.refreshTokens(refreshTokenValue)
        }
    }

    @Test
    fun `refreshTokens should succeed when token tenant matches context`() {
        val refreshTokenValue = "matching-token"
        val claims = buildRefreshClaims(subject)
        val oldEntity = buildEntity(subject, tenantId = tenantId)
        val newPair = buildTokenPair(subject)

        TenantContextHolder.set(TenantContext(
            tenantSlug = "test-tenant",
            tenantId = tenantId,
            userId = userId,
            role = TenantRole.MEMBER
        ))

        every { jwtService.verifyTokenWithType(refreshTokenValue, TokenType.REFRESH, any()) } returns claims
        every { refreshTokenService.validateAndConsume(refreshTokenValue) } returns oldEntity
        every { jwtService.issueTokenPair(any(), any(), any(), any(), any(), any(), any()) } returns newPair
        every { refreshTokenService.rotateToken(any(), any(), any(), any()) } returns oldEntity

        val result = useCase.refreshTokens(refreshTokenValue)

        assertNotNull(result)
    }

    @Test
    fun `refreshTokens should allow global token (tenantId=null) regardless of context`() {
        val refreshTokenValue = "global-token"
        val claims = buildRefreshClaims(subject)
        val oldEntity = buildEntity(subject, tenantId = null)
        val newPair = buildTokenPair(subject)

        TenantContextHolder.set(TenantContext(
            tenantSlug = "any-tenant",
            tenantId = tenantId,
            userId = userId,
            role = TenantRole.MEMBER
        ))

        every { jwtService.verifyTokenWithType(refreshTokenValue, TokenType.REFRESH, any()) } returns claims
        every { refreshTokenService.validateAndConsume(refreshTokenValue) } returns oldEntity
        every { jwtService.issueTokenPair(any(), any(), any(), any(), any(), any(), any()) } returns newPair
        every { refreshTokenService.rotateToken(any(), any(), any(), any()) } returns oldEntity

        val result = useCase.refreshTokens(refreshTokenValue)

        assertNotNull(result)
    }

    @Test
    fun `refreshTokens should allow tenant token when no context is present`() {
        val refreshTokenValue = "tenant-token-no-context"
        val claims = buildRefreshClaims(subject)
        val oldEntity = buildEntity(subject, tenantId = tenantId)
        val newPair = buildTokenPair(subject)

        // No context set
        every { jwtService.verifyTokenWithType(refreshTokenValue, TokenType.REFRESH, any()) } returns claims
        every { refreshTokenService.validateAndConsume(refreshTokenValue) } returns oldEntity
        every { jwtService.issueTokenPair(any(), any(), any(), any(), any(), any(), any()) } returns newPair
        every { refreshTokenService.rotateToken(any(), any(), any(), any()) } returns oldEntity

        val result = useCase.refreshTokens(refreshTokenValue)

        assertNotNull(result)
    }

    @Test
    fun `revokeAllSessions should delegate to refreshTokenService`() {
        every { refreshTokenService.revokeAllTokensForSubject(subject) } returns 3

        val count = useCase.revokeAllSessions(subject)

        assert(count == 3)
        verify { refreshTokenService.revokeAllTokensForSubject(subject) }
    }

    // --- helpers ---

    private fun buildRefreshClaims(subject: String) = TokenClaims(
        issuer = "sonature-auth",
        subject = subject,
        audience = null,
        expiresAt = now.plusSeconds(3600),
        issuedAt = now,
        tokenId = "jti-test",
        tokenType = TokenType.REFRESH
    )

    private fun buildEntity(subject: String, tenantId: UUID?) = RefreshTokenEntity(
        tokenHash = "fakehash",
        subject = subject,
        clientId = null,
        tenantId = tenantId,
        issuedAt = now.minusSeconds(60),
        expiresAt = now.plusSeconds(7 * 24 * 3600)
    )

    private fun buildTokenPair(subject: String): TokenPair {
        val accessToken = Token(
            value = "access-token-value",
            type = TokenType.ACCESS,
            algorithm = Algorithm.HS256,
            expiresAt = now.plusSeconds(900),
            issuedAt = now
        )
        val refreshToken = Token(
            value = "refresh-token-value",
            type = TokenType.REFRESH,
            algorithm = Algorithm.HS256,
            expiresAt = now.plusSeconds(7 * 24 * 3600),
            issuedAt = now
        )
        return TokenPair(accessToken = accessToken, refreshToken = refreshToken)
    }
}
