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
  return (
    <Pressable onPress={() => showTxDetail(tx.id)} style={{ backgroundColor: c.card, borderRadius: R.md, borderWidth: 1, borderColor: edge, padding: 12, gap: 3 }}>
      <View style={{ flexDirection: "row", alignItems: "center", gap: 8 }}>
        <T weight="600" style={{ flex: 1 }}>#{tx.id.slice(-4)} · {tx.time}</T>
        <Dot status={tx.sync} />
        <TxStatusBadge tx={tx} />
        <T weight="700">{rupiah(tx.total)}</T>
      </View>
      <Muted>{tx.items.reduce((s, i) => s + i.qty, 0)} item · {tx.method === "cash" ? "Cash" : "QRIS"} · {tx.cashier}</Muted>
      {tx.status === "partial_refund" && <T size={12} color={c.warn}>Refund sebagian · {rupiah(tx.refundAmount || 0)}</T>}
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
