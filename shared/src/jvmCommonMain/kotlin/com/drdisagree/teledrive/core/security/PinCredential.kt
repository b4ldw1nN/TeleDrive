package com.drdisagree.teledrive.core.security

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlin.io.encoding.Base64

/**
 * The app lock PIN, kept as a salted PBKDF2 hash. Verification is constant time
 * so a wrong PIN cannot be narrowed down by timing, and nothing about the PIN is
 * recoverable from what is stored.
 */
object PinCredential {

    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 120_000
    private const val KEY_BITS = 256
    private const val SALT_BYTES = 16

    fun create(pin: String): String {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        return "${Base64.encode(salt)}:${Base64.encode(derive(pin, salt))}"
    }

    fun verify(pin: String, stored: String): Boolean {
        val parts = stored.split(':')
        if (parts.size != 2) return false
        val salt = runCatching { Base64.decode(parts[0]) }.getOrNull() ?: return false
        val expected = runCatching { Base64.decode(parts[1]) }.getOrNull() ?: return false
        return MessageDigest.isEqual(derive(pin, salt), expected)
    }

    private fun derive(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}
