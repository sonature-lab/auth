package com.sonature.auth.api.v1.jwt.dto

data class JwtVerifyResponse(
    val valid: Boolean,
    val subject: String,
    val issuer: String,
    val audience: String?,
    val expiresAt: Long,
    val issuedAt: Long,
    val tokenId: String,
    val tokenType: String,
    val customClaims: Map<String, Any>
)
