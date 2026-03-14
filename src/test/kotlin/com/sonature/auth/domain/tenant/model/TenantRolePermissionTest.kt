package com.sonature.auth.domain.tenant.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TenantRolePermissionTest {

    // --- OWNER permissions ---

    @Test
    fun `OWNER should have all permissions`() {
        val ownerPermissions = TenantRole.OWNER.permissions()
        assertEquals(Permission.entries.toSet(), ownerPermissions)
    }

    @Test
    fun `OWNER hasPermission should return true for every permission`() {
        Permission.entries.forEach { permission ->
            assertTrue(TenantRole.OWNER.hasPermission(permission)) {
                "OWNER should have permission $permission"
            }
        }
    }

    // --- ADMIN permissions ---

    @Test
    fun `ADMIN should have all permissions except TENANT_DELETE`() {
        val adminPermissions = TenantRole.ADMIN.permissions()
        val expected = Permission.entries.toSet() - setOf(Permission.TENANT_DELETE)
        assertEquals(expected, adminPermissions)
    }

    @Test
    fun `ADMIN hasPermission should return false for TENANT_DELETE`() {
        assertFalse(TenantRole.ADMIN.hasPermission(Permission.TENANT_DELETE))
    }

    @Test
    fun `ADMIN hasPermission should return true for TENANT_MANAGE`() {
        assertTrue(TenantRole.ADMIN.hasPermission(Permission.TENANT_MANAGE))
    }

    @Test
    fun `ADMIN hasPermission should return true for MEMBER_INVITE`() {
        assertTrue(TenantRole.ADMIN.hasPermission(Permission.MEMBER_INVITE))
    }

    @Test
    fun `ADMIN hasPermission should return true for MEMBER_REMOVE`() {
        assertTrue(TenantRole.ADMIN.hasPermission(Permission.MEMBER_REMOVE))
    }

    @Test
    fun `ADMIN hasPermission should return true for MEMBER_ROLE_CHANGE`() {
        assertTrue(TenantRole.ADMIN.hasPermission(Permission.MEMBER_ROLE_CHANGE))
    }

    @Test
    fun `ADMIN hasPermission should return true for API_KEY_MANAGE`() {
        assertTrue(TenantRole.ADMIN.hasPermission(Permission.API_KEY_MANAGE))
    }

    // --- MEMBER permissions ---

    @Test
    fun `MEMBER should have only AUTH_READ and AUTH_WRITE`() {
        val memberPermissions = TenantRole.MEMBER.permissions()
        assertEquals(setOf(Permission.AUTH_READ, Permission.AUTH_WRITE), memberPermissions)
    }

    @Test
    fun `MEMBER hasPermission should return true for AUTH_READ`() {
        assertTrue(TenantRole.MEMBER.hasPermission(Permission.AUTH_READ))
    }

    @Test
    fun `MEMBER hasPermission should return true for AUTH_WRITE`() {
        assertTrue(TenantRole.MEMBER.hasPermission(Permission.AUTH_WRITE))
    }

    @Test
    fun `MEMBER hasPermission should return false for TENANT_MANAGE`() {
        assertFalse(TenantRole.MEMBER.hasPermission(Permission.TENANT_MANAGE))
    }

    @Test
    fun `MEMBER hasPermission should return false for MEMBER_INVITE`() {
        assertFalse(TenantRole.MEMBER.hasPermission(Permission.MEMBER_INVITE))
    }

    // --- VIEWER permissions ---

    @Test
    fun `VIEWER should have only AUTH_READ`() {
        val viewerPermissions = TenantRole.VIEWER.permissions()
        assertEquals(setOf(Permission.AUTH_READ), viewerPermissions)
    }

    @Test
    fun `VIEWER hasPermission should return true for AUTH_READ`() {
        assertTrue(TenantRole.VIEWER.hasPermission(Permission.AUTH_READ))
    }

    @Test
    fun `VIEWER hasPermission should return false for AUTH_WRITE`() {
        assertFalse(TenantRole.VIEWER.hasPermission(Permission.AUTH_WRITE))
    }

    @Test
    fun `VIEWER hasPermission should return false for TENANT_MANAGE`() {
        assertFalse(TenantRole.VIEWER.hasPermission(Permission.TENANT_MANAGE))
    }

    @Test
    fun `VIEWER hasPermission should return false for MEMBER_INVITE`() {
        assertFalse(TenantRole.VIEWER.hasPermission(Permission.MEMBER_INVITE))
    }

    // --- Role hierarchy ---

    @Test
    fun `OWNER should have more permissions than ADMIN`() {
        assertTrue(TenantRole.OWNER.permissions().size > TenantRole.ADMIN.permissions().size)
    }

    @Test
    fun `ADMIN should have more permissions than MEMBER`() {
        assertTrue(TenantRole.ADMIN.permissions().size > TenantRole.MEMBER.permissions().size)
    }

    @Test
    fun `MEMBER should have more permissions than VIEWER`() {
        assertTrue(TenantRole.MEMBER.permissions().size > TenantRole.VIEWER.permissions().size)
    }

    @Test
    fun `ADMIN permissions should be a subset of OWNER permissions`() {
        assertTrue(TenantRole.OWNER.permissions().containsAll(TenantRole.ADMIN.permissions()))
    }

    @Test
    fun `MEMBER permissions should be a subset of ADMIN permissions`() {
        assertTrue(TenantRole.ADMIN.permissions().containsAll(TenantRole.MEMBER.permissions()))
    }

    @Test
    fun `VIEWER permissions should be a subset of MEMBER permissions`() {
        assertTrue(TenantRole.MEMBER.permissions().containsAll(TenantRole.VIEWER.permissions()))
    }
}
