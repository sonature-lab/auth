package com.sonature.auth.domain.token.model

enum class Algorithm(
    val value: String,
    val isSymmetric: Boolean
) {
    HS256("HS256", isSymmetric = true),
    RS256("RS256", isSymmetric = false),
    PASETO_V4_LOCAL("v4.local", isSymmetric = true),
    PASETO_V4_PUBLIC("v4.public", isSymmetric = false);

    companion object {
        fun fromValue(value: String): Algorithm =
            entries.find { it.value.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unsupported algorithm: $value")
    }
}
