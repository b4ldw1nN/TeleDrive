package com.drdisagree.teledrive.data.local.dao

/** One stored file's fingerprint, enough to recognize the same bytes again. */
data class StoredContent(
    val sizeBytes: Long,
    val contentHash: String,
    val localPath: String?
)
