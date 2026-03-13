package com.sonature.auth.domain.token.model

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration
import kotlin.test.assertEquals

class TokenConfigTest {

    @Test
    fun `should create TokenConfig with default expiration values`() {
        val config = TokenConfig(
            algorithm = Algorithm.HS256,
            issuer = "sonature-auth"
        )

        assertEquals(Algorithm.HS256, config.algorithm)
        assertEquals("sonature-auth", config.issuer)
        assertEquals(Duration.ofMinutes(15), config.accessTokenExpiration)
        assertEquals(Duration.ofDays(7), config.refreshTokenExpiration)
    }

    @Test
    fun `should create TokenConfig with custom expiration values`() {
        val config = TokenConfig(
            algorithm = Algorithm.RS256,
            issuer = "custom-issuer",
            accessTokenExpiration = Duration.ofMinutes(30),
            refreshTokenExpiration = Duration.ofDays(14)
        )

        assertEquals(Duration.ofMinutes(30), config.accessTokenExpiration)
        assertEquals(Duration.ofDays(14), config.refreshTokenExpiration)
    }

    @Test
    fun `should throw when issuer is blank`() {
        assertThrows<IllegalArgumentException> {
            TokenConfig(
                algorithm = Algorithm.HS256,
                issuer = ""
            )
        }
    }

    @Test
    fun `should throw when accessTokenExpiration is zero`() {
        assertThrows<IllegalArgumentException> {
            TokenConfig(
                algorithm = Algorithm.HS256,
                issuer = "issuer",
                accessTokenExpiration = Duration.ZERO
            )
        }
    }

    @Test
    fun `should throw when accessTokenExpiration is negative`() {
        assertThrows<IllegalArgumentException> {
            TokenConfig(
                algorithm = Algorithm.HS256,
                issuer = "issuer",
                accessTokenExpiration = Duration.ofMinutes(-1)
            )
        }
    }

    @Test
    fun `should throw when refreshTokenExpiration is zero`() {
        assertThrows<IllegalArgumentException> {
            TokenConfig(
                algorithm = Algorithm.HS256,
                issuer = "issuer",
                refreshTokenExpiration = Duration.ZERO
            )
        }
    }

    @Test
    fun `should throw when refreshTokenExpiration is negative`() {
        assertThrows<IllegalArgumentException> {
            TokenConfig(
                algorithm = Algorithm.HS256,
                issuer = "issuer",
                refreshTokenExpiration = Duration.ofDays(-1)
            )
        }
    }

    @Test
    fun `companion object should have correct default values`() {
        assertEquals(Duration.ofMinutes(15), TokenConfig.DEFAULT_ACCESS_EXPIRATION)
        assertEquals(Duration.ofDays(7), TokenConfig.DEFAULT_REFRESH_EXPIRATION)
        assertEquals(Duration.ofMinutes(1), TokenConfig.MIN_ACCESS_EXPIRATION)
        assertEquals(Duration.ofHours(1), TokenConfig.MAX_ACCESS_EXPIRATION)
        assertEquals(Duration.ofHours(1), TokenConfig.MIN_REFRESH_EXPIRATION)
        assertEquals(Duration.ofDays(30), TokenConfig.MAX_REFRESH_EXPIRATION)
    }
}
