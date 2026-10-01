package com.drdisagree.teledrive.core.transfer

import com.drdisagree.teledrive.core.files.PathScope
import com.drdisagree.teledrive.domain.model.BackupState
import java.io.File

object LocalCopyRelease {

    const val HASH_LIMIT_BYTES = 512L * 1024 * 1024

    fun cleanupRequested(
        globalEnabled: Boolean,
        cleanupFolders: Collection<String>,
        localPath: String
    ): Boolean =
        globalEnabled || cleanupFolders.any { PathScope.inside(localPath, it) }

    fun remoteCopyStored(
        state: BackupState,
        messageId: Long?,
        remoteFileId: String?,
        remoteUniqueId: String?
    ): Boolean =
        state == BackupState.BACKED_UP &&
                messageId != null &&
                !remoteFileId.isNullOrBlank() &&
                !remoteUniqueId.isNullOrBlank()

    fun localFileUnchanged(
        file: File,
        uploadedSizeBytes: Long,
        uploadedHash: String?,
        hashOf: (File) -> String?
    ): Boolean {
        if (!file.isFile || file.length() != uploadedSizeBytes) return false
        if (file.length() > HASH_LIMIT_BYTES) return true
        val hash = uploadedHash ?: return false
        return hashOf(file) == hash
    }
}
