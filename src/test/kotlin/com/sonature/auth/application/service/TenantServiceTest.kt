package com.sonature.auth.application.service

import com.sonature.auth.domain.tenant.entity.TenantEntity
import com.sonature.auth.domain.tenant.entity.TenantMembershipEntity
import com.sonature.auth.domain.tenant.exception.AlreadyTenantMemberException
import com.sonature.auth.domain.tenant.exception.NotTenantMemberException
import com.sonature.auth.domain.tenant.exception.TenantNotFoundException
import com.sonature.auth.domain.tenant.exception.TenantSlugAlreadyExistsException
import com.sonature.auth.domain.tenant.model.TenantPlan
import com.sonature.auth.domain.tenant.model.TenantRole
import com.sonature.auth.domain.tenant.model.TenantStatus
import com.sonature.auth.domain.tenant.repository.TenantMembershipRepository
import com.sonature.auth.domain.tenant.repository.TenantRepository
import com.sonature.auth.domain.user.entity.UserEntity
import com.sonature.auth.domain.user.exception.UserNotFoundException
import com.sonature.auth.domain.user.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.Optional
import java.util.UUID

class TenantServiceTest {

    private lateinit var tenantRepository: TenantRepository
    private lateinit var tenantMembershipRepository: TenantMembershipRepository
    private lateinit var userRepository: UserRepository
    private lateinit var tenantService: TenantService

    private val testUser = UserEntity(
        email = "user@example.com",
        name = "Test User"
    )

    @BeforeEach
    fun setUp() {
        tenantRepository = mockk()
        tenantMembershipRepository = mockk()
        userRepository = mockk()
        tenantService = TenantService(tenantRepository, tenantMembershipRepository, userRepository)
    }

    // --- createTenant ---

    @Test
    fun `createTenant should create tenant successfully`() {
        every { tenantRepository.existsBySlug("sonature-lab") } returns false
        val tenantSlot = slot<TenantEntity>()
        every { tenantRepository.save(capture(tenantSlot)) } answers { tenantSlot.captured }

        val tenant = tenantService.createTenant("Sonature Lab", "sonature-lab", TenantPlan.FREE)

        assertEquals("Sonature Lab", tenant.name)
        assertEquals("sonature-lab", tenant.slug)
        assertEquals(TenantPlan.FREE, tenant.plan)
        assertEquals(TenantStatus.ACTIVE, tenant.status)
        assertNotNull(tenant.id)
    }

    @Test
    fun `createTenant with duplicate slug should throw TenantSlugAlreadyExistsException`() {
        every { tenantRepository.existsBySlug("existing-slug") } returns true

        assertThrows<TenantSlugAlreadyExistsException> {
            tenantService.createTenant("Some Name", "existing-slug", TenantPlan.PRO)
        }
    }

    // --- getTenantBySlug ---

    @Test
    fun `getTenantBySlug should return tenant when found`() {
        val tenant = TenantEntity(name = "Lab", slug = "lab")
        every { tenantRepository.findBySlug("lab") } returns tenant

        val result = tenantService.getTenantBySlug("lab")

        assertEquals("lab", result.slug)
        assertEquals("Lab", result.name)
    }

    @Test
    fun `getTenantBySlug should throw TenantNotFoundException when not found`() {
        every { tenantRepository.findBySlug("nonexistent") } returns null

        assertThrows<TenantNotFoundException> {
            tenantService.getTenantBySlug("nonexistent")
        }
    }

    // --- addMember ---

    @Test
    fun `addMember should add user to tenant successfully`() {
        val tenant = TenantEntity(name = "Lab", slug = "lab")
        every { tenantRepository.findBySlug("lab") } returns tenant
        every { userRepository.findById(testUser.id) } returns Optional.of(testUser)
        every { tenantMembershipRepository.existsByTenantAndUser(tenant, testUser) } returns false
        val membershipSlot = slot<TenantMembershipEntity>()
        every { tenantMembershipRepository.save(capture(membershipSlot)) } answers { membershipSlot.captured }

        val membership = tenantService.addMember("lab", testUser.id)

        assertEquals(tenant, membership.tenant)
        assertEquals(testUser, membership.user)
        assertEquals(TenantRole.MEMBER, membership.role)
        assertNotNull(membership.joinedAt)
    }

    @Test
    fun `addMember should throw TenantNotFoundException when tenant not found`() {
        every { tenantRepository.findBySlug("nonexistent") } returns null

        assertThrows<TenantNotFoundException> {
            tenantService.addMember("nonexistent", testUser.id)
        }
    }

    @Test
    fun `addMember should throw UserNotFoundException when user not found`() {
        val tenant = TenantEntity(name = "Lab", slug = "lab")
        val randomId = UUID.randomUUID()
        every { tenantRepository.findBySlug("lab") } returns tenant
        every { userRepository.findById(randomId) } returns Optional.empty()

        assertThrows<UserNotFoundException> {
            tenantService.addMember("lab", randomId)
        }
    }

    @Test
    fun `addMember should throw AlreadyTenantMemberException when already member`() {
        val tenant = TenantEntity(name = "Lab", slug = "lab")
        every { tenantRepository.findBySlug("lab") } returns tenant
        every { userRepository.findById(testUser.id) } returns Optional.of(testUser)
        every { tenantMembershipRepository.existsByTenantAndUser(tenant, testUser) } returns true

        assertThrows<AlreadyTenantMemberException> {
            tenantService.addMember("lab", testUser.id)
        }
    }

    // --- getMembers ---

    @Test
    fun `getMembers should return list of memberships`() {
        val tenant = TenantEntity(name = "Lab", slug = "lab")
        val membership = TenantMembershipEntity(tenant = tenant, user = testUser)
        every { tenantRepository.findBySlug("lab") } returns tenant
        every { tenantMembershipRepository.findAllByTenant(tenant) } returns listOf(membership)

        val result = tenantService.getMembers("lab")

        assertEquals(1, result.size)
        assertEquals(testUser, result[0].user)
    }

    @Test
    fun `getMembers should throw TenantNotFoundException when tenant not found`() {
        every { tenantRepository.findBySlug("nonexistent") } returns null

        assertThrows<TenantNotFoundException> {
            tenantService.getMembers("nonexistent")
        }
    }

    // --- getUserTenants ---

    @Test
    fun `getUserTenants should return list of memberships for user`() {
        val tenant = TenantEntity(name = "Lab", slug = "lab")
        val membership = TenantMembershipEntity(tenant = tenant, user = testUser)
        every { userRepository.findById(testUser.id) } returns Optional.of(testUser)
        every { tenantMembershipRepository.findAllByUser(testUser) } returns listOf(membership)

        val result = tenantService.getUserTenants(testUser.id)

        assertEquals(1, result.size)
        assertEquals("lab", result[0].tenant.slug)
    }

    @Test
    fun `getUserTenants should throw UserNotFoundException when user not found`() {
        val randomId = UUID.randomUUID()
        every { userRepository.findById(randomId) } returns Optional.empty()

        assertThrows<UserNotFoundException> {
            tenantService.getUserTenants(randomId)
        }
    }

    // --- removeMember ---

    @Test
    fun `removeMember should remove membership successfully`() {
        val tenant = TenantEntity(name = "Lab", slug = "lab")
        val membership = TenantMembershipEntity(tenant = tenant, user = testUser)
        every { tenantRepository.findBySlug("lab") } returns tenant
        every { userRepository.findById(testUser.id) } returns Optional.of(testUser)
        every { tenantMembershipRepository.findByTenantAndUser(tenant, testUser) } returns membership
        every { tenantMembershipRepository.delete(membership) } returns Unit

        tenantService.removeMember("lab", testUser.id)

        verify { tenantMembershipRepository.delete(membership) }
    }

    @Test
    fun `removeMember should throw TenantNotFoundException when tenant not found`() {
        every { tenantRepository.findBySlug("nonexistent") } returns null

        assertThrows<TenantNotFoundException> {
            tenantService.removeMember("nonexistent", testUser.id)
        }
    }

    @Test
    fun `removeMember should throw UserNotFoundException when user not found`() {
        val tenant = TenantEntity(name = "Lab", slug = "lab")
        val randomId = UUID.randomUUID()
        every { tenantRepository.findBySlug("lab") } returns tenant
        every { userRepository.findById(randomId) } returns Optional.empty()

        assertThrows<UserNotFoundException> {
            tenantService.removeMember("lab", randomId)
        }
    }

    @Test
    fun `removeMember should throw NotTenantMemberException when not a member`() {
        val tenant = TenantEntity(name = "Lab", slug = "lab")
        every { tenantRepository.findBySlug("lab") } returns tenant
        every { userRepository.findById(testUser.id) } returns Optional.of(testUser)
        every { tenantMembershipRepository.findByTenantAndUser(tenant, testUser) } returns null

        assertThrows<NotTenantMemberException> {
            tenantService.removeMember("lab", testUser.id)
        }
    }

    // --- createTenant with creatorUserId ---

    @Test
    fun `createTenant with creatorUserId should add creator as OWNER`() {
        every { tenantRepository.existsBySlug("lab") } returns false
        val tenantSlot = slot<TenantEntity>()
        every { tenantRepository.save(capture(tenantSlot)) } answers { tenantSlot.captured }
        every { userRepository.findById(testUser.id) } returns Optional.of(testUser)
        val membershipSlot = slot<TenantMembershipEntity>()
        every { tenantMembershipRepository.save(capture(membershipSlot)) } answers { membershipSlot.captured }

        tenantService.createTenant("Lab", "lab", TenantPlan.FREE, testUser.id)

        assertEquals(TenantRole.OWNER, membershipSlot.captured.role)
        assertEquals(testUser, membershipSlot.captured.user)
    }

    @Test
    fun `createTenant without creatorUserId should not add any member`() {
        every { tenantRepository.existsBySlug("lab") } returns false
        val tenantSlot = slot<TenantEntity>()
        every { tenantRepository.save(capture(tenantSlot)) } answers { tenantSlot.captured }

        tenantService.createTenant("Lab", "lab", TenantPlan.FREE)

        verify(exactly = 0) { tenantMembershipRepository.save(any()) }
    }

    // --- addMember with role ---

    @Test
    fun `addMember with ADMIN role should set role to ADMIN`() {
        val tenant = TenantEntity(name = "Lab", slug = "lab")
        every { tenantRepository.findBySlug("lab") } returns tenant
        every { userRepository.findById(testUser.id) } returns Optional.of(testUser)
        every { tenantMembershipRepository.existsByTenantAndUser(tenant, testUser) } returns false
        val membershipSlot = slot<TenantMembershipEntity>()
        every { tenantMembershipRepository.save(capture(membershipSlot)) } answers { membershipSlot.captured }

        val membership = tenantService.addMember("lab", testUser.id, TenantRole.ADMIN)

        assertEquals(TenantRole.ADMIN, membership.role)
    }

    @Test
    fun `addMember with VIEWER role should set role to VIEWER`() {
        val tenant = TenantEntity(name = "Lab", slug = "lab")
        every { tenantRepository.findBySlug("lab") } returns tenant
        every { userRepository.findById(testUser.id) } returns Optional.of(testUser)
        every { tenantMembershipRepository.existsByTenantAndUser(tenant, testUser) } returns false
        val membershipSlot = slot<TenantMembershipEntity>()
        every { tenantMembershipRepository.save(capture(membershipSlot)) } answers { membershipSlot.captured }

        val membership = tenantService.addMember("lab", testUser.id, TenantRole.VIEWER)

        assertEquals(TenantRole.VIEWER, membership.role)
    }

    // --- changeMemberRole ---

    @Test
    fun `changeMemberRole should update role successfully`() {
        val tenant = TenantEntity(name = "Lab", slug = "lab")
        val membership = TenantMembershipEntity(tenant = tenant, user = testUser, role = TenantRole.MEMBER)
        every { tenantRepository.findBySlug("lab") } returns tenant
        every { userRepository.findById(testUser.id) } returns Optional.of(testUser)
        every { tenantMembershipRepository.findByTenantAndUser(tenant, testUser) } returns membership
        val savedSlot = slot<TenantMembershipEntity>()
        every { tenantMembershipRepository.save(capture(savedSlot)) } answers { savedSlot.captured }

        val result = tenantService.changeMemberRole("lab", testUser.id, TenantRole.ADMIN)

        assertEquals(TenantRole.ADMIN, result.role)
        assertEquals(membership.id, result.id)
        assertEquals(membership.joinedAt, result.joinedAt)
    }

    @Test
    fun `changeMemberRole should throw TenantNotFoundException when tenant not found`() {
        every { tenantRepository.findBySlug("nonexistent") } returns null

        assertThrows<TenantNotFoundException> {
            tenantService.changeMemberRole("nonexistent", testUser.id, TenantRole.ADMIN)
        }
    }

    @Test
    fun `changeMemberRole should throw UserNotFoundException when user not found`() {
        val tenant = TenantEntity(name = "Lab", slug = "lab")
        val randomId = UUID.randomUUID()
        every { tenantRepository.findBySlug("lab") } returns tenant
        every { userRepository.findById(randomId) } returns Optional.empty()

        assertThrows<UserNotFoundException> {
            tenantService.changeMemberRole("lab", randomId, TenantRole.ADMIN)
        }
    }

    @Test
    fun `changeMemberRole should throw NotTenantMemberException when not a member`() {
        val tenant = TenantEntity(name = "Lab", slug = "lab")
        every { tenantRepository.findBySlug("lab") } returns tenant
        every { userRepository.findById(testUser.id) } returns Optional.of(testUser)
        every { tenantMembershipRepository.findByTenantAndUser(tenant, testUser) } returns null

        assertThrows<NotTenantMemberException> {
            tenantService.changeMemberRole("lab", testUser.id, TenantRole.ADMIN)
        }
    }

    // --- getMemberRole ---

    @Test
    fun `getMemberRole should return role of member`() {
        val tenant = TenantEntity(name = "Lab", slug = "lab")
        val membership = TenantMembershipEntity(tenant = tenant, user = testUser, role = TenantRole.ADMIN)
        every { tenantRepository.findBySlug("lab") } returns tenant
        every { userRepository.findById(testUser.id) } returns Optional.of(testUser)
        every { tenantMembershipRepository.findByTenantAndUser(tenant, testUser) } returns membership

        val role = tenantService.getMemberRole("lab", testUser.id)

        assertEquals(TenantRole.ADMIN, role)
    }

    @Test
    fun `getMemberRole should throw NotTenantMemberException when not a member`() {
        val tenant = TenantEntity(name = "Lab", slug = "lab")
        every { tenantRepository.findBySlug("lab") } returns tenant
        every { userRepository.findById(testUser.id) } returns Optional.of(testUser)
        every { tenantMembershipRepository.findByTenantAndUser(tenant, testUser) } returns null

        assertThrows<NotTenantMemberException> {
            tenantService.getMemberRole("lab", testUser.id)
        }
    }
}
