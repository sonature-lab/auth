package com.sonature.auth.infrastructure.config

import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.source.ImmutableJWKSet
import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.proc.SecurityContext
import com.sonature.auth.domain.oauth2.entity.OAuth2ClientEntity
import com.sonature.auth.domain.oauth2.repository.OAuth2ClientRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.http.MediaType
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher
import com.sonature.auth.infrastructure.oauth2.CustomOAuth2UserService
import com.sonature.auth.infrastructure.oauth2.OAuth2LoginSuccessHandler
import com.sonature.auth.infrastructure.security.TenantContextFilter
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.time.Duration
import java.util.UUID

@Configuration
class AuthorizationServerConfig(
    private val customOAuth2UserService: CustomOAuth2UserService,
    private val oAuth2LoginSuccessHandler: OAuth2LoginSuccessHandler,
    private val tenantContextFilter: TenantContextFilter
) {

    @Bean
    @Order(1)
    fun authorizationServerSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        val authorizationServerConfigurer = OAuth2AuthorizationServerConfigurer.authorizationServer()

        http
            .securityMatcher(authorizationServerConfigurer.endpointsMatcher)
            .with(authorizationServerConfigurer) { authServer ->
                authServer.oidc(Customizer.withDefaults())
                authServer.authorizationEndpoint { endpoint ->
                    endpoint.consentPage("/oauth2/consent")
                }
            }
            .exceptionHandling { exceptions ->
                exceptions.defaultAuthenticationEntryPointFor(
                    LoginUrlAuthenticationEntryPoint("/login"),
                    MediaTypeRequestMatcher(MediaType.TEXT_HTML)
                )
            }
            .oauth2ResourceServer { it.jwt(Customizer.withDefaults()) }

        return http.build()
    }

    @Bean
    @Order(2)
    fun defaultSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        return http
            .csrf { it.disable() }
            .authorizeHttpRequests { auth ->
                auth.requestMatchers("/api/v1/**").permitAll()
                auth.requestMatchers("/swagger-ui/**", "/api-docs/**", "/v3/api-docs/**").permitAll()
                auth.requestMatchers("/h2-console/**").permitAll()
                auth.requestMatchers("/actuator/**").permitAll()
                auth.requestMatchers("/login").permitAll()
                auth.anyRequest().authenticated()
            }
            .headers { headers ->
                headers.frameOptions { it.disable() }
            }
            .addFilterBefore(tenantContextFilter, UsernamePasswordAuthenticationFilter::class.java)
            .formLogin(Customizer.withDefaults())
            .oauth2Login { oauth2 ->
                oauth2.userInfoEndpoint { userInfo ->
                    userInfo.userService { userRequest ->
                        customOAuth2UserService.loadUser(userRequest)
                    }
                }
                oauth2.successHandler(oAuth2LoginSuccessHandler)
            }
            .build()
    }

    @Bean
    fun jpaRegisteredClientRepository(
        oauth2ClientRepository: OAuth2ClientRepository,
        passwordEncoder: PasswordEncoder
    ): RegisteredClientRepository {
        return JpaBackedRegisteredClientRepository(oauth2ClientRepository, passwordEncoder)
    }

    @Bean
    fun authorizationServerSettings(): AuthorizationServerSettings {
        return AuthorizationServerSettings.builder()
            .issuer("http://localhost:8080")
            .build()
    }

    @Bean
    fun jwkSource(): JWKSource<SecurityContext> {
        val keyPair = generateRsaKey()
        val publicKey = keyPair.public as RSAPublicKey
        val privateKey = keyPair.private as RSAPrivateKey
        val rsaKey = RSAKey.Builder(publicKey)
            .privateKey(privateKey)
            .keyID(UUID.randomUUID().toString())
            .build()
        val jwkSet = JWKSet(rsaKey)
        return ImmutableJWKSet(jwkSet)
    }

    @Bean
    fun jwtDecoder(jwkSource: JWKSource<SecurityContext>): JwtDecoder {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource)
    }

    private fun generateRsaKey(): KeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
        keyPairGenerator.initialize(2048)
        return keyPairGenerator.generateKeyPair()
    }
}

class JpaBackedRegisteredClientRepository(
    private val oauth2ClientRepository: OAuth2ClientRepository,
    private val passwordEncoder: PasswordEncoder
) : RegisteredClientRepository {

    override fun save(registeredClient: RegisteredClient) {
        val entity = OAuth2ClientEntity(
            id = registeredClient.id,
            clientId = registeredClient.clientId,
            clientSecret = registeredClient.clientSecret,
            clientName = registeredClient.clientName,
            redirectUris = registeredClient.redirectUris.joinToString(","),
            scopes = registeredClient.scopes.joinToString(","),
            grantTypes = registeredClient.authorizationGrantTypes.joinToString(",") { it.value },
            requirePkce = registeredClient.clientSettings.isRequireProofKey,
            accessTokenTtlSeconds = registeredClient.tokenSettings.accessTokenTimeToLive.seconds,
            refreshTokenTtlSeconds = registeredClient.tokenSettings.refreshTokenTimeToLive.seconds
        )
        oauth2ClientRepository.save(entity)
    }

    override fun findById(id: String): RegisteredClient? {
        val entity = oauth2ClientRepository.findById(id).orElse(null) ?: return null
        return toRegisteredClient(entity)
    }

    override fun findByClientId(clientId: String): RegisteredClient? {
        val entity = oauth2ClientRepository.findByClientId(clientId) ?: return null
        return toRegisteredClient(entity)
    }

    private fun toRegisteredClient(entity: OAuth2ClientEntity): RegisteredClient {
        val builder = RegisteredClient.withId(entity.id)
            .clientId(entity.clientId)
            .clientName(entity.clientName)

        if (entity.clientSecret != null) {
            builder
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .clientSecret(entity.clientSecret)
        } else {
            builder.clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
        }

        entity.getGrantTypeList().forEach { grantType ->
            when (grantType) {
                "authorization_code" -> builder.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                "refresh_token" -> builder.authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                "client_credentials" -> builder.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            }
        }

        entity.getRedirectUriList().forEach { builder.redirectUri(it) }
        entity.getScopeList().forEach { builder.scope(it) }

        builder.clientSettings(
            ClientSettings.builder()
                .requireProofKey(entity.requirePkce)
                .requireAuthorizationConsent(true)
                .build()
        )

        builder.tokenSettings(
            TokenSettings.builder()
                .accessTokenTimeToLive(Duration.ofSeconds(entity.accessTokenTtlSeconds))
                .refreshTokenTimeToLive(Duration.ofSeconds(entity.refreshTokenTtlSeconds))
                .reuseRefreshTokens(false)
                .build()
        )

        return builder.build()
    }
}
