package com.sonature.auth.application.port.output

import com.sonature.auth.domain.token.model.Algorithm
import java.security.Key

interface KeyManager {
    fun getSigningKey(algorithm: Algorithm): Key
    fun getVerificationKey(algorithm: Algorithm): Key
    fun hasKey(algorithm: Algorithm): Boolean
}
