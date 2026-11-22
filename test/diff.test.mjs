import test from 'node:test';
import assert from 'node:assert/strict';
import { computeOps, coalesce, ser } from '../src/lib/diff.js';

test('item baru → upsert', () => {
  const { ops } = computeOps(new Map(), [{ id: 1, name: 'A' }]);
  assert.deepEqual(ops.map((o) => [o.id, o.op]), [['1', 'upsert']]);
});
test('tidak berubah → tidak ada operasi', () => {
  const prev = new Map([['1', ser({ id: 1, name: 'A' })]]);
  assert.equal(computeOps(prev, [{ id: 1, name: 'A' }]).ops.length, 0);
});
test('field sync tidak memicu upsert', () => {
  const prev = new Map([['1', ser({ id: 1, sync: 'pending' })]]);
  assert.equal(computeOps(prev, [{ id: 1, sync: 'synced' }]).ops.length, 0);
});
test('berubah → upsert, hilang → delete', () => {
  const prev = new Map([['1', ser({ id: 1, n: 1 })], ['2', ser({ id: 2 })]]);
  const { ops } = computeOps(prev, [{ id: 1, n: 2 }]);
  assert.deepEqual(ops.map((o) => [o.id, o.op]).sort(), [['1', 'upsert'], ['2', 'delete']]);
});
test('coalesce: operasi terakhir menang', () => {
  let box = coalesce([], { table: 't', id: '1', op: 'upsert' });
  box = coalesce(box, { table: 't', id: '1', op: 'delete' });
  assert.deepEqual(box, [{ table: 't', id: '1', op: 'delete' }]);
});
