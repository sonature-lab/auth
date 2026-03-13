package com.sonature.auth.infrastructure.crypto.key

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "crypto.keys")
data class KeyConfig(
    val hs256Secret: String? = null,
    val rs256PrivateKey: String? = null,
    val rs256PublicKey: String? = null,
    val pasetoSecretKey: String? = null,
    val pasetoPrivateKey: String? = null,
    val pasetoPublicKey: String? = null
)
