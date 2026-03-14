package com.sonature.auth.domain.tenant.model

enum class Permission {
    TENANT_MANAGE,
    TENANT_DELETE,
    MEMBER_INVITE,
    MEMBER_REMOVE,
    MEMBER_ROLE_CHANGE,
    API_KEY_MANAGE,
    AUTH_READ,
    AUTH_WRITE
}
