package com.sonature.auth.domain.token.model

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AlgorithmTest {

    @Test
    fun `fromValue should return HS256 for HS256 string`() {
        val algorithm = Algorithm.fromValue("HS256")
        assertEquals(Algorithm.HS256, algorithm)
        assertTrue(algorithm.isSymmetric)
    }

    @Test
    fun `fromValue should return RS256 for RS256 string case insensitive`() {
        val algorithm = Algorithm.fromValue("rs256")
        assertEquals(Algorithm.RS256, algorithm)
        assertFalse(algorithm.isSymmetric)
    }

    @Test
    fun `fromValue should return PASETO_V4_LOCAL for v4 local string`() {
        val algorithm = Algorithm.fromValue("v4.local")
        assertEquals(Algorithm.PASETO_V4_LOCAL, algorithm)
        assertTrue(algorithm.isSymmetric)
    }

    @Test
    fun `fromValue should return PASETO_V4_PUBLIC for v4 public string`() {
        val algorithm = Algorithm.fromValue("v4.public")
        assertEquals(Algorithm.PASETO_V4_PUBLIC, algorithm)
        assertFalse(algorithm.isSymmetric)
    }

    @Test
    fun `fromValue should throw IllegalArgumentException for unknown algorithm`() {
        assertThrows<IllegalArgumentException> {
            Algorithm.fromValue("UNKNOWN")
        }
    }

    @Test
    fun `algorithm value should match expected string`() {
        assertEquals("HS256", Algorithm.HS256.value)
        assertEquals("RS256", Algorithm.RS256.value)
        assertEquals("v4.local", Algorithm.PASETO_V4_LOCAL.value)
        assertEquals("v4.public", Algorithm.PASETO_V4_PUBLIC.value)
    }
}
