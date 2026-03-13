package com.sonature.auth.api.v1.paseto.dto

import jakarta.validation.constraints.NotBlank

data class PasetoVerifyRequest(
    @field:NotBlank(message = "Token is required")
    val token: String
)
