package com.sonature.auth.domain.tenant.model

enum class TenantRole {
    OWNER,
    ADMIN,
    MEMBER,
    VIEWER;

    companion object {
        private val rolePermissions: Map<TenantRole, Set<Permission>> = mapOf(
            OWNER to Permission.entries.toSet(),
            ADMIN to Permission.entries.toSet() - setOf(Permission.TENANT_DELETE),
            MEMBER to setOf(Permission.AUTH_READ, Permission.AUTH_WRITE),
            VIEWER to setOf(Permission.AUTH_READ)
        )
    }

    fun permissions(): Set<Permission> = rolePermissions[this] ?: emptySet()

    fun hasPermission(permission: Permission): Boolean = permission in permissions()
}
