package com.sonature.auth.api.v1.tenant

import com.fasterxml.jackson.databind.ObjectMapper
import com.sonature.auth.api.v1.tenant.dto.AddMemberRequest
import com.sonature.auth.api.v1.tenant.dto.ChangeMemberRoleRequest
import com.sonature.auth.api.v1.tenant.dto.CreateTenantRequest
import com.sonature.auth.application.service.JwtService
import com.sonature.auth.domain.tenant.model.TenantPlan
import com.sonature.auth.domain.tenant.model.TenantRole
import com.sonature.auth.domain.user.entity.UserEntity
import com.sonature.auth.domain.user.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TenantControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var jwtService: JwtService

    private lateinit var testUser: UserEntity
    private lateinit var secondUser: UserEntity

    @BeforeEach
    fun setUp() {
        testUser = userRepository.save(
            UserEntity(
                email = "tenant-test-${System.nanoTime()}@example.com",
                name = "Tenant Test User"
            )
        )
        secondUser = userRepository.save(
            UserEntity(
                email = "second-${System.nanoTime()}@example.com",
                name = "Second User"
            )
        )
    }

    private fun issueTokenWithTenant(userId: String, tenantSlug: String, role: TenantRole): String {
        val token = jwtService.issueAccessToken(
            subject = userId,
            customClaims = mapOf(
                "tenants" to listOf(
                    mapOf("slug" to tenantSlug, "role" to role.name)
                )
            )
        )
        return token.value
    }

    private fun issueBasicToken(userId: String): String {
        return jwtService.issueAccessToken(
            subject = userId,
            customClaims = emptyMap()
        ).value
    }

    // --- POST /api/v1/tenants ---

    @Test
    fun `POST tenants should create tenant and return 201`() {
        val slug = "test-tenant-${System.nanoTime()}"
        val request = CreateTenantRequest(
            name = "Test Tenant",
            slug = slug,
            plan = TenantPlan.FREE
        )
        val token = issueBasicToken(testUser.id.toString())

        mockMvc.post("/api/v1/tenants") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
            header("Authorization", "Bearer $token")
        }.andExpect {
            status { isCreated() }
            jsonPath("$.data.name") { value("Test Tenant") }
            jsonPath("$.data.slug") { value(slug) }
            jsonPath("$.data.plan") { value("FREE") }
            jsonPath("$.data.status") { value("ACTIVE") }
            jsonPath("$.data.id") { isNotEmpty() }
            jsonPath("$.data.createdAt") { isNotEmpty() }
        }
    }

    @Test
    fun `POST tenants with PRO plan should create tenant`() {
        val slug = "pro-tenant-${System.nanoTime()}"
        val request = CreateTenantRequest(
            name = "Pro Tenant",
            slug = slug,
            plan = TenantPlan.PRO
        )
        val token = issueBasicToken(testUser.id.toString())

        mockMvc.post("/api/v1/tenants") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
            header("Authorization", "Bearer $token")
        }.andExpect {
            status { isCreated() }
            jsonPath("$.data.plan") { value("PRO") }
        }
    }

    @Test
    fun `POST tenants with duplicate slug should return 409`() {
        val slug = "dup-slug-${System.nanoTime()}"
        val request = CreateTenantRequest(name = "First", slug = slug)
        val token = issueBasicToken(testUser.id.toString())

        mockMvc.post("/api/v1/tenants") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
            header("Authorization", "Bearer $token")
        }.andExpect { status { isCreated() } }

        mockMvc.post("/api/v1/tenants") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
            header("Authorization", "Bearer $token")
        }.andExpect {
            status { isConflict() }
            jsonPath("$.error.code") { value("TENANT_SLUG_ALREADY_EXISTS") }
        }
    }

    @Test
    fun `POST tenants with blank name should return 400`() {
        val request = mapOf("name" to "", "slug" to "valid-slug")
        val token = issueBasicToken(testUser.id.toString())

        mockMvc.post("/api/v1/tenants") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
            header("Authorization", "Bearer $token")
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `POST tenants with invalid slug should return 400`() {
        val request = CreateTenantRequest(
            name = "Valid Name",
            slug = "INVALID SLUG!"
        )
        val token = issueBasicToken(testUser.id.toString())

        mockMvc.post("/api/v1/tenants") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
            header("Authorization", "Bearer $token")
        }.andExpect {
            status { isBadRequest() }
        }
    }

    // --- GET /api/v1/tenants/{slug} ---

    @Test
    fun `GET tenants by slug should return tenant`() {
        val slug = "get-tenant-${System.nanoTime()}"
        val createRequest = CreateTenantRequest(name = "Get Test", slug = slug)
        val token = issueBasicToken(testUser.id.toString())

        mockMvc.post("/api/v1/tenants") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(createRequest)
            header("Authorization", "Bearer $token")
        }.andExpect { status { isCreated() } }

        mockMvc.get("/api/v1/tenants/$slug") {
            header("Authorization", "Bearer $token")
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.slug") { value(slug) }
            jsonPath("$.data.name") { value("Get Test") }
        }
    }

    @Test
    fun `GET tenants by nonexistent slug should return 404`() {
        val token = issueBasicToken(testUser.id.toString())
        mockMvc.get("/api/v1/tenants/nonexistent-slug-12345") {
            header("Authorization", "Bearer $token")
        }.andExpect {
            status { isNotFound() }
            jsonPath("$.error.code") { value("TENANT_NOT_FOUND") }
        }
    }

    // --- POST /api/v1/tenants/{slug}/members (requires MEMBER_INVITE) ---

    @Test
    fun `POST members should add member and return 201`() {
        val slug = "member-tenant-${System.nanoTime()}"
        val basicToken = issueBasicToken(testUser.id.toString())

        mockMvc.post("/api/v1/tenants") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                CreateTenantRequest(name = "Member Test", slug = slug, creatorUserId = testUser.id.toString())
            )
            header("Authorization", "Bearer $basicToken")
        }.andExpect { status { isCreated() } }

        val token = issueTokenWithTenant(testUser.id.toString(), slug, TenantRole.OWNER)
        val addRequest = AddMemberRequest(userId = secondUser.id.toString())

        mockMvc.post("/api/v1/tenants/$slug/members") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(addRequest)
            header("Authorization", "Bearer $token")
            header("X-Tenant-Slug", slug)
        }.andExpect {
            status { isCreated() }
            jsonPath("$.data.userId") { value(secondUser.id.toString()) }
            jsonPath("$.data.email") { value(secondUser.email) }
            jsonPath("$.data.role") { value("MEMBER") }
            jsonPath("$.data.joinedAt") { isNotEmpty() }
        }
    }

    @Test
    fun `POST members without permission should return 403`() {
        val slug = "noperm-member-${System.nanoTime()}"
        val basicToken = issueBasicToken(testUser.id.toString())

        mockMvc.post("/api/v1/tenants") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                CreateTenantRequest(name = "No Perm", slug = slug, creatorUserId = testUser.id.toString())
            )
            header("Authorization", "Bearer $basicToken")
        }.andExpect { status { isCreated() } }

        val viewerToken = issueTokenWithTenant(testUser.id.toString(), slug, TenantRole.VIEWER)

        mockMvc.post("/api/v1/tenants/$slug/members") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(AddMemberRequest(userId = secondUser.id.toString()))
            header("Authorization", "Bearer $viewerToken")
            header("X-Tenant-Slug", slug)
        }.andExpect {
            status { isForbidden() }
            jsonPath("$.error.code") { value("INSUFFICIENT_PERMISSION") }
        }
    }

    @Test
    fun `POST members without tenant context should return 400`() {
        val slug = "nocontext-${System.nanoTime()}"
        val basicToken = issueBasicToken(testUser.id.toString())

        mockMvc.post("/api/v1/tenants") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(CreateTenantRequest(name = "No Context", slug = slug))
            header("Authorization", "Bearer $basicToken")
        }.andExpect { status { isCreated() } }

        // Authenticated but no X-Tenant-Slug header => TENANT_CONTEXT_REQUIRED
        mockMvc.post("/api/v1/tenants/$slug/members") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(AddMemberRequest(userId = testUser.id.toString()))
            header("Authorization", "Bearer $basicToken")
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.error.code") { value("TENANT_CONTEXT_REQUIRED") }
        }
    }

    @Test
    fun `POST members with duplicate user should return 409`() {
        val slug = "dup-member-${System.nanoTime()}"
        val basicToken = issueBasicToken(testUser.id.toString())

        mockMvc.post("/api/v1/tenants") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                CreateTenantRequest(name = "Dup Member Test", slug = slug, creatorUserId = testUser.id.toString())
            )
            header("Authorization", "Bearer $basicToken")
        }.andExpect { status { isCreated() } }

        val token = issueTokenWithTenant(testUser.id.toString(), slug, TenantRole.OWNER)

        mockMvc.post("/api/v1/tenants/$slug/members") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(AddMemberRequest(userId = secondUser.id.toString()))
            header("Authorization", "Bearer $token")
            header("X-Tenant-Slug", slug)
        }.andExpect { status { isCreated() } }

        mockMvc.post("/api/v1/tenants/$slug/members") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(AddMemberRequest(userId = secondUser.id.toString()))
            header("Authorization", "Bearer $token")
            header("X-Tenant-Slug", slug)
        }.andExpect {
            status { isConflict() }
            jsonPath("$.error.code") { value("ALREADY_TENANT_MEMBER") }
        }
    }

    // --- GET /api/v1/tenants/{slug}/members ---

    @Test
    fun `GET members should return member list`() {
        val slug = "list-members-${System.nanoTime()}"
        val basicToken = issueBasicToken(testUser.id.toString())

        mockMvc.post("/api/v1/tenants") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                CreateTenantRequest(name = "List Members", slug = slug, creatorUserId = testUser.id.toString())
            )
            header("Authorization", "Bearer $basicToken")
        }.andExpect { status { isCreated() } }

        val token = issueTokenWithTenant(testUser.id.toString(), slug, TenantRole.OWNER)

        mockMvc.post("/api/v1/tenants/$slug/members") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(AddMemberRequest(userId = secondUser.id.toString()))
            header("Authorization", "Bearer $token")
            header("X-Tenant-Slug", slug)
        }.andExpect { status { isCreated() } }

        mockMvc.get("/api/v1/tenants/$slug/members") {
            header("Authorization", "Bearer $token")
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.length()") { value(2) }
        }
    }

    @Test
    fun `GET members of empty tenant should return empty list`() {
        val slug = "empty-tenant-${System.nanoTime()}"
        val token = issueBasicToken(testUser.id.toString())

        mockMvc.post("/api/v1/tenants") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(CreateTenantRequest(name = "Empty", slug = slug))
            header("Authorization", "Bearer $token")
        }.andExpect { status { isCreated() } }

        mockMvc.get("/api/v1/tenants/$slug/members") {
            header("Authorization", "Bearer $token")
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.length()") { value(0) }
        }
    }

    // --- DELETE /api/v1/tenants/{slug}/members/{userId} (requires MEMBER_REMOVE) ---

    @Test
    fun `DELETE member should remove and return 204`() {
        val slug = "del-member-${System.nanoTime()}"
        val basicToken = issueBasicToken(testUser.id.toString())

        mockMvc.post("/api/v1/tenants") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                CreateTenantRequest(name = "Del Member", slug = slug, creatorUserId = testUser.id.toString())
            )
            header("Authorization", "Bearer $basicToken")
        }.andExpect { status { isCreated() } }

        val token = issueTokenWithTenant(testUser.id.toString(), slug, TenantRole.OWNER)

        mockMvc.post("/api/v1/tenants/$slug/members") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(AddMemberRequest(userId = secondUser.id.toString()))
            header("Authorization", "Bearer $token")
            header("X-Tenant-Slug", slug)
        }.andExpect { status { isCreated() } }

        mockMvc.delete("/api/v1/tenants/$slug/members/${secondUser.id}") {
            header("Authorization", "Bearer $token")
            header("X-Tenant-Slug", slug)
        }.andExpect {
            status { isNoContent() }
        }

        mockMvc.get("/api/v1/tenants/$slug/members") {
            header("Authorization", "Bearer $token")
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.length()") { value(1) }
        }
    }

    @Test
    fun `DELETE non-member should return 404`() {
        val slug = "notmember-${System.nanoTime()}"
        val basicToken = issueBasicToken(testUser.id.toString())

        mockMvc.post("/api/v1/tenants") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                CreateTenantRequest(name = "Not Member", slug = slug, creatorUserId = testUser.id.toString())
            )
            header("Authorization", "Bearer $basicToken")
        }.andExpect { status { isCreated() } }

        val token = issueTokenWithTenant(testUser.id.toString(), slug, TenantRole.OWNER)

        mockMvc.delete("/api/v1/tenants/$slug/members/${secondUser.id}") {
            header("Authorization", "Bearer $token")
            header("X-Tenant-Slug", slug)
        }.andExpect {
            status { isNotFound() }
            jsonPath("$.error.code") { value("NOT_TENANT_MEMBER") }
        }
    }

    // --- PUT /api/v1/tenants/{slug}/members/{userId}/role (requires MEMBER_ROLE_CHANGE) ---

    @Test
    fun `PUT member role should change role and return 200`() {
        val slug = "role-change-${System.nanoTime()}"
        val basicToken = issueBasicToken(testUser.id.toString())

        mockMvc.post("/api/v1/tenants") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                CreateTenantRequest(name = "Role Change", slug = slug, creatorUserId = testUser.id.toString())
            )
            header("Authorization", "Bearer $basicToken")
        }.andExpect { status { isCreated() } }

        val token = issueTokenWithTenant(testUser.id.toString(), slug, TenantRole.OWNER)

        mockMvc.post("/api/v1/tenants/$slug/members") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(AddMemberRequest(userId = secondUser.id.toString()))
            header("Authorization", "Bearer $token")
            header("X-Tenant-Slug", slug)
        }.andExpect { status { isCreated() } }

        mockMvc.put("/api/v1/tenants/$slug/members/${secondUser.id}/role") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(ChangeMemberRoleRequest(role = TenantRole.ADMIN))
            header("Authorization", "Bearer $token")
            header("X-Tenant-Slug", slug)
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.userId") { value(secondUser.id.toString()) }
            jsonPath("$.data.role") { value("ADMIN") }
        }
    }

    @Test
    fun `PUT member role for non-member should return 404`() {
        val slug = "role-nonmember-${System.nanoTime()}"
        val basicToken = issueBasicToken(testUser.id.toString())

        mockMvc.post("/api/v1/tenants") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                CreateTenantRequest(name = "Role Non Member", slug = slug, creatorUserId = testUser.id.toString())
            )
            header("Authorization", "Bearer $basicToken")
        }.andExpect { status { isCreated() } }

        val token = issueTokenWithTenant(testUser.id.toString(), slug, TenantRole.OWNER)

        mockMvc.put("/api/v1/tenants/$slug/members/${secondUser.id}/role") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(ChangeMemberRoleRequest(role = TenantRole.ADMIN))
            header("Authorization", "Bearer $token")
            header("X-Tenant-Slug", slug)
        }.andExpect {
            status { isNotFound() }
            jsonPath("$.error.code") { value("NOT_TENANT_MEMBER") }
        }
    }

    @Test
    fun `PUT member role should persist the change`() {
        val slug = "role-persist-${System.nanoTime()}"
        val basicToken = issueBasicToken(testUser.id.toString())

        mockMvc.post("/api/v1/tenants") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                CreateTenantRequest(name = "Role Persist", slug = slug, creatorUserId = testUser.id.toString())
            )
            header("Authorization", "Bearer $basicToken")
        }.andExpect { status { isCreated() } }

        val token = issueTokenWithTenant(testUser.id.toString(), slug, TenantRole.OWNER)

        mockMvc.post("/api/v1/tenants/$slug/members") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(AddMemberRequest(userId = secondUser.id.toString()))
            header("Authorization", "Bearer $token")
            header("X-Tenant-Slug", slug)
        }.andExpect { status { isCreated() } }

        mockMvc.put("/api/v1/tenants/$slug/members/${secondUser.id}/role") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(ChangeMemberRoleRequest(role = TenantRole.OWNER))
            header("Authorization", "Bearer $token")
            header("X-Tenant-Slug", slug)
        }.andExpect { status { isOk() } }

        mockMvc.get("/api/v1/tenants/$slug/members") {
            header("Authorization", "Bearer $token")
        }.andExpect {
            status { isOk() }
            jsonPath("$.data[?(@.userId == '${secondUser.id}')].role") { value("OWNER") }
        }
    }

    // --- POST /api/v1/tenants with creatorUserId ---

    @Test
    fun `POST tenants with creatorUserId should add creator as OWNER`() {
        val slug = "creator-tenant-${System.nanoTime()}"
        val request = CreateTenantRequest(
            name = "Creator Tenant",
            slug = slug,
            plan = TenantPlan.FREE,
            creatorUserId = testUser.id.toString()
        )
        val token = issueBasicToken(testUser.id.toString())

        mockMvc.post("/api/v1/tenants") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
            header("Authorization", "Bearer $token")
        }.andExpect {
            status { isCreated() }
            jsonPath("$.data.slug") { value(slug) }
        }

        mockMvc.get("/api/v1/tenants/$slug/members") {
            header("Authorization", "Bearer $token")
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.length()") { value(1) }
            jsonPath("$.data[0].userId") { value(testUser.id.toString()) }
            jsonPath("$.data[0].role") { value("OWNER") }
        }
    }
}
