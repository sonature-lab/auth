package com.sonature.auth.domain.tenant.exception

sealed class TenantException(
    override val message: String,
    val errorCode: String,
    override val cause: Throwable? = null
) : RuntimeException(message, cause)

class TenantNotFoundException(
    slug: String
) : TenantException("Tenant not found: $slug", "TENANT_NOT_FOUND")

class TenantSlugAlreadyExistsException(
    slug: String
) : TenantException("Tenant slug already exists: $slug", "TENANT_SLUG_ALREADY_EXISTS")

class AlreadyTenantMemberException(
    tenantSlug: String,
    userId: String
) : TenantException("User $userId is already a member of tenant $tenantSlug", "ALREADY_TENANT_MEMBER")

class NotTenantMemberException(
    tenantSlug: String,
    userId: String
) : TenantException("User $userId is not a member of tenant $tenantSlug", "NOT_TENANT_MEMBER")
