package com.sonature.auth.api.common

import com.sonature.auth.domain.token.exception.InvalidTokenTypeException
import com.sonature.auth.domain.token.exception.RefreshTokenRevokedException
import com.sonature.auth.domain.token.exception.RefreshTokenReusedException
import com.sonature.auth.domain.token.exception.TokenException
import com.sonature.auth.domain.token.exception.TokenExpiredException
import com.sonature.auth.domain.token.exception.TokenInvalidException
import com.sonature.auth.domain.token.exception.TokenMalformedException
import com.sonature.auth.domain.token.exception.UnsupportedAlgorithmException
import com.sonature.auth.domain.tenant.exception.AlreadyTenantMemberException
import com.sonature.auth.domain.tenant.exception.InsufficientPermissionException
import com.sonature.auth.domain.tenant.exception.InvalidAccessTokenException
import com.sonature.auth.domain.tenant.exception.NotTenantMemberException
import com.sonature.auth.domain.tenant.exception.TenantContextRequiredException
import com.sonature.auth.domain.tenant.exception.TenantMismatchException
import com.sonature.auth.domain.tenant.exception.TenantNotFoundException
import com.sonature.auth.domain.tenant.exception.TenantSlugAlreadyExistsException
import com.sonature.auth.domain.user.exception.AuthException
import com.sonature.auth.domain.user.exception.EmailAlreadyExistsException
import com.sonature.auth.domain.user.exception.InvalidCredentialsException
import com.sonature.auth.domain.user.exception.UserNotFoundException
import com.sonature.auth.domain.user.exception.UserSuspendedException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(TokenExpiredException::class)
    fun handleTokenExpired(ex: TokenExpiredException): ResponseEntity<ApiResponse<Nothing>> {
        logger.warn("Token expired: ${ex.message}")
        val error = ApiError(
            code = ex.errorCode,
            message = ex.message,
            details = ex.expiredAt?.let { mapOf("expiredAt" to it.toString()) }
        )
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error(error))
    }

    @ExceptionHandler(TokenInvalidException::class)
    fun handleTokenInvalid(ex: TokenInvalidException): ResponseEntity<ApiResponse<Nothing>> {
        logger.warn("Invalid token: ${ex.message}")
        val error = ApiError(code = ex.errorCode, message = ex.message)
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error(error))
    }

    @ExceptionHandler(TokenMalformedException::class)
    fun handleTokenMalformed(ex: TokenMalformedException): ResponseEntity<ApiResponse<Nothing>> {
        logger.warn("Malformed token: ${ex.message}")
        val error = ApiError(code = ex.errorCode, message = ex.message)
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error(error))
    }

    @ExceptionHandler(UnsupportedAlgorithmException::class)
    fun handleUnsupportedAlgorithm(ex: UnsupportedAlgorithmException): ResponseEntity<ApiResponse<Nothing>> {
        val error = ApiError(code = ex.errorCode, message = ex.message)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(error))
    }

    @ExceptionHandler(RefreshTokenRevokedException::class)
    fun handleRefreshTokenRevoked(ex: RefreshTokenRevokedException): ResponseEntity<ApiResponse<Nothing>> {
        logger.warn("Revoked refresh token used: ${ex.message}")
        val error = ApiError(code = ex.errorCode, message = ex.message)
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error(error))
    }

    @ExceptionHandler(RefreshTokenReusedException::class)
    fun handleRefreshTokenReuse(ex: RefreshTokenReusedException): ResponseEntity<ApiResponse<Nothing>> {
        logger.error("SECURITY ALERT - Refresh token reuse detected: ${ex.message}")
        val error = ApiError(code = ex.errorCode, message = ex.message)
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error(error))
    }

    @ExceptionHandler(InvalidTokenTypeException::class)
    fun handleInvalidTokenType(ex: InvalidTokenTypeException): ResponseEntity<ApiResponse<Nothing>> {
        logger.warn("Invalid token type: ${ex.message}")
        val error = ApiError(code = ex.errorCode, message = ex.message)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(error))
    }

    @ExceptionHandler(EmailAlreadyExistsException::class)
    fun handleEmailExists(ex: EmailAlreadyExistsException): ResponseEntity<ApiResponse<Nothing>> {
        val error = ApiError(code = ex.errorCode, message = ex.message)
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ApiResponse.error(error))
    }

    @ExceptionHandler(InvalidCredentialsException::class)
    fun handleInvalidCredentials(ex: InvalidCredentialsException): ResponseEntity<ApiResponse<Nothing>> {
        val error = ApiError(code = ex.errorCode, message = ex.message)
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error(error))
    }

    @ExceptionHandler(UserNotFoundException::class)
    fun handleUserNotFound(ex: UserNotFoundException): ResponseEntity<ApiResponse<Nothing>> {
        val error = ApiError(code = ex.errorCode, message = ex.message)
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(error))
    }

    @ExceptionHandler(UserSuspendedException::class)
    fun handleUserSuspended(ex: UserSuspendedException): ResponseEntity<ApiResponse<Nothing>> {
        val error = ApiError(code = ex.errorCode, message = ex.message)
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.error(error))
    }

    @ExceptionHandler(InsufficientPermissionException::class)
    fun handleInsufficientPermission(ex: InsufficientPermissionException): ResponseEntity<ApiResponse<Nothing>> {
        val error = ApiError(code = ex.errorCode, message = ex.message)
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.error(error))
    }

    @ExceptionHandler(TenantContextRequiredException::class)
    fun handleTenantContextRequired(ex: TenantContextRequiredException): ResponseEntity<ApiResponse<Nothing>> {
        val error = ApiError(code = ex.errorCode, message = ex.message)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(error))
    }

    @ExceptionHandler(InvalidAccessTokenException::class)
    fun handleInvalidAccessToken(ex: InvalidAccessTokenException): ResponseEntity<ApiResponse<Nothing>> {
        val error = ApiError(code = ex.errorCode, message = ex.message)
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error(error))
    }

    @ExceptionHandler(TenantNotFoundException::class)
    fun handleTenantNotFound(ex: TenantNotFoundException): ResponseEntity<ApiResponse<Nothing>> {
        val error = ApiError(code = ex.errorCode, message = ex.message)
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(error))
    }

    @ExceptionHandler(TenantSlugAlreadyExistsException::class)
    fun handleTenantSlugExists(ex: TenantSlugAlreadyExistsException): ResponseEntity<ApiResponse<Nothing>> {
        val error = ApiError(code = ex.errorCode, message = ex.message)
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ApiResponse.error(error))
    }

    @ExceptionHandler(AlreadyTenantMemberException::class)
    fun handleAlreadyTenantMember(ex: AlreadyTenantMemberException): ResponseEntity<ApiResponse<Nothing>> {
        val error = ApiError(code = ex.errorCode, message = ex.message)
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ApiResponse.error(error))
    }

    @ExceptionHandler(NotTenantMemberException::class)
    fun handleNotTenantMember(ex: NotTenantMemberException): ResponseEntity<ApiResponse<Nothing>> {
        val error = ApiError(code = ex.errorCode, message = ex.message)
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(error))
    }

    @ExceptionHandler(TenantMismatchException::class)
    fun handleTenantMismatch(ex: TenantMismatchException): ResponseEntity<ApiResponse<Nothing>> {
        logger.warn("Tenant mismatch: ${ex.message}")
        val error = ApiError(code = ex.errorCode, message = ex.message)
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.error(error))
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException): ResponseEntity<ApiResponse<Nothing>> {
        val error = ApiError(code = "INVALID_REQUEST", message = ex.message ?: "Invalid request")
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(error))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ApiResponse<Nothing>> {
        val details = ex.bindingResult.fieldErrors.associate {
            it.field to (it.defaultMessage ?: "Invalid value")
        }
        val error = ApiError(
            code = "MISSING_PARAMETER",
            message = "Validation failed",
            details = details
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(error))
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneric(ex: Exception): ResponseEntity<ApiResponse<Nothing>> {
        logger.error("Unexpected error", ex)
        val error = ApiError(code = "INTERNAL_ERROR", message = "Internal server error")
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error(error))
    }
}
