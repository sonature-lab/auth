package com.sonature.auth.application.service

import com.sonature.auth.application.port.output.TokenProvider
import com.sonature.auth.common.util.IdGenerator
import com.sonature.auth.common.util.TimeProvider
import com.sonature.auth.domain.token.exception.UnsupportedAlgorithmException
import com.sonature.auth.domain.token.model.Algorithm
import com.sonature.auth.domain.token.model.Token
import com.sonature.auth.domain.token.model.TokenClaims
import com.sonature.auth.domain.token.model.TokenConfig
import com.sonature.auth.domain.token.model.TokenType
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class PasetoService(
    tokenProviders: List<TokenProvider>,
    private val timeProvider: TimeProvider,
    private val idGenerator: IdGenerator
) {
    private val providerMap: Map<Algorithm, TokenProvider> =
        tokenProviders.associateBy { it.supportedAlgorithm() }

    fun issueToken(
        subject: String,
        audience: String? = null,
        issuer: String = DEFAULT_ISSUER,
        expiration: Duration = DEFAULT_EXPIRATION,
        customClaims: Map<String, Any> = emptyMap()
    ): Token {
        val provider = getProvider()
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
            algorithm = Algorithm.PASETO_V4_LOCAL,
            issuer = issuer,
            accessTokenExpiration = expiration
        )

        val tokenValue = provider.issue(claims, config)

        return Token(
            value = tokenValue,
            type = TokenType.ACCESS,
            algorithm = Algorithm.PASETO_V4_LOCAL,
            expiresAt = expiresAt,
            issuedAt = now
        )
    }

    fun verifyToken(token: String): TokenClaims {
        val provider = getProvider()
        return provider.verify(token)
    }

    private fun getProvider(): TokenProvider {
        return providerMap[Algorithm.PASETO_V4_LOCAL]
            ?: throw UnsupportedAlgorithmException(Algorithm.PASETO_V4_LOCAL.value)
    }

    companion object {
        const val DEFAULT_ISSUER = "sonature-auth"
        val DEFAULT_EXPIRATION: Duration = Duration.ofMinutes(15)
    }
}
