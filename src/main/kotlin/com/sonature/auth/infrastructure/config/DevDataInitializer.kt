package com.sonature.auth.infrastructure.config

import com.sonature.auth.domain.oauth2.entity.OAuth2ClientEntity
import com.sonature.auth.domain.oauth2.repository.OAuth2ClientRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@Profile("dev", "test")
class DevDataInitializer(
    private val oauth2ClientRepository: OAuth2ClientRepository,
    private val passwordEncoder: PasswordEncoder
) : ApplicationRunner {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments?) {
        if (oauth2ClientRepository.findByClientId("sonature-dev-client") != null) {
            return
        }

        // Public client (SPA/mobile) — PKCE required, no secret
        oauth2ClientRepository.save(
            OAuth2ClientEntity(
                id = UUID.randomUUID().toString(),
                clientId = "sonature-dev-client",
                clientSecret = null,
                clientName = "Sonature Dev Client (Public)",
                redirectUris = "http://localhost:3000/callback,http://localhost:5173/callback",
                scopes = "openid,profile,email",
                grantTypes = "authorization_code,refresh_token",
                requirePkce = true
            )
        )

        // Confidential client (server-to-server)
        oauth2ClientRepository.save(
            OAuth2ClientEntity(
                id = UUID.randomUUID().toString(),
                clientId = "sonature-backend-client",
                clientSecret = passwordEncoder.encode("backend-secret"),
                clientName = "Sonature Backend Client (Confidential)",
                redirectUris = "http://localhost:8081/callback",
                scopes = "openid,profile,email",
                grantTypes = "authorization_code,refresh_token,client_credentials",
                requirePkce = false
            )
        )

        logger.info("Dev OAuth2 clients initialized")
    }
}
