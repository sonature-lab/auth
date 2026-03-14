package com.sonature.auth.api.v1.tenant

import com.sonature.auth.api.common.ApiResponse
import com.sonature.auth.api.v1.tenant.dto.AddMemberRequest
import com.sonature.auth.api.v1.tenant.dto.ChangeMemberRoleRequest
import com.sonature.auth.api.v1.tenant.dto.CreateTenantRequest
import com.sonature.auth.api.v1.tenant.dto.TenantMemberResponse
import com.sonature.auth.api.v1.tenant.dto.TenantResponse
import com.sonature.auth.application.service.TenantService
import com.sonature.auth.domain.tenant.context.TenantContextHolder
import com.sonature.auth.domain.tenant.entity.TenantEntity
import com.sonature.auth.domain.tenant.entity.TenantMembershipEntity
import com.sonature.auth.domain.tenant.model.Permission
import com.sonature.auth.infrastructure.security.RequirePermission
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Tag(name = "Tenant", description = "Tenant Management API")
@RestController
@RequestMapping("/api/v1/tenants")
class TenantController(
    private val tenantService: TenantService
) {

    @Operation(summary = "Create tenant", description = "Create a new tenant")
    @PostMapping
    fun createTenant(
        @Valid @RequestBody request: CreateTenantRequest
    ): ResponseEntity<ApiResponse<TenantResponse>> {
        val tenant = tenantService.createTenant(
            name = request.name,
            slug = request.slug,
            plan = request.plan,
            creatorUserId = request.creatorUserId?.let { UUID.fromString(it) }
        )
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(toTenantResponse(tenant)))
    }

    @Operation(summary = "Get tenant", description = "Get tenant by slug")
    @GetMapping("/{slug}")
    fun getTenant(
        @PathVariable slug: String
    ): ResponseEntity<ApiResponse<TenantResponse>> {
        val tenant = tenantService.getTenantBySlug(slug)
        return ResponseEntity.ok(ApiResponse.success(toTenantResponse(tenant)))
    }

    @RequirePermission(Permission.MEMBER_INVITE)
    @Operation(summary = "Add member", description = "Add a user to a tenant")
    @PostMapping("/{slug}/members")
    fun addMember(
        @PathVariable slug: String,
        @Valid @RequestBody request: AddMemberRequest
    ): ResponseEntity<ApiResponse<TenantMemberResponse>> {
        val membership = tenantService.addMember(slug, UUID.fromString(request.userId))
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(toMemberResponse(membership)))
    }

    @Operation(summary = "Get members", description = "Get all members of a tenant")
    @GetMapping("/{slug}/members")
    fun getMembers(
        @PathVariable slug: String
    ): ResponseEntity<ApiResponse<List<TenantMemberResponse>>> {
        val members = tenantService.getMembers(slug)
        return ResponseEntity.ok(
            ApiResponse.success(members.map { toMemberResponse(it) })
        )
    }

    @RequirePermission(Permission.MEMBER_ROLE_CHANGE)
    @Operation(summary = "Change member role", description = "Change a member's role in a tenant")
    @PutMapping("/{slug}/members/{userId}/role")
    fun changeMemberRole(
        @PathVariable slug: String,
        @PathVariable userId: UUID,
        @Valid @RequestBody request: ChangeMemberRoleRequest
    ): ResponseEntity<ApiResponse<TenantMemberResponse>> {
        val membership = tenantService.changeMemberRole(slug, userId, request.role)
        return ResponseEntity.ok(ApiResponse.success(toMemberResponse(membership)))
    }

    @RequirePermission(Permission.MEMBER_REMOVE)
    @Operation(summary = "Remove member", description = "Remove a user from a tenant")
    @DeleteMapping("/{slug}/members/{userId}")
    fun removeMember(
        @PathVariable slug: String,
        @PathVariable userId: UUID
    ): ResponseEntity<Void> {
        tenantService.removeMember(slug, userId)
        return ResponseEntity.noContent().build()
    }

    private fun toTenantResponse(tenant: TenantEntity): TenantResponse {
        return TenantResponse(
            id = tenant.id.toString(),
            name = tenant.name,
            slug = tenant.slug,
            plan = tenant.plan,
            status = tenant.status.name,
            createdAt = tenant.createdAt
        )
    }

    private fun toMemberResponse(membership: TenantMembershipEntity): TenantMemberResponse {
        return TenantMemberResponse(
            userId = membership.user.id.toString(),
            email = membership.user.email,
            name = membership.user.name,
            role = membership.role,
            joinedAt = membership.joinedAt
        )
    }
}
