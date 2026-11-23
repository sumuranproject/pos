import test from 'node:test';
import assert from 'node:assert/strict';
import { receiptBlocks, sampleReceiptText, withDefaults } from '../src/lib/receipt.js';

const tx = { id: 'TRX-1', date: '2026-11-22', time: '10:00', outlet: 'Es Teh', cashier: 'Andi', discount: 0, tax: 0, total: 36000,
  items: [{ name: 'Kopi', qty: 2, price: 18000 }], method: 'cash', received: 50000, change: 14000, status: 'completed' };
const texts = (r, info) => receiptBlocks(tx, r, info).filter((b) => b.k === 'text').map((b) => b.s);

test('nama outlet sama dengan nama bisnis tidak dicetak dobel', () => {
  assert.equal(texts({ bizName: 'Es Teh' }).filter((s) => s === 'Es Teh').length, 1);
});
test('nama outlet berbeda tetap dicetak', () => assert.ok(texts({ bizName: 'Es Teh Group' }).includes('Es Teh')));
test('alamat & telepon mengikuti sakelar', () => {
  const info = { address: 'Jl. A 1', phone: '0311' };
  assert.ok(texts({ bizName: 'X' }, info).includes('Jl. A 1'));
  assert.ok(!texts({ bizName: 'X', showAddress: false, showPhone: false }, info).some((s) => s.includes('0311') || s === 'Jl. A 1'));
});
test('pengaturan lama tanpa kunci baru memakai default', () => assert.equal(withDefaults({ bizName: 'X' }).showAddress, true));
test('tidak ada teks BILL di struk', () => assert.ok(!sampleReceiptText({ bizName: 'X' }, {}).includes('BILL')));
test('jumlah barang hanya muncul bila diaktifkan', () => {
  assert.ok(!sampleReceiptText({ bizName: 'X' }, {}).includes('Jml barang'));
  assert.ok(sampleReceiptText({ bizName: 'X', showItemCount: true }, {}).includes('Jml barang'));
});
