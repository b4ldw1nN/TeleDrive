package com.drdisagree.teledrive.core.telegram

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteFileTypeTest {

    @Test
    fun `a photo id is recognised`() {
        assertTrue(isPhotoRemoteId("AgACAgUAAyEFAATuN2S1AAIFjGq8vh-KQWj8aKAPhvz64T1kWhAGAAKcGGsbbgPhVTS-KMt98RBCAQADAgADeQADOAQ"))
    }

    @Test
    fun `a document id is not a photo`() {
        assertFalse(isPhotoRemoteId("BAACAgUAAyEFAATuN2S1AAIFmWq8vh_qulYW2DbGW3w3XtBE5_XtAAIBJgACbgPhVfIv08rIpo_qOAQ"))
    }

    @Test
    fun `an empty id falls back to a document`() {
        assertFalse(isPhotoRemoteId(""))
    }

    @Test
    fun `an image stored as a document is not a photo`() {
        assertFalse(isPhotoRemoteId("BQACAgUAAyEGAAMBB4v3fQADNGq80pwV8jd6JfhR6udThpr1kAMcAALGIQAC4rTgVcT3W_dJyZt_OAQ"))
    }

    @Test
    fun `a unique id is not mistaken for a remote id`() {
        assertFalse(isPhotoRemoteId("AgAD1CAAAmsw2FU"))
        assertFalse(isPhotoRemoteId("AQADSxRrG2sw0FV8"))
    }
}
