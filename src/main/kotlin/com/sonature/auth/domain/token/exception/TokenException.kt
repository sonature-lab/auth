package com.sonature.auth.domain.token.exception

import java.time.Instant

sealed class TokenException(
    override val message: String,
    val errorCode: String,
    override val cause: Throwable? = null
) : RuntimeException(message, cause)

class TokenExpiredException(
    message: String = "The token has expired",
    val expiredAt: Instant? = null
) : TokenException(message, "TOKEN_EXPIRED")

class TokenInvalidException(
    message: String = "The token signature is invalid"
) : TokenException(message, "TOKEN_INVALID")

class TokenMalformedException(
    message: String = "The token format is malformed"
) : TokenException(message, "TOKEN_MALFORMED")

class UnsupportedAlgorithmException(
    algorithm: String
) : TokenException("Unsupported algorithm: $algorithm", "INVALID_ALGORITHM")

class RefreshTokenRevokedException(
    message: String = "The refresh token has been revoked"
) : TokenException(message, "REFRESH_TOKEN_REVOKED")

class RefreshTokenReusedException(
    message: String = "Refresh token reuse detected - possible token theft"
) : TokenException(message, "REFRESH_TOKEN_REUSE_DETECTED")

class InvalidTokenTypeException(
    expectedType: String,
    actualType: String
) : TokenException(
    "Invalid token type: expected $expectedType but got $actualType",
    "INVALID_TOKEN_TYPE"
)
