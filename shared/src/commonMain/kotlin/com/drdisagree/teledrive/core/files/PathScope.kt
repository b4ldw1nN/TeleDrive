package com.drdisagree.teledrive.core.files

object PathScope {

    fun inside(path: String, folder: String): Boolean {
        val root = folder.replace('\\', '/').trimEnd('/').lowercase()
        if (root.isEmpty()) return false
        val target = path.replace('\\', '/').lowercase()
        return target == root || target.startsWith("$root/")
    }
}
