import React, { useState } from "react";
import { View } from "react-native";
import { closeSheet, commit, openSheet, state, useApp } from "../lib/store";
import { nowTime, rupiah } from "../lib/format";
import { Btn, Card, Empty, ListRow, Muted, Page, PageHead, RupiahInput, Section, SummaryRow, T, useC } from "../ui/kit";
import { confirmModal } from "../ui/overlays";

function ShiftRows({ s }) {
  return (
    <View>
      <SummaryRow label="Kas awal" value={rupiah(s.openingCash)} />
      <SummaryRow label="Transaksi" value={String(s.transactions)} />
      <SummaryRow label="Cash" value={rupiah(s.cashSales)} />
      <SummaryRow label="QRIS" value={rupiah(s.qrisSales)} />
      <SummaryRow label="Total penjualan" value={rupiah(s.cashSales + s.qrisSales)} />
      <SummaryRow strong label="Expected cash" value={rupiah(s.openingCash + s.cashSales)} />
    </View>
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

export function ShiftPage() {
  const app = useApp();
  const c = useC();
  const s = app.activeShift;
  const mine = app.user.role === "owner" ? app.shiftHistory : app.shiftHistory.filter((x) => x.cashierId === app.user.username);
  return (
    <Page>
      <PageHead title="Shift" sub="Riwayat & status" />
      {s ? (
        <Card>
          <T weight="700" color={c.cash}>Shift aktif</T>
          <Muted size={13} style={{ marginBottom: 8 }}>Mulai {s.startTime} · {app.user.displayName}</Muted>
          <ShiftRows s={s} />
          <Btn block title="Tutup shift" style={{ marginTop: 10 }} onPress={() => openCloseShiftSheet()} />
        </Card>
      ) : <Empty icon="time-outline" title="Belum ada shift aktif" sub="Mulai shift dari halaman Kasir." />}
      <Section title="Riwayat shift" />
      {mine.length ? mine.map((sh) => (
        <ListRow key={sh.id} title={sh.id + " · " + sh.start + "–" + sh.end} sub={sh.tx + " transaksi · Selisih " + rupiah(sh.variance)} amount={rupiah(sh.cash + sh.qris)} />
      )) : <Muted size={13}>Belum ada riwayat.</Muted>}
    </Page>
  );
}
