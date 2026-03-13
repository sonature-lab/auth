package com.sonature.auth.api.v1.paseto.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive

data class PasetoIssueRequest(
    @field:NotBlank(message = "Subject is required")
    val subject: String,

    val audience: String? = null,

    @field:Positive(message = "Expiration must be positive")
    val expiresIn: Long? = null,

    val customClaims: Map<String, Any>? = null
)
