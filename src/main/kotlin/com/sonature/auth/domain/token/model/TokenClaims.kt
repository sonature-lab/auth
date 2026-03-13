package com.sonature.auth.domain.token.model

import java.time.Instant

data class TokenClaims(
    val issuer: String,
    val subject: String,
    val audience: String?,
    val expiresAt: Instant,
    val issuedAt: Instant,
    val tokenId: String,
    val tokenType: TokenType,
    val customClaims: Map<String, Any> = emptyMap()
) {
    init {
        require(issuer.isNotBlank()) { "Issuer must not be blank" }
        require(subject.isNotBlank()) { "Subject must not be blank" }
        require(tokenId.isNotBlank()) { "Token ID must not be blank" }
        require(expiresAt.isAfter(issuedAt)) { "ExpiresAt must be after IssuedAt" }
    }

    fun isExpired(now: Instant = Instant.now()): Boolean = now.isAfter(expiresAt)
}
