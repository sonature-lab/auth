package com.sonature.auth.api.v1.jwt.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive

data class JwtIssueRequest(
    @field:NotBlank(message = "Subject is required")
    val subject: String,

    val audience: String? = null,

    val algorithm: String = "HS256",

    @field:Positive(message = "Expiration must be positive")
    val expiresIn: Long? = null,

    val customClaims: Map<String, Any>? = null
)
