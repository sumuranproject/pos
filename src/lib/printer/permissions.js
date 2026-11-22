// Izin runtime Bluetooth (Android). iOS meminta izin sendiri saat BleManager pertama dipakai.
import { PermissionsAndroid, Platform } from "react-native";

export async function ensureBluetoothPermissions() {
  if (Platform.OS !== "android") return true;
  const P = PermissionsAndroid.PERMISSIONS;
  // Android 12+ (API 31): izin "perangkat sekitar"; di bawahnya BLE scan butuh lokasi.
  const need = Platform.Version >= 31 ? [P.BLUETOOTH_SCAN, P.BLUETOOTH_CONNECT] : [P.ACCESS_FINE_LOCATION];
  const res = await PermissionsAndroid.requestMultiple(need);
  const ok = need.every((p) => res[p] === PermissionsAndroid.RESULTS.GRANTED);
  if (!ok) throw new Error("Izin Bluetooth ditolak. Aktifkan izin Perangkat sekitar di Pengaturan > Aplikasi > SakuKasir.");
  return true;
}
