// Transport Bluetooth Classic (SPP / RFCOMM) — hanya Android — memakai react-native-bluetooth-classic.
// Mayoritas printer thermal 58mm murah memakai SPP. Pasangkan (pair) dulu dari dalam app atau Pengaturan Android.
import { Platform } from "react-native";
import { Buffer } from "buffer";
import { ensureBluetoothPermissions } from "./permissions";

export const supportsClassic = () => Platform.OS === "android";

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
let RNBC = null;
let connectedAddress = null;
let dropSub = null;

function lib() {
  if (!supportsClassic()) throw new Error("Bluetooth klasik (SPP) hanya tersedia di Android. Di iOS pakai printer BLE.");
  if (!RNBC) RNBC = require("react-native-bluetooth-classic").default;
  return RNBC;
}

async function ensureEnabled() {
  await ensureBluetoothPermissions();
  const m = lib();
  if (!(await m.isBluetoothEnabled())) {
    const ok = await m.requestBluetoothEnabled();
    if (!ok) throw new Error("Bluetooth belum dinyalakan.");
  }
}

const toItem = (d) => ({ address: d.address, name: d.name || d.address, bonded: Boolean(d.bonded) });

export const classicTransport = {
  id: "spp",

  /** Perangkat yang sudah dipasangkan (paired). */
  async listPaired() {
    await ensureEnabled();
    return (await lib().getBondedDevices()).map(toItem);
  },

  /** Cari perangkat baru di sekitar (±12 dtk). Hasil sudah paired maupun belum. */
  async discover() {
    await ensureEnabled();
    const m = lib();
    try { return (await m.startDiscovery()).map(toItem); }
    finally { await m.cancelDiscovery().catch(() => {}); }
  },

  /** Pairing dari dalam app (Android akan menampilkan dialog PIN bila perlu, umumnya 0000 / 1234). */
  async pair(address) {
    await ensureEnabled();
    return lib().pairDevice(address);
  },

  async connect(address, onDisconnect) {
    await ensureEnabled();
    const m = lib();
    try { await m.cancelDiscovery(); } catch { /* abaikan */ }
    if (!(await m.isDeviceConnected(address).catch(() => false))) {
      try { await m.connectToDevice(address, { connectorType: "rfcomm", secureSocket: true }); }
      catch { await m.connectToDevice(address, { connectorType: "rfcomm", secureSocket: false }); } // sebagian printer hanya mau insecure
    }
    connectedAddress = address;
    dropSub?.remove();
    dropSub = m.onDeviceDisconnected((e) => {
      if (e?.device?.address === address) { connectedAddress = null; onDisconnect?.(); }
    });
  },

  async write(bytes) {
    if (!connectedAddress) throw new Error("Printer belum terhubung");
    const dev = await lib().getConnectedDevice(connectedAddress);
    const CHUNK = 256;
    for (let i = 0; i < bytes.length; i += CHUNK) {
      await dev.write(Buffer.from(bytes.slice(i, i + CHUNK)));
      await sleep(15);
    }
  },

  async disconnect(address) {
    connectedAddress = null;
    dropSub?.remove(); dropSub = null;
    await lib().disconnectFromDevice(address).catch(() => {});
  },

  async isReady(address) {
    if (!connectedAddress) return false;
    try { return await lib().isDeviceConnected(address); } catch { return false; }
  },
};
