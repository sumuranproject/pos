import React, { useState } from "react";
import { Image, Pressable, View } from "react-native";
import { closeSheet, commit, goTab, goToPrinterSettings, openSheet, state, toast, useApp } from "../lib/store";
import { calculateTotals, nowTime, rupiah, todayStr } from "../lib/format";
import { printTransactionReal } from "../lib/printer";
import { Btn, Card, Chip, Empty, Field, Icon, Muted, NumField, Page, PageHead, Pill, Row2, RupiahInput, Section, SummaryRow, T, useC, R } from "../ui/kit";
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
        <T size={32} weight="700">{rupiah(t.total)}</T>
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
          <Row2>
            <Btn kind="ghost" title="Pas" onPress={() => setCashReceived(t.total)} />
            <Btn kind="ghost" title="50rb" onPress={() => setCashReceived(50000)} />
            <Btn kind="ghost" title="100rb" onPress={() => setCashReceived(100000)} />
          </Row2>
          <SummaryRow label="Kembalian" value={change >= 0 ? rupiah(change) : "—"} color={change >= 0 ? c.cash : c.muted} strong />
        </>
      ) : (
        <View style={{ alignItems: "center", gap: 8, paddingVertical: 8 }}>
          {app.settings.qris.image
            ? <Image source={{ uri: app.settings.qris.image }} style={{ width: 220, height: 220, borderRadius: R.sm }} resizeMode="contain" />
            : <View style={{ width: 160, height: 160, borderRadius: R.md, backgroundColor: c.surfaceAlt, alignItems: "center", justifyContent: "center" }}><Icon name="qr-code-outline" size={90} color={c.faint} /></View>}
          <Muted size={13} style={{ textAlign: "center" }}>Setelah customer scan & bayar, tap Bayar untuk foto bukti QRIS.</Muted>
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
          <Card key={item.id} style={{ gap: 8 }}>
            <View style={{ flexDirection: "row", alignItems: "center", gap: 10 }}>
              <View style={{ flex: 1 }}><T weight="600">{item.name}</T><Muted>{rupiah(item.price)} × {item.qty}</Muted></View>
              <View style={{ flexDirection: "row", alignItems: "center", gap: 6 }}>
                <Pressable onPress={() => change(item.id, "minus")} style={{ width: 36, height: 36, borderRadius: 18, backgroundColor: c.surfaceAlt, alignItems: "center", justifyContent: "center" }}><Icon name="remove" size={20} /></Pressable>
                <T weight="700" style={{ minWidth: 24, textAlign: "center" }}>{item.qty}</T>
                <Pressable onPress={() => change(item.id, "plus")} style={{ width: 36, height: 36, borderRadius: 18, backgroundColor: c.primarySoft, alignItems: "center", justifyContent: "center" }}><Icon name="add" size={20} color={c.primary} /></Pressable>
              </View>
            </View>
            <View style={{ flexDirection: "row", justifyContent: "space-between" }}><Muted>Subtotal</Muted><T weight="600">{rupiah(item.price * item.qty)}</T></View>
          </Card>
        )) : <Empty icon="bag-outline" title="Cart masih kosong" sub="Buka tab Kasir untuk memilih produk." action={<Btn title="Buka Kasir" onPress={() => goTab("pos")} />} />}
      </Page>
      {cartCount > 0 && (
        <Pressable onPress={() => openSheet(<CheckoutSheet />)} style={{ position: "absolute", left: 16, right: 16, bottom: 12, backgroundColor: c.primary, borderRadius: R.lg, padding: 14, flexDirection: "row", alignItems: "center", justifyContent: "space-between" }}>
          <View><T size={12} color="rgba(255,255,255,.85)">{cartCount} item</T><T size={18} weight="700" color="#fff">{rupiah(subtotal)}</T></View>
          <T weight="700" color="#fff">Bayar sekarang</T>
        </Pressable>
      )}
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
      <View style={{ alignItems: "center", gap: 8, paddingTop: 32 }}>
        <View style={{ width: 80, height: 80, borderRadius: 40, backgroundColor: c.cashSoft, alignItems: "center", justifyContent: "center" }}><Icon name="checkmark" size={44} color={c.cash} /></View>
        <T size={22} weight="700">Transaksi berhasil</T>
        <Muted size={13}>{tx.id}</Muted>
        <T size={34} weight="700">{rupiah(tx.total)}</T>
        <View style={{ flexDirection: "row", alignItems: "center", gap: 8 }}>
          <Pill text={tx.method === "cash" ? "Cash" : "QRIS"} tone={tx.method} />
          {tx.method === "cash" ? <Muted size={13}>Kembalian {rupiah(tx.change)}</Muted> : null}
          {tx.sync === "pending" ? <T size={13} color={c.warn}>Menunggu sync</T> : null}
        </View>
      </View>
      <View style={{ gap: 10, marginTop: 24 }}>
        <Btn lg block icon="print" loading={printing} title={hasPrinter ? "Cetak struk" : "Hubungkan printer"} onPress={doPrint} />
        <Btn lg block kind="ghost" title="Transaksi baru" onPress={() => { state.success = null; state.hideNav = false; commit(); }} />
        <Btn block kind="soft" title="Lihat detail" onPress={() => { state.hideNav = false; commit(); showTxDetail(tx.id); }} />
      </View>
    </Page>
  );
}
