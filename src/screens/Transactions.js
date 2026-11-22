import React from "react";
import { Pressable, View } from "react-native";
import { useApp } from "../lib/store";
import { rupiah } from "../lib/format";
import { Dot, Empty, Muted, Page, PageHead, Pill, T, useC, R } from "../ui/kit";
import { showTxDetail } from "./TxSheets";

export function TxStatusBadge({ tx }) {
  if (tx.status === "void") return <Pill text="VOID" tone="alert" />;
  if (tx.status === "refunded") return <Pill text="REFUND" tone="warn" />;
  if (tx.status === "partial_refund") return <Pill text="PARTIAL" tone="warn" />;
  return null;
}

export function TxItem({ tx }) {
  const c = useC();
  const edge = tx.status === "void" ? c.alert : tx.status === "refunded" || tx.status === "partial_refund" ? c.warn : c.border;
  const sync = tx.sync === "synced" ? { t: "Synced", tone: "cash", i: "✓" } : tx.sync === "error" ? { t: "Error", tone: "alert", i: "▲" } : { t: "Pending", tone: "warn", i: "⏳" };
  return (
    <Pressable onPress={() => showTxDetail(tx.id)} style={{ backgroundColor: c.card, borderRadius: R.lg, borderWidth: 1, borderColor: edge, padding: 12, flexDirection: "row", alignItems: "center", gap: 8 }}>
      <View style={{ flex: 1, gap: 4 }}>
        <View style={{ flexDirection: "row", alignItems: "center", gap: 6, flexWrap: "wrap" }}>
          <T size={12} color={c.muted} style={{ fontFamily: "monospace" }}>#{tx.id.split("-")[2] || tx.id.slice(-4)} · {tx.time}</T>
          <Pill text={sync.t} tone={sync.tone} icon={sync.i} />
          <TxStatusBadge tx={tx} />
        </View>
        <Muted>{tx.items.reduce((s, i) => s + i.qty, 0)} item · {tx.method === "cash" ? "Cash" : "QRIS"}{tx.status === "partial_refund" ? " · refund " + rupiah(tx.refundAmount || 0) : ""}</Muted>
      </View>
      <T weight="700" size={16}>{rupiah(tx.total)}</T>
    </Pressable>
  );
}

export function TransactionsPage() {
  const app = useApp();
  const owner = app.user.role === "owner";
  const list = owner ? app.transactions : app.transactions.filter((t) => t.cashierId === app.user.username);
  return (
    <Page>
      <PageHead title="Riwayat Transaksi" sub={list.length + " transaksi"} />
      {list.length ? list.map((tx) => <TxItem key={tx.id} tx={tx} />) : <Empty icon="list-outline" title="Belum ada transaksi" sub="Transaksi akan muncul di sini." />}
    </Page>
  );
}
