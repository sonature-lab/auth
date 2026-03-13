package com.sonature.auth.domain.user.exception

sealed class AuthException(
    override val message: String,
    val errorCode: String,
    override val cause: Throwable? = null
) : RuntimeException(message, cause)

class EmailAlreadyExistsException(
    email: String
) : AuthException("Email already registered: $email", "EMAIL_ALREADY_EXISTS")

class InvalidCredentialsException(
    message: String = "Invalid email or password"
) : AuthException(message, "INVALID_CREDENTIALS")

class UserNotFoundException(
    message: String = "User not found"
) : AuthException(message, "USER_NOT_FOUND")

class UserSuspendedException(
    message: String = "User account is suspended"
) : AuthException(message, "USER_SUSPENDED")
