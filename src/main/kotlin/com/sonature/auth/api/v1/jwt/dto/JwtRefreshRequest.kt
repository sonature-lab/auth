package com.sonature.auth.api.v1.jwt.dto

import jakarta.validation.constraints.NotBlank

data class JwtRefreshRequest(
    @field:NotBlank(message = "Refresh token is required")
    val refreshToken: String,

    val algorithm: String = "HS256"
)
