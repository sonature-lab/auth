package com.sonature.auth.application.usecase

import com.sonature.auth.application.service.JwtService
import com.sonature.auth.application.service.RefreshTokenService
import com.sonature.auth.domain.token.model.Algorithm
import com.sonature.auth.domain.token.model.TokenConfig
import com.sonature.auth.domain.token.model.TokenPair
import com.sonature.auth.domain.token.model.TokenType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.util.UUID

@Service
class TokenRefreshUseCase(
    private val jwtService: JwtService,
    private val refreshTokenService: RefreshTokenService
) {
    @Transactional
    fun issueTokenPair(
        subject: String,
        audience: String? = null,
        algorithm: Algorithm = Algorithm.HS256,
        clientId: String? = null,
        tenantId: UUID? = null,
        accessExpiration: Duration = TokenConfig.DEFAULT_ACCESS_EXPIRATION,
        refreshExpiration: Duration = TokenConfig.DEFAULT_REFRESH_EXPIRATION,
        customClaims: Map<String, Any> = emptyMap()
    ): TokenPair {
        val tokenPair = jwtService.issueTokenPair(
            subject = subject,
            audience = audience,
            algorithm = algorithm,
            accessExpiration = accessExpiration,
            refreshExpiration = refreshExpiration,
            clientId = clientId,
            customClaims = customClaims
        )

        refreshTokenService.storeRefreshToken(
            tokenValue = tokenPair.refreshToken.value,
            subject = subject,
            clientId = clientId,
            issuedAt = tokenPair.refreshToken.issuedAt,
            expiresAt = tokenPair.refreshToken.expiresAt,
            tenantId = tenantId
        )

        return tokenPair
    }

    @Transactional
    fun refreshTokens(
        refreshToken: String,
        algorithm: Algorithm = Algorithm.HS256,
        accessExpiration: Duration = TokenConfig.DEFAULT_ACCESS_EXPIRATION,
        refreshExpiration: Duration = TokenConfig.DEFAULT_REFRESH_EXPIRATION
    ): TokenPair {
        val claims = jwtService.verifyTokenWithType(
            token = refreshToken,
            expectedType = TokenType.REFRESH,
            algorithm = algorithm
        )

        val oldTokenEntity = refreshTokenService.validateAndConsume(refreshToken)

        val newTokenPair = jwtService.issueTokenPair(
            subject = claims.subject,
            audience = claims.audience,
            algorithm = algorithm,
            accessExpiration = accessExpiration,
            refreshExpiration = refreshExpiration,
            clientId = oldTokenEntity.clientId
        )

        refreshTokenService.rotateToken(
            oldEntity = oldTokenEntity,
            newTokenValue = newTokenPair.refreshToken.value,
            newIssuedAt = newTokenPair.refreshToken.issuedAt,
            newExpiresAt = newTokenPair.refreshToken.expiresAt
        )

        return newTokenPair
    }

    @Transactional
    fun revokeAllSessions(subject: String): Int {
        return refreshTokenService.revokeAllTokensForSubject(subject)
    }
}
