package com.sonature.auth.infrastructure.security

import com.sonature.auth.application.service.JwtService
import com.sonature.auth.domain.token.model.Algorithm
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Validates Bearer JWT tokens (HS256) from Authorization header and sets
 * Spring Security authentication context. This allows Spring Security's
 * authorizeHttpRequests rules (e.g., authenticated()) to apply to API endpoints
 * that use our custom JWT tokens.
 *
 * Note: Virtual Threads — if spring.threads.virtual.enabled is set to true,
 * this ThreadLocal-backed filter must be migrated to use ScopedValue.
 * See TenantContextHolder for the same concern.
 */
@Component
class JwtBearerAuthenticationFilter(
    private val jwtService: JwtService
) : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authHeader = request.getHeader("Authorization")

        if (authHeader != null && authHeader.startsWith("Bearer ") &&
            SecurityContextHolder.getContext().authentication == null
        ) {
            val token = authHeader.substring(7)
            try {
                val claims = jwtService.verifyToken(token, Algorithm.HS256)
                val authentication = UsernamePasswordAuthenticationToken(
                    claims.subject,
                    null,
                    listOf(SimpleGrantedAuthority("ROLE_USER"))
                )
                SecurityContextHolder.getContext().authentication = authentication
            } catch (e: Exception) {
                log.debug("Bearer token validation failed: ${e.message}")
                // Do not set authentication — Spring Security will enforce authenticated() rules
            }
        }

        filterChain.doFilter(request, response)
    }
}
