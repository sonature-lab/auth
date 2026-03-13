package com.sonature.auth.api.v1.jwt.dto

data class JwtIssueResponse(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long,
    val issuedAt: Long
)
