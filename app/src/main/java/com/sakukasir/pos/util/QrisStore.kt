package com.sakukasir.pos.util

import com.sakukasir.pos.domain.QrisProof
import java.io.File

class QrisStore {
    fun uploadWhenOnline(proof: QrisProof): QrisProof = proof

    /** Local retention cleanup. Cloud upload remains a backend integration boundary until real Firebase credentials are supplied. */
    fun cleanupExpired(localDir: File, now: Long = System.currentTimeMillis()): Int {
        if (!localDir.exists()) return 0
        var deleted = 0
        localDir.listFiles()?.forEach { file ->
            if (file.isFile && file.lastModified() > 0 && now - file.lastModified() >= 35L * 24 * 60 * 60 * 1000) {
                if (file.delete()) deleted++
            }
        }
        return deleted
    }
}
