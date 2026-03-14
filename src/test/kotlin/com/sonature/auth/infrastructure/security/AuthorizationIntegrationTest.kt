package com.sonature.auth.infrastructure.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.sonature.auth.api.v1.tenant.dto.AddMemberRequest
import com.sonature.auth.api.v1.tenant.dto.ChangeMemberRoleRequest
import com.sonature.auth.api.v1.tenant.dto.CreateTenantRequest
import com.sonature.auth.application.service.JwtService
import com.sonature.auth.domain.tenant.model.TenantRole
import com.sonature.auth.domain.user.entity.UserEntity
import com.sonature.auth.domain.user.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthorizationIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var jwtService: JwtService

    private lateinit var owner: UserEntity
    private lateinit var admin: UserEntity
    private lateinit var member: UserEntity
    private lateinit var viewer: UserEntity
    private lateinit var outsider: UserEntity
    private lateinit var tenantSlug: String

    @BeforeEach
    fun setUp() {
        val ts = System.nanoTime()
        owner = userRepository.save(UserEntity(email = "owner-$ts@test.com", name = "Owner"))
        admin = userRepository.save(UserEntity(email = "admin-$ts@test.com", name = "Admin"))
        member = userRepository.save(UserEntity(email = "member-$ts@test.com", name = "Member"))
        viewer = userRepository.save(UserEntity(email = "viewer-$ts@test.com", name = "Viewer"))
        outsider = userRepository.save(UserEntity(email = "outsider-$ts@test.com", name = "Outsider"))

        tenantSlug = "auth-test-$ts"

        // Create tenant with owner
        mockMvc.post("/api/v1/tenants") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                CreateTenantRequest(name = "Auth Test", slug = tenantSlug, creatorUserId = owner.id.toString())
            )
        }.andExpect { status { isCreated() } }

        // Add other members with proper roles using owner token
        val ownerToken = issueToken(owner, tenantSlug, TenantRole.OWNER)
        addMemberWithAuth(ownerToken, admin.id.toString())
        addMemberWithAuth(ownerToken, member.id.toString())
        addMemberWithAuth(ownerToken, viewer.id.toString())

        // Change roles
        changeRoleWithAuth(ownerToken, admin.id.toString(), TenantRole.ADMIN)
        changeRoleWithAuth(ownerToken, viewer.id.toString(), TenantRole.VIEWER)
    }

    private fun issueToken(user: UserEntity, slug: String, role: TenantRole): String {
        return jwtService.issueAccessToken(
            subject = user.id.toString(),
            customClaims = mapOf(
                "tenants" to listOf(mapOf("slug" to slug, "role" to role.name))
            )
        ).value
    }

    private fun addMemberWithAuth(token: String, userId: String) {
        mockMvc.post("/api/v1/tenants/$tenantSlug/members") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(AddMemberRequest(userId = userId))
            header("Authorization", "Bearer $token")
            header("X-Tenant-Slug", tenantSlug)
        }.andExpect { status { isCreated() } }
    }

    private fun changeRoleWithAuth(token: String, userId: String, role: TenantRole) {
        mockMvc.put("/api/v1/tenants/$tenantSlug/members/$userId/role") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(ChangeMemberRoleRequest(role = role))
            header("Authorization", "Bearer $token")
            header("X-Tenant-Slug", tenantSlug)
        }.andExpect { status { isOk() } }
    }

    @Nested
    inner class MemberInvitePermission {

        @Test
        fun `OWNER can invite members`() {
            val newUser = userRepository.save(UserEntity(email = "new-${System.nanoTime()}@test.com", name = "New"))
            val token = issueToken(owner, tenantSlug, TenantRole.OWNER)

            mockMvc.post("/api/v1/tenants/$tenantSlug/members") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(AddMemberRequest(userId = newUser.id.toString()))
                header("Authorization", "Bearer $token")
                header("X-Tenant-Slug", tenantSlug)
            }.andExpect { status { isCreated() } }
        }

        @Test
        fun `ADMIN can invite members`() {
            val newUser = userRepository.save(UserEntity(email = "new-${System.nanoTime()}@test.com", name = "New"))
            val token = issueToken(admin, tenantSlug, TenantRole.ADMIN)

            mockMvc.post("/api/v1/tenants/$tenantSlug/members") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(AddMemberRequest(userId = newUser.id.toString()))
                header("Authorization", "Bearer $token")
                header("X-Tenant-Slug", tenantSlug)
            }.andExpect { status { isCreated() } }
        }

        @Test
        fun `MEMBER cannot invite members`() {
            val newUser = userRepository.save(UserEntity(email = "new-${System.nanoTime()}@test.com", name = "New"))
            val token = issueToken(member, tenantSlug, TenantRole.MEMBER)

            mockMvc.post("/api/v1/tenants/$tenantSlug/members") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(AddMemberRequest(userId = newUser.id.toString()))
                header("Authorization", "Bearer $token")
                header("X-Tenant-Slug", tenantSlug)
            }.andExpect {
                status { isForbidden() }
                jsonPath("$.error.code") { value("INSUFFICIENT_PERMISSION") }
            }
        }

        @Test
        fun `VIEWER cannot invite members`() {
            val newUser = userRepository.save(UserEntity(email = "new-${System.nanoTime()}@test.com", name = "New"))
            val token = issueToken(viewer, tenantSlug, TenantRole.VIEWER)

            mockMvc.post("/api/v1/tenants/$tenantSlug/members") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(AddMemberRequest(userId = newUser.id.toString()))
                header("Authorization", "Bearer $token")
                header("X-Tenant-Slug", tenantSlug)
            }.andExpect {
                status { isForbidden() }
            }
        }
    }

    @Nested
    inner class MemberRemovePermission {

        @Test
        fun `OWNER can remove members`() {
            val target = userRepository.save(UserEntity(email = "target-${System.nanoTime()}@test.com", name = "Target"))
            val ownerToken = issueToken(owner, tenantSlug, TenantRole.OWNER)
            addMemberWithAuth(ownerToken, target.id.toString())

            mockMvc.delete("/api/v1/tenants/$tenantSlug/members/${target.id}") {
                header("Authorization", "Bearer $ownerToken")
                header("X-Tenant-Slug", tenantSlug)
            }.andExpect { status { isNoContent() } }
        }

        @Test
        fun `MEMBER cannot remove members`() {
            val token = issueToken(member, tenantSlug, TenantRole.MEMBER)

            mockMvc.delete("/api/v1/tenants/$tenantSlug/members/${viewer.id}") {
                header("Authorization", "Bearer $token")
                header("X-Tenant-Slug", tenantSlug)
            }.andExpect {
                status { isForbidden() }
                jsonPath("$.error.code") { value("INSUFFICIENT_PERMISSION") }
            }
        }
    }

    @Nested
    inner class RoleChangePermission {

        @Test
        fun `OWNER can change roles`() {
            val token = issueToken(owner, tenantSlug, TenantRole.OWNER)

            mockMvc.put("/api/v1/tenants/$tenantSlug/members/${member.id}/role") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(ChangeMemberRoleRequest(role = TenantRole.ADMIN))
                header("Authorization", "Bearer $token")
                header("X-Tenant-Slug", tenantSlug)
            }.andExpect { status { isOk() } }
        }

        @Test
        fun `ADMIN can change roles`() {
            val token = issueToken(admin, tenantSlug, TenantRole.ADMIN)

            mockMvc.put("/api/v1/tenants/$tenantSlug/members/${member.id}/role") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(ChangeMemberRoleRequest(role = TenantRole.VIEWER))
                header("Authorization", "Bearer $token")
                header("X-Tenant-Slug", tenantSlug)
            }.andExpect { status { isOk() } }
        }

        @Test
        fun `MEMBER cannot change roles`() {
            val token = issueToken(member, tenantSlug, TenantRole.MEMBER)

            mockMvc.put("/api/v1/tenants/$tenantSlug/members/${viewer.id}/role") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(ChangeMemberRoleRequest(role = TenantRole.ADMIN))
                header("Authorization", "Bearer $token")
                header("X-Tenant-Slug", tenantSlug)
            }.andExpect {
                status { isForbidden() }
                jsonPath("$.error.code") { value("INSUFFICIENT_PERMISSION") }
            }
        }
    }

    @Nested
    inner class TenantContextRequired {

        @Test
        fun `request without tenant context returns 400`() {
            mockMvc.post("/api/v1/tenants/$tenantSlug/members") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(AddMemberRequest(userId = outsider.id.toString()))
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.error.code") { value("TENANT_CONTEXT_REQUIRED") }
            }
        }

        @Test
        fun `request with non-member token returns 400`() {
            val token = jwtService.issueAccessToken(
                subject = outsider.id.toString(),
                customClaims = mapOf(
                    "tenants" to listOf(mapOf("slug" to "other-org", "role" to "OWNER"))
                )
            ).value

            mockMvc.post("/api/v1/tenants/$tenantSlug/members") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(AddMemberRequest(userId = outsider.id.toString()))
                header("Authorization", "Bearer $token")
                header("X-Tenant-Slug", tenantSlug)
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.error.code") { value("TENANT_CONTEXT_REQUIRED") }
            }
        }
    }
}
