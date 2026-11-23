import test from 'node:test';
import assert from 'node:assert/strict';
import { proofExpiresAt, isProofExpired, expiredProofTx, purgedProof } from '../src/lib/proof.js';

const DAY = 86400000;
const now = 100 * DAY;
test('bukti kadaluarsa tepat setelah retensi', () => {
  const p = { capturedAt: now - 35 * DAY, path: 'b/1.jpg' };
  assert.equal(proofExpiresAt(p, 35), now);
  assert.equal(isProofExpired(p, 35, now), true);
  assert.equal(isProofExpired({ capturedAt: now - 34 * DAY, path: 'x' }, 35, now), false);
});
test('mengubah retensi ikut berlaku ke bukti lama', () => {
  const p = { capturedAt: now - 10 * DAY, dataUrl: 'data:' };
  assert.equal(isProofExpired(p, 35, now), false);
  assert.equal(isProofExpired(p, 7, now), true);
});
test('bukti yang sudah dihapus atau tanpa file tidak diproses lagi', () => {
  assert.equal(isProofExpired({ capturedAt: 1, purgedAt: 5 }, 1, now), false);
  assert.equal(isProofExpired({ capturedAt: 1 }, 1, now), false);
  assert.equal(isProofExpired(undefined, 1, now), false);
});
test('expiredProofTx hanya mengambil yang lewat retensi', () => {
  const tx = [{ id: 'a', qrisProof: { capturedAt: 1, path: 'p' } }, { id: 'b', qrisProof: { capturedAt: now, path: 'p' } }, { id: 'c' }];
  assert.deepEqual(expiredProofTx(tx, 35, now).map((t) => t.id), ['a']);
});
test('purgedProof membuang foto & path tapi menyisakan metadata', () => {
  const r = purgedProof({ capturedAt: 7, deviceModel: 'M', dataUrl: 'x', path: 'y', fileSize: 9 }, 50);
  assert.deepEqual(r, { capturedAt: 7, deviceModel: 'M', purgedAt: 50 });
});
