import React, { useState } from "react";
import { Image, Pressable, View } from "react-native";
import { closeSheet, commit, goTab, goToPrinterSettings, openSheet, state, toast, useApp } from "../lib/store";
import { calculateTotals, nowTime, rupiah, todayStr } from "../lib/format";
import { printTransactionReal } from "../lib/printer";
import { Btn, CartCta, Card, Chip, Empty, Field, Icon, LinkBtn, Muted, NumField, Page, PageHead, Pill, Row2, RupiahInput, Section, SummaryRow, T, useC, R } from "../ui/kit";
import { captureQrisProof, confirmModal, showQrisCaptureModal } from "../ui/overlays";
import { showTxDetail } from "./TxSheets";

/** ID transaksi unik lintas perangkat: urutan lokal + 4 karakter acak (hindari tabrakan antar kasir). */
function newTxId() {
  const today = todayStr();
  const seq = state.transactions.filter((t) => t.date === today).length + 1;
  const rand = Math.random().toString(36).slice(2, 6).toUpperCase().padEnd(4, "0");
  return "TRX-" + today.replace(/-/g, "") + "-" + String(seq).padStart(4, "0") + "-" + rand;
}

function completeTransaction(t, method, cashReceived, proof) {
  const tx = {
    id: newTxId(), time: nowTime(), date: todayStr(), createdAt: Date.now(),
    items: state.cart.map((i) => ({ name: i.name, qty: i.qty, price: i.price })),
    total: t.total, method, sync: "pending",
    cashier: state.user.displayName.split(" ")[0], cashierId: state.user.username,
    outlet: state.user.role === "cashier" ? state.user.outlet : state.outlet.name,
    discount: t.discount, tax: t.tax, taxPct: t.taxPct,
    received: method === "cash" ? cashReceived : null,
    change: method === "cash" ? cashReceived - t.total : 0,
    status: "completed",
    qrisProof: method === "qris" && proof ? {
      dataUrl: proof.dataUrl, capturedAt: proof.capturedAt, deviceModel: proof.deviceModel, fileSize: proof.fileSize,
      expiredAt: proof.capturedAt + state.settings.qris.retentionDays * 86400000,
    } : null,
  };
  state.transactions.unshift(tx);
  if (state.activeShift) {
    state.activeShift.transactions++;
    if (method === "cash") state.activeShift.cashSales += t.total; else state.activeShift.qrisSales += t.total;
  }
  state.cart.forEach((item) => {
    const p = state.products.find((x) => x.id === item.id);
    if (p && p.track) p.stock = Math.max(0, p.stock - item.qty);
  });
  state.cart = [];
  state.sheet = null;
  state.success = tx;
  state.hideNav = true;
  commit();
}

function CheckoutSheet() {
  const app = useApp();
  const c = useC();
  const [method, setMethod] = useState("cash");
  const [discount, setDiscount] = useState(0);
  const [taxStr, setTaxStr] = useState("0");
  const [cashReceived, setCashReceived] = useState(0);
  const taxPct = Math.max(0, Math.min(100, parseInt(taxStr, 10) || 0));
  const subtotal = app.cart.reduce((s, i) => s + i.price * i.qty, 0);
  const t = calculateTotals(app.cart, Math.min(discount, subtotal), taxPct);
  const change = cashReceived - t.total;

  const pay = () => {
    if (!app.cart.length) return;
    if (method === "cash") {
      if (cashReceived < t.total) return toast("Uang diterima kurang dari total", "error");
      return completeTransaction(t, "cash", cashReceived, null);
    }
    closeSheet();
    setTimeout(() => showQrisCaptureModal(() => captureQrisProof((proof) => completeTransaction(t, "qris", cashReceived, proof))), 120);
  };

  return (
    <View style={{ gap: 12 }}>
      <T size={17} weight="700">Pembayaran</T>
      <View style={{ alignItems: "center", paddingVertical: 10, borderBottomWidth: 0.5, borderBottomColor: c.border }}>
        <Muted size={13}>Total</Muted>
        <T size={36} weight="700">{rupiah(t.total)}</T>
      </View>
      <Section title="Diskon & Pajak" style={{ marginTop: 0 }} />
      <Row2>
        <RupiahInput label="Diskon (Rp)" value={discount} onChange={setDiscount} />
        <NumField label="Pajak (%)" value={taxStr} onChange={setTaxStr} maxLength={3} />
      </Row2>
      <Section title="Metode Pembayaran" style={{ marginTop: 0 }} />
      <Row2>
        <Btn title="Cash" kind={method === "cash" ? "primary" : "ghost"} onPress={() => setMethod("cash")} />
        <Btn title="QRIS" kind={method === "qris" ? "primary" : "ghost"} onPress={() => setMethod("qris")} />
      </Row2>
      {method === "cash" ? (
        <>
          <RupiahInput label="Uang diterima" value={cashReceived} onChange={setCashReceived} />
          <View style={{ flexDirection: "row", gap: 8 }}>
            <Btn kind="ghost" title="Pas" onPress={() => setCashReceived(t.total)} />
            <Btn kind="ghost" title="50rb" onPress={() => setCashReceived(50000)} />
            <Btn kind="ghost" title="100rb" onPress={() => setCashReceived(100000)} />
          </View>
          <View style={{ flexDirection: "row", justifyContent: "space-between", alignItems: "center", paddingVertical: 4 }}>
            <T size={17} weight="500">Kembalian</T>
            <T size={17} weight="700" color={change >= 0 ? c.cash : c.muted}>{change >= 0 ? rupiah(change) : "—"}</T>
          </View>
        </>
      ) : (
        <View style={{ alignItems: "center", gap: 10, paddingVertical: 18, paddingHorizontal: 16, backgroundColor: c.surfaceAlt, borderRadius: R.lg }}>
          {app.settings.qris.image
            ? <Image source={{ uri: app.settings.qris.image }} style={{ width: 220, height: 220, borderRadius: R.sm }} resizeMode="contain" />
            : <Icon name="qr-code-outline" size={140} color={c.text} />}
          <Muted size={13} style={{ textAlign: "center" }}>Scan QR ini untuk membayar</Muted>
          <Muted size={12} style={{ textAlign: "center" }}>Setelah customer scan & bayar, tap Bayar untuk foto bukti QRIS.</Muted>
        </View>
      )}
      <View>
        <SummaryRow label="Subtotal" value={rupiah(t.subtotal)} />
        <SummaryRow label="Diskon" value={t.discount > 0 ? "-" + rupiah(t.discount) : ""} color={c.alert} />
        <SummaryRow label={"Pajak " + taxPct + "%"} value={t.tax > 0 ? rupiah(t.tax) : ""} />
        <SummaryRow strong label="Total" value={rupiah(t.total)} />
      </View>
      <Btn lg block title={"Bayar " + rupiah(t.total)} onPress={pay} />
    </View>
  );
}

export function CheckoutPage() {
  const app = useApp();
  const c = useC();
  const cartCount = app.cart.reduce((s, i) => s + i.qty, 0);
  const { subtotal } = calculateTotals(app.cart);

  const change = (id, dir) => {
    const item = state.cart.find((x) => x.id === id);
    if (!item) return;
    if (dir === "plus") {
      const p = state.products.find((x) => x.id === id);
      if (p && p.track && item.qty >= p.stock) return toast("Stok tidak cukup", "error");
      item.qty++;
    } else {
      item.qty--;
      if (item.qty <= 0) state.cart = state.cart.filter((x) => x.id !== id);
    }
    commit();
  };

  return (
    <View style={{ flex: 1 }}>
      <Page bottom={100}>
        <PageHead
          title="Checkout" sub={cartCount ? cartCount + " item siap dibayar" : "Belum ada item di cart"}
          right={cartCount > 0 ? <Btn kind="ghost" title="Kosongkan" onPress={() => confirmModal("Kosongkan cart?", "Semua item akan dihapus.", "Kosongkan", () => { state.cart = []; commit(); })} /> : null}
        />
        {cartCount ? app.cart.map((item) => (
          <Card key={item.id} style={{ gap: 10 }}>
            <View style={{ flexDirection: "row", alignItems: "center", gap: 10 }}>
              <View style={{ flex: 1 }}><T weight="600">{item.name}</T><Muted>{rupiah(item.price)} × {item.qty}</Muted></View>
              <View style={{ flexDirection: "row", alignItems: "center", backgroundColor: c.surfaceAlt, borderRadius: 10, padding: 2 }}>
                <Pressable onPress={() => change(item.id, "minus")} style={{ width: 32, height: 32, alignItems: "center", justifyContent: "center" }}><Icon name="remove" size={16} /></Pressable>
                <T weight="700" style={{ minWidth: 28, textAlign: "center" }}>{item.qty}</T>
                <Pressable onPress={() => change(item.id, "plus")} style={{ width: 32, height: 32, alignItems: "center", justifyContent: "center" }}><Icon name="add" size={16} /></Pressable>
              </View>
            </View>
            <View style={{ flexDirection: "row", justifyContent: "space-between", alignItems: "center", borderTopWidth: 1, borderTopColor: c.border, borderStyle: "dashed", paddingTop: 10 }}>
              <T size={11} weight="700" color={c.muted} style={{ letterSpacing: 0.8 }}>SUBTOTAL</T>
              <T weight="700" size={16} color={c.primary}>{rupiah(item.price * item.qty)}</T>
            </View>
          </Card>
        )) : <Empty icon="bag-outline" title="Cart masih kosong" sub="Buka tab Kasir untuk memilih produk." action={<Btn title="Buka Kasir" onPress={() => goTab("pos")} />} />}
      </Page>
      {cartCount > 0 && <CartCta count={cartCount} total={rupiah(subtotal)} label="Bayar sekarang" onPress={() => openSheet(<CheckoutSheet />)} />}
    </View>
  );
}

export function SuccessScreen({ tx }) {
  const c = useC();
  const [printing, setPrinting] = useState(false);
  const hasPrinter = Boolean(state.settings.printer.deviceId);
  const doPrint = async () => {
    if (!hasPrinter) return goToPrinterSettings();
    setPrinting(true);
    try { toast("Struk sedang dicetak…"); await printTransactionReal(tx); }
    catch (e) { toast("Gagal mencetak: " + (e.message || e), "error"); }
    finally { setPrinting(false); }
  };
  return (
    <Page>
      <PageHead title="Checkout" />
      <View style={{ alignItems: "center", gap: 8, paddingTop: 36 }}>
        <View style={{ width: 64, height: 64, borderRadius: 32, backgroundColor: c.cashSoft, alignItems: "center", justifyContent: "center" }}><Icon name="checkmark" size={32} color={c.cash} /></View>
        <T size={22} weight="700" style={{ marginTop: 6 }}>Transaksi berhasil</T>
        <T size={12} color={c.muted} style={{ fontFamily: "monospace" }}>{tx.id}</T>
        <T size={28} weight="700" style={{ marginTop: 6 }}>{rupiah(tx.total)}</T>
        <View style={{ flexDirection: "row", alignItems: "center", gap: 8 }}>
          <Pill text={tx.method === "cash" ? "Cash" : "QRIS"} tone={tx.method} />
          {tx.method === "cash" ? <Muted size={13}>· Kembalian {rupiah(tx.change)}</Muted> : null}
          {tx.sync === "pending" ? <T size={13} color={c.warn}>Menunggu sync</T> : null}
        </View>
      </View>
      <View style={{ gap: 10, marginTop: 28 }}>
        <Btn block icon="print" loading={printing} title={hasPrinter ? "Cetak struk" : "Hubungkan printer"} onPress={doPrint} />
        <Btn block kind="ghost" title="Transaksi baru" onPress={() => { state.success = null; state.hideNav = false; commit(); }} />
        <View style={{ alignItems: "center", paddingTop: 4 }}><LinkBtn title="Lihat detail" onPress={() => { state.hideNav = false; commit(); showTxDetail(tx.id); }} /></View>
      </View>
    </Page>
  );
}
