package com.sonature.auth.domain.token.model

data class TokenPair(
    val accessToken: Token,
    val refreshToken: Token
)
