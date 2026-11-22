// Sheet detail transaksi, void, dan refund (aturan sama dengan versi web).
import React, { useEffect, useState } from "react";
import { Image, Pressable, View } from "react-native";
import { supabase } from "../lib/supabase";
import { closeSheet, hasPerm, openImageViewer, openSheet, pushAudit, pushNotif, state, toast, useApp } from "../lib/store";
import { formatTimestamp, nowDateTime, rupiah, txAgeMinutes } from "../lib/format";
import { Banner, Btn, Chip, Field, Icon, Muted, Row2, Section, SummaryRow, T, useC, R } from "../ui/kit";
import { pinModal, printReceipt } from "../ui/overlays";

function StatusBanner({ tx }) {
  const c = useC();
  if (tx.status === "void") {
    return <Banner tone="alert" icon="ban-outline"><T size={13} weight="700" color={c.alert}>VOID {tx.voidReason || ""}</T><Muted>oleh {tx.voidedByName || "-"} · {tx.voidedAt || ""}</Muted></Banner>;
  }
  if (tx.status === "refunded" || tx.status === "partial_refund") {
    return (
      <Banner tone="warn" icon="return-up-back-outline">
        <T size={13} weight="700" color={c.warn}>{tx.status === "refunded" ? "REFUND PENUH" : "REFUND SEBAGIAN"} {rupiah(tx.refundAmount)} · {tx.refundMethod === "cash" ? "Cash" : "QRIS"} · {tx.refundReason || ""}</T>
        <Muted>oleh {tx.refundedByName || "-"} · {tx.refundedAt || ""}</Muted>
      </Banner>
    );
  }
  return null;
}

/** Bukti dari Storage: ambil signed URL (berlaku 1 jam). */
function useProofUrl(proof) {
  const [url, setUrl] = useState(proof?.dataUrl || null);
  useEffect(() => {
    if (proof?.dataUrl) { setUrl(proof.dataUrl); return undefined; }
    if (!proof?.path || !supabase) return undefined;
    let alive = true;
    supabase.storage.from("qris-proofs").createSignedUrl(proof.path, 3600).then(({ data }) => { if (alive) setUrl(data?.signedUrl || null); });
    return () => { alive = false; };
  }, [proof?.dataUrl, proof?.path]);
  return url;
}

function QrisProof({ tx }) {
  const c = useC();
  const proofUrl = useProofUrl(tx.qrisProof);
  if (tx.method !== "qris") return null;
  const proof = tx.qrisProof;
  if (proof && (proof.dataUrl || proof.path)) {
    if (proof.expiredAt && Date.now() > proof.expiredAt) {
      return <Banner tone="warn"><T size={13} color={c.warn}>Bukti QRIS kadaluarsa · retensi {state.settings.qris.retentionDays} hari</T></Banner>;
    }
    return (
      <View style={{ gap: 6 }}>
        <Muted size={13}>Bukti QRIS · {formatTimestamp(proof.capturedAt)}</Muted>
        {proofUrl
          ? <Pressable onPress={() => openImageViewer(proofUrl)}><Image source={{ uri: proofUrl }} style={{ width: "100%", height: 180, borderRadius: R.md, backgroundColor: c.surfaceAlt }} resizeMode="cover" /></Pressable>
          : <Muted size={13}>Memuat…</Muted>}
      </View>
    );
  }
  return <Muted size={13}>Tidak ada bukti QRIS</Muted>;
}

function TxDetailSheet({ id }) {
  const app = useApp();
  const c = useC();
  const tx = app.transactions.find((t) => t.id === id);
  if (!tx) return null;
  const owner = app.user.role === "owner";
  const isMyTx = tx.cashierId === app.user.username;
  const canVoid = tx.status === "completed" && owner;
  const canRefund = (tx.status === "completed" || tx.status === "partial_refund") && (owner || (isMyTx && hasPerm("refund")));
  return (
    <View style={{ gap: 8 }}>
      <T size={17} weight="700">{tx.id}</T>
      <Muted size={13}>{tx.date} · {tx.time} · {tx.outlet}</Muted>
      <StatusBanner tx={tx} />
      <SummaryRow label="Kasir" value={tx.cashier} />
      <SummaryRow label="Metode" value={tx.method === "cash" ? "Cash" : "QRIS"} />
      <SummaryRow label="Status sync" value={tx.sync} />
      {tx.createdAt ? <SummaryRow label="Umur" value={txAgeMinutes(tx) + " menit lalu"} /> : null}
      <QrisProof tx={tx} />
      <Section title="Item" />
      {tx.items.map((i, idx) => (
        <View key={idx} style={{ flexDirection: "row", justifyContent: "space-between", paddingVertical: 4 }}>
          <View><T weight="500">{i.name}</T><Muted>{rupiah(i.price)} × {i.qty}</Muted></View>
          <T weight="600">{rupiah(i.price * i.qty)}</T>
        </View>
      ))}
      <SummaryRow label="Subtotal" value={rupiah(tx.items.reduce((s, i) => s + i.price * i.qty, 0))} />
      {tx.discount > 0 && <SummaryRow label="Diskon" value={"-" + rupiah(tx.discount)} color={c.alert} />}
      {tx.tax > 0 && <SummaryRow label={"Pajak " + tx.taxPct + "%"} value={rupiah(tx.tax)} />}
      <SummaryRow strong label="Total" value={rupiah(tx.total)} />
      {tx.method === "cash" && (<><SummaryRow label="Diterima" value={rupiah(tx.received)} /><SummaryRow label="Kembalian" value={rupiah(tx.change)} /></>)}
      <View style={{ gap: 8, marginTop: 8 }}>
        <Btn block icon="print" title="Cetak ulang struk" onPress={() => { closeSheet(); setTimeout(() => printReceipt(tx), 120); }} />
        {(canVoid || canRefund) && (
          <Row2>
            {canVoid ? <Btn kind="danger" title="Void" onPress={() => openVoidSheet(tx.id)} /> : null}
            {canRefund ? <Btn kind="ghost" title="Refund" onPress={() => openRefundSheet(tx.id)} /> : null}
          </Row2>
        )}
      </View>
    </View>
  );
}
export const showTxDetail = (id) => { if (state.transactions.some((t) => t.id === id)) openSheet(<TxDetailSheet id={id} />); };

// ---------------- VOID ----------------
const VOID_REASONS = ["Salah input", "Customer batal", "Double entry", "Lainnya"];
function VoidSheet({ id }) {
  const app = useApp();
  const tx = app.transactions.find((t) => t.id === id);
  const [reason, setReason] = useState(VOID_REASONS[0]);
  const [note, setNote] = useState("");
  if (!tx) return null;
  const confirm = () => {
    let finalReason = reason;
    if (reason === "Lainnya") {
      if (!note.trim()) return toast("Keterangan wajib diisi", "error");
      finalReason = note.trim();
    }
    tx.status = "void"; tx.voidReason = finalReason; tx.voidedAt = nowDateTime();
    tx.voidedBy = state.user.username; tx.voidedByName = state.user.displayName;
    pushAudit("void", { trxId: tx.id, amount: tx.total, reason: finalReason, outlet: tx.outlet, cashier: tx.cashier });
    pushNotif("void", "Void transaksi", tx.id + " · " + rupiah(tx.total) + " · oleh " + state.user.displayName);
    closeSheet();
    toast("Transaksi di-void", "success");
  };
  return (
    <View style={{ gap: 10 }}>
      <T size={17} weight="700">Void transaksi</T>
      <Muted size={13}>{tx.id} · {rupiah(tx.total)} · {tx.method === "cash" ? "Cash" : "QRIS"}</Muted>
      <Banner tone="alert">Void tidak menghapus transaksi dari riwayat. Transaksi akan ditandai VOID dan tidak dihitung di penjualan.</Banner>
      <Section title="Alasan void" style={{ marginTop: 0 }} />
      <View style={{ flexDirection: "row", flexWrap: "wrap", gap: 8 }}>{VOID_REASONS.map((r) => <Chip key={r} label={r} active={r === reason} onPress={() => setReason(r)} />)}</View>
      {reason === "Lainnya" && <Field label="Keterangan" placeholder="Tulis alasan…" value={note} onChangeText={setNote} autoCapitalize="sentences" />}
      <Row2 style={{ marginTop: 8 }}><Btn kind="ghost" title="Batal" onPress={closeSheet} /><Btn kind="danger" title="Void transaksi" onPress={confirm} /></Row2>
    </View>
  );
}
export function openVoidSheet(id) {
  if (state.user.role !== "owner") return toast("Hanya owner yang bisa void transaksi", "error");
  openSheet(<VoidSheet id={id} />);
}

// ---------------- REFUND ----------------
const REFUND_REASONS = ["Barang rusak", "Salah pesan", "Komplain", "Lainnya"];
function RefundSheet({ id }) {
  const app = useApp();
  const c = useC();
  const tx = app.transactions.find((t) => t.id === id);
  const owner = app.user.role === "owner";
  const sec = app.settings.security;
  const already = tx?.refundedItems || [];
  const refundable = (tx?.items || [])
    .map((it, idx) => ({ idx, name: it.name, qty: it.qty, price: it.price, maxQty: it.qty - (already.find((r) => r.idx === idx)?.qty || 0) }))
    .filter((it) => it.maxQty > 0);
  const [checked, setChecked] = useState(() => new Set(refundable.map((it) => it.idx)));
  const [method, setMethod] = useState(tx?.method === "cash" ? "cash" : "qris");
  const [reason, setReason] = useState(REFUND_REASONS[0]);
  const [note, setNote] = useState("");
  if (!tx) return null;

  const qtyOf = (it) => it.maxQty; // refund selalu sisa maksimal per item (sama dengan versi web)
  const totalRefund = refundable.filter((it) => checked.has(it.idx)).reduce((sum, it) => sum + it.price * qtyOf(it), 0);
  const isFull = refundable.every((it) => checked.has(it.idx));
  const exceedsLimit = !owner && totalRefund > sec.refundLimitCashier;
  const toggle = (idx) => setChecked((prev) => { const n = new Set(prev); if (n.has(idx)) n.delete(idx); else n.add(idx); return n; });

  const doRefund = (finalReason) => {
    const selected = refundable.filter((it) => checked.has(it.idx)).map((it) => ({ idx: it.idx, name: it.name, qty: qtyOf(it), amount: it.price * qtyOf(it) }));
    const merged = (tx.refundedItems || []).map((m) => ({ ...m }));
    selected.forEach((si) => { const ex = merged.find((m) => m.idx === si.idx); if (ex) ex.qty += si.qty; else merged.push(si); });
    tx.refundedItems = merged;
    tx.refundAmount = (tx.refundAmount || 0) + totalRefund;
    tx.refundMethod = method; tx.refundReason = finalReason; tx.refundedAt = nowDateTime();
    tx.refundedBy = state.user.username; tx.refundedByName = state.user.displayName;
    const allRefunded = tx.items.every((it, idx) => { const r = merged.find((m) => m.idx === idx); return r && r.qty >= it.qty; });
    tx.status = allRefunded ? "refunded" : "partial_refund";
    pushAudit("refund", { trxId: tx.id, amount: totalRefund, method, reason: finalReason, isFull: allRefunded, outlet: tx.outlet, cashier: tx.cashier });
    pushNotif("refund", "Refund transaksi", tx.id + " · " + rupiah(totalRefund) + " · " + (allRefunded ? "Penuh" : "Sebagian"));
    closeSheet();
    toast(allRefunded ? "Refund penuh berhasil" : "Refund sebagian berhasil", "success");
  };

  const confirm = () => {
    let finalReason = reason;
    if (reason === "Lainnya") {
      if (!note.trim()) return toast("Keterangan wajib diisi", "error");
      finalReason = note.trim();
    }
    if (exceedsLimit) pinModal("PIN Owner", "Nominal refund " + rupiah(totalRefund) + " melebihi limit. Masukkan PIN owner.", () => doRefund(finalReason));
    else doRefund(finalReason);
  };

  return (
    <View style={{ gap: 10 }}>
      <T size={17} weight="700">Refund transaksi</T>
      <Muted size={13}>{tx.id} · Total asli {rupiah(tx.total)}</Muted>
      <Section title="Pilih item yang di-refund" style={{ marginTop: 0 }} />
      {refundable.map((it) => {
        const on = checked.has(it.idx);
        return (
          <Pressable key={it.idx} onPress={() => toggle(it.idx)} style={{ flexDirection: "row", alignItems: "center", gap: 10, padding: 10, borderRadius: R.md, borderWidth: 1, borderColor: on ? c.primary : c.border, backgroundColor: on ? c.primarySoft : c.card }}>
            <Icon name={on ? "checkbox" : "square-outline"} size={22} color={on ? c.primary : c.faint} />
            <View style={{ flex: 1 }}><T weight="500">{it.name}</T><Muted>{rupiah(it.price)} × {qtyOf(it)} (max {it.maxQty})</Muted></View>
            <T weight="600">{rupiah(it.price * qtyOf(it))}</T>
          </Pressable>
        );
      })}
      <SummaryRow strong label="Total refund" value={rupiah(totalRefund)} color={c.warn} />
      {exceedsLimit && <Banner tone="warn">Nominal refund ({rupiah(totalRefund)}) melebihi limit kasir ({rupiah(sec.refundLimitCashier)}). Butuh PIN owner.</Banner>}
      <Section title="Metode refund" />
      <Row2><Btn title="Cash" kind={method === "cash" ? "primary" : "ghost"} onPress={() => setMethod("cash")} /><Btn title="QRIS" kind={method === "qris" ? "primary" : "ghost"} onPress={() => setMethod("qris")} /></Row2>
      <Section title="Alasan refund" style={{ marginTop: 0 }} />
      <View style={{ flexDirection: "row", flexWrap: "wrap", gap: 8 }}>{REFUND_REASONS.map((r) => <Chip key={r} label={r} active={r === reason} onPress={() => setReason(r)} />)}</View>
      {reason === "Lainnya" && <Field label="Keterangan" placeholder="Tulis alasan…" value={note} onChangeText={setNote} autoCapitalize="sentences" />}
      <Row2 style={{ marginTop: 8 }}>
        <Btn kind="ghost" title="Batal" onPress={closeSheet} />
        <Btn title={(isFull ? "Refund penuh" : "Refund sebagian") + " · " + rupiah(totalRefund)} disabled={totalRefund <= 0} onPress={confirm} />
      </Row2>
    </View>
  );
}
export const openRefundSheet = (id) => openSheet(<RefundSheet id={id} />);
