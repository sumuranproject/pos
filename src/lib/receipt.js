// Satu sumber tata letak struk: dipakai preview (Pengaturan > Struk), tampilan struk di layar, dan printer ESC/POS.
// Terinspirasi struk resto: baris kecil di atas nama, nama besar, alamat/telepon, item, jumlah barang, total.
export const RECEIPT_LINE = '-'.repeat(32);

export const DEFAULT_RECEIPT = {
  bizName: '', tagline: '', footer: 'Terima kasih sudah berbelanja',
  showTagline: false, showAddress: true, showPhone: true, showOutlet: true, showTxNumber: true, showDateTime: true, showCashier: true,
  showItemPrice: true, showSubtotal: true, showItemCount: false, showPayment: true, showChange: true, showFooter: true,
};
/** Gabungkan dengan default supaya pengaturan lama (tanpa kunci baru) tetap jalan. */
export const withDefaults = (r) => ({ ...DEFAULT_RECEIPT, ...(r || {}) });

const fmt = (n) => Math.round(n).toLocaleString('id-ID');
/** Nama outlet hanya ditampilkan bila berbeda dari nama bisnis di header (hindari baris dobel). */
export const showOutletLine = (r, outlet) => Boolean(r.showOutlet && outlet && outlet.trim().toLowerCase() !== String(r.bizName || '').trim().toLowerCase());

/**
 * Blok struk: { k:'text', s, align?, bold?, big? } | { k:'row', l, r, bold? } | { k:'line' }.
 * info = { address, phone } milik outlet transaksi.
 */
export function receiptBlocks(tx, r0, info = {}) {
  const r = withDefaults(r0);
  const B = [];
  const text = (s, o = {}) => { if (s) B.push({ k: 'text', s: String(s), ...o }); };
  const row = (l, rr, o = {}) => B.push({ k: 'row', l, r: rr, ...o });
  const line = () => B.push({ k: 'line' });

  if (r.showTagline) text(r.tagline, { align: 'center' });
  text(r.bizName, { align: 'center', bold: true, big: true });
  if (r.showAddress) text(info.address, { align: 'center' });
  if (r.showPhone && info.phone) text('Telp ' + info.phone, { align: 'center' });
  line();
  if (r.showTxNumber) text(tx.id);
  if (r.showDateTime) text(`${tx.date} ${tx.time}`);
  if (showOutletLine(r, tx.outlet)) text(tx.outlet);
  if (r.showCashier) text('Kasir: ' + tx.cashier);
  line();
  tx.items.forEach((i) => {
    if (r.showItemPrice) { text(i.name); row(`  ${i.qty} x ${fmt(i.price)}`, fmt(i.price * i.qty)); }
    else row(`${i.qty} ${i.name}`, fmt(i.price * i.qty));
  });
  line();
  if (r.showSubtotal) {
    row('Subtotal', fmt(tx.items.reduce((s, i) => s + i.price * i.qty, 0)));
    if (tx.discount) row('Diskon', '-' + fmt(tx.discount));
    if (tx.tax) row(`Pajak ${tx.taxPct}%`, fmt(tx.tax));
  }
  if (r.showItemCount) row('Jml barang', String(tx.items.reduce((s, i) => s + i.qty, 0)));
  row('TOTAL', fmt(tx.total), { bold: true });
  if (r.showPayment) row(tx.method === 'cash' ? 'Cash' : 'QRIS', fmt(tx.received || tx.total));
  if (r.showChange && tx.method === 'cash') row('Kembali', fmt(tx.change));
  if (tx.status === 'void') text('*** VOID ***', { align: 'center', bold: true });
  if (tx.status === 'refunded') text('*** REFUND ***', { align: 'center', bold: true });
  if (tx.status === 'partial_refund') text('*** REFUND SEBAGIAN ***', { align: 'center', bold: true });
  if (r.showFooter && r.footer) { line(); text(r.footer, { align: 'center' }); }
  return B;
}

const center = (s, cols) => (s.length >= cols ? s : ' '.repeat(Math.floor((cols - s.length) / 2)) + s);
function rowText(l, rr, cols) {
  const room = cols - rr.length - 1;
  if (l.length <= room) return [l + ' '.repeat(cols - l.length - rr.length) + rr];
  return [l, ' '.repeat(Math.max(0, cols - rr.length)) + rr];
}
/** Render blok jadi teks monospace (preview). Nama besar di printer = 2x lebar, di preview tetap 1x. */
export function blocksToText(blocks, cols = 32) {
  const L = [];
  blocks.forEach((b) => {
    if (b.k === 'line') L.push('-'.repeat(cols));
    else if (b.k === 'row') L.push(...rowText(b.l, b.r, cols));
    else L.push(b.align === 'center' ? center(b.s, cols) : b.s);
  });
  return L.join('\n');
}

const SAMPLE = {
  id: 'TRX-20261006-0042', date: '2026-10-06', time: '14:32', cashier: 'Andi',
  items: [{ name: 'Kopi Susu', qty: 2, price: 18000 }, { name: 'Roti Bakar Coklat', qty: 1, price: 15000 }],
  discount: 0, tax: 0, total: 51000, method: 'cash', received: 60000, change: 9000, status: 'completed',
};
/** Preview contoh di Pengaturan > Struk. outlet = { name, address, phone }. */
export function sampleReceiptText(r, outlet = {}, cols = 32) {
  return blocksToText(receiptBlocks({ ...SAMPLE, outlet: outlet.name }, r, outlet), cols);
}
/** Struk nyata satu transaksi (tampilan layar). */
export function transactionReceiptText(tx, r, cols = 32, info = {}) {
  return blocksToText(receiptBlocks(tx, r, info), cols);
}
