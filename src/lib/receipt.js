// Pembuat teks struk (preview di Pengaturan Struk & cetak struk transaksi).
export const RECEIPT_LINE = '──────────────────────────';

/** Preview contoh di halaman Pengaturan > Struk. */
export function sampleReceiptText(r, outletName) {
  return `${r.bizName}
${RECEIPT_LINE}
${r.showTxNumber ? 'TRX-20261006-0042' : ''}
06 Okt 2026 · 14:32
${r.showOutlet ? outletName : ''}
${r.showCashier ? 'Kasir: Andi' : ''}
${RECEIPT_LINE}
Kopi Susu   2× 18.000
            36.000
${RECEIPT_LINE}
Subtotal        36.000
${r.showPayment ? 'Cash            50.000' : ''}
${r.showChange ? 'Kembali         14.000' : ''}
${RECEIPT_LINE}
${r.footer}`.trim();
}

/** Struk nyata untuk satu transaksi. */
export function transactionReceiptText(tx, r) {
  const lines = [r.bizName, RECEIPT_LINE];
  if (r.showTxNumber) lines.push(tx.id);
  lines.push(`${tx.date} · ${tx.time}`);
  if (r.showOutlet) lines.push(tx.outlet);
  if (r.showCashier) lines.push('Kasir: ' + tx.cashier);
  lines.push(RECEIPT_LINE);
  tx.items.forEach((i) => {
    lines.push(`${i.name}`);
    lines.push(`  ${i.qty}×${i.price}    ${i.price * i.qty}`);
  });
  lines.push(RECEIPT_LINE);
  const subtotal = tx.items.reduce((s, i) => s + i.price * i.qty, 0);
  lines.push(`Subtotal        ${subtotal}`);
  if (tx.discount) lines.push(`Diskon         -${tx.discount}`);
  if (tx.tax) lines.push(`Pajak ${tx.taxPct}%      ${tx.tax}`);
  lines.push(`Total           ${tx.total}`);
  if (r.showPayment) lines.push(`${tx.method === 'cash' ? 'Cash' : 'QRIS'}            ${tx.received || tx.total}`);
  if (r.showChange && tx.method === 'cash') lines.push(`Kembali         ${tx.change}`);
  if (tx.status === 'void') lines.push('*** VOID ***');
  if (tx.status === 'refunded') lines.push('*** REFUND ***');
  if (tx.status === 'partial_refund') lines.push('*** REFUND SEBAGIAN ***');
  lines.push(RECEIPT_LINE);
  lines.push(r.footer);
  return lines.join('\n');
}
