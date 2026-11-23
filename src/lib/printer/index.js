// API printer tingkat tinggi untuk UI. Status koneksi disimpan di state.settings.printer (per perangkat).
// Dua transport: BLE (Android/iOS) dan Bluetooth Classic SPP (Android). Pencetakan diserialkan lewat antrean.
import { commit, savePrinterSettings, state } from "../store";
import { buildReceipt, buildTestPage } from "./escpos";
import { bleTransport } from "./ble";
import { classicTransport, supportsClassic } from "./classic";

const transports = { ble: bleTransport, spp: classicTransport };
const cur = () => state.settings.printer;
const cols = () => cur().cols || 32;

function setPrinter(patch) {
  Object.assign(cur(), patch);
  savePrinterSettings();
  commit();
}
const dropped = () => setPrinter({ connected: false });

export { supportsClassic };

// ---- BLE ----
export const scanBlePrinters = (onFound, onError) => bleTransport.scan(onFound, onError);
export async function connectBlePrinter({ deviceId, name }) {
  await bleTransport.connect(deviceId, dropped);
  setPrinter({ connected: true, transport: "ble", device: name, deviceId });
}

// ---- Classic SPP ----
export const listClassicPrinters = () => classicTransport.listPaired();
export const discoverClassicPrinters = () => classicTransport.discover();
export const pairClassicPrinter = (address) => classicTransport.pair(address);
export async function connectClassicPrinter({ address, name }) {
  await classicTransport.connect(address, dropped);
  setPrinter({ connected: true, transport: "spp", device: name, deviceId: address });
}

export async function disconnectPrinter() {
  const p = cur();
  if (p.transport && p.deviceId) await transports[p.transport].disconnect(p.deviceId).catch(() => {});
  setPrinter({ connected: false });
}

export function setPaperCols(n) { setPrinter({ cols: n }); }

/** Pastikan tersambung; sambung ulang otomatis ke printer tersimpan bila putus (2x percobaan). */
async function ensureConnected() {
  const p = cur();
  if (!p.transport || !p.deviceId) throw new Error("Printer belum dipilih. Buka Kelola > Printer.");
  const t = transports[p.transport];
  if (await t.isReady(p.deviceId)) return t;
  let lastErr;
  for (let i = 0; i < 2; i++) {
    try {
      await t.connect(p.deviceId, dropped);
      setPrinter({ connected: true });
      return t;
    } catch (e) { lastErr = e; await new Promise((r) => setTimeout(r, 600)); }
  }
  setPrinter({ connected: false });
  throw new Error("Tidak bisa tersambung ke printer (" + (lastErr?.message || "gagal") + "). Pastikan printer menyala & dekat.");
}

let queue = Promise.resolve();
function send(bytes) {
  const job = queue.then(async () => {
    const t = await ensureConnected();
    try { await t.write(bytes); } catch (e) { dropped(); throw e; }
  });
  queue = job.catch(() => {}); // satu job gagal tidak menghentikan antrean
  return job;
}

export const printTransactionReal = (tx) => send(buildReceipt(tx, state.settings.receipt, cols(), outletInfo(tx.outlet)));
const outletInfo = (name) => { const o = (state.outlets || []).find((x) => x.name === name) || {}; return { address: o.address, phone: o.phone }; };
export const printTestPage = () => send(buildTestPage(cur().device, cols()));
