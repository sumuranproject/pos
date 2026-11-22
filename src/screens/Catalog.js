import React, { useState } from "react";
import { Pressable, View } from "react-native";
import { closeSheet, commit, openSheet, state, toast, useApp } from "../lib/store";
import { rupiah } from "../lib/format";
import { Btn, Empty, Field, Icon, IconBtn, Kpi, ListRow, Muted, NumField, Page, PageHead, Pill, Row2, RupiahInput, Select, SwitchRow, T, useC, R } from "../ui/kit";
import { confirmModal, promptModal } from "../ui/overlays";

export const RowActions = ({ onEdit, onDelete, onToggle }) => {
  const c = useC();
  return (
    <View style={{ flexDirection: "row", gap: 14, alignItems: "center", paddingLeft: 4 }}>
      {onEdit ? <Pressable onPress={onEdit} hitSlop={10}><Icon name="create-outline" size={17} color={c.text} /></Pressable> : null}
      {onToggle ? <Pressable onPress={onToggle} hitSlop={10}><Icon name="checkmark-circle-outline" size={18} color={c.text} /></Pressable> : null}
      {onDelete ? <Pressable onPress={onDelete} hitSlop={10}><Icon name="trash-outline" size={17} color={c.text} /></Pressable> : null}
    </View>
  );
};

function ProductForm({ id }) {
  const app = useApp();
  const p = id ? state.products.find((x) => x.id === id) : null;
  const isEdit = !!p;
  const [f, setF] = useState(() => ({ name: p?.name ?? "", price: p?.price ?? 0, unit: p?.unit ?? "", cat: p?.cat ?? (state.categories[0]?.name || ""), stock: String(p?.stock ?? 0), low: String(p?.low ?? 5), track: p?.track ?? true, active: p?.active ?? true }));
  const set = (k) => (v) => setF((prev) => ({ ...prev, [k]: v }));
  const save = () => {
    const name = f.name.trim();
    if (!name || f.price <= 0) return toast("Nama dan harga wajib diisi", "error");
    const obj = { name, price: f.price, unit: f.unit.trim() || "pcs", cat: f.cat, track: f.track, stock: f.track ? parseInt(f.stock, 10) || 0 : 0, low: f.track ? parseInt(f.low, 10) || 0 : 0, active: f.active };
    if (isEdit) Object.assign(p, obj); else state.products.push({ id: Date.now(), ...obj });
    closeSheet(); commit(); toast(isEdit ? "Produk diperbarui" : "Produk ditambahkan", "success");
  };
  return (
    <View style={{ gap: 12 }}>
      <T size={17} weight="700">{isEdit ? "Edit produk" : "Produk baru"}</T>
      <Field label="Nama produk" value={f.name} onChangeText={set("name")} autoCapitalize="words" />
      <Select label="Kategori" value={f.cat} options={app.categories.map((c) => c.name)} onChange={set("cat")} />
      <Field label="Unit" value={f.unit} placeholder="porsi / cup" onChangeText={set("unit")} />
      <RupiahInput label="Harga jual (Rp)" value={f.price} onChange={set("price")} />
      <SwitchRow title="Stock tracking" sub="Pantau stok & notifikasi low stock" on={f.track} onChange={set("track")} />
      {f.track && <Row2><NumField label="Stok saat ini" value={f.stock} onChange={set("stock")} /><NumField label="Batas low stock" value={f.low} onChange={set("low")} /></Row2>}
      <SwitchRow title="Aktif" sub="Nonaktif tidak muncul di Kasir" on={f.active} onChange={set("active")} />
      <Row2><Btn kind="ghost" title="Batal" onPress={closeSheet} /><Btn title={isEdit ? "Simpan" : "Tambah"} onPress={save} /></Row2>
    </View>
  );
}

export function ProductsPage() {
  const app = useApp();
  const [q, setQ] = useState("");
  const list = app.products.filter((p) => p.name.toLowerCase().includes(q.toLowerCase()));
  const remove = (p) => confirmModal("Hapus produk?", "\"" + p.name + "\" akan dihapus permanen.", "Hapus", () => { state.products = state.products.filter((x) => x.id !== p.id); commit(); toast("Produk dihapus"); });
  return (
    <Page>
      <PageHead title="Produk" sub={app.products.length + " produk"} back right={<Btn icon="add" title="Tambah" onPress={() => { if (!app.categories.length) return toast("Buat kategori dulu", "error"); openSheet(<ProductForm id={null} />); }} />} />
      <Field placeholder="Cari produk…" icon="search" value={q} onChangeText={setQ} />
      {list.length ? list.map((p) => (
        <ListRow key={p.id} title={p.name} sub={p.cat + " · " + (p.track ? "stok " + p.stock + " " + p.unit : "tanpa stok") + (p.active ? "" : " · nonaktif")}
          right={<View style={{ flexDirection: "row", alignItems: "center", gap: 8 }}><T weight="600">{rupiah(p.price)}</T><RowActions onEdit={() => openSheet(<ProductForm id={p.id} />)} onDelete={() => remove(p)} /></View>} />
      )) : <Empty title="Belum ada produk" sub="Tekan + Tambah." />}
    </Page>
  );
}

export function CategoriesPage() {
  const app = useApp();
  const add = () => promptModal("Kategori baru", "Nama kategori", "", (v) => { if (!v.trim()) return; state.categories.push({ id: Date.now(), name: v.trim(), active: true }); commit(); toast("Kategori ditambahkan", "success"); });
  const edit = (c) => promptModal("Edit kategori", "Nama", c.name, (v) => { if (!v.trim()) return; const old = c.name; c.name = v.trim(); state.products.forEach((p) => { if (p.cat === old) p.cat = c.name; }); commit(); });
  const remove = (c) => {
    const used = state.products.filter((p) => p.cat === c.name).length;
    if (used > 0) return toast("Tidak bisa hapus · " + used + " produk pakai kategori ini", "error");
    confirmModal("Hapus kategori?", "\"" + c.name + "\" akan dihapus.", "Hapus", () => { state.categories = state.categories.filter((x) => x.id !== c.id); commit(); });
  };
  return (
    <Page>
      <PageHead title="Kategori" sub={app.categories.length + " kategori"} back right={<Btn icon="add" title="Tambah" onPress={add} />} />
      {app.categories.length ? app.categories.map((c) => <ListRow key={c.id} title={c.name} sub={app.products.filter((p) => p.cat === c.name).length + " produk"} right={<RowActions onEdit={() => edit(c)} onDelete={() => remove(c)} />} />) : <Empty title="Belum ada kategori" />}
    </Page>
  );
}

function AdjustSheet({ id }) {
  const p = state.products.find((x) => x.id === id);
  const [mode, setMode] = useState("plus");
  const [qty, setQty] = useState("1");
  if (!p) return null;
  const save = () => {
    const n = parseInt(qty, 10);
    if (!n || n < 1) return toast("Jumlah tidak valid", "error");
    p.stock = Math.max(0, p.stock + (mode === "plus" ? n : -n));
    closeSheet(); commit(); toast("Stok diperbarui", "success");
  };
  return (
    <View style={{ gap: 12 }}>
      <T size={17} weight="700">{p.name}</T>
      <Muted size={13}>Stok saat ini: {p.stock} {p.unit}</Muted>
      <Row2><Btn icon="add" title="Tambah" kind={mode === "plus" ? "primary" : "ghost"} onPress={() => setMode("plus")} /><Btn title="− Kurang" kind={mode === "minus" ? "primary" : "ghost"} onPress={() => setMode("minus")} /></Row2>
      <NumField label="Jumlah" value={qty} onChange={setQty} />
      <Row2><Btn kind="ghost" title="Batal" onPress={closeSheet} /><Btn title="Simpan" onPress={save} /></Row2>
    </View>
  );
}

export function InventoryPage() {
  const app = useApp();
  const c = useC();
  const tracked = app.products.filter((p) => p.track);
  const low = tracked.filter((p) => p.stock > 0 && p.stock <= p.low);
  const out = tracked.filter((p) => p.stock === 0);
  return (
    <Page>
      <PageHead title="Stok" sub={tracked.length + " produk dilacak"} back />
      <View style={{ flexDirection: "row", backgroundColor: c.card, borderRadius: R.lg, borderWidth: 1, borderColor: c.border }}>
        <View style={{ flex: 1, padding: 12 }}><Muted size={12}>Low stock</Muted><T size={20} weight="700" color={c.warn}>{String(low.length)}</T></View>
        <View style={{ width: 1, backgroundColor: c.border }} />
        <View style={{ flex: 1, padding: 12 }}><Muted size={12}>Out of stock</Muted><T size={20} weight="700" color={c.alert}>{String(out.length)}</T></View>
      </View>
      {tracked.length ? tracked.map((p) => (
        <ListRow key={p.id} title={<View style={{ flexDirection: "row", alignItems: "center", gap: 6 }}><T weight="600">{p.name}</T>{p.stock === 0 ? <Pill text="Habis" tone="alert" /> : p.stock <= p.low ? <Pill text="Low" tone="warn" /> : null}</View>} sub={p.cat + " · low threshold " + p.low}
          right={<View style={{ flexDirection: "row", alignItems: "center", gap: 12 }}>
            <T weight="700">{p.stock} {p.unit}</T>
            <Pressable onPress={() => openSheet(<AdjustSheet id={p.id} />)} hitSlop={10}><Icon name="add" size={18} /></Pressable>
          </View>} />
      )) : <Empty title="Belum ada produk tracked" />}
    </Page>
  );
}
