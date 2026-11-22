// Store global ringan: satu objek `state` yang dimutasi lalu di-`commit()`.
// Komponen berlangganan lewat `useApp()` dan render ulang setiap commit.
// Versi native: tanpa data contoh (semua data datang dari Supabase), penyimpanan lewat AsyncStorage.
import { Appearance, Platform } from 'react-native';
import { useSyncExternalStore } from 'react';
import { uid, nowTime } from './format';
import { kv } from './storage';

export const PERMISSIONS = [
  { id: 'pos',          label: 'Kasir',            desc: 'Buat transaksi penjualan' },
  { id: 'dashboard',    label: 'Laporan',          desc: 'Ringkasan & riwayat transaksi sendiri' },
  { id: 'transactions', label: 'Transaksi',        desc: 'Lihat riwayat transaksi sendiri' },
  { id: 'shift',        label: 'Shift',            desc: 'Mulai & tutup shift' },
  { id: 'expense',      label: 'Pengeluaran',      desc: 'Catat & kelola pengeluaran' },
  { id: 'refund',       label: 'Refund Transaksi', desc: 'Refund transaksi sendiri' },
  { id: 'printer',      label: 'Printer',          desc: 'Setup & cetak struk Bluetooth' },
  { id: 'sync',         label: 'Sinkronisasi',     desc: 'Status & manual sync' },
  { id: 'theme',        label: 'Ganti Tema',       desc: 'Terang / gelap' },
  { id: 'profile',      label: 'Profil',           desc: 'Lihat info akun sendiri' },
];

const emptyFilter = () => ({ period: 'all', user: 'all', dateFrom: '', dateTo: '' });

export const state = {
  // ---- data domain (diisi dari Supabase) ----
  user: null,
  pendingRegister: null,
  outlet: { id: 0, name: '' },
  outlets: [],
  theme: Appearance.getColorScheme() === 'dark' ? 'dark' : 'light',
  online: true,
  syncQueue: [],
  cart: [],
  activeShift: null,
  shiftHistory: [],
  auditLog: [],
  ownerTab: 'pos',
  activeView: 'pos',
  manageRoute: null,
  reportFilter: emptyFilter(),
  transactions: [],
  expenses: [],
  categories: [],
  products: [],
  workers: [],
  notifications: [],
  settings: {
    qris: { image: null, active: true, outlet: '', retentionDays: 35 },
    // printer disimpan per perangkat (bukan di server)
    printer: { connected: false, device: '', deviceId: '', transport: null, cols: 32 },
    receipt: { bizName: '', showOutlet: true, showTxNumber: true, showCashier: true, showPayment: true, showChange: true, footer: 'Terima kasih sudah berbelanja' },
    notifications: { lowStock: true, outOfStock: true, system: true },
    security: { voidWindowMinutes: 5, voidLimitCashier: 50000, refundLimitCashier: 50000, ownerPin: '', alertVoidPerDay: 5 },
  },

  // ---- state UI ----
  authView: 'login',   // login | register | setup | forgot | app
  sheet: null,         // { key, el }
  modal: null,         // { key, el }
  receipt: null,       // { key, el }
  imageViewer: null,   // uri gambar
  toasts: [],
  success: null,       // tx yang baru selesai (layar sukses)
  hideNav: false,
  pageSub: '',
  booting: true,
};

/** Kosongkan semua data bisnis (dipakai saat logout / ganti akun). */
export function resetData() {
  Object.assign(state, {
    outlets: [], categories: [], products: [], transactions: [], expenses: [], shiftHistory: [], auditLog: [],
    workers: [], notifications: [], syncQueue: [], cart: [], activeShift: null, outlet: { id: 0, name: '' },
    reportFilter: emptyFilter(),
  });
  state.settings.qris = { image: null, active: true, outlet: '', retentionDays: 35 };
  state.settings.security.ownerPin = '';
  state.settings.receipt.bizName = '';
}

// ---------- hydrate (async, dipanggil sekali saat app start) ----------
export async function hydrate() {
  const theme = await kv.get('sk-theme', null);
  if (theme === 'dark' || theme === 'light') state.theme = theme;
  const printer = await kv.getJson('sk-printer', null);
  if (printer) state.settings.printer = { ...state.settings.printer, ...printer, connected: false };
  commit();
}
export function savePrinterSettings() { kv.set('sk-printer', state.settings.printer); }

// ---------- shift aktif bertahan walau app ditutup ----------
const shiftKey = () => (state.user ? 'sk-shift:' + state.user.id : null);
let shiftTimer = null;
function persistShift() {
  const k = shiftKey();
  if (!k) return;
  if (state.activeShift) kv.set(k, state.activeShift); else kv.remove(k);
}
export async function restoreShift() {
  const k = shiftKey();
  if (!k) return;
  const s = await kv.getJson(k, null);
  if (s && !state.activeShift) { state.activeShift = s; commit(); }
}

// ---------- subscribe / commit ----------
let version = 0;
const listeners = new Set();
export const subscribe = (l) => { listeners.add(l); return () => listeners.delete(l); };
export function commit() {
  version++;
  listeners.forEach((l) => l());
  clearTimeout(shiftTimer);
  shiftTimer = setTimeout(persistShift, 400);
}
/** Jalankan mutasi lalu notifikasi semua komponen. */
export function update(fn) { fn(state); commit(); }
/** Hook: render ulang setiap state berubah. Mengembalikan objek state. */
export function useApp() {
  useSyncExternalStore(subscribe, () => version);
  return state;
}

// ---------- helper state ----------
export function hasPerm(id) {
  if (!state.user) return false;
  if (state.user.role === 'owner') return true;
  const p = state.user.permissions;
  return Array.isArray(p) ? p.includes(id) : false;
}
export const isOwner = () => state.user?.role === 'owner';
export const currentTab = () => (state.user?.role === 'cashier' ? state.activeView : state.ownerTab);

export function applyTheme(t) {
  state.theme = t;
  kv.set('sk-theme', t);
  commit();
}

// ---------- toast ----------
let toastSeq = 0;
export function toast(msg, type = '') {
  const id = ++toastSeq;
  state.toasts = [...state.toasts, { id, msg, type }];
  commit();
  setTimeout(() => { state.toasts = state.toasts.filter((t) => t.id !== id); commit(); }, 2600);
}

// ---------- audit & notifikasi ----------
export function pushAudit(type, data) {
  const entry = {
    id: uid('AUD'), type, at: Date.now(),
    by: state.user ? state.user.username : 'system',
    byName: state.user ? state.user.displayName : 'System',
    device: Platform.OS === 'ios' ? 'iOS' : 'Android',
    ...data,
  };
  state.auditLog.unshift(entry);
  return entry;
}
export function pushNotif(type, title, body) {
  state.notifications.unshift({ id: Date.now(), type, title, body, time: nowTime(), read: false });
}

// ---------- sheet / modal ----------
let overlaySeq = 0;
export function openSheet(el) { state.sheet = { key: ++overlaySeq, el }; commit(); }
export function closeSheet() { state.sheet = null; commit(); }
export function openModal(el) { state.modal = { key: ++overlaySeq, el }; commit(); }
export function closeModal() { state.modal = null; commit(); }
export function openReceiptModal(el) { state.receipt = { key: ++overlaySeq, el }; commit(); }
export function closeReceiptModal() { state.receipt = null; commit(); }
export function openImageViewer(src) { state.imageViewer = src; commit(); }
export function closeImageViewer() { state.imageViewer = null; commit(); }

// ---------- navigasi (berbasis state, tombol Back Android lewat tryCloseSomething) ----------
export function showAuthView(v) { state.authView = v; commit(); }

export function goTab(v) {
  if (state.user.role === 'cashier') state.activeView = v; else state.ownerTab = v;
  state.manageRoute = null;
  state.success = null; state.hideNav = false;
  commit();
}
export function goRoute(route) { state.manageRoute = route; commit(); }
export function backToManage() { state.manageRoute = null; commit(); }

export function goToManage() {
  if (!state.user) return;
  const owner = state.user.role === 'owner';
  const inManage = owner ? state.ownerTab === 'manage' : state.activeView === 'manage';
  state.success = null; state.hideNav = false;
  if (inManage && state.manageRoute) { state.manageRoute = null; return commit(); }
  if (inManage) {
    if (owner) state.ownerTab = 'pos';
    else state.activeView = hasPerm('pos') ? 'pos' : hasPerm('dashboard') ? 'dashboard' : 'manage';
    return commit();
  }
  if (owner) state.ownerTab = 'manage'; else state.activeView = 'manage';
  state.manageRoute = null;
  commit();
}

export function goToPrinterSettings() {
  if (!state.user) return;
  if (state.user.role === 'owner') {
    state.ownerTab = 'manage'; state.manageRoute = 'printer';
  } else {
    if (!hasPerm('printer')) return toast('Tidak ada akses printer', 'error');
    state.activeView = 'manage'; state.manageRoute = 'cashier-printer';
  }
  state.success = null; state.hideNav = false;
  commit();
}

/** Tombol Back Android: tutup lapisan teratas / naik satu level. true bila ada yang ditangani. */
export function tryCloseSomething() {
  if (state.imageViewer) { closeImageViewer(); return true; }
  if (state.sheet) { closeSheet(); return true; }
  if (state.modal) { closeModal(); return true; }
  if (state.receipt) { closeReceiptModal(); return true; }
  if (!state.user) {
    if (state.authView === 'setup') { showAuthView('register'); return true; }
    if (state.authView === 'register' || state.authView === 'forgot') { showAuthView('login'); return true; }
    return false;
  }
  if (state.success) { state.success = null; state.hideNav = false; commit(); return true; }
  if (state.manageRoute) { state.manageRoute = null; commit(); return true; }
  if (state.user.role === 'owner') {
    if (state.ownerTab !== 'pos') { state.ownerTab = 'pos'; commit(); return true; }
  } else {
    if (state.activeView === 'manage') { state.activeView = 'pos'; commit(); return true; }
    const def = hasPerm('pos') ? 'pos' : hasPerm('dashboard') ? 'dashboard' : 'pos';
    if (state.activeView !== def) { state.activeView = def; commit(); return true; }
  }
  return false;
}

// ---------- sesi ----------
export function enterApp() {
  state.authView = 'app';
  state.manageRoute = null;
  state.success = null; state.hideNav = false;
  if (state.user.role === 'cashier') {
    state.activeView = hasPerm('pos') ? 'pos' : hasPerm('dashboard') ? 'dashboard' : 'manage';
  } else {
    state.ownerTab = 'pos';
  }
  commit();
  restoreShift();
}

export function doLogout() {
  state.user = null;
  resetData();
  state.manageRoute = null;
  state.success = null; state.hideNav = false;
  state.authView = 'login';
  commit();
}

export function updateSyncStatus() { commit(); }
