package com.sonature.auth.api.common

import java.time.Instant

data class ApiError(
    val code: String,
    val message: String,
    val details: Map<String, Any>? = null,
    val timestamp: Instant = Instant.now()
)
