package com.drdisagree.teledrive.core.telegram

/**
 * TDLib hands out two families of remote id, and `getRemoteFile` rejects one
 * when asked for the other. Photo ids are recognisable by their prefix, which is
 * what lets the right file type be requested first time.
 */
const val PHOTO_REMOTE_ID_PREFIX = "AgACAg"

fun isPhotoRemoteId(remoteFileId: String): Boolean =
    remoteFileId.startsWith(PHOTO_REMOTE_ID_PREFIX)
