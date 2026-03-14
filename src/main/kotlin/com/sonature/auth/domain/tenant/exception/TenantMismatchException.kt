package com.sonature.auth.domain.tenant.exception

import java.util.UUID

class TenantMismatchException(
    tokenTenantId: UUID,
    contextTenantSlug: String
) : TenantException(
    message = "Token belongs to tenant $tokenTenantId but current context is for tenant '$contextTenantSlug'",
    errorCode = "TENANT_MISMATCH"
)
