package com.sonature.auth.domain.tenant.context

import com.sonature.auth.domain.tenant.model.TenantRole
import java.util.UUID

data class TenantContext(
    val tenantSlug: String,
    val tenantId: UUID? = null,
    val userId: UUID,
    val role: TenantRole
)

object TenantContextHolder {

    private val context = ThreadLocal<TenantContext?>()

    fun set(tenantContext: TenantContext) {
        context.set(tenantContext)
    }

    fun get(): TenantContext? = context.get()

    fun require(): TenantContext = context.get()
        ?: throw IllegalStateException("Tenant context not set. Ensure X-Tenant-Slug header is provided.")

    fun clear() {
        context.remove()
    }
}
