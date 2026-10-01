package com.drdisagree.teledrive.core.transfer

import com.drdisagree.teledrive.domain.model.BackupState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class LocalCopyReleaseTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `global setting asks for cleanup on its own`() {
        val asked = LocalCopyRelease.cleanupRequested(
            globalEnabled = true,
            cleanupFolders = emptySet(),
            localPath = "/sdcard/DCIM/a.jpg"
        )
        assertTrue(asked)
    }

    @Test
    fun `folder opt in asks for cleanup without the global setting`() {
        val asked = LocalCopyRelease.cleanupRequested(
            globalEnabled = false,
            cleanupFolders = setOf("/sdcard/DCIM"),
            localPath = "/sdcard/DCIM/camera/a.jpg"
        )
        assertTrue(asked)
    }

    @Test
    fun `another folder is not asked for cleanup`() {
        val asked = LocalCopyRelease.cleanupRequested(
            globalEnabled = false,
            cleanupFolders = setOf("/sdcard/DCIM"),
            localPath = "/sdcard/Movies/a.mp4"
        )
        assertFalse(asked)
    }

    @Test
    fun `remote copy needs a stored state and a full mapping`() {
        assertTrue(stored(BackupState.BACKED_UP, 7L, "remote", "unique"))
        assertFalse(stored(BackupState.UPLOADING, 7L, "remote", "unique"))
        assertFalse(stored(BackupState.BACKED_UP, null, "remote", "unique"))
        assertFalse(stored(BackupState.BACKED_UP, 7L, "", "unique"))
        assertFalse(stored(BackupState.BACKED_UP, 7L, "remote", null))
    }

    @Test
    fun `unchanged file passes`() {
        val file = file("photo.jpg", "bytes")
        assertTrue(unchanged(file, file.length(), "bytes"))
    }

    @Test
    fun `edited file is kept`() {
        val file = file("photo.jpg", "bytes")
        assertFalse(unchanged(file, file.length(), "other-hash"))
    }

    @Test
    fun `growing file is kept`() {
        val file = file("photo.jpg", "bytes plus more")
        assertFalse(unchanged(file, "bytes".length.toLong(), "bytes"))
    }

    @Test
    fun `missing file is kept`() {
        val file = File(temporaryFolder.root, "gone.jpg")
        assertFalse(unchanged(file, 5L, "bytes"))
    }

    @Test
    fun `file without a known hash is kept`() {
        val file = file("photo.jpg", "bytes")
        assertFalse(unchanged(file, file.length(), null))
    }

    private fun stored(
        state: BackupState,
        messageId: Long?,
        remoteFileId: String?,
        remoteUniqueId: String?
    ) = LocalCopyRelease.remoteCopyStored(state, messageId, remoteFileId, remoteUniqueId)

    private fun file(name: String, content: String): File =
        temporaryFolder.newFile(name).apply { writeText(content) }

    private fun unchanged(file: File, size: Long, hash: String?): Boolean =
        LocalCopyRelease.localFileUnchanged(file, size, hash) { it.readText() }
}
