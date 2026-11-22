import test from 'node:test';
import assert from 'node:assert/strict';
import { EscPos, buildReceipt, toAscii } from '../src/lib/printer/escpos.js';

const tx = { id: 'TRX-1', date: '2026-11-22', time: '10:00', outlet: 'Toko', cashier: 'Andi', discount: 0, tax: 0, total: 36000,
  items: [{ name: 'Kopi', qty: 2, price: 18000 }], method: 'cash', received: 50000, change: 14000, status: 'completed' };
const rcp = { bizName: 'Toko', showOutlet: true, showTxNumber: true, showCashier: true, showPayment: true, showChange: true, footer: 'Terima kasih ×' };

test('toAscii menghapus aksen & simbol non-ASCII', () => assert.equal(toAscii('Café ─ ×'), 'Cafe - x'));
test('struk hanya berisi byte < 128 dan diawali ESC @', () => {
  const b = buildReceipt(tx, rcp, 32);
  assert.deepEqual([b[0], b[1]], [0x1b, 0x40]);
  assert.ok(b.every((x) => x < 128));
});
test('row() rata kanan tepat selebar kolom', () => {
  const p = new EscPos(32).row('Total', '36.000');
  const text = Buffer.from(p.bytes().slice(2, -1)).toString('latin1');
  assert.equal(text.length, 32);
  assert.ok(text.endsWith('36.000'));
});
test('struk berakhir dengan perintah potong', () => {
  const b = buildReceipt(tx, rcp, 32);
  assert.deepEqual([...b.slice(-4)], [0x1d, 0x56, 0x42, 0x00]);
});
