package com.sonature.auth.domain.token.model

import java.time.Instant

data class Token(
    val value: String,
    val type: TokenType,
    val algorithm: Algorithm,
    val expiresAt: Instant,
    val issuedAt: Instant
) {
    init {
        require(value.isNotBlank()) { "Token value must not be blank" }
    }
}
