package com.sonature.auth.api.v1.tenant.dto

import com.sonature.auth.domain.tenant.model.TenantPlan
import com.sonature.auth.domain.tenant.model.TenantRole
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.Instant

data class CreateTenantRequest(
    @field:NotBlank(message = "Name is required")
    @field:Size(max = 100, message = "Name must be at most 100 characters")
    val name: String,

    @field:NotBlank(message = "Slug is required")
    @field:Size(max = 100, message = "Slug must be at most 100 characters")
    @field:Pattern(
        regexp = "^[a-z0-9][a-z0-9-]*[a-z0-9]$",
        message = "Slug must be URL-safe (lowercase letters, numbers, and hyphens)"
    )
    val slug: String,

    val plan: TenantPlan = TenantPlan.FREE,

    val creatorUserId: String? = null
)

data class TenantResponse(
    val id: String,
    val name: String,
    val slug: String,
    val plan: TenantPlan,
    val status: String,
    val createdAt: Instant
)

data class AddMemberRequest(
    @field:NotBlank(message = "userId is required")
    val userId: String
)

data class AddMemberWithRoleRequest(
    @field:NotBlank(message = "userId is required")
    val userId: String,

    val role: TenantRole = TenantRole.MEMBER
)

data class ChangeMemberRoleRequest(
    @field:NotNull(message = "role is required")
    val role: TenantRole
)

data class TenantMemberResponse(
    val userId: String,
    val email: String,
    val name: String?,
    val role: TenantRole,
    val joinedAt: Instant
)
