package com.sonature.auth.infrastructure.security

import com.sonature.auth.domain.tenant.context.TenantContextHolder
import com.sonature.auth.domain.tenant.exception.InsufficientPermissionException
import com.sonature.auth.domain.tenant.exception.TenantContextRequiredException
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.stereotype.Component

@Aspect
@Component
class PermissionAspect {

    @Around("@annotation(requirePermission)")
    fun checkPermission(joinPoint: ProceedingJoinPoint, requirePermission: RequirePermission): Any? {
        val tenantContext = TenantContextHolder.get()
            ?: throw TenantContextRequiredException()

        val permission = requirePermission.value

        if (!tenantContext.role.hasPermission(permission)) {
            throw InsufficientPermissionException(
                permission = permission.name,
                tenantSlug = tenantContext.tenantSlug
            )
        }

        return joinPoint.proceed()
    }
}
