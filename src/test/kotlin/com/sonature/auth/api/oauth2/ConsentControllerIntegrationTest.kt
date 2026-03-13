package com.sonature.auth.api.oauth2

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ConsentControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `GET consent page should render with valid parameters`() {
        mockMvc.get("/oauth2/consent") {
            param("scope", "openid profile email")
            param("client_id", "sonature-dev-client")
            param("state", "test-state-123")
            with(user("testuser").roles("USER"))
        }.andExpect {
            status { isOk() }
            view { name("consent") }
            model {
                attributeExists("clientId")
                attributeExists("clientName")
                attributeExists("state")
                attributeExists("scopes")
                attributeExists("scopeDefinitions")
            }
        }
    }

    @Test
    fun `GET consent page should show scope definitions with descriptions`() {
        mockMvc.get("/oauth2/consent") {
            param("scope", "openid profile auth:read")
            param("client_id", "sonature-dev-client")
            param("state", "test-state-123")
            with(user("testuser").roles("USER"))
        }.andExpect {
            status { isOk() }
            content {
                // Verify that scope descriptions are present in the HTML
                string(org.hamcrest.Matchers.containsString("openid"))
                string(org.hamcrest.Matchers.containsString("auth:read"))
            }
        }
    }

    @Test
    fun `GET consent page without authentication should redirect to login`() {
        mockMvc.get("/oauth2/consent") {
            param("scope", "openid")
            param("client_id", "sonature-dev-client")
            param("state", "test-state-123")
        }.andExpect {
            status { is3xxRedirection() }
        }
    }

    @Test
    fun `GET consent page without client_id should handle gracefully`() {
        mockMvc.get("/oauth2/consent") {
            param("scope", "openid")
            param("state", "test-state-123")
            with(user("testuser").roles("USER"))
        }.andExpect {
            status { isOk() }
            view { name("consent") }
            model {
                attributeExists("clientId")
            }
        }
    }

    @Test
    fun `GET consent page should handle unknown client gracefully`() {
        mockMvc.get("/oauth2/consent") {
            param("scope", "openid profile")
            param("client_id", "non-existent-client")
            param("state", "test-state-123")
            with(user("testuser").roles("USER"))
        }.andExpect {
            status { isOk() }
            view { name("consent") }
        }
    }
}
