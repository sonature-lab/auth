package com.sonature.auth.api.oauth2

import com.sonature.auth.domain.oauth2.model.ScopeDefinition
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import java.security.Principal

@Controller
class ConsentController(
    private val registeredClientRepository: RegisteredClientRepository
) {

    @GetMapping("/oauth2/consent")
    fun consent(
        principal: Principal,
        model: Model,
        @RequestParam("scope", required = false, defaultValue = "") scope: String,
        @RequestParam("client_id", required = false, defaultValue = "") clientId: String,
        @RequestParam("state", required = false, defaultValue = "") state: String
    ): String {
        val scopes = scope.split(" ").filter { it.isNotBlank() }.toSet()
        val scopeDefinitions = ScopeDefinition.findByScopes(scopes)

        // Look up client name from repository
        val clientName = if (clientId.isNotBlank()) {
            registeredClientRepository.findByClientId(clientId)?.clientName ?: clientId
        } else {
            ""
        }

        model.addAttribute("clientId", clientId)
        model.addAttribute("clientName", clientName)
        model.addAttribute("state", state)
        model.addAttribute("scopes", scopes)
        model.addAttribute("scopeDefinitions", scopeDefinitions)
        model.addAttribute("principalName", principal.name)

        return "consent"
    }
}
