package com.sonature.auth.api.v1.jwt.dto

import jakarta.validation.constraints.NotBlank

data class JwtVerifyRequest(
    @field:NotBlank(message = "Token is required")
    val token: String,

    val algorithm: String = "HS256"
)
