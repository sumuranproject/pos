import React, { useState } from "react";
import { View } from "react-native";
import { closeSheet, commit, openSheet, state, useApp } from "../lib/store";
import { nowTime, rupiah } from "../lib/format";
import { Btn, Card, Empty, ListRow, Muted, Page, PageHead, RupiahInput, Section, SummaryRow, T, useC } from "../ui/kit";
import { confirmModal } from "../ui/overlays";

function ShiftRows({ s }) {
  const c = useC();
  return (
    <View>
      <SummaryRow line label="Kas awal" value={rupiah(s.openingCash)} />
      <SummaryRow line label="Transaksi" value={String(s.transactions)} />
      <SummaryRow line label="Cash" value={rupiah(s.cashSales)} />
      <SummaryRow line label="QRIS" value={rupiah(s.qrisSales)} />
      <SummaryRow strong label="Expected cash" value={rupiah(s.openingCash + s.cashSales)} color={c.primary} />
    </View>
  );
}
export function ShiftPage() {
  const app = useApp();
  const c = useC();
  const s = app.activeShift;
  const mine = app.user.role === "owner" ? app.shiftHistory : app.shiftHistory.filter((x) => x.cashierId === app.user.username);
  return (
    <Page>
      <PageHead title="Shift" sub="Riwayat & status" />
      {s ? (
        <Card style={{ padding: 16 }}>
          <View style={{ flexDirection: "row", alignItems: "center", gap: 8 }}><View style={{ width: 8, height: 8, borderRadius: 4, backgroundColor: c.cash }} /><T size={12} weight="700" color={c.cash} style={{ letterSpacing: 0.6 }}>SHIFT AKTIF</T></View>
          <Muted size={13} style={{ marginVertical: 8 }}>Mulai {s.startTime} · {app.user.displayName}</Muted>
          <ShiftRows s={s} />
          <Btn block title="Tutup shift" style={{ marginTop: 14 }} onPress={() => openCloseShiftSheet()} />
        </Card>
      ) : <Empty icon="time-outline" title="Belum ada shift aktif" sub="Mulai shift dari halaman Kasir." />}
      <Section title="Riwayat shift" />
      {mine.length ? mine.map((sh) => (
        <Card key={sh.id} style={{ flexDirection: "row", alignItems: "center", justifyContent: "space-between", paddingVertical: 12 }}>
          <View style={{ flex: 1, gap: 4 }}>
            <T size={12} color={c.muted} style={{ fontFamily: "monospace" }}>{sh.id} · {sh.start}-{sh.end}</T>
            <Muted>{sh.tx} transaksi · Variance {rupiah(sh.variance)}</Muted>
          </View>
          <T weight="700" size={16}>{rupiah(sh.cash + sh.qris)}</T>
        </Card>
      )) : <Muted size={13}>Belum ada riwayat.</Muted>}
    </Page>
  );
}

function ShiftDetailSheet() {
  const { activeShift: s, user } = useApp();
  if (!s) return null;
  return (
    <View style={{ gap: 6 }}>
      <T size={17} weight="700">Shift aktif</T>
      <Muted size={13} style={{ marginBottom: 8 }}>Mulai {s.startTime} · {user.displayName}</Muted>
      <ShiftRows s={s} />
      <Btn block title="Tutup shift" style={{ marginTop: 12 }} onPress={() => openCloseShiftSheet()} />
    </View>
  );
}
export const openShiftDetailSheet = () => { if (state.activeShift) openSheet(<ShiftDetailSheet />); };

function CloseShiftSheet() {
  const s = state.activeShift;
  const expected = s.openingCash + s.cashSales;
  const [closing, setClosing] = useState(expected);
  const doClose = () => {
    const variance = closing - expected;
    state.shiftHistory.unshift({
      id: s.id, start: s.startTime, end: nowTime(), opening: s.openingCash, closing, expected, variance,
      cash: s.cashSales, qris: s.qrisSales, tx: s.transactions,
      cashierId: state.user.username, cashierName: state.user.displayName, outlet: state.user.outlet || state.outlet.name,
    });
    state.activeShift = null;
    closeSheet();
    const vText = variance === 0 ? "Pas, tidak ada selisih." : variance > 0 ? "Lebih " + rupiah(variance) + "." : "Kurang " + rupiah(Math.abs(variance)) + ".";
    setTimeout(() => confirmModal("Shift ditutup", vText, "OK", () => {}, false), 120);
  };
  return (
    <View style={{ gap: 10 }}>
      <T size={17} weight="700">Tutup shift</T>
      <Muted size={13}>Hitung uang fisik, lalu masukkan jumlahnya.</Muted>
      <SummaryRow label="Expected cash" value={rupiah(expected)} />
      <RupiahInput label="Uang fisik (Rp)" value={closing} onChange={setClosing} />
      <Btn block title="Tutup shift" onPress={doClose} />
    </View>
  );
}
export const openCloseShiftSheet = () => { if (state.activeShift) openSheet(<CloseShiftSheet />); };

