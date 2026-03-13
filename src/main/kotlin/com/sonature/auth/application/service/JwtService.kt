package com.sonature.auth.application.service

import com.sonature.auth.application.port.output.TokenProvider
import com.sonature.auth.common.util.IdGenerator
import com.sonature.auth.common.util.TimeProvider
import com.sonature.auth.domain.token.exception.UnsupportedAlgorithmException
import com.sonature.auth.domain.token.model.Algorithm
import com.sonature.auth.domain.token.exception.InvalidTokenTypeException
import com.sonature.auth.domain.token.model.Token
import com.sonature.auth.domain.token.model.TokenClaims
import com.sonature.auth.domain.token.model.TokenConfig
import com.sonature.auth.domain.token.model.TokenPair
import com.sonature.auth.domain.token.model.TokenType
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class JwtService(
    tokenProviders: List<TokenProvider>,
    private val timeProvider: TimeProvider,
    private val idGenerator: IdGenerator
) {
    private val providerMap: Map<Algorithm, TokenProvider> =
        tokenProviders.associateBy { it.supportedAlgorithm() }

    fun issueAccessToken(
        subject: String,
        audience: String? = null,
        algorithm: Algorithm = Algorithm.HS256,
        issuer: String = DEFAULT_ISSUER,
        expiration: Duration = TokenConfig.DEFAULT_ACCESS_EXPIRATION,
        customClaims: Map<String, Any> = emptyMap()
    ): Token {
        val provider = getProvider(algorithm)
        val now = timeProvider.now()
        val expiresAt = now.plus(expiration)

        val claims = TokenClaims(
            issuer = issuer,
            subject = subject,
            audience = audience,
            expiresAt = expiresAt,
            issuedAt = now,
            tokenId = idGenerator.generateId(),
            tokenType = TokenType.ACCESS,
            customClaims = customClaims
        )

        val config = TokenConfig(
            algorithm = algorithm,
            issuer = issuer,
            accessTokenExpiration = expiration
        )

        val tokenValue = provider.issue(claims, config)

        return Token(
            value = tokenValue,
            type = TokenType.ACCESS,
            algorithm = algorithm,
            expiresAt = expiresAt,
            issuedAt = now
        )
    }

    fun verifyToken(token: String, algorithm: Algorithm = Algorithm.HS256): TokenClaims {
        val provider = getProvider(algorithm)
        return provider.verify(token)
    }

    fun verifyTokenWithType(
        token: String,
        expectedType: TokenType,
        algorithm: Algorithm = Algorithm.HS256
    ): TokenClaims {
        val claims = verifyToken(token, algorithm)
        if (claims.tokenType != expectedType) {
            throw InvalidTokenTypeException(
                expectedType = expectedType.name,
                actualType = claims.tokenType.name
            )
        }
        return claims
    }

    fun issueTokenPair(
        subject: String,
        audience: String? = null,
        algorithm: Algorithm = Algorithm.HS256,
        issuer: String = DEFAULT_ISSUER,
        accessExpiration: Duration = TokenConfig.DEFAULT_ACCESS_EXPIRATION,
        refreshExpiration: Duration = TokenConfig.DEFAULT_REFRESH_EXPIRATION,
        clientId: String? = null,
        customClaims: Map<String, Any> = emptyMap()
    ): TokenPair {
        val accessToken = issueAccessToken(
            subject = subject,
            audience = audience,
            algorithm = algorithm,
            issuer = issuer,
            expiration = accessExpiration,
            customClaims = customClaims
        )

        val refreshToken = issueRefreshToken(
            subject = subject,
            audience = audience,
            algorithm = algorithm,
            issuer = issuer,
            expiration = refreshExpiration
        )

        return TokenPair(
            accessToken = accessToken,
            refreshToken = refreshToken
        )
    }

    fun issueRefreshToken(
        subject: String,
        audience: String? = null,
        algorithm: Algorithm = Algorithm.HS256,
        issuer: String = DEFAULT_ISSUER,
        expiration: Duration = TokenConfig.DEFAULT_REFRESH_EXPIRATION
    ): Token {
        val provider = getProvider(algorithm)
        val now = timeProvider.now()
        val expiresAt = now.plus(expiration)

        val claims = TokenClaims(
            issuer = issuer,
            subject = subject,
            audience = audience,
            expiresAt = expiresAt,
            issuedAt = now,
            tokenId = idGenerator.generateId(),
            tokenType = TokenType.REFRESH,
            customClaims = emptyMap()
        )

        val config = TokenConfig(
            algorithm = algorithm,
            issuer = issuer,
            refreshTokenExpiration = expiration
        )

        val tokenValue = provider.issue(claims, config)

        return Token(
            value = tokenValue,
            type = TokenType.REFRESH,
            algorithm = algorithm,
            expiresAt = expiresAt,
            issuedAt = now
        )
    }

    private fun getProvider(algorithm: Algorithm): TokenProvider {
        return providerMap[algorithm]
            ?: throw UnsupportedAlgorithmException(algorithm.value)
    }

    companion object {
        const val DEFAULT_ISSUER = "sonature-auth"
    }
}
