package com.sonature.auth.api.v1.paseto.dto

data class PasetoIssueResponse(
    val token: String,
    val tokenType: String = "paseto",
    val expiresIn: Long,
    val issuedAt: Long
)
