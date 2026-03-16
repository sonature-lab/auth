package com.sonature.auth.infrastructure.crypto.paseto

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.sonature.auth.application.port.output.KeyManager
import com.sonature.auth.application.port.output.TokenProvider
import com.sonature.auth.common.util.TimeProvider
import com.sonature.auth.domain.token.exception.TokenExpiredException
import com.sonature.auth.domain.token.exception.TokenMalformedException
import com.sonature.auth.domain.token.model.Algorithm
import com.sonature.auth.domain.token.model.TokenClaims
import com.sonature.auth.domain.token.model.TokenConfig
import com.sonature.auth.domain.token.model.TokenType
import org.paseto4j.commons.SecretKey
import org.paseto4j.commons.Version
import org.paseto4j.version4.Paseto
import org.springframework.stereotype.Component
import java.time.Instant
import javax.crypto.spec.SecretKeySpec

@Component
class PasetoV4LocalProvider(
    private val keyManager: KeyManager,
    private val objectMapper: ObjectMapper,
    private val timeProvider: TimeProvider
) : TokenProvider {

    override fun supportedAlgorithm(): Algorithm = Algorithm.PASETO_V4_LOCAL

    override fun issue(claims: TokenClaims, config: TokenConfig): String {
        val key = keyManager.getSigningKey(Algorithm.PASETO_V4_LOCAL) as SecretKeySpec
        val secretKey = SecretKey(key.encoded, Version.V4)

        val payload = buildPayload(claims)
        val payloadJson = objectMapper.writeValueAsString(payload)

        return Paseto.encrypt(secretKey, payloadJson, "")
    }

    override fun verify(token: String): TokenClaims {
        val key = keyManager.getVerificationKey(Algorithm.PASETO_V4_LOCAL) as SecretKeySpec
        val secretKey = SecretKey(key.encoded, Version.V4)

        try {
            val decryptedPayload = Paseto.decrypt(secretKey, token, "")
            val payload: Map<String, Any> = objectMapper.readValue(decryptedPayload)

            return mapToTokenClaims(payload)
        } catch (e: TokenExpiredException) {
            throw e
        } catch (e: TokenMalformedException) {
            throw e
        } catch (e: Exception) {
            throw TokenMalformedException("Invalid PASETO token: ${e.message}")
        }
    }

    private fun buildPayload(claims: TokenClaims): Map<String, Any> {
        val payload = mutableMapOf<String, Any>(
            "iss" to claims.issuer,
            "sub" to claims.subject,
            "exp" to claims.expiresAt.toString(),
            "iat" to claims.issuedAt.toString(),
            "jti" to claims.tokenId,
            "typ" to claims.tokenType.name
        )

        claims.audience?.let { payload["aud"] = it }
        payload.putAll(claims.customClaims)

        return payload
    }

    private fun mapToTokenClaims(payload: Map<String, Any>): TokenClaims {
        val exp = Instant.parse(payload["exp"] as String)

        if (timeProvider.now().isAfter(exp)) {
            throw TokenExpiredException(
                message = "The token has expired",
                expiredAt = exp
            )
        }

        val standardClaims = setOf("iss", "sub", "aud", "exp", "iat", "jti", "typ")
        val customClaims = payload.filterKeys { it !in standardClaims }

        return TokenClaims(
            issuer = payload["iss"] as? String
                ?: throw TokenMalformedException("Missing issuer"),
            subject = payload["sub"] as? String
                ?: throw TokenMalformedException("Missing subject"),
            audience = payload["aud"] as? String,
            expiresAt = exp,
            issuedAt = Instant.parse(payload["iat"] as String),
            tokenId = payload["jti"] as? String
                ?: throw TokenMalformedException("Missing jti"),
            tokenType = TokenType.valueOf(
                payload["typ"] as? String ?: TokenType.ACCESS.name
            ),
            customClaims = customClaims
        )
    }
}
