package com.sonature.auth.api.v1.auth.dto

data class AuthResponse(
    val user: UserInfo,
    val accessToken: String,
    val refreshToken: String,
    val accessExpiresIn: Long,
    val refreshExpiresIn: Long
)

data class UserInfo(
    val id: String,
    val email: String,
    val name: String?,
    val provider: String
)
