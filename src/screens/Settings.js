import React, { useEffect, useRef, useState } from "react";
import { Image, Pressable, View } from "react-native";
import { backToManage, closeSheet, commit, applyTheme, openSheet, state, toast, useApp } from "../lib/store";
import { changePassword, saveOwnerPin, saveProfile } from "../lib/auth";
import { purgeExpiredProofs, syncNow } from "../lib/sync";
import {
  connectBlePrinter, connectClassicPrinter, disconnectPrinter, discoverClassicPrinters, listClassicPrinters,
  pairClassicPrinter, printTestPage, scanBlePrinters, setPaperCols, supportsClassic,
} from "../lib/printer";
import { formatTimestamp, rupiah } from "../lib/format";
import { sampleReceiptText, withDefaults } from "../lib/receipt";
import { Banner, Btn, Card, Dot, ReceiptText, Empty, Field, IconBtn, ListRow, Muted, NumField, Page, PageHead, Pill, Row2, RupiahInput, Section, Select, SummaryRow, SwitchRow, T, useC, R } from "../ui/kit";
import { confirmModal, pickImageDataUrl } from "../ui/overlays";
import { ExpenseRows, openExpenseForm } from "./Reports";

// ---------------- PRINTER ----------------
function BleScanSheet() {
  const [devices, setDevices] = useState([]);
  const [scanning, setScanning] = useState(true);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState("");
  const stopRef = useRef(null);
  useEffect(() => {
    let alive = true;
    scanBlePrinters(
      (d) => alive && setDevices((p) => (p.some((x) => x.deviceId === d.deviceId) ? p : [...p, d])),
      (e) => { if (alive) { setError(e.message || String(e)); setScanning(false); } },
    ).then((stop) => { stopRef.current = stop; if (!alive) stop(); })
      .catch((e) => { if (alive) { setError(e.message || String(e)); setScanning(false); } });
    const t = setTimeout(() => { stopRef.current?.(); if (alive) setScanning(false); }, 20000);
    return () => { alive = false; clearTimeout(t); stopRef.current?.(); };
  }, []);
  const connect = async (d) => {
    setBusy(d.deviceId); setError("");
    try { stopRef.current?.(); await connectBlePrinter(d); closeSheet(); toast(d.name + " terhubung", "success"); }
    catch (e) { setError("Gagal tersambung: " + (e.message || e)); setBusy(""); }
  };
  return (
    <View style={{ gap: 8 }}>
      <T size={17} weight="700">Printer BLE</T>
      <Muted size={13}>Nyalakan printer & dekatkan ke perangkat. {scanning ? "Mencari…" : "Pencarian selesai."}</Muted>
      {error ? <Banner tone="alert">{error}</Banner> : null}
      {devices.map((d) => <ListRow key={d.deviceId} title={d.name} sub={d.deviceId + (d.rssi != null ? " · " + d.rssi + " dBm" : "")} right={busy === d.deviceId ? <Muted>Menyambung…</Muted> : null} onPress={busy ? undefined : () => connect(d)} />)}
      {!devices.length && !scanning && !error ? <Empty icon="bluetooth-outline" title="Tidak ada printer ditemukan" sub="Pastikan printer menyala & belum tersambung ke perangkat lain." /> : null}
    </View>
  );
}

function ClassicSheet() {
  const [devices, setDevices] = useState(null);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState("");
  const [discovering, setDiscovering] = useState(false);
  useEffect(() => { listClassicPrinters().then(setDevices).catch((e) => { setError(e.message || String(e)); setDevices([]); }); }, []);
  const discover = async () => {
    setDiscovering(true); setError("");
    try {
      const found = await discoverClassicPrinters();
      setDevices((prev) => { const m = new Map((prev || []).map((d) => [d.address, d])); found.forEach((d) => m.set(d.address, { ...m.get(d.address), ...d })); return [...m.values()]; });
    } catch (e) { setError(e.message || String(e)); } finally { setDiscovering(false); }
  };
  const connect = async (d) => {
    setBusy(d.address); setError("");
    try {
      if (!d.bonded) await pairClassicPrinter(d.address);
      await connectClassicPrinter(d); closeSheet(); toast(d.name + " terhubung", "success");
    } catch (e) { setError("Gagal tersambung: " + (e.message || e)); setBusy(""); }
  };
  return (
    <View style={{ gap: 8 }}>
      <T size={17} weight="700">Bluetooth Classic (SPP)</T>
      <Muted size={13}>Pilih printer yang sudah dipasangkan. Printer baru bisa dicari lalu dipasangkan dari sini (PIN biasanya 0000 / 1234).</Muted>
      {error ? <Banner tone="alert">{error}</Banner> : null}
      {devices === null ? <Muted size={13}>Memuat perangkat…</Muted> : null}
      {(devices || []).map((d) => <ListRow key={d.address} title={d.name} sub={d.address + (d.bonded ? " · terpasang" : " · belum dipasangkan")} right={busy === d.address ? <Muted>Menyambung…</Muted> : null} onPress={busy ? undefined : () => connect(d)} />)}
      {devices && !devices.length && !error ? <Empty icon="bluetooth-outline" title="Belum ada perangkat terpasang" sub="Tekan Cari perangkat baru." /> : null}
      <Btn kind="ghost" block loading={discovering} title="Cari perangkat baru" icon="search" onPress={discover} />
    </View>
  );
}

export function PrinterPage() {
  const p = useApp().settings.printer;
  const c = useC();
  const [busy, setBusy] = useState(false);
  const run = async (fn, ok) => {
    setBusy(true);
    try { await fn(); if (ok) toast(ok, "success"); } catch (e) { toast(e.message || String(e), "error"); } finally { setBusy(false); }
  };
  const has = Boolean(p.deviceId);
  const kind = p.transport === "spp" ? "Bluetooth Classic" : "BLE";
  return (
    <Page>
      <PageHead title="Printer" sub="Bluetooth thermal (ESC/POS)" back />
      <Card style={{ gap: 10 }}>
        <View style={{ flexDirection: "row", alignItems: "center", gap: 10 }}>
          <View style={{ flex: 1 }}>
            <T weight="600">{has ? p.device : "Belum terhubung"}</T>
            <Muted size={13}>{p.connected ? "Siap mencetak · " + kind : has ? "Terputus · akan tersambung otomatis saat mencetak" : "Pilih printer di bawah"}</Muted>
          </View>
          <Dot status={p.connected ? "synced" : "error"} />
        </View>
        {has && <Row2><Btn kind="ghost" title="Test print" disabled={busy} loading={busy} onPress={() => run(printTestPage, "Test print terkirim")} /><Btn kind="danger" title="Putuskan" disabled={busy} onPress={() => run(disconnectPrinter, "Printer diputus")} /></Row2>}
      </Card>
      <Section title="Hubungkan printer" />
      <Btn block icon="bluetooth" title="Cari printer BLE" onPress={() => openSheet(<BleScanSheet />)} />
      {supportsClassic()
        ? <Btn block kind="ghost" icon="bluetooth-outline" title="Bluetooth Classic (SPP)" onPress={() => openSheet(<ClassicSheet />)} />
        : <Muted size={12}>Bluetooth Classic (SPP) hanya tersedia di Android. Di iOS gunakan printer BLE.</Muted>}
      <Section title="Lebar kertas" />
      <Select value={p.cols || 32} options={[{ value: 32, label: "58 mm (32 kolom)" }, { value: 48, label: "80 mm (48 kolom)" }]} onChange={setPaperCols} />
    </Page>
  );
}

// ---------------- STRUK ----------------
const RECEIPT_GROUPS = [
  ["Header", [["showTagline", "Baris di atas nama bisnis"], ["showAddress", "Alamat outlet"], ["showPhone", "Telepon outlet"]]],
  ["Info transaksi", [["showTxNumber", "Nomor transaksi"], ["showDateTime", "Tanggal & jam"], ["showOutlet", "Nama outlet"], ["showCashier", "Nama kasir"]]],
  ["Rincian", [["showItemPrice", "Qty x harga per item"], ["showSubtotal", "Subtotal, diskon & pajak"], ["showItemCount", "Jumlah barang"], ["showPayment", "Metode bayar"], ["showChange", "Kembalian"]]],
  ["Footer", [["showFooter", "Tampilkan footer"]]],
];
export function ReceiptPage() {
  const app = useApp();
  const c = useC();
  const r = withDefaults(app.settings.receipt);
  const set = (k, v) => { app.settings.receipt[k] = v; commit(); };
  const outlet = app.outlets.find((o) => o.name === app.outlet.name) || app.outlet;
  return (
    <Page>
      <PageHead title="Struk" sub="Format & konten" back />
      <Card style={{ gap: 10 }}>
        <Field label="Nama bisnis (header)" value={r.bizName} onChangeText={(v) => set("bizName", v)} autoCapitalize="words" />
        {r.showTagline ? <Field label="Baris di atas nama bisnis" maxLength={40} value={r.tagline} onChangeText={(v) => set("tagline", v)} placeholder="mis. RUMAH MAKAN AYAM" autoCapitalize="characters" /> : null}
        {r.showFooter ? <Field label="Footer (max 120 karakter)" maxLength={120} value={r.footer} onChangeText={(v) => set("footer", v)} autoCapitalize="sentences" /> : null}
      </Card>
      {RECEIPT_GROUPS.map(([title, items]) => (
        <View key={title} style={{ gap: 6 }}>
          <Section title={title} />
          <Card>{items.map(([k, label]) => <SwitchRow key={k} title={label} on={r[k]} onChange={(v) => set(k, v)} />)}</Card>
        </View>
      ))}
      <Muted size={12}>Alamat & telepon diambil dari data outlet (Kelola &gt; Outlet).</Muted>
      <Section title="Preview struk" />
      <Card style={{ padding: 16 }}>
        <View style={{ backgroundColor: c.card, borderRadius: R.sm, borderWidth: 1, borderColor: c.border, padding: 14 }}>
          <ReceiptText text={sampleReceiptText(r, { name: outlet.name, address: outlet.address || "Jl. Contoh No. 1, Surabaya", phone: outlet.phone || "031-5345270" }, app.settings.printer.cols || 32)} />
        </View>
      </Card>
    </Page>
  );
}

export function QrisPage() {
  const app = useApp();
  const c = useC();
  const q = app.settings.qris;
  const [days, setDays] = useState(String(q.retentionDays));
  return (
    <Page>
      <PageHead title="QRIS" sub="Pengaturan pembayaran QR" back />
      <Card style={{ gap: 12 }}>
        <View style={{ alignItems: "center", padding: 26, borderWidth: 1, borderStyle: "dashed", borderColor: c.borderStrong, borderRadius: R.md }}>
          {q.image ? <Image source={{ uri: q.image }} style={{ width: 200, height: 200 }} resizeMode="contain" /> : <Muted size={13}>Belum ada gambar QRIS</Muted>}
        </View>
        <SwitchRow title="Aktifkan QRIS" sub="Muncul sebagai metode bayar di Kasir" on={q.active} onChange={(v) => { q.active = v; commit(); }} />
        <Row2>
          <Btn kind="ghost" title={q.image ? "Ganti QR" : "Upload QR"} onPress={async () => { try { const u = await pickImageDataUrl(700); if (u) { q.image = u; commit(); toast("QRIS diunggah", "success"); } } catch (e) { toast(e.message, "error"); } }} />
          {q.image ? <Btn kind="danger" title="Hapus" onPress={() => confirmModal("Hapus QRIS?", "QRIS tidak akan tampil di Kasir.", "Hapus", () => { q.image = null; commit(); })} /> : null}
        </Row2>
      </Card>
      <Card style={{ gap: 12 }}>
        <Banner tone="info"><T size={12} color={c.muted}><T size={12} weight="700" color={c.muted}>Retensi bukti QRIS{"\n"}</T>Foto bukti QRIS dihapus otomatis setelah {q.retentionDays} hari (dicek tiap app dibuka dan tiap sinkronisasi, dijalankan dari perangkat owner). Data transaksi tetap tersimpan.</T></Banner>
        <NumField label="Retensi (hari)" value={days} onChange={setDays} maxLength={3} />
        <Btn block kind="ghost" title="Hapus bukti kadaluarsa sekarang" onPress={async () => { const n = await purgeExpiredProofs(); toast(n ? n + " foto bukti dihapus" : "Tidak ada bukti yang kadaluarsa", n ? "success" : undefined); }} />
        <Btn block title="Simpan retensi" onPress={() => { q.retentionDays = Math.max(1, Math.min(365, parseInt(days, 10) || 7)); setDays(String(q.retentionDays)); commit(); toast("Retensi disimpan", "success"); }} />
      </Card>
    </Page>
  );
}

export function NotifSettingsPage() {
  const n = useApp().settings.notifications;
  return (
    <Page>
      <PageHead title="Notifikasi" sub="Preferensi" back />
      <Card>{[["lowStock", "Stok menipis"], ["outOfStock", "Stok habis"], ["system", "Sistem"]].map(([k, l]) => <SwitchRow key={k} title={l} on={n[k]} onChange={(v) => { n[k] = v; commit(); }} />)}</Card>
    </Page>
  );
}

export function SyncSettingsPage() {
  const app = useApp();
  return (
    <Page>
      <PageHead title="Sinkronisasi" sub="Status & antrean" back />
      <Card style={{ paddingVertical: 8 }}>
        <SummaryRow line label="Status" value={app.online ? "Online" : "Offline"} />
        <SummaryRow label="Pending" value={String(app.syncQueue.length)} />
      </Card>
      <Btn block title={app.online ? "Sync sekarang" : "Offline"} disabled={!app.online} onPress={() => syncNow()} />
    </Page>
  );
}

export function SecurityPage() {
  const sec = useApp().settings.security;
  const [limit, setLimit] = useState(sec.refundLimitCashier);
  const [pin, setPin] = useState("");
  const [alertN, setAlertN] = useState(String(sec.alertVoidPerDay));
  const save = async () => {
    const newPin = pin.trim();
    if (newPin && !/^\d{4}$/.test(newPin)) return toast("PIN harus 4 digit angka", "error");
    try { if (newPin) await saveOwnerPin(newPin); } catch (e) { return toast(e.message, "error"); }
    sec.refundLimitCashier = limit; sec.alertVoidPerDay = parseInt(alertN, 10) || 5;
    commit(); setPin(""); toast("Pengaturan keamanan disimpan", "success");
  };
  return (
    <Page>
      <PageHead title="Keamanan" sub="Aturan void & refund" back />
      <Banner tone="info">Void hanya bisa dilakukan oleh Owner. Refund oleh kasir dibatasi limit & butuh PIN owner kalau melebihi.</Banner>
      <Card style={{ gap: 12 }}>
        <RupiahInput label="Limit refund kasir (Rp)" value={limit} onChange={setLimit} />
        <Field label="PIN Owner (4 digit)" secure keyboardType="number-pad" maxLength={4} value={pin} onChangeText={(t) => setPin(t.replace(/\D/g, ""))} placeholder="Kosongkan jika tidak diubah" />
        <NumField label="Alert kalau kasir refund ≥ X kali/hari" value={alertN} onChange={setAlertN} maxLength={3} />
        <Btn block title="Simpan" onPress={save} />
      </Card>
    </Page>
  );
}

export function AuditLogPage() {
  const { auditLog } = useApp();
  const c = useC();
  return (
    <Page>
      <PageHead title="Audit Log" sub={auditLog.length + " catatan"} back />
      {auditLog.length ? auditLog.map((a) => (
        <Card key={a.id} style={{ gap: 4 }}>
          <View style={{ flexDirection: "row", justifyContent: "space-between" }}><Pill text={a.type === "void" ? "VOID" : "REFUND"} tone={a.type === "void" ? "alert" : "warn"} /><Muted>{formatTimestamp(a.at)}</Muted></View>
          <T><T weight="700">{a.trxId || "-"}</T> · {rupiah(a.amount || 0)}{a.isFull === false ? " (partial)" : ""}</T>
          <Muted>oleh {a.byName || a.by} · {a.reason || "-"}{a.device ? " · " + a.device : ""}</Muted>
        </Card>
      )) : <Empty icon={null} title="Belum ada catatan" sub="Void/refund yang dilakukan akan tercatat di sini." />}
    </Page>
  );
}

export function ThemePage() {
  const app = useApp();
  const c = useC();
  const opts = [["light", "Terang", "Cocok untuk ruangan terang", { bg: "#FFFFFF", fg: "#1A1A1A", sub: "#6B6B63", bd: c.border, accent: "#2563EB" }], ["dark", "Gelap", "Hemat mata di malam hari", { bg: "#17171A", fg: "#F5F5F0", sub: "#A0A09A", bd: "#17171A", accent: "#3B82F6" }]];
  return (
    <Page>
      <PageHead title="Tema" back />
      <Card style={{ gap: 8, padding: 10 }}>
        {opts.map(([t, l, sub, o]) => (
          <Pressable key={t} onPress={() => applyTheme(t)} style={{ flexDirection: "row", alignItems: "center", backgroundColor: o.bg, borderWidth: 1, borderColor: o.bd, borderRadius: R.md, paddingVertical: 12, paddingHorizontal: 14 }}>
            <View style={{ flex: 1 }}><T weight="600" color={o.fg}>{l}</T><T size={12} color={o.sub}>{sub}</T></View>
            {app.theme === t ? <T weight="700" color={o.accent}>Aktif</T> : null}
          </Pressable>
        ))}
      </Card>
    </Page>
  );
}

export function AboutPage() {
  return (
    <Page>
      <PageHead title="Tentang" back />
      <Card style={{ gap: 4 }}><Image source={require("../../assets/icon.png")} style={{ width: 64, height: 64, borderRadius: 16, marginBottom: 8 }} /><T weight="700" size={16}>SakuKasir</T><Muted>React Native + Expo · v0.2.0</Muted><Muted size={13} style={{ marginTop: 8 }}>Aplikasi kasir offline-first untuk usaha kecil dan menengah. Backend Supabase.</Muted></Card>
    </Page>
  );
}

const KASIR_CATS = ["Bahan", "Operasional"];
export function ExpensesPage() {
  const app = useApp();
  const c = useC();
  const total = app.expenses.reduce((s, e) => s + e.amount, 0);
  return (
    <Page>
      <PageHead title="Pengeluaran" sub={app.expenses.length + " catatan"} back right={<Btn icon="add" title="Tambah" onPress={() => openExpenseForm(null, KASIR_CATS)} />} />
      <SummaryRow strong label="Total pengeluaran" value={rupiah(total)} color={c.alert} />
      <Banner tone="info">Kasir hanya bisa mencatat pengeluaran (Bahan & Operasional). Untuk koreksi atau hapus, hubungi owner.</Banner>
      {app.user.role === "owner" ? <ExpenseRows list={app.expenses} /> : app.expenses.length
        ? app.expenses.map((e) => <ListRow key={e.id} title={e.note || e.category} sub={e.category + " · " + e.date + " · " + e.by} amount={"-" + rupiah(e.amount)} />)
        : <Empty title="Belum ada pengeluaran" />}
    </Page>
  );
}

export function ProfilePage() {
  const app = useApp();
  const c = useC();
  const u = app.user;
  const owner = u.role === "owner";
  const [name, setName] = useState(u.displayName || "");
  const [wa, setWa] = useState(u.whatsapp || "");
  const [pw, setPw] = useState({ old: "", next: "", confirm: "" });
  const setP = (k) => (v) => setPw((p) => ({ ...p, [k]: v }));
  const save = async () => {
    if (!name.trim()) return toast("Nama wajib diisi", "error");
    const changing = owner && (pw.old || pw.next || pw.confirm);
    if (changing) {
      if (!pw.old) return toast("Password lama wajib diisi", "error");
      if (!pw.next || pw.next.length < 6) return toast("Password baru minimal 6 karakter", "error");
      if (pw.next !== pw.confirm) return toast("Konfirmasi password tidak cocok", "error");
    }
    try {
      await saveProfile({ displayName: name.trim(), whatsapp: wa.trim() });
      if (changing) { await changePassword(pw.old, pw.next); setPw({ old: "", next: "", confirm: "" }); }
    } catch (e) { return toast(e.message, "error"); }
    u.displayName = name.trim(); u.whatsapp = wa.trim();
    if (!owner) { const w = state.workers.find((x) => x.username === u.username); if (w) { w.displayName = u.displayName; w.whatsapp = u.whatsapp; } }
    commit(); toast("Profil diperbarui", "success");
  };
  return (
    <Page>
      <PageHead title="Profil" />
      <View style={{ alignSelf: "flex-start" }}><IconBtn name="chevron-back" onPress={backToManage} /></View>
      <T size={22} weight="700">Profil Akun</T>
      <Card style={{ gap: 12 }}>
        <T weight="500" color={c.muted}>Informasi Pribadi</T>
        <Field label="Nama" icon="person-outline" value={name} onChangeText={setName} autoCapitalize="words" />
        <Field label={owner ? "Email" : "Username"} icon="mail-outline" value={(owner ? u.email : u.username) || ""} editable={false} />
        <Field label="WhatsApp" icon="logo-whatsapp" keyboardType="phone-pad" value={wa} onChangeText={setWa} placeholder="Nomor WhatsApp (opsional)" />
        <Field label="Role" icon="shield-outline" value={owner ? "Owner" : "Kasir"} editable={false} />
      </Card>
      {owner && (
        <Card style={{ gap: 12 }}>
          <T weight="500" color={c.muted}>Keamanan</T>
          <Field label="Password Lama" icon="lock-closed-outline" secure placeholder="Masukkan password lama" value={pw.old} onChangeText={setP("old")} />
          <Field label="Password Baru" icon="lock-closed-outline" secure placeholder="Masukkan password baru" value={pw.next} onChangeText={setP("next")} />
          <Field label="Konfirmasi Password Baru" icon="lock-closed-outline" secure placeholder="Ulangi password baru" value={pw.confirm} onChangeText={setP("confirm")} />
        </Card>
      )}
      <Btn block title="Simpan" onPress={save} />
    </Page>
  );
}
