import "react-native-url-polyfill/auto";
import AsyncStorage from "@react-native-async-storage/async-storage";
import { AppState } from "react-native";
import { createClient } from "@supabase/supabase-js";

// Expo menginline process.env.EXPO_PUBLIC_* saat bundling (harus ditulis literal seperti ini).
const url = process.env.EXPO_PUBLIC_SUPABASE_URL;
const key = process.env.EXPO_PUBLIC_SUPABASE_ANON_KEY;

/** Aplikasi native selalu memakai backend Supabase yang sudah ada. Tanpa env, App menampilkan layar konfigurasi. */
export const isConfigured = Boolean(url && key);

export const supabase = isConfigured
  ? createClient(url, key, {
      auth: { storage: AsyncStorage, persistSession: true, autoRefreshToken: true, detectSessionInUrl: false },
    })
  : null;

// Di React Native, auto-refresh token harus dijeda saat app di background.
if (supabase) {
  AppState.addEventListener("change", (s) => {
    if (s === "active") supabase.auth.startAutoRefresh();
    else supabase.auth.stopAutoRefresh();
  });
}

/** username kasir -> email internal untuk Supabase Auth (sama dengan versi web) */
export const workerEmail = (username) => String(username).trim().toLowerCase() + "@worker.sakukasir.local";
