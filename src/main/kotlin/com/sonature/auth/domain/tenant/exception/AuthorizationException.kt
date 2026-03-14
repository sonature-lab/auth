package com.sonature.auth.domain.tenant.exception

sealed class AuthorizationException(
    override val message: String,
    val errorCode: String
) : RuntimeException(message)

class InsufficientPermissionException(
    permission: String,
    tenantSlug: String
) : AuthorizationException(
    message = "Insufficient permission '$permission' for tenant '$tenantSlug'",
    errorCode = "INSUFFICIENT_PERMISSION"
)

class TenantContextRequiredException : AuthorizationException(
    message = "Tenant context is required. Provide X-Tenant-Slug header with a valid access token.",
    errorCode = "TENANT_CONTEXT_REQUIRED"
)

class InvalidAccessTokenException(
    reason: String = "Invalid or missing access token"
) : AuthorizationException(
    message = reason,
    errorCode = "INVALID_ACCESS_TOKEN"
)
