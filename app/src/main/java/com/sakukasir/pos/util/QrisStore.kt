package com.sakukasir.pos.util
import com.sakukasir.pos.domain.QrisProof
class QrisStore {
    fun uploadWhenOnline(proof:QrisProof):QrisProof = proof
    // Backend contract: uploadQrisProof Edge Function -> Storage URL -> RTDB metadata.
}
