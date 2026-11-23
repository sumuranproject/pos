// Teks struk untuk preview (Pengaturan > Struk) dan tampilan struk di layar.
// Tata letaknya sengaja sama dengan struk yang dicetak printer (lib/printer/escpos.js): ASCII, lebar tetap 32/48 kolom.
export const RECEIPT_LINE = '-'.repeat(32);

const fmt = (n) => Math.round(n).toLocaleString('id-ID');
const center = (s, cols) => (s.length >= cols ? s : ' '.repeat(Math.floor((cols - s.length) / 2)) + s);
/** kiri & kanan dalam satu baris; kalau kiri terlalu panjang, kanan turun ke baris berikutnya */
function row(left, right, cols) {
  const room = cols - right.length - 1;
  if (left.length <= room) return [left + ' '.repeat(cols - left.length - right.length) + right];
  return [left, ' '.repeat(Math.max(0, cols - right.length)) + right];
}
/** Nama outlet hanya ditampilkan bila berbeda dari nama bisnis di header (hindari baris dobel). */
export const showOutletLine = (r, outlet) => Boolean(r.showOutlet && outlet && outlet.trim().toLowerCase() !== String(r.bizName || '').trim().toLowerCase());

export function receiptLines(tx, r, cols = 32) {
  const line = '-'.repeat(cols);
  const L = [center(r.bizName || '', cols), line];
  if (r.showTxNumber) L.push(tx.id);
  L.push(`${tx.date} ${tx.time}`);
  if (showOutletLine(r, tx.outlet)) L.push(tx.outlet);
  if (r.showCashier) L.push('Kasir: ' + tx.cashier);
  L.push(line);
  tx.items.forEach((i) => { L.push(i.name); L.push(...row(`  ${i.qty} x ${fmt(i.price)}`, fmt(i.price * i.qty), cols)); });
  L.push(line);
  L.push(...row('Subtotal', fmt(tx.items.reduce((s, i) => s + i.price * i.qty, 0)), cols));
  if (tx.discount) L.push(...row('Diskon', '-' + fmt(tx.discount), cols));
  if (tx.tax) L.push(...row(`Pajak ${tx.taxPct}%`, fmt(tx.tax), cols));
  L.push(...row('TOTAL', fmt(tx.total), cols));
  if (r.showPayment) L.push(...row(tx.method === 'cash' ? 'Cash' : 'QRIS', fmt(tx.received || tx.total), cols));
  if (r.showChange && tx.method === 'cash') L.push(...row('Kembali', fmt(tx.change), cols));
  if (tx.status === 'void') L.push(center('*** VOID ***', cols));
  if (tx.status === 'refunded') L.push(center('*** REFUND ***', cols));
  if (tx.status === 'partial_refund') L.push(center('*** REFUND SEBAGIAN ***', cols));
  L.push(line);
  if (r.footer) L.push(center(r.footer, cols));
  return L;
}

/** Preview contoh di halaman Pengaturan > Struk. */
export function sampleReceiptText(r, outletName, cols = 32) {
  const tx = {
    id: 'TRX-20261006-0042', date: '2026-10-06', time: '14:32', outlet: outletName, cashier: 'Andi',
    items: [{ name: 'Kopi Susu', qty: 2, price: 18000 }], total: 36000, method: 'cash', received: 50000, change: 14000, status: 'completed',
  };
  return receiptLines(tx, r, cols).join('\n');
}

/** Struk nyata untuk satu transaksi (tampilan di layar). */
export function transactionReceiptText(tx, r, cols = 32) {
  return receiptLines(tx, r, cols).join('\n');
}
