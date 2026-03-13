package com.sonature.auth.infrastructure.oauth2

import com.sonature.auth.common.util.TimeProvider
import com.sonature.auth.domain.user.entity.UserEntity
import com.sonature.auth.domain.user.model.AuthProvider
import com.sonature.auth.domain.user.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.user.DefaultOAuth2User
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CustomOAuth2UserService(
    private val userRepository: UserRepository,
    private val timeProvider: TimeProvider,
    private val delegateOAuth2UserService: DefaultOAuth2UserService = DefaultOAuth2UserService()
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
        val oAuth2User = delegateOAuth2UserService.loadUser(userRequest)

        val registrationId = userRequest.clientRegistration.registrationId
        val provider = resolveProvider(registrationId)
        val profile = OAuth2UserProfileMapper.map(provider, oAuth2User.attributes)

        val user = findOrCreateUser(profile)

        val enhancedAttributes = oAuth2User.attributes.toMutableMap()
        enhancedAttributes["userId"] = user.id.toString()

        val nameAttributeKey = userRequest.clientRegistration.providerDetails
            .userInfoEndpoint.userNameAttributeName

        return DefaultOAuth2User(
            oAuth2User.authorities,
            enhancedAttributes,
            nameAttributeKey
        )
    }

    private fun findOrCreateUser(profile: OAuth2UserProfile): UserEntity {
        // Case 1: Existing social account
        val existingByProvider = userRepository.findByProviderAndProviderId(
            profile.provider, profile.providerId
        )
        if (existingByProvider != null) {
            existingByProvider.recordLogin(timeProvider.now())
            return userRepository.save(existingByProvider)
        }

        val email = profile.email
            ?: throw OAuth2AuthenticationException(
                OAuth2Error("email_not_found"),
                "Email is required for social login but was not provided by ${profile.provider}"
            )

        // Case 2: Existing LOCAL account with same email -> link
        val existingByEmail = userRepository.findByEmail(email)
        if (existingByEmail != null) {
            existingByEmail.provider = profile.provider
            existingByEmail.providerId = profile.providerId
            existingByEmail.name = profile.name ?: existingByEmail.name
            existingByEmail.recordLogin(timeProvider.now())
            return userRepository.save(existingByEmail)
        }

        // Case 3: Create new user
        val newUser = UserEntity(
            email = email,
            name = profile.name,
            provider = profile.provider,
            providerId = profile.providerId
        )
        logger.info("Created new user via social login: provider=${profile.provider}, email=$email")
        return userRepository.save(newUser)
    }

    private fun resolveProvider(registrationId: String): AuthProvider {
        return when (registrationId.lowercase()) {
            "google" -> AuthProvider.GOOGLE
            "github" -> AuthProvider.GITHUB
            "kakao" -> AuthProvider.KAKAO
            else -> throw OAuth2AuthenticationException(
                OAuth2Error("unsupported_provider"),
                "Unsupported OAuth2 provider: $registrationId"
            )
        }
    }
}
