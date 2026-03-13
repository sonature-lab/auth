package com.sonature.auth.domain.oauth2.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ScopeDefinitionTest {

    @Test
    fun `should have openid scope with descriptions`() {
        val scope = ScopeDefinition.findByScope("openid")
        assertNotNull(scope)
        assertEquals("openid", scope.scope)
        assertNotNull(scope.descriptionKo)
        assertNotNull(scope.descriptionEn)
    }

    @Test
    fun `should have profile scope with descriptions`() {
        val scope = ScopeDefinition.findByScope("profile")
        assertNotNull(scope)
        assertEquals("profile", scope.scope)
    }

    @Test
    fun `should have email scope with descriptions`() {
        val scope = ScopeDefinition.findByScope("email")
        assertNotNull(scope)
        assertEquals("email", scope.scope)
    }

    @Test
    fun `should have auth_read custom scope`() {
        val scope = ScopeDefinition.findByScope("auth:read")
        assertNotNull(scope)
        assertEquals("auth:read", scope.scope)
    }

    @Test
    fun `should have auth_write custom scope`() {
        val scope = ScopeDefinition.findByScope("auth:write")
        assertNotNull(scope)
        assertEquals("auth:write", scope.scope)
    }

    @Test
    fun `should return null for unknown scope`() {
        val scope = ScopeDefinition.findByScope("unknown:scope")
        assertNull(scope)
    }

    @Test
    fun `should return all defined scopes`() {
        val all = ScopeDefinition.all()
        assertEquals(5, all.size)
    }

    @Test
    fun `getDescription should return Korean description by default`() {
        val scope = ScopeDefinition.findByScope("openid")!!
        assertNotNull(scope.descriptionKo)
        assert(scope.descriptionKo.isNotBlank())
    }

    @Test
    fun `getDescription should return English description`() {
        val scope = ScopeDefinition.findByScope("openid")!!
        assertNotNull(scope.descriptionEn)
        assert(scope.descriptionEn.isNotBlank())
    }

    @Test
    fun `findByScopes should return definitions for multiple scopes`() {
        val scopes = ScopeDefinition.findByScopes(setOf("openid", "profile", "auth:read"))
        assertEquals(3, scopes.size)
    }

    @Test
    fun `findByScopes should skip unknown scopes`() {
        val scopes = ScopeDefinition.findByScopes(setOf("openid", "unknown"))
        assertEquals(1, scopes.size)
        assertEquals("openid", scopes[0].scope)
    }
}
