package com.sonature.auth.domain.token.model

import java.time.Duration

data class TokenConfig(
    val algorithm: Algorithm,
    val issuer: String,
    val accessTokenExpiration: Duration = DEFAULT_ACCESS_EXPIRATION,
    val refreshTokenExpiration: Duration = DEFAULT_REFRESH_EXPIRATION
) {
    init {
        require(issuer.isNotBlank()) { "Issuer must not be blank" }
        require(!accessTokenExpiration.isNegative && !accessTokenExpiration.isZero) {
            "Access token expiration must be positive"
        }
        require(!refreshTokenExpiration.isNegative && !refreshTokenExpiration.isZero) {
            "Refresh token expiration must be positive"
        }
    }

    companion object {
        val DEFAULT_ACCESS_EXPIRATION: Duration = Duration.ofMinutes(15)
        val DEFAULT_REFRESH_EXPIRATION: Duration = Duration.ofDays(7)
        val MIN_ACCESS_EXPIRATION: Duration = Duration.ofMinutes(1)
        val MAX_ACCESS_EXPIRATION: Duration = Duration.ofHours(1)
        val MIN_REFRESH_EXPIRATION: Duration = Duration.ofHours(1)
        val MAX_REFRESH_EXPIRATION: Duration = Duration.ofDays(30)
    }
}
