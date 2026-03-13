package com.sonature.auth.application.port.output

import com.sonature.auth.domain.token.model.Algorithm
import com.sonature.auth.domain.token.model.TokenClaims
import com.sonature.auth.domain.token.model.TokenConfig

interface TokenProvider {
    fun issue(claims: TokenClaims, config: TokenConfig): String
    fun verify(token: String): TokenClaims
    fun supportedAlgorithm(): Algorithm
}
