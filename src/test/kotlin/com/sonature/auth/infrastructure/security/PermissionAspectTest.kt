package com.sonature.auth.infrastructure.security

import com.sonature.auth.domain.tenant.context.TenantContext
import com.sonature.auth.domain.tenant.context.TenantContextHolder
import com.sonature.auth.domain.tenant.exception.InsufficientPermissionException
import com.sonature.auth.domain.tenant.exception.TenantContextRequiredException
import com.sonature.auth.domain.tenant.model.Permission
import com.sonature.auth.domain.tenant.model.TenantRole
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.aspectj.lang.ProceedingJoinPoint
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class PermissionAspectTest {

    private val aspect = PermissionAspect()
    private val joinPoint = mockk<ProceedingJoinPoint>(relaxed = true)

    @AfterEach
    fun cleanup() {
        TenantContextHolder.clear()
    }

    @Test
    fun `throws TenantContextRequiredException when no context`() {
        val annotation = mockRequirePermission(Permission.MEMBER_INVITE)

        assertThrows(TenantContextRequiredException::class.java) {
            aspect.checkPermission(joinPoint, annotation)
        }
    }

    @Test
    fun `throws InsufficientPermissionException when role lacks permission`() {
        TenantContextHolder.set(
            TenantContext(
                tenantSlug = "my-org",
                userId = UUID.randomUUID(),
                role = TenantRole.VIEWER
            )
        )
        val annotation = mockRequirePermission(Permission.MEMBER_INVITE)

        assertThrows(InsufficientPermissionException::class.java) {
            aspect.checkPermission(joinPoint, annotation)
        }
    }

    @Test
    fun `proceeds when role has permission`() {
        TenantContextHolder.set(
            TenantContext(
                tenantSlug = "my-org",
                userId = UUID.randomUUID(),
                role = TenantRole.OWNER
            )
        )
        val annotation = mockRequirePermission(Permission.MEMBER_INVITE)
        every { joinPoint.proceed() } returns "result"

        val result = aspect.checkPermission(joinPoint, annotation)

        assertEquals("result", result)
        verify { joinPoint.proceed() }
    }

    @Test
    fun `ADMIN can manage members but not delete tenant`() {
        TenantContextHolder.set(
            TenantContext(
                tenantSlug = "my-org",
                userId = UUID.randomUUID(),
                role = TenantRole.ADMIN
            )
        )

        every { joinPoint.proceed() } returns "ok"

        val inviteAnnotation = mockRequirePermission(Permission.MEMBER_INVITE)
        assertDoesNotThrow { aspect.checkPermission(joinPoint, inviteAnnotation) }

        val deleteAnnotation = mockRequirePermission(Permission.TENANT_DELETE)
        assertThrows(InsufficientPermissionException::class.java) {
            aspect.checkPermission(joinPoint, deleteAnnotation)
        }
    }

    @Test
    fun `MEMBER can only read and write auth`() {
        TenantContextHolder.set(
            TenantContext(
                tenantSlug = "my-org",
                userId = UUID.randomUUID(),
                role = TenantRole.MEMBER
            )
        )

        every { joinPoint.proceed() } returns "ok"

        val readAnnotation = mockRequirePermission(Permission.AUTH_READ)
        assertDoesNotThrow { aspect.checkPermission(joinPoint, readAnnotation) }

        val inviteAnnotation = mockRequirePermission(Permission.MEMBER_INVITE)
        assertThrows(InsufficientPermissionException::class.java) {
            aspect.checkPermission(joinPoint, inviteAnnotation)
        }
    }

    @Test
    fun `VIEWER can only read`() {
        TenantContextHolder.set(
            TenantContext(
                tenantSlug = "my-org",
                userId = UUID.randomUUID(),
                role = TenantRole.VIEWER
            )
        )

        every { joinPoint.proceed() } returns "ok"

        val readAnnotation = mockRequirePermission(Permission.AUTH_READ)
        assertDoesNotThrow { aspect.checkPermission(joinPoint, readAnnotation) }

        val writeAnnotation = mockRequirePermission(Permission.AUTH_WRITE)
        assertThrows(InsufficientPermissionException::class.java) {
            aspect.checkPermission(joinPoint, writeAnnotation)
        }
    }

    private fun mockRequirePermission(permission: Permission): RequirePermission {
        val annotation = mockk<RequirePermission>()
        every { annotation.value } returns permission
        return annotation
    }
}
