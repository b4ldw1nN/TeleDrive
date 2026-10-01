package com.drdisagree.teledrive.core.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinCredentialTest {

    @Test
    fun `the pin verifies against its own hash`() {
        assertTrue(PinCredential.verify("482913", PinCredential.create("482913")))
    }

    @Test
    fun `a wrong pin is rejected`() {
        assertFalse(PinCredential.verify("482914", PinCredential.create("482913")))
    }

    @Test
    fun `the pin itself is not recoverable from what is stored`() {
        val stored = PinCredential.create("482913")
        assertFalse(stored.contains("482913"))
    }

    @Test
    fun `each hash uses a fresh salt`() {
        assertNotEquals(PinCredential.create("482913"), PinCredential.create("482913"))
    }

    @Test
    fun `a hash from one pin does not verify another`() {
        val stored = PinCredential.create("482913")
        assertFalse(PinCredential.verify("111111", stored))
    }

    @Test
    fun `an empty pin is still a pin`() {
        assertTrue(PinCredential.verify("", PinCredential.create("")))
    }

    @Test
    fun `garbage in place of a hash is rejected instead of throwing`() {
        assertFalse(PinCredential.verify("482913", ""))
        assertFalse(PinCredential.verify("482913", "not-base64:also-not"))
        assertFalse(PinCredential.verify("482913", "a:b:c"))
    }
}
