package com.drdisagree.teledrive.core.transfer

import com.drdisagree.teledrive.data.local.dao.StoredContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class StoredContentIndexTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `a file the drive stores at another path is a duplicate`() {
        val candidate = file("photo (1).jpg", "bytes")
        val index = index(stored(candidate, path = "/other/photo (1) (1).jpg"))
        assertTrue("abc" in StoredContentIndex.hashesFor(candidate, index))
    }

    @Test
    fun `the entry that already points at the file is not a duplicate of it`() {
        val candidate = file("photo.jpg", "bytes")
        val index = StoredContentIndex.bySize(
            listOf(stored(size = candidate.length(), path = candidate.absolutePath))
        )
        assertTrue(StoredContentIndex.hashesFor(candidate, index).isEmpty())
    }

    @Test
    fun `same size but a different hash is not a duplicate`() {
        val candidate = file("photo.jpg", "bytes")
        val index = index(stored(size = candidate.length(), hash = "other"))
        assertFalse("abc" in StoredContentIndex.hashesFor(candidate, index))
    }

    @Test
    fun `a different size is not looked at`() {
        val candidate = file("photo.jpg", "bytes")
        val index = index(stored(size = candidate.length() + 1))
        assertTrue(StoredContentIndex.hashesFor(candidate, index).isEmpty())
    }

    @Test
    fun `an empty index matches nothing`() {
        assertTrue(StoredContentIndex.hashesFor(file("photo.jpg", "bytes"), emptyMap()).isEmpty())
    }

    @Test
    fun `every orphan of one stored file is recognized`() {
        val candidate = file("photo.jpg", "bytes")
        val index = index(
            stored(size = candidate.length(), path = "/other/photo (1) (1).jpg"),
            stored(size = candidate.length(), path = null)
        )
        assertEquals(setOf("abc"), StoredContentIndex.hashesFor(candidate, index))
    }

    private fun index(vararg rows: StoredContent) = StoredContentIndex.bySize(rows.toList())

    private fun file(name: String, content: String): File =
        temporaryFolder.newFile(name).apply { writeText(content) }

    private fun stored(
        size: Long = 5,
        hash: String = "abc",
        path: String? = null
    ) = StoredContent(sizeBytes = size, contentHash = hash, localPath = path)

    private fun stored(candidate: File, path: String?) =
        StoredContent(
            sizeBytes = candidate.length(),
            contentHash = "abc",
            localPath = path
        )
}
