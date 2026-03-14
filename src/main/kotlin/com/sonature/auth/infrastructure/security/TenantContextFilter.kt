package com.sonature.auth.infrastructure.security

import com.sonature.auth.application.service.JwtService
import com.sonature.auth.domain.tenant.context.TenantContext
import com.sonature.auth.domain.tenant.context.TenantContextHolder
import com.sonature.auth.domain.tenant.model.TenantRole
import com.sonature.auth.domain.tenant.repository.TenantRepository
import com.sonature.auth.domain.token.model.Algorithm
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

@Component
class TenantContextFilter(
    private val jwtService: JwtService,
    private val tenantRepository: TenantRepository
) : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            val tenantSlug = request.getHeader(TENANT_HEADER)
            val authHeader = request.getHeader("Authorization")

            if (tenantSlug != null && authHeader != null && authHeader.startsWith("Bearer ")) {
                val token = authHeader.substring(7)
                try {
                    val claims = jwtService.verifyToken(token, Algorithm.HS256)
                    val userId = UUID.fromString(claims.subject)

                    val tenantRoles = extractTenantRoles(claims.customClaims)
                    val role = tenantRoles[tenantSlug]

                    if (role != null) {
                        val tenantId = tenantRepository.findBySlug(tenantSlug)?.id
                        TenantContextHolder.set(
                            TenantContext(
                                tenantSlug = tenantSlug,
                                tenantId = tenantId,
                                userId = userId,
                                role = role
                            )
                        )
                    }
                } catch (e: Exception) {
                    log.debug("Failed to extract tenant context from token: ${e.message}")
                }
            }

            filterChain.doFilter(request, response)
        } finally {
            TenantContextHolder.clear()
        }
    }

    internal fun extractTenantRoles(customClaims: Map<String, Any>): Map<String, TenantRole> {
        @Suppress("UNCHECKED_CAST")
        val tenants = customClaims["tenants"] as? List<Map<String, Any>> ?: return emptyMap()

        return tenants.mapNotNull { tenant ->
            val slug = tenant["slug"] as? String ?: return@mapNotNull null
            val roleName = tenant["role"] as? String ?: return@mapNotNull null
            try {
                slug to TenantRole.valueOf(roleName)
            } catch (e: IllegalArgumentException) {
                null
            }
        }.toMap()
    }

    companion object {
        const val TENANT_HEADER = "X-Tenant-Slug"
    }
}
