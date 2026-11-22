import React, { useState } from "react";
import { StyleSheet, View } from "react-native";
import { saveWorker } from "../lib/auth";
import { closeSheet, commit, openSheet, PERMISSIONS, state, toast, useApp } from "../lib/store";
import { rupiah } from "../lib/format";
import { Btn, Card, Empty, Field, ListRow, Muted, Page, PageHead, Pill, Row2, Section, Select, Switch, SwitchRow, SummaryRow, T, useC } from "../ui/kit";
import { confirmModal } from "../ui/overlays";
import { RowActions } from "./Catalog";

function OutletForm({ id }) {
  const o = id ? state.outlets.find((x) => x.id === id) : null;
  const isEdit = !!o;
  const [f, setF] = useState({ name: o?.name ?? "", address: o?.address ?? "", phone: o?.phone ?? "", active: o?.active ?? true });
  const set = (k) => (v) => setF((p) => ({ ...p, [k]: v }));
  const save = () => {
    const name = f.name.trim();
    if (!name) return toast("Nama outlet wajib diisi", "error");
    const obj = { name, address: f.address.trim(), phone: f.phone.trim(), active: f.active };
    if (isEdit) Object.assign(o, obj); else state.outlets.push({ id: Date.now(), ...obj });
    closeSheet(); commit(); toast(isEdit ? "Outlet diperbarui" : "Outlet ditambahkan", "success");
  };
  return (
    <View style={{ gap: 12 }}>
      <T size={17} weight="700">{isEdit ? "Edit outlet" : "Outlet baru"}</T>
      <Field label="Nama outlet" value={f.name} onChangeText={set("name")} autoCapitalize="words" />
      <Field label="Alamat" value={f.address} onChangeText={set("address")} autoCapitalize="sentences" />
      <Field label="Telepon" keyboardType="phone-pad" value={f.phone} onChangeText={set("phone")} />
      <SwitchRow title="Aktif" on={f.active} onChange={set("active")} />
      <Row2><Btn kind="ghost" title="Batal" onPress={closeSheet} /><Btn title={isEdit ? "Simpan" : "Tambah"} onPress={save} /></Row2>
    </View>
  );
}
export function OutletsPage() {
  const app = useApp();
  const remove = (o) => {
    if (app.outlets.length <= 1) return toast("Minimal harus ada 1 outlet", "error");
    confirmModal("Hapus outlet?", "\"" + o.name + "\" akan dihapus.", "Hapus", () => { state.outlets = state.outlets.filter((x) => x.id !== o.id); commit(); });
  };
  return (
    <Page>
      <PageHead title="Outlet" sub={app.outlets.length + " outlet"} back right={<Btn icon="add" title="Tambah" onPress={() => openSheet(<OutletForm id={null} />)} />} />
      {app.outlets.length ? app.outlets.map((o) => <ListRow key={o.id} title={o.name} sub={(o.address || "-") + (o.active ? "" : " · nonaktif")} right={<RowActions onEdit={() => openSheet(<OutletForm id={o.id} />)} onDelete={() => remove(o)} />} />) : <Empty title="Belum ada outlet" />}
    </Page>
  );
}

function WorkerForm({ id }) {
  const app = useApp();
  const w = id ? state.workers.find((x) => x.id === id) : null;
  const isEdit = !!w;
  const [f, setF] = useState({ displayName: w?.displayName ?? "", username: w?.username ?? "", password: "", password2: "", outlet: w?.outlet ?? (state.outlets[0]?.name || ""), whatsapp: w?.whatsapp ?? "", active: w?.active ?? true });
  const [perms, setPerms] = useState(w?.permissions ?? ["pos", "dashboard", "shift", "printer", "theme", "profile"]);
  const [busy, setBusy] = useState(false);
  const set = (k) => (v) => setF((p) => ({ ...p, [k]: v }));
  const togglePerm = (pid) => setPerms((prev) => (prev.includes(pid) ? prev.filter((x) => x !== pid) : [...prev, pid]));
  const save = async () => {
    const displayName = f.displayName.trim(); const username = f.username.trim();
    if (!displayName || !username) return toast("Nama dan username wajib", "error");
    if (!isEdit) {
      if (!f.password || f.password.length < 6) return toast("Password minimal 6 karakter", "error");
      if (f.password !== f.password2) return toast("Konfirmasi password tidak cocok", "error");
    } else if (f.password && f.password.length < 6) return toast("Password minimal 6 karakter", "error");
    if (!perms.length) return toast("Minimal 1 fitur harus diaktifkan", "error");
    const obj = { displayName, username, outlet: f.outlet, whatsapp: f.whatsapp.trim(), active: f.active, permissions: perms };
    if (f.password) obj.password = f.password;
    setBusy(true);
    try { await saveWorker(isEdit ? "update" : "create", { ...obj, id: w?.id }); } catch (e) { setBusy(false); return toast(e.message, "error"); }
    setBusy(false); closeSheet(); toast(isEdit ? "Kasir diperbarui" : "Kasir ditambahkan", "success");
  };
  return (
    <View style={{ gap: 12 }}>
      <T size={17} weight="700">{isEdit ? "Edit kasir" : "Kasir baru"}</T>
      <Field label="Nama tampilan" value={f.displayName} onChangeText={set("displayName")} autoCapitalize="words" />
      <Field label="Username" value={f.username} editable={!isEdit} onChangeText={set("username")} />
      <Field label={isEdit ? "Password baru (kosongkan jika tidak diubah)" : "Password"} secure value={f.password} onChangeText={set("password")} placeholder="Minimal 6 karakter" />
      {!isEdit && <Field label="Konfirmasi password" secure value={f.password2} onChangeText={set("password2")} />}
      <Select label="Outlet" value={f.outlet} options={app.outlets.map((o) => o.name)} onChange={set("outlet")} />
      <Field label="Nomor WhatsApp (opsional)" keyboardType="phone-pad" value={f.whatsapp} onChangeText={set("whatsapp")} />
      <SwitchRow title="Akun aktif" on={f.active} onChange={set("active")} />
      <Section title="Akses Fitur" />
      {PERMISSIONS.map((p) => <SwitchRow key={p.id} title={p.label} sub={p.desc} on={perms.includes(p.id)} onChange={() => togglePerm(p.id)} />)}
      <Row2><Btn kind="ghost" title="Batal" onPress={closeSheet} /><Btn title={isEdit ? "Simpan" : "Tambah"} loading={busy} onPress={save} /></Row2>
    </View>
  );
}
export function WorkersPage() {
  const app = useApp();
  return (
    <Page>
      <PageHead title="Kasir" sub={app.workers.length + " akun"} back right={<Btn icon="add" title="Tambah" onPress={() => { if (!app.outlets.length) return toast("Buat outlet dulu", "error"); openSheet(<WorkerForm id={null} />); }} />} />
      {app.workers.length ? app.workers.map((w) => (
        <Card key={w.id} style={{ flexDirection: "row", alignItems: "center", gap: 10, paddingVertical: 12 }}>
          <View style={{ flex: 1, gap: 2 }}>
            <View style={{ flexDirection: "row", alignItems: "center", gap: 8 }}><T weight="600" size={15}>{w.displayName}</T><Pill text={w.active ? "Aktif" : "Nonaktif"} tone={w.active ? "cash" : "muted"} /></View>
            <Muted>{"@" + w.username + " · " + (w.outlet || "-") + " · " + (w.permissions || []).length + " fitur"}</Muted>
          </View>
          <RowActions onEdit={() => openSheet(<WorkerForm id={w.id} />)} onToggle={() => saveWorker("update", { displayName: w.displayName, username: w.username, outlet: w.outlet, whatsapp: w.whatsapp, active: !w.active, permissions: w.permissions, id: w.id }).then(() => toast(w.active ? "Kasir dinonaktifkan" : "Kasir diaktifkan", "success")).catch((e) => toast(e.message, "error"))} />
        </Card>
      )) : <Empty title="Belum ada kasir" sub="Tambahkan akun kasir untuk outlet Anda." />}
    </Page>
  );
}

export function ShiftHistoryPage() {
  const app = useApp();
  const c = useC();
  const [who, setWho] = useState("all");
  const names = [...new Set(app.shiftHistory.map((x) => x.cashierName || x.cashierId))];
  const list = app.shiftHistory.filter((x) => (who === "all" || (x.cashierName || x.cashierId) === who));
  const totCash = list.reduce((a, x) => a + (x.cash || 0), 0);
  const totQris = list.reduce((a, x) => a + (x.qris || 0), 0);
  const totVar = list.reduce((a, x) => a + (x.variance || 0), 0);
  const varText = totVar === 0 ? "Pas" : (totVar > 0 ? "+" : "-") + rupiah(Math.abs(totVar));
  const Row = ({ k, v, color, last }) => (
    <View style={{ flexDirection: "row", justifyContent: "space-between", paddingVertical: 9, borderBottomWidth: last ? 0 : StyleSheet.hairlineWidth, borderBottomColor: c.border }}>
      <T size={13} color={c.muted}>{k}</T><T size={13} weight="700" color={color}>{v}</T>
    </View>
  );
  return (
    <Page>
      <PageHead title="Riwayat Shift" sub={app.shiftHistory.length + " shift tercatat"} back />
      <Card>
        <T size={13} color={c.muted}>Total penjualan semua shift</T>
        <T size={28} weight="700" style={{ marginTop: 2 }}>{rupiah(totCash + totQris)}</T>
        <T size={13} weight="600" color={c.cash}>{list.length} shift</T>
        <View style={{ height: 1, backgroundColor: c.border, marginVertical: 10 }} />
        <SummaryRow label="Cash" value={rupiah(totCash)} />
        <SummaryRow label="QRIS" value={rupiah(totQris)} />
        <SummaryRow label="Selisih kas total" value={varText} color={totVar === 0 ? c.cash : c.alert} />
      </Card>
      <Section title="Filter" />
      <Select label="Kasir" value={who} options={[{ value: "all", label: "Semua kasir" }, ...names.map((n) => ({ value: n, label: n }))]} onChange={setWho} />
      <Section title="Daftar shift" />
      {list.length ? list.map((sh) => (
        <Card key={sh.id} style={{ paddingVertical: 10 }}>
          <View style={{ flexDirection: "row", justifyContent: "space-between", marginBottom: 4 }}>
            <View style={{ flex: 1 }}><T weight="700" size={15}>{sh.cashierName || sh.cashierId}</T><Muted>{sh.outlet} · {sh.id}</Muted></View>
            <View style={{ alignItems: "flex-end" }}><T weight="700" size={15}>{rupiah((sh.cash || 0) + (sh.qris || 0))}</T><Muted>{sh.tx} transaksi</Muted></View>
          </View>
          <Row k="Waktu" v={sh.start + " – " + sh.end} />
          <Row k="Kas awal" v={rupiah(sh.opening || 0)} />
          <Row k="Cash" v={rupiah(sh.cash || 0)} />
          <Row k="QRIS" v={rupiah(sh.qris || 0)} />
          <Row k="Kas akhir" v={rupiah(sh.closing || 0)} />
          <Row k="Expected" v={rupiah(sh.expected || 0)} />
          <Row last k="Selisih" v={sh.variance === 0 ? "Pas" : (sh.variance > 0 ? "+" : "-") + rupiah(Math.abs(sh.variance))} color={sh.variance === 0 ? c.cash : c.alert} />
        </Card>
      )) : <Empty title="Belum ada riwayat shift" />}
    </Page>
  );
}
