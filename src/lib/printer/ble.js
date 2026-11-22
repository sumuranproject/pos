// Transport BLE (Android & iOS) memakai react-native-ble-plx.
// Printer thermal BLE biasanya punya 1 karakteristik tulis (mis. service 18F0 / FF00 / FFE0 / 49535343-...).
import { BleManager, State } from "react-native-ble-plx";
import { Buffer } from "buffer";
import { ensureBluetoothPermissions } from "./permissions";

// UUID layanan printer yang umum: dicoba lebih dulu saat mencari karakteristik tulis.
export const KNOWN_SERVICES = [
  "000018f0-0000-1000-8000-00805f9b34fb",
  "e7810a71-73ae-499d-8c15-faa9aef0c3f2",
  "49535343-fe7d-4ae5-8fa9-9fafd205e455",
  "0000ff00-0000-1000-8000-00805f9b34fb",
  "0000fee7-0000-1000-8000-00805f9b34fb",
  "0000ffe0-0000-1000-8000-00805f9b34fb",
];

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
let manager = null;
let target = null; // { device, service, characteristic, noResponse, chunk }
let dropSub = null;

const mgr = () => (manager ||= new BleManager());

async function waitPoweredOn(ms = 6000) {
  const m = mgr();
  if ((await m.state()) === State.PoweredOn) return;
  await new Promise((resolve, reject) => {
    const t = setTimeout(() => { sub.remove(); reject(new Error("Bluetooth mati. Nyalakan Bluetooth lalu coba lagi.")); }, ms);
    const sub = m.onStateChange((s) => { if (s === State.PoweredOn) { clearTimeout(t); sub.remove(); resolve(); } }, true);
  });
}

export const bleTransport = {
  id: "ble",

  /** Mulai scan. onFound({deviceId,name,rssi}) dipanggil per perangkat BERNAMA. Mengembalikan fungsi stop(). */
  async scan(onFound, onError) {
    await ensureBluetoothPermissions();
    await waitPoweredOn();
    const m = mgr();
    const seen = new Set();
    m.startDeviceScan(null, { allowDuplicates: false }, (err, d) => {
      if (err) { m.stopDeviceScan(); onError?.(err); return; }
      const name = d?.name || d?.localName;
      if (!d || !name || seen.has(d.id)) return;
      seen.add(d.id);
      onFound({ deviceId: d.id, name, rssi: d.rssi });
    });
    return () => m.stopDeviceScan();
  },

  async connect(deviceId, onDisconnect) {
    await ensureBluetoothPermissions();
    await waitPoweredOn();
    const m = mgr();
    m.stopDeviceScan();
    let dev = await m.connectToDevice(deviceId, { timeout: 12000, requestMTU: 185 });
    dev = await dev.discoverAllServicesAndCharacteristics();
    dropSub?.remove();
    dropSub = dev.onDisconnected(() => { target = null; onDisconnect?.(); });

    const services = await dev.services();
    const ordered = [...services].sort((a, b) => {
      const ia = KNOWN_SERVICES.indexOf(a.uuid.toLowerCase()); const ib = KNOWN_SERVICES.indexOf(b.uuid.toLowerCase());
      return (ia < 0 ? 99 : ia) - (ib < 0 ? 99 : ib);
    });
    for (const s of ordered) {
      const chars = await s.characteristics();
      const c = chars.find((x) => x.isWritableWithoutResponse) || chars.find((x) => x.isWritableWithResponse);
      if (c) {
        const mtu = dev.mtu || 23;
        target = {
          device: dev, service: s.uuid, characteristic: c.uuid,
          noResponse: !c.isWritableWithResponse && c.isWritableWithoutResponse,
          chunk: Math.max(20, Math.min(mtu - 3, 180)),
        };
        return;
      }
    }
    await dev.cancelConnection().catch(() => {});
    throw new Error("Perangkat ini tidak punya karakteristik tulis (bukan printer BLE?)");
  },

  async write(bytes) {
    if (!target) throw new Error("Printer belum terhubung");
    const { device, service, characteristic, noResponse, chunk } = target;
    for (let i = 0; i < bytes.length; i += chunk) {
      const b64 = Buffer.from(bytes.slice(i, i + chunk)).toString("base64");
      if (noResponse) await device.writeCharacteristicWithoutResponseForService(service, characteristic, b64);
      else await device.writeCharacteristicWithResponseForService(service, characteristic, b64);
      await sleep(20); // beri waktu buffer printer
    }
  },

  async disconnect(deviceId) {
    target = null;
    dropSub?.remove(); dropSub = null;
    await mgr().cancelDeviceConnection(deviceId).catch(() => {});
  },

  async isReady(deviceId) {
    if (!target) return false;
    try { return await mgr().isDeviceConnected(deviceId); } catch { return false; }
  },
};
