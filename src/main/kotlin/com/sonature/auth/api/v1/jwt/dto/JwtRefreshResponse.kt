package com.sonature.auth.api.v1.jwt.dto

data class JwtRefreshResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val accessExpiresIn: Long,
    val refreshExpiresIn: Long,
    val issuedAt: Long
)
