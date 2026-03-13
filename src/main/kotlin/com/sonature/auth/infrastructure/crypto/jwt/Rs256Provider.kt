package com.sonature.auth.infrastructure.crypto.jwt

import com.sonature.auth.application.port.output.KeyManager
import com.sonature.auth.application.port.output.TokenProvider
import com.sonature.auth.domain.token.exception.TokenExpiredException
import com.sonature.auth.domain.token.exception.TokenInvalidException
import com.sonature.auth.domain.token.exception.TokenMalformedException
import com.sonature.auth.domain.token.model.Algorithm
import com.sonature.auth.domain.token.model.TokenClaims
import com.sonature.auth.domain.token.model.TokenConfig
import com.sonature.auth.domain.token.model.TokenType
import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.security.SignatureException
import org.springframework.stereotype.Component
import java.security.PrivateKey
import java.security.PublicKey
import java.util.Date

@Component
class Rs256Provider(
    private val keyManager: KeyManager
) : TokenProvider {

    override fun supportedAlgorithm(): Algorithm = Algorithm.RS256

    override fun issue(claims: TokenClaims, config: TokenConfig): String {
        val signingKey = keyManager.getSigningKey(Algorithm.RS256) as PrivateKey

        val builder = Jwts.builder()
            .issuer(claims.issuer)
            .subject(claims.subject)
            .id(claims.tokenId)
            .issuedAt(Date.from(claims.issuedAt))
            .expiration(Date.from(claims.expiresAt))
            .claim("typ", claims.tokenType.name)

        claims.audience?.let { aud ->
            builder.audience().add(aud)
        }

        claims.customClaims.forEach { (key, value) ->
            builder.claim(key, value)
        }

        return builder
            .signWith(signingKey, Jwts.SIG.RS256)
            .compact()
    }

    override fun verify(token: String): TokenClaims {
        val verificationKey = keyManager.getVerificationKey(Algorithm.RS256) as PublicKey

        try {
            val jws = Jwts.parser()
                .verifyWith(verificationKey)
                .build()
                .parseSignedClaims(token)

            return mapToTokenClaims(jws.payload)
        } catch (e: ExpiredJwtException) {
            throw TokenExpiredException(
                message = "The token has expired",
                expiredAt = e.claims.expiration?.toInstant()
            )
        } catch (e: SignatureException) {
            throw TokenInvalidException("The token signature is invalid")
        } catch (e: MalformedJwtException) {
            throw TokenMalformedException("The token format is malformed")
        } catch (e: Exception) {
            throw TokenMalformedException("Invalid token: ${e.message}")
        }
    }

    private fun mapToTokenClaims(payload: Claims): TokenClaims {
        val audience = payload.audience?.firstOrNull()

        return TokenClaims(
            issuer = payload.issuer ?: throw TokenMalformedException("Missing issuer"),
            subject = payload.subject ?: throw TokenMalformedException("Missing subject"),
            audience = audience,
            expiresAt = payload.expiration?.toInstant()
                ?: throw TokenMalformedException("Missing expiration"),
            issuedAt = payload.issuedAt?.toInstant()
                ?: throw TokenMalformedException("Missing issuedAt"),
            tokenId = payload.id ?: throw TokenMalformedException("Missing jti"),
            tokenType = TokenType.valueOf(
                payload["typ"] as? String ?: TokenType.ACCESS.name
            ),
            customClaims = extractCustomClaims(payload)
        )
    }

    private fun extractCustomClaims(payload: Claims): Map<String, Any> {
        val standardClaims = setOf("iss", "sub", "aud", "exp", "iat", "jti", "typ")
        return payload.filterKeys { it !in standardClaims }
    }
}
