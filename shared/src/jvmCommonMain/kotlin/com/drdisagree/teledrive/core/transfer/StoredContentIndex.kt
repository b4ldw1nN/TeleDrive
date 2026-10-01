package com.drdisagree.teledrive.core.transfer

import com.drdisagree.teledrive.data.local.dao.StoredContent
import java.io.File

/**
 * The fingerprints of everything the drive already holds, read once per backup
 * so a second copy of the same bytes is recognised without a query per file.
 */
object StoredContentIndex {

    fun bySize(rows: List<StoredContent>): Map<Long, List<StoredContent>> =
        rows.groupBy { it.sizeBytes }

    /**
     * Hashes of stored content that matches this file's size but sits at another
     * path. A repeated download leaves the previous file on disk with nothing
     * pointing at it, and every such orphan shares the hash of the one copy the
     * drive already holds.
     */
    fun hashesFor(candidate: File, bySize: Map<Long, List<StoredContent>>): Set<String> =
        bySize[candidate.length()]
            .orEmpty()
            .asSequence()
            .filterNot { it.localPath == candidate.absolutePath }
            .map { it.contentHash }
            .toSet()
}
