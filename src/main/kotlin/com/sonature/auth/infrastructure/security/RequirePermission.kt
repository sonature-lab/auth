package com.sonature.auth.infrastructure.security

import com.sonature.auth.domain.tenant.model.Permission

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RequirePermission(val value: Permission)
