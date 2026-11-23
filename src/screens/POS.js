import React, { useState } from "react";
import { FlatList, Pressable, ScrollView, TextInput, View, useWindowDimensions } from "react-native";
import { commit, goTab, state, toast, useApp } from "../lib/store";
import { nowTime, rupiah, uid, calculateTotals } from "../lib/format";
import { Btn, CartCta, Chip, Empty, Icon, Muted, Page, PageHead, R, T, useC, useSidebarW, useWide } from "../ui/kit";
import { promptModal } from "../ui/overlays";
import { openShiftDetailSheet } from "./Shift";

export function addToCart(id) {
  const p = state.products.find((x) => x.id === id);
  if (!p) return;
  const ex = state.cart.find((x) => x.id === id);
  if (ex) {
    if (p.track && ex.qty >= p.stock) return toast("Stok tidak cukup", "error");
    ex.qty++;
  } else {
    if (p.track && p.stock <= 0) return toast("Stok habis", "error");
    state.cart.push({ id: p.id, name: p.name, price: p.price, qty: 1, unit: p.unit });
  }
  commit();
}

function ProductCard({ p, width }) {
  const c = useC();
  const out = p.track && p.stock === 0;
  const low = p.track && p.stock > 0 && p.stock <= p.low;
  const inCart = state.cart.find((x) => x.id === p.id)?.qty || 0;
  return (
    <Pressable onPress={() => addToCart(p.id)} style={{ width, backgroundColor: c.card, borderRadius: R.lg, borderWidth: 1, borderColor: inCart ? c.primary : c.border, padding: 12, minHeight: 96, opacity: out ? 0.45 : 1 }}>
      {(out || low) && (
        <View style={{ position: "absolute", top: 8, right: 8, backgroundColor: out ? c.alertSoft : c.warnSoft, borderRadius: 6, paddingHorizontal: 6, paddingVertical: 2 }}>
          <T size={10} weight="700" color={out ? c.alert : c.warn}>{out ? "HABIS" : "SISA " + p.stock}</T>
        </View>
      )}
      <T weight="600" numberOfLines={2} style={{ marginTop: out || low ? 14 : 0 }}>{p.name}</T>
      <Muted size={11}>/ {p.unit}</Muted>
      <T weight="700" size={14} color={c.primary} style={{ marginTop: "auto", paddingTop: 6 }}>{rupiah(p.price)}</T>
      {inCart > 0 && <View style={{ position: "absolute", bottom: 8, right: 8, backgroundColor: c.primary, borderRadius: 10, minWidth: 20, height: 20, alignItems: "center", justifyContent: "center", paddingHorizontal: 5 }}><T size={11} weight="700" color="#fff">{inCart}</T></View>}
    </Pressable>
  );
}

function CartBar() {
  const app = useApp();
  if (!app.cart.length) return null;
  const count = app.cart.reduce((s, i) => s + i.qty, 0);
  const { total } = calculateTotals(app.cart);
  return <CartCta count={count} total={rupiah(total)} label="Checkout" onPress={() => goTab("checkout")} />;
}

export default function POSPage() {
  const app = useApp();
  const c = useC();
  const { width: SW } = useWindowDimensions();
  const wide = useWide();
  const sideW = useSidebarW();
  const W = wide ? SW - sideW : SW;
  const [q, setQ] = useState("");
  const [cat, setCat] = useState("Semua");

  if (app.user.role === "cashier" && !app.activeShift) {
    const startShift = () => promptModal("Mulai shift", "Kas awal (Rp)", "200000", (v) => {
      state.activeShift = { id: uid("SH"), startTime: nowTime(), openingCash: parseInt(v, 10) || 0, transactions: 0, cashSales: 0, qrisSales: 0 };
      toast("Shift dimulai", "success"); commit();
    }, true);
    return (
      <Page>
        <View style={{ marginBottom: 4 }}><T size={22} weight="700">Kasir</T><Muted size={13}>Mulai shift dulu untuk transaksi</Muted></View>
        <Empty icon="time-outline" title="Shift belum dimulai" sub="Anda harus mulai shift dan mengisi kas awal sebelum bisa membuat transaksi." action={<Btn title="Mulai Shift Sekarang" onPress={startShift} />} />
      </Page>
    );
  }

  const s = app.activeShift;
  const cats = ["Semua", ...app.categories.filter((x) => x.active).map((x) => x.name)];
  let list = app.products.filter((p) => p.active);
  if (cat !== "Semua") list = list.filter((p) => p.cat === cat);
  if (q) list = list.filter((p) => p.name.toLowerCase().includes(q.toLowerCase()));

  const cols = W >= 1000 ? 5 : W >= 640 ? 4 : W >= 460 ? 3 : 2;
  const gap = 10; const pad = 16;
  const cardW = Math.floor((Math.min(W, 1100) - pad * 2 - gap * (cols - 1)) / cols);

  return (
    <View style={{ flex: 1 }}>
      <View style={{ backgroundColor: wide ? "transparent" : c.card, borderBottomWidth: wide ? 0 : 1, borderBottomColor: c.border, paddingTop: wide ? 16 : 4, paddingBottom: wide ? 4 : 12, gap: 10 }}>
        <View style={{ flexDirection: "row", alignItems: "center", backgroundColor: c.card, borderWidth: 1, borderColor: c.borderStrong, borderRadius: 22, paddingHorizontal: 14, minHeight: 44, marginHorizontal: pad }}>
          <Icon name="search" size={18} color={c.faint} style={{ marginRight: 8 }} />
          <TextInput value={q} onChangeText={setQ} placeholder="Cari produk…" placeholderTextColor={c.faint} style={{ flex: 1, color: c.text, fontSize: 15, fontFamily: "inter_regular" }} />
        </View>
        <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={{ gap: 8, paddingHorizontal: pad }}>
          {cats.map((x) => <Chip key={x} label={x} active={x === cat} onPress={() => setCat(x)} />)}
        </ScrollView>
      </View>
      <FlatList
        key={cols} numColumns={cols} data={list} keyExtractor={(p) => String(p.id)}
        columnWrapperStyle={cols > 1 ? { gap } : undefined}
        contentContainerStyle={{ padding: pad, gap, paddingBottom: 110, alignSelf: "center", width: "100%", maxWidth: 1100 }}
        keyboardShouldPersistTaps="handled" extraData={app.cart.length + ":" + app.cart.reduce((a, i) => a + i.qty, 0)}
        ListHeaderComponent={s ? (
          <Pressable onPress={openShiftDetailSheet} style={{ flexDirection: "row", alignItems: "center", gap: 10, backgroundColor: c.card, borderRadius: R.md, borderWidth: 1, borderColor: c.border, padding: 10, marginBottom: 4 }}>
            <View style={{ width: 9, height: 9, borderRadius: 5, backgroundColor: c.cash }} />
            <View style={{ flex: 1 }}><T weight="600" size={13}>Shift aktif · {s.startTime}</T><Muted>{s.transactions} transaksi · {rupiah(s.cashSales + s.qrisSales)}</Muted></View>
            <Icon name="chevron-forward" size={18} color={c.muted} />
          </Pressable>
        ) : null}
        ListEmptyComponent={<Empty icon="search" title={app.products.length ? "Produk tidak ditemukan" : "Belum ada produk"} sub={app.products.length ? "Coba kata kunci lain." : "Tambahkan produk di Kelola › Produk."} />}
        renderItem={({ item }) => <ProductCard p={item} width={cardW} />}
      />
      <CartBar />
    </View>
  );
}
