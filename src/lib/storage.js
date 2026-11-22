// Pembungkus AsyncStorage (pengganti localStorage di web). Semua error ditelan: penyimpanan bersifat best-effort.
import AsyncStorage from "@react-native-async-storage/async-storage";

export const kv = {
  async get(k, fallback = null) {
    try { const v = await AsyncStorage.getItem(k); return v == null ? fallback : v; } catch { return fallback; }
  },
  async getJson(k, fallback = null) {
    try { const v = await AsyncStorage.getItem(k); return v == null ? fallback : JSON.parse(v); } catch { return fallback; }
  },
  set(k, v) { return AsyncStorage.setItem(k, typeof v === "string" ? v : JSON.stringify(v)).catch(() => {}); },
  remove(k) { return AsyncStorage.removeItem(k).catch(() => {}); },
};
