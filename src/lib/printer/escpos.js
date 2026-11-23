// Pembuat perintah ESC/POS untuk printer thermal 58mm (32 kolom) / 80mm (48 kolom).
// Teks dipaksa ASCII agar aman di semua printer & transport (tanpa code page).
const ESC = 0x1b; const GS = 0x1d; const LF = 0x0a;

const MAP = { '─': '-', '—': '-', '–': '-', '×': 'x', '·': '-', '•': '*', '’': "'", '‘': "'", '“': '"', '”': '"', '…': '...' };
export function toAscii(s) {
  return String(s ?? '')
    .replace(/[─—–×·•’‘“”…]/g, (c) => MAP[c])
    .normalize('NFD').replace(/[̀-ͯ]/g, '')
    .replace(/[^\x20-\x7e]/g, '?');
}

export class EscPos {
  constructor(cols = 32) { this.cols = cols; this.buf = []; this.init(); }
  raw(...b) { this.buf.push(...b); return this; }
  init() { return this.raw(ESC, 0x40); }
  align(a) { return this.raw(ESC, 0x61, { left: 0, center: 1, right: 2 }[a] ?? 0); }
  bold(on) { return this.raw(ESC, 0x45, on ? 1 : 0); }
  /** 1 = normal, 2 = tinggi & lebar ganda */
  size(n) { return this.raw(GS, 0x21, n === 2 ? 0x11 : 0x00); }
  text(s = '') { for (const ch of toAscii(s)) this.buf.push(ch.charCodeAt(0)); return this.raw(LF); }
  line(ch = '-') { return this.text(ch.repeat(this.cols)); }
  /** kiri & kanan dalam satu baris; kiri dipotong/dibungkus bila terlalu panjang */
  row(left, right = '') {
    left = toAscii(left); right = toAscii(right);
    const room = this.cols - right.length - 1;
    if (left.length <= room) return this.text(left + ' '.repeat(this.cols - left.length - right.length) + right);
    this.text(left);
    return this.text(' '.repeat(Math.max(0, this.cols - right.length)) + right);
  }
  feed(n = 3) { return this.raw(ESC, 0x64, n); }
  cut() { return this.raw(GS, 0x56, 0x42, 0x00); }
  bytes() { return Uint8Array.from(this.buf); }
}

const fmt = (n) => Math.round(n).toLocaleString('id-ID');

/** Struk transaksi sesuai pengaturan Struk (r = state.settings.receipt). */
export function buildReceipt(tx, r, cols = 32) {
  const p = new EscPos(cols);
  p.align('center').bold(true).size(2).text(r.bizName).size(1).bold(false);
  p.align('left').line();
  if (r.showTxNumber) p.text(tx.id);
  p.text(`${tx.date} ${tx.time}`);
  if (r.showOutlet && tx.outlet && tx.outlet.trim().toLowerCase() !== String(r.bizName || '').trim().toLowerCase()) p.text(tx.outlet);
  if (r.showCashier) p.text('Kasir: ' + tx.cashier);
  p.line();
  tx.items.forEach((i) => { p.text(i.name); p.row(`  ${i.qty} x ${fmt(i.price)}`, fmt(i.price * i.qty)); });
  p.line();
  p.row('Subtotal', fmt(tx.items.reduce((s, i) => s + i.price * i.qty, 0)));
  if (tx.discount) p.row('Diskon', '-' + fmt(tx.discount));
  if (tx.tax) p.row(`Pajak ${tx.taxPct}%`, fmt(tx.tax));
  p.bold(true).row('TOTAL', fmt(tx.total)).bold(false);
  if (r.showPayment) p.row(tx.method === 'cash' ? 'Cash' : 'QRIS', fmt(tx.received || tx.total));
  if (r.showChange && tx.method === 'cash') p.row('Kembali', fmt(tx.change));
  if (tx.status === 'void') p.align('center').bold(true).text('*** VOID ***').bold(false).align('left');
  if (tx.status === 'refunded') p.align('center').bold(true).text('*** REFUND ***').bold(false).align('left');
  if (tx.status === 'partial_refund') p.align('center').bold(true).text('*** REFUND SEBAGIAN ***').bold(false).align('left');
  p.line();
  p.align('center').text(r.footer).feed(4).cut();
  return p.bytes();
}

export function buildTestPage(name, cols = 32) {
  const p = new EscPos(cols);
  p.align('center').bold(true).size(2).text('SakuKasir').size(1).bold(false);
  p.text('Test print berhasil').line();
  p.align('left').text('Printer : ' + (name || '-'));
  p.text('Kolom   : ' + cols);
  p.text('Waktu   : ' + new Date().toLocaleString('id-ID'));
  p.line().text('ABCDEFGHIJKLMNOPQRSTUVWXYZ').text('abcdefghijklmnopqrstuvwxyz').text('0123456789 Rp 1.234.567');
  p.feed(4).cut();
  return p.bytes();
}
