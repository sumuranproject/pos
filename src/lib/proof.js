// Aturan retensi foto bukti QRIS (murni, tanpa dependensi React Native agar bisa diuji).
const DAY = 86400000;

/** Kapan bukti kadaluarsa: capturedAt + retensi SAAT INI (mengubah retensi di Pengaturan ikut berlaku ke bukti lama). */
export const proofExpiresAt = (proof, retentionDays) => (proof?.capturedAt || 0) + Math.max(1, retentionDays || 7) * DAY;

/** Masih punya file/foto yang bisa dihapus? */
export const hasProofFile = (proof) => Boolean(proof && (proof.dataUrl || proof.path) && !proof.purgedAt);

export const isProofExpired = (proof, retentionDays, now = Date.now()) => hasProofFile(proof) && proof.capturedAt && now >= proofExpiresAt(proof, retentionDays);

/** Transaksi yang buktinya sudah lewat retensi. */
export const expiredProofTx = (transactions, retentionDays, now = Date.now()) => (transactions || []).filter((t) => isProofExpired(t.qrisProof, retentionDays, now));

/** Bentuk bukti setelah dihapus: metadata tetap ada (kapan diambil), foto/path hilang. */
export const purgedProof = (proof, now = Date.now()) => ({ capturedAt: proof.capturedAt, deviceModel: proof.deviceModel, purgedAt: now });
