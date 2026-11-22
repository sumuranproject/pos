// Sinkronisasi offline-first state <-> Supabase (backend yang sama dengan versi web).
//  - UI memutasi `state` lalu commit(); perubahan dihitung (diff per id) dan masuk "outbox".
//  - Outbox disimpan di AsyncStorage, dikirim saat online (upsert/delete idempoten, id dibuat klien).
//  - Saat outbox kosong, data ditarik ulang berkala supaya perubahan dari perangkat lain muncul (last-write-wins).
import NetInfo from '@react-native-community/netinfo';
import { decode as b64decode } from 'base64-arraybuffer';
import { state, commit, subscribe, toast, closeSheet } from './store';
import { supabase } from './supabase';
import { computeOps, coalesce, ser } from './diff';
import { kv } from './storage';

const COLLECTIONS = [
  ['outlets', 'outlets'], ['categories', 'categories'], ['products', 'products'], ['transactions', 'transactions'],
  ['expenses', 'expenses'], ['shiftHistory', 'shifts'], ['auditLog', 'audit_log'], ['notifications', 'notifications'],
];
const SETTINGS_TABLE = 'settings';

let businessId = null;
let userKey = '';
let snapshots = {};       // table -> Map(id -> json)
let outbox = [];          // { table, id, op: 'upsert'|'delete', row? }
let unsub = null;
let timers = [];
let flushing = false;
let flushTimer = null;
let diffTimer = null;
let pulling = false;
let started = false;
let netOnline = true;
let forcedOffline = false;

const outboxKey = () => `sk-outbox:${userKey}`;

/** Bagian settings yang disinkron (PIN & printer tidak ikut). */
export function syncableSettings() {
  const { qris, receipt, notifications, security } = state.settings;
  const { ownerPin, ...sec } = security; // eslint-disable-line no-unused-vars
  return { qris, receipt, notifications, security: sec };
}

function persistOutbox() { kv.set(outboxKey(), outbox); }
function publishQueue() {
  if (state.syncQueue.length !== outbox.length) { state.syncQueue = [...outbox]; commit(); }
}
function enqueue(op) { outbox = coalesce(outbox, op); }

function diffAndQueue() {
  if (!businessId) return;
  let changed = false;
  for (const [key, table] of COLLECTIONS) {
    const { ops, next } = computeOps(snapshots[table] || new Map(), state[key] || []);
    snapshots[table] = next;
    for (const o of ops) {
      changed = true;
      enqueue(o.op === 'upsert'
        ? { table, id: o.id, op: 'upsert', row: { id: o.id, business_id: businessId, data: { ...o.item, sync: undefined } } }
        : { table, id: o.id, op: 'delete' });
    }
  }
  const sj = JSON.stringify(syncableSettings());
  if (snapshots[SETTINGS_TABLE]?.get('main') !== sj) {
    snapshots[SETTINGS_TABLE] = new Map([['main', sj]]);
    changed = true;
    enqueue({ table: SETTINGS_TABLE, id: 'main', op: 'upsert', row: { business_id: businessId, data: JSON.parse(sj) } });
  }
  if (changed) { persistOutbox(); publishQueue(); scheduleFlush(); }
}

function scheduleFlush(ms = 300) { clearTimeout(flushTimer); flushTimer = setTimeout(flush, ms); }

const isNetworkError = (e) => /fetch|network|timeout|offline/i.test(e?.message || '');

export async function flush() {
  if (!supabase || !businessId || flushing || !state.online || !outbox.length) return;
  flushing = true;
  try {
    const batch = [...outbox];
    const done = new Set();
    for (const table of new Set(batch.map((o) => o.table))) {
      const ups = batch.filter((o) => o.table === table && o.op === 'upsert');
      const dels = batch.filter((o) => o.table === table && o.op === 'delete');
      if (table === 'transactions') for (const o of ups) await attachProof(o);
      if (ups.length) {
        const { error } = await supabase.from(table).upsert(ups.map((o) => o.row), { onConflict: table === SETTINGS_TABLE ? 'business_id' : 'business_id,id' });
        if (error) { if (isNetworkError(error)) throw error; toast('Gagal sinkron ' + table + ': ' + error.message, 'error'); }
        ups.forEach((o) => done.add(o)); // error non-jaringan (mis. RLS) -> buang agar tidak macet selamanya
      }
      if (dels.length) {
        const { error } = await supabase.from(table).delete().eq('business_id', businessId).in('id', dels.map((o) => o.id));
        if (error) { if (isNetworkError(error)) throw error; toast('Gagal hapus ' + table + ': ' + error.message, 'error'); }
        dels.forEach((o) => done.add(o));
      }
    }
    outbox = outbox.filter((o) => !done.has(o));
    for (const o of batch) {
      if (o.table === 'transactions' && o.op === 'upsert') {
        const tx = state.transactions.find((t) => String(t.id) === o.id);
        if (tx) tx.sync = 'synced';
      }
    }
    persistOutbox();
    state.syncQueue = [...outbox];
    commit();
  } catch { /* offline: coba lagi nanti */ } finally { flushing = false; }
}

/** Unggah foto bukti QRIS ke Storage, lalu ganti data URL di dokumen dengan path-nya. */
async function attachProof(o) {
  const pr = o.row.data.qrisProof;
  if (!pr?.dataUrl) return;
  const path = `${businessId}/${o.id}.jpg`;
  const base64 = pr.dataUrl.split(',')[1] || '';
  const { error } = await supabase.storage.from('qris-proofs').upload(path, b64decode(base64), { upsert: true, contentType: 'image/jpeg' });
  if (error) {
    if (isNetworkError(error)) throw error;
    toast('Gagal unggah bukti QRIS: ' + error.message, 'error');
    return;
  }
  o.row.data = { ...o.row.data, qrisProof: { ...pr, dataUrl: undefined, path } };
  persistOutbox();
}

async function fetchTable(table, order = 'updated_at', limit = 5000) {
  const { data, error } = await supabase.from(table).select('data').eq('business_id', businessId).order(order, { ascending: true }).limit(limit);
  if (error) throw error;
  return data.map((r) => r.data);
}

export async function loadWorkers() {
  const { data, error } = await supabase.from('profiles').select('*').eq('business_id', businessId).eq('role', 'cashier').order('created_at');
  if (error) throw error;
  state.workers = data.map((p) => ({
    id: p.id, username: p.username, displayName: p.display_name, outlet: p.outlet || '', whatsapp: p.whatsapp || '',
    active: p.active, permissions: p.permissions || [],
  }));
}

/** Tarik semua data bisnis dari Supabase ke state. */
export async function loadAll() {
  const results = await Promise.all(COLLECTIONS.map(([, t]) => fetchTable(t)));
  COLLECTIONS.forEach(([key], i) => { state[key] = results[i]; });
  state.transactions.forEach((t) => { t.sync = 'synced'; });
  state.transactions.sort((a, b) => (b.createdAt || 0) - (a.createdAt || 0));
  state.auditLog.sort((a, b) => (b.at || 0) - (a.at || 0));
  state.notifications.sort((a, b) => (b.id || 0) - (a.id || 0));
  state.shiftHistory.reverse();

  const { data: s } = await supabase.from(SETTINGS_TABLE).select('data').eq('business_id', businessId).maybeSingle();
  if (s?.data) {
    const d = s.data;
    state.settings.qris = { ...state.settings.qris, ...d.qris };
    state.settings.receipt = { ...state.settings.receipt, ...d.receipt };
    state.settings.notifications = { ...state.settings.notifications, ...d.notifications };
    state.settings.security = { ...state.settings.security, ...d.security };
  }
  if (state.user?.role === 'owner' || !state.user) await loadWorkers().catch(() => {});
  snapshotAll();
}

function snapshotAll() {
  snapshots = {};
  for (const [key, table] of COLLECTIONS) snapshots[table] = new Map((state[key] || []).map((it) => [String(it.id), ser(it)]));
  snapshots[SETTINGS_TABLE] = new Map([['main', JSON.stringify(syncableSettings())]]);
}

async function pull() {
  if (pulling || !state.online || outbox.length || flushing) return;
  pulling = true;
  try { await loadAll(); commit(); } catch { /* abaikan */ } finally { pulling = false; }
}

/** Mulai sinkronisasi untuk bisnis ini (dipanggil setelah login). */
export async function startSync(bid, uid) {
  stopSync(false);
  businessId = bid; userKey = uid;
  outbox = await kv.getJson(outboxKey(), []);
  if (!Array.isArray(outbox)) outbox = [];
  await loadAll();
  // outbox yang belum terkirim ditimpa ulang di atas data server (perubahan offline tidak hilang)
  for (const o of outbox) {
    const key = COLLECTIONS.find(([, t]) => t === o.table)?.[0];
    if (!key || o.op !== 'upsert') continue;
    const i = state[key].findIndex((x) => String(x.id) === o.id);
    if (i >= 0) state[key][i] = o.row.data; else state[key].push(o.row.data);
  }
  started = true;
  unsub = subscribe(() => { if (started) { clearTimeout(diffTimer); diffTimer = setTimeout(diffAndQueue, 250); } });

  const applyNet = () => { state.online = !forcedOffline && netOnline; commit(); if (state.online) flush(); };
  const offNet = NetInfo.addEventListener((s) => {
    netOnline = Boolean(s.isConnected) && s.isInternetReachable !== false;
    applyNet();
  });
  timers.push(setInterval(() => { flush(); pull(); }, 30000));
  timers.push(offNet);
  publishQueue();
  flush();
}

export async function stopSync(flushFirst = true) {
  if (flushFirst) await flush();
  started = false;
  unsub?.(); unsub = null;
  timers.forEach((t) => (typeof t === 'function' ? t() : clearInterval(t)));
  timers = [];
  clearTimeout(diffTimer); clearTimeout(flushTimer);
  businessId = null; snapshots = {}; outbox = [];
}

// ---- simulasi offline manual (tombol di sheet Sinkronisasi) ----
export function setForcedOffline(v) { forcedOffline = v; state.online = !v && netOnline; commit(); if (state.online) flush(); }
export const isForcedOffline = () => forcedOffline;
export const syncNowReal = () => flush().then(() => pull());

/** Sync manual dengan umpan balik toast. */
export function syncNow(closeAfter = false) {
  if (!state.online) return toast('Tidak bisa sync saat offline', 'error');
  return syncNowReal().then(() => {
    if (closeAfter) closeSheet();
    toast(state.syncQueue.length ? 'Sebagian belum terkirim, akan dicoba lagi' : 'Sinkronisasi selesai', state.syncQueue.length ? 'error' : 'success');
  });
}
