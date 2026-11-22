import React, { useEffect } from "react";
import { ActivityIndicator, BackHandler, View } from "react-native";
import { StatusBar } from "expo-status-bar";
import { SafeAreaProvider, SafeAreaView } from "react-native-safe-area-context";
import { hydrate, state, commit, tryCloseSomething, useApp } from "./lib/store";
import { isConfigured } from "./lib/supabase";
import { restoreSession } from "./lib/auth";
import { useC, T, Muted } from "./ui/kit";
import { OverlayHost, Toasts } from "./ui/overlays";
import AuthScreen from "./screens/Auth";
import Shell from "./screens/Shell";

function Root() {
  const app = useApp();
  const c = useC();
  useEffect(() => {
    (async () => {
      await hydrate();
      if (isConfigured) await restoreSession().catch(() => {});
      state.booting = false; commit();
    })();
    const sub = BackHandler.addEventListener("hardwareBackPress", () => tryCloseSomething());
    return () => sub.remove();
  }, []);

  if (!isConfigured) {
    return (
      <SafeAreaView style={{ flex: 1, backgroundColor: c.surface, justifyContent: "center", padding: 24, gap: 8 }}>
        <T size={20} weight="700">Supabase belum dikonfigurasi</T>
        <Muted size={14}>Isi EXPO_PUBLIC_SUPABASE_URL dan EXPO_PUBLIC_SUPABASE_ANON_KEY di file .env (lihat .env.example), lalu jalankan ulang build.</Muted>
      </SafeAreaView>
    );
  }
  if (app.booting) return <View style={{ flex: 1, backgroundColor: c.surface, alignItems: "center", justifyContent: "center" }}><ActivityIndicator color={c.primary} /></View>;
  return app.authView === "app" && app.user ? <Shell /> : <AuthScreen />;
}

export default function App() {
  const app = useApp();
  return (
    <SafeAreaProvider>
      <StatusBar style={app.theme === "dark" ? "light" : "dark"} />
      <Root />
      <OverlayHost />
      <Toasts />
    </SafeAreaProvider>
  );
}
