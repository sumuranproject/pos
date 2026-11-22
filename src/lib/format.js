// Helper murni (format angka, tanggal, periode laporan). Tidak menyentuh state/DOM.

export function rupiah(n) {
  const num = Math.round(n);
  const sign = num < 0 ? '-' : '';
  return sign + 'Rp ' + Math.abs(num).toLocaleString('id-ID');
}

export function calculateTotals(cart, discount = 0, taxPct = 0) {
  const subtotal = cart.reduce((s, i) => s + i.price * i.qty, 0);
  const disc = Math.min(Math.max(0, Math.round(discount)), subtotal);
  const afterDisc = subtotal - disc;
  const tax = Math.round(afterDisc * (taxPct / 100));
  return { subtotal, discount: disc, taxable: afterDisc, tax, total: afterDisc + tax, taxPct };
}

export const uid = (p) => p + '-' + Date.now().toString(36).slice(-6).toUpperCase();
export const nowTime = () => new Date().toLocaleTimeString('id-ID', { hour: '2-digit', minute: '2-digit' });
export const nowDateTime = () =>
  new Date().toLocaleString('id-ID', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' });
export const todayStr = () => new Date().toISOString().slice(0, 10);

export function dateStr(d) {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const dd = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${dd}`;
}

export function formatTimestamp(ts) {
  if (!ts) return '-';
  return new Date(ts).toLocaleString('id-ID', {
    day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit', second: '2-digit',
  });
}

export function txAgeMinutes(tx) {
  if (!tx.createdAt) return 0;
  return Math.floor((Date.now() - tx.createdAt) / 60000);
}

export function formatRupiahInput(value) {
  const digits = String(value).replace(/\D/g, '');
  if (!digits) return '';
  return parseInt(digits, 10).toLocaleString('id-ID');
}
export function parseRupiahInput(value) {
  const digits = String(value).replace(/\D/g, '');
  return digits ? parseInt(digits, 10) : 0;
}

export function periodRange(period, customFrom, customTo) {
  if (period === 'custom' && customFrom && customTo) return { from: customFrom, to: customTo };
  const now = new Date();
  if (period === 'all') return { from: '0000-01-01', to: '9999-12-31' };
  if (period === 'today') return { from: dateStr(now), to: dateStr(now) };
  if (period === 'week') {
    const start = new Date(now); start.setDate(now.getDate() - 6);
    return { from: dateStr(start), to: dateStr(now) };
  }
  if (period === 'month') {
    return {
      from: dateStr(new Date(now.getFullYear(), now.getMonth(), 1)),
      to: dateStr(new Date(now.getFullYear(), now.getMonth() + 1, 0)),
    };
  }
  if (period === 'lastmonth') {
    return {
      from: dateStr(new Date(now.getFullYear(), now.getMonth() - 1, 1)),
      to: dateStr(new Date(now.getFullYear(), now.getMonth(), 0)),
    };
  }
  return { from: dateStr(now), to: dateStr(now) };
}

export function getPrevPeriodRange(period, customFrom, customTo) {
  const now = new Date();
  if (period === 'today') {
    const y = new Date(now); y.setDate(now.getDate() - 1);
    return { from: dateStr(y), to: dateStr(y) };
  }
  if (period === 'week') {
    const start = new Date(now); start.setDate(now.getDate() - 13);
    const end = new Date(now); end.setDate(now.getDate() - 7);
    return { from: dateStr(start), to: dateStr(end) };
  }
  if (period === 'month') {
    return {
      from: dateStr(new Date(now.getFullYear(), now.getMonth() - 1, 1)),
      to: dateStr(new Date(now.getFullYear(), now.getMonth(), 0)),
    };
  }
  if (period === 'lastmonth') {
    return {
      from: dateStr(new Date(now.getFullYear(), now.getMonth() - 2, 1)),
      to: dateStr(new Date(now.getFullYear(), now.getMonth() - 1, 0)),
    };
  }
  if (period === 'custom' && customFrom && customTo) {
    const from = new Date(customFrom);
    const to = new Date(customTo);
    const days = Math.round((to - from) / 86400000) + 1;
    const prevTo = new Date(from); prevTo.setDate(prevTo.getDate() - 1);
    const prevFrom = new Date(prevTo); prevFrom.setDate(prevFrom.getDate() - days + 1);
    return { from: dateStr(prevFrom), to: dateStr(prevTo) };
  }
  if (period === 'all') return { from: '0000-01-01', to: '0000-01-01' };
  return { from: dateStr(now), to: dateStr(now) };
}

export const periodLabel = (p) =>
  ({ all: 'Semua', today: 'Hari ini', week: '7 hari', month: 'Bulan ini', lastmonth: 'Bulan lalu', custom: 'Custom' })[p] || 'Semua';

export function computeGrowth(current, previous) {
  if (previous === 0) {
    if (current === 0) return { pct: 0, direction: 'flat' };
    return { pct: 100, direction: 'up' };
  }
  const pct = ((current - previous) / previous) * 100;
  return { pct: Math.abs(pct), direction: pct > 0.5 ? 'up' : pct < -0.5 ? 'down' : 'flat' };
}
export const growthIcon = (dir) => (dir === 'up' ? '↑' : dir === 'down' ? '↓' : '·');

/** Penjualan bersih satu transaksi (total dikurangi refund). */
export const netOf = (t) => t.total - (t.refundAmount || 0);
