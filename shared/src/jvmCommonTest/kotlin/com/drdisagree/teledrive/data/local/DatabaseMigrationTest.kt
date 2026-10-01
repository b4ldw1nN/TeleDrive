package com.drdisagree.teledrive.data.local

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.drdisagree.teledrive.data.local.database.ALL_MIGRATIONS
import com.drdisagree.teledrive.data.local.database.TeleDriveDatabase
import com.drdisagree.teledrive.data.local.entity.StorageChannelEntity
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Opening a database built by an older version of the app. Room validates the
 * schema it finds against the entities on open, so a migration that is missing
 * or does not produce the expected shape fails here instead of on a device.
 */
class DatabaseMigrationTest {

    @Test
    fun `a version 12 database upgrades and keeps its rows`() {
        val file = File.createTempFile("teledrive", ".db")
        try {
            runBlocking {
                createSchema(file, FROM_VERSION)

                val legacy = openDatabase(file)
                try {
                    legacy.storageChannelDao().upsert(channel(chatId = 42L, title = "My drive"))
                    legacy.storageChannelDao().upsert(channel(chatId = 43L, title = "Old group"))
                } finally {
                    legacy.close()
                }

                val upgraded = openDatabase(file)
                try {
                    val rows = upgraded.storageChannelDao().all()
                    assertEquals(2, rows.size)
                    assertEquals("My drive", rows.first { it.chatId == 42L }.title)
                } finally {
                    upgraded.close()
                }
            }
        } finally {
            file.delete()
        }
    }

    @Test
    fun `the latest migration swaps the mislaid remote ids`() {
        val file = File.createTempFile("teledrive", ".db")
        try {
            runBlocking {
                createSchema(file, SWAPPED_VERSION)
                seedMislaidIds(file)

                val upgraded = openDatabase(file)
                try {
                    val swapped = upgraded.fileDao().byId("f1")
                    assertEquals(REMOTE_ID, swapped?.remoteFileId)
                    assertEquals(STABLE_ID, swapped?.remoteUniqueId)

                    val halfKnown = upgraded.fileDao().byId("f2")
                    assertEquals(STABLE_ID, halfKnown?.remoteFileId)
                    assertNull(halfKnown?.remoteUniqueId)
                } finally {
                    upgraded.close()
                }
            }
        } finally {
            file.delete()
        }
    }

    /**
     * Written straight to the old schema, because opening the file through Room
     * would migrate it before the rows were in place and the test would prove
     * nothing.
     */
    private suspend fun seedMislaidIds(file: File) {
        val connection = BundledSQLiteDriver().open(file.absolutePath)
        listOf(
            Triple("f1", STABLE_ID, REMOTE_ID),
            Triple("f2", STABLE_ID, null)
        ).forEach { (id, fileId, uniqueId) ->
            connection.execSQL(
                """INSERT INTO files
                   (id, folderId, name, sizeBytes, mimeType, category, localPath,
                    contentHash, chatId, messageId, remoteFileId, remoteUniqueId,
                    backupState, isHidden, isArchived, isFavorite, isPinned,
                    isEncrypted, trashedAt, preTrashFolderId, pendingPublish,
                    partCount, iconFileId, width, height, durationMs,
                    createdAt, modifiedAt, addedAt)
                   VALUES ('$id', NULL, 'video.mp4', 1024, 'video/mp4', 'VIDEO',
                    NULL, NULL, 1, 7, '$fileId', ${uniqueId?.let { "'$it'" } ?: "NULL"},
                    'BACKED_UP', 0, 0, 0, 0, 0, NULL, NULL, 0, 0, NULL,
                    NULL, NULL, NULL, 1, 1, 1)"""
            )
        }
        connection.close()
    }

    private suspend fun openDatabase(file: File): TeleDriveDatabase =
        Room.databaseBuilder<TeleDriveDatabase>(name = file.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .addMigrations(*ALL_MIGRATIONS)
            .build()

    private fun channel(chatId: Long, title: String) = StorageChannelEntity(
        chatId = chatId,
        title = title,
        addedAt = 1L,
        lastOpenedAt = 1L
    )

    /** Rebuilds the schema of [version] straight from the exported Room schema. */
    private suspend fun createSchema(file: File, version: Int) {
        val connection = BundledSQLiteDriver().open(file.absolutePath)
        val json = Json.parseToJsonElement(schemaFile(version).readText())
            .jsonObject["database"]!!
            .jsonObject
        json["entities"]!!.jsonArray.forEach { entity ->
            val table = entity.jsonObject["tableName"]!!.jsonPrimitive.content
            connection.execSQL(resolve(entity.jsonObject["createSql"]!!, table))
            entity.jsonObject["indices"]?.jsonArray.orEmpty().forEach { index ->
                connection.execSQL(resolve(index.jsonObject["createSql"]!!, table))
            }
        }
        json["setupQueries"]!!.jsonArray.forEach { setup ->
            connection.execSQL(setup.jsonPrimitive.content)
        }
        connection.execSQL("PRAGMA user_version = $version")
        connection.close()
    }

    private fun resolve(sql: JsonElement, table: String): String =
        sql.jsonPrimitive.content.replace(TABLE_NAME_PLACEHOLDER, table)

    private fun schemaFile(version: Int): File {
        val name = "com.drdisagree.teledrive.data.local.database.TeleDriveDatabase/$version.json"
        val direct = File("schemas/$name")
        if (direct.isFile) return direct
        var dir: File? = File(".").absoluteFile
        while (dir != null) {
            val found = File(dir, "shared/schemas/$name")
            if (found.isFile) return found
            dir = dir.parentFile
        }
        error("Exported schema for version $version not found near ${File(".").absolutePath}")
    }

    private companion object {
        const val FROM_VERSION = 12
        const val SWAPPED_VERSION = 14
        const val TABLE_NAME_PLACEHOLDER = "\${TABLE_NAME}"
        const val STABLE_ID = "AgAD1SAAAmsw2FU"
        const val REMOTE_ID = "BQACAgUAAyEFAAMBC6dFxAACBztq"
    }
}
