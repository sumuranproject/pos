import React, { useState } from "react";
import { Pressable, ScrollView, View } from "react-native";
import { closeSheet, commit, openSheet, state, toast, useApp } from "../lib/store";
import { computeGrowth, dateStr, getPrevPeriodRange, growthIcon, netOf, nowDateTime, periodLabel, periodRange, rupiah, todayStr, uid } from "../lib/format";
import { Btn, Card, Chip, Empty, Field, Icon, IconBtn, Kpi, ListRow, Muted, Page, PageHead, Row2, RupiahInput, Section, Select, SummaryRow, T, useC, R } from "../ui/kit";
import { confirmModal } from "../ui/overlays";
import { TxItem } from "./Transactions";

export const EXPENSE_CATS = ["Bahan", "Operasional", "Gaji", "Lainnya"];
const sum = (list, fn) => list.reduce((s, t) => s + fn(t), 0);

// ---------------- Dashboard kasir (hari ini) ----------------
export function DashboardPage() {
  const app = useApp();
  const c = useC();
  const today = todayStr();
  const yDate = new Date(); yDate.setDate(yDate.getDate() - 1);
  const yesterday = dateStr(yDate);
  const myTxs = app.transactions.filter((t) => t.cashierId === app.user.username);
  const todayTx = myTxs.filter((t) => t.date === today);
  const active = todayTx.filter((t) => t.status !== "void");
  const totalSales = sum(active, netOf);
  const cash = sum(active.filter((t) => t.method === "cash"), netOf);
  const qris = sum(active.filter((t) => t.method === "qris"), netOf);
  const voidAmount = sum(todayTx.filter((t) => t.status === "void"), (t) => t.total);
  const refundAmount = sum(todayTx, (t) => t.refundAmount || 0);
  const ySales = sum(myTxs.filter((t) => t.date === yesterday && t.status !== "void"), netOf);
  const growth = computeGrowth(totalSales, ySales);
  const sh = app.activeShift;
  return (
    <Page>
      <PageHead title="Laporan" sub="Ringkasan & riwayat hari ini" />
      <Kpi hero label="Ringkasan Penjualan · Hari ini" value={rupiah(totalSales)} sub={active.length + " transaksi · " + growthIcon(growth.direction) + " " + growth.pct.toFixed(0) + "% dari kemarin (" + rupiah(ySales) + ")"} />
      <View style={{ flexDirection: "row", flexWrap: "wrap", gap: 10 }}>
        <Kpi label="Cash" value={rupiah(cash)} /><Kpi label="QRIS" value={rupiah(qris)} />
        <Kpi label="Void" value={rupiah(voidAmount)} color={c.alert} /><Kpi label="Refund" value={rupiah(refundAmount)} color={c.warn} />
      </View>
      {sh && (
        <Card>
          <T weight="700" color={c.cash}>Shift aktif</T><Muted size={13}>Mulai {sh.startTime}</Muted>
          <SummaryRow label="Transaksi shift ini" value={String(sh.transactions)} />
          <SummaryRow label="Cash" value={rupiah(sh.cashSales)} /><SummaryRow label="QRIS" value={rupiah(sh.qrisSales)} />
          <SummaryRow strong label="Total" value={rupiah(sh.cashSales + sh.qrisSales)} />
        </Card>
      )}
      <Section title={"Riwayat Transaksi Hari Ini (" + todayTx.length + ")"} />
      {todayTx.length ? todayTx.map((tx) => <TxItem key={tx.id} tx={tx} />) : <Empty title="Belum ada transaksi hari ini" sub="Transaksi yang kamu buat hari ini akan muncul di sini." />}
    </Page>
  );
}

// ---------------- Filter ----------------
const PERIODS = [["all", "Semua"], ["today", "Hari ini"], ["week", "7 hari"], ["month", "Bulan ini"], ["lastmonth", "Bulan lalu"], ["custom", "Custom"]];
function FilterSheet() {
  const app = useApp();
  const rf = app.reportFilter;
  const [period, setPeriod] = useState(rf.period);
  const [user, setUser] = useState(rf.user);
  const [dateFrom, setDateFrom] = useState(rf.dateFrom || todayStr());
  const [dateTo, setDateTo] = useState(rf.dateTo || todayStr());
  const users = [{ value: "all", label: "Semua kasir" }, ...app.workers.map((w) => ({ value: w.username, label: w.displayName }))];
  return (
    <View style={{ gap: 10 }}>
      <T size={17} weight="700">Filter Laporan</T>
      <Select label="Periode" value={period} options={PERIODS.map(([value, label]) => ({ value, label }))} onChange={setPeriod} />
      {period === "custom" && (
        <Row2>
          <Field label="Dari (YYYY-MM-DD)" value={dateFrom} onChangeText={setDateFrom} placeholder="2026-11-01" keyboardType="numbers-and-punctuation" />
          <Field label="Sampai (YYYY-MM-DD)" value={dateTo} onChangeText={setDateTo} placeholder="2026-11-30" keyboardType="numbers-and-punctuation" />
        </Row2>
      )}
      <Select label="Kasir" value={user} options={users} onChange={setUser} />
      <Row2 style={{ marginTop: 8 }}>
        <Btn kind="ghost" title="Reset" onPress={() => { setPeriod("all"); setUser("all"); setDateFrom(todayStr()); setDateTo(todayStr()); }} />
        <Btn title="Terapkan" onPress={() => {
          if (period === "custom" && !(/^\d{4}-\d{2}-\d{2}$/.test(dateFrom) && /^\d{4}-\d{2}-\d{2}$/.test(dateTo))) return toast("Format tanggal harus YYYY-MM-DD", "error");
          state.reportFilter = { period, user, dateFrom, dateTo }; closeSheet(); commit();
        }} />
      </Row2>
    </View>
  );
}
const openFilterSheet = () => openSheet(<FilterSheet />);

// ---------------- Form pengeluaran ----------------
export function ExpenseSheet({ id, cats = EXPENSE_CATS }) {
  const existing = id ? state.expenses.find((x) => x.id === id) : null;
  const isEdit = !!existing;
  const [amount, setAmount] = useState(existing?.amount || 0);
  const [category, setCategory] = useState(existing?.category || cats[0]);
  const [note, setNote] = useState(existing?.note || "");
  const save = () => {
    if (amount <= 0) return toast("Nominal harus > 0", "error");
    const obj = { amount, category, note: note.trim(), date: isEdit ? existing.date : nowDateTime(), by: state.user.displayName };
    if (isEdit) Object.assign(existing, obj); else state.expenses.push({ id: uid("EXP"), ...obj });
    closeSheet(); commit(); toast("Pengeluaran disimpan", "success");
  };
  return (
    <View style={{ gap: 12 }}>
      <T size={17} weight="700">{isEdit ? "Edit pengeluaran" : "Pengeluaran baru"}</T>
      <RupiahInput label="Nominal (Rp)" value={amount} onChange={setAmount} />
      <Select label="Kategori" value={category} options={cats} onChange={setCategory} />
      <Field label="Catatan" value={note} onChangeText={setNote} placeholder="Contoh: beli kopi 2kg" autoCapitalize="sentences" />
      <Row2><Btn kind="ghost" title="Batal" onPress={closeSheet} /><Btn title="Simpan" onPress={save} /></Row2>
    </View>
  );
}
export const openExpenseForm = (id, cats) => openSheet(<ExpenseSheet id={id} cats={cats} />);

export function deleteExpense(e) {
  confirmModal("Hapus pengeluaran?", "\"" + (e.note || e.category) + "\" akan dihapus.", "Hapus", () => {
    state.expenses = state.expenses.filter((x) => x.id !== e.id); commit(); toast("Pengeluaran dihapus");
  });
}

export function ExpenseRows({ list }) {
  const c = useC();
  return list.length ? list.map((e) => (
    <ListRow key={e.id} title={e.note || e.category} sub={e.category + " · " + e.date + " · " + e.by}
      right={<View style={{ flexDirection: "row", alignItems: "center", gap: 6 }}>
        <T weight="600" color={c.alert}>-{rupiah(e.amount)}</T>
        <IconBtn name="create-outline" size={18} onPress={() => openExpenseForm(e.id)} style={{ width: 34, height: 34 }} />
        <IconBtn name="trash-outline" size={18} onPress={() => deleteExpense(e)} style={{ width: 34, height: 34 }} />
      </View>} />
  )) : <Empty title="Belum ada pengeluaran di periode ini" />;
}

// ---------------- Laporan owner ----------------
const TABS = [["transactions", "Transaksi"], ["expenses", "Pengeluaran"], ["reports", "Laporan"]];
export function ReportsPage() {
  const app = useApp();
  const c = useC();
  const [tab, setTab] = useState("transactions");
  const rf = app.reportFilter;
  const range = periodRange(rf.period, rf.dateFrom, rf.dateTo);
  const prevRange = getPrevPeriodRange(rf.period, rf.dateFrom, rf.dateTo);
  const byUser = (t) => (rf.user === "all" ? true : t.cashierId === rf.user);

  const userList = app.transactions.filter((t) => t.date >= range.from && t.date <= range.to).filter(byUser);
  const active = userList.filter((t) => t.status !== "void");
  const sales = sum(active, netOf);
  const cash = sum(active.filter((t) => t.method === "cash"), netOf);
  const qris = sum(active.filter((t) => t.method === "qris"), netOf);
  const discount = sum(userList, (t) => t.discount || 0);
  const tax = sum(userList, (t) => t.tax || 0);
  const voidAmount = sum(userList.filter((t) => t.status === "void"), (t) => t.total);
  const refundAmount = sum(userList, (t) => t.refundAmount || 0);
  const inRange = (r) => (e) => { const d = e.date.slice(0, 10); return d >= r.from && d <= r.to; };
  const expensesInPeriod = app.expenses.filter(inRange(range));
  const expenseInPeriod = sum(expensesInPeriod, (e) => e.amount);
  const net = sales - expenseInPeriod;
  const gross = sales + discount;
  const prevActive = app.transactions.filter((t) => t.date >= prevRange.from && t.date <= prevRange.to).filter(byUser).filter((t) => t.status !== "void");
  const prevSales = sum(prevActive, netOf);
  const prevExpense = sum(app.expenses.filter(inRange(prevRange)), (e) => e.amount);
  const salesGrowth = computeGrowth(sales, prevSales);
  const expenseGrowth = computeGrowth(expenseInPeriod, prevExpense);
  const expenseByCat = EXPENSE_CATS.map((cat) => ({ cat, total: sum(expensesInPeriod.filter((e) => e.category === cat), (e) => e.amount) }));

  const dayNames = ["M", "S", "S", "R", "K", "J", "S"];
  const last7 = [];
  for (let i = 6; i >= 0; i--) {
    const d = new Date(); d.setDate(d.getDate() - i);
    const ds = dateStr(d);
    last7.push({ date: ds, label: dayNames[d.getDay()], total: sum(app.transactions.filter((t) => t.date === ds && t.status !== "void"), netOf), isToday: i === 0 });
  }
  const maxDay = Math.max(...last7.map((d) => d.total), 1);
  const cashierRecap = {};
  userList.forEach((t) => {
    const r = (cashierRecap[t.cashier] = cashierRecap[t.cashier] || { voidCount: 0, voidAmount: 0, refundCount: 0, refundAmount: 0 });
    if (t.status === "void") { r.voidCount++; r.voidAmount += t.total; }
    if (t.status === "refunded" || t.status === "partial_refund") { r.refundCount++; r.refundAmount += t.refundAmount || 0; }
  });
  const byOutlet = {}; const byCashier = {};
  active.forEach((t) => { byOutlet[t.outlet] = (byOutlet[t.outlet] || 0) + netOf(t); byCashier[t.cashier] = (byCashier[t.cashier] || 0) + netOf(t); });

  const periodText = rf.period === "custom" ? rf.dateFrom + " → " + rf.dateTo : periodLabel(rf.period);
  const userText = rf.user === "all" ? "Semua kasir" : app.workers.find((w) => w.username === rf.user)?.displayName || rf.user;

  return (
    <Page>
      <PageHead title="Laporan" sub={periodText + " · " + userText} right={<Btn kind="ghost" icon="filter" title="Filter" onPress={openFilterSheet} />} />
      <View style={{ flexDirection: "row", backgroundColor: c.surfaceAlt, borderRadius: R.md, padding: 4 }}>
        {TABS.map(([id, label]) => (
          <Pressable key={id} onPress={() => setTab(id)} style={{ flex: 1, paddingVertical: 8, borderRadius: R.sm, alignItems: "center", backgroundColor: tab === id ? c.card : "transparent" }}>
            <T size={13} weight={tab === id ? "700" : "500"} color={tab === id ? c.primary : c.muted}>{label}</T>
          </Pressable>
        ))}
      </View>

      {tab === "transactions" && (userList.length ? userList.map((tx) => <TxItem key={tx.id} tx={tx} />) : <Empty title="Belum ada transaksi" sub="Coba ubah filter atau periode." />)}

      {tab === "expenses" && (
        <>
          <Kpi hero label={"Total Pengeluaran · " + periodText} value={rupiah(expenseInPeriod)} sub={expensesInPeriod.length + " catatan · " + growthIcon(expenseGrowth.direction) + " " + expenseGrowth.pct.toFixed(0) + "% dari periode lalu"} />
          <Card>{expenseByCat.map((row) => <SummaryRow key={row.cat} label={row.cat} value={rupiah(row.total)} />)}</Card>
          <View style={{ flexDirection: "row", alignItems: "center", justifyContent: "space-between" }}>
            <Section title="Daftar pengeluaran" /><Btn title="+ Tambah" onPress={() => openExpenseForm(null)} />
          </View>
          <ExpenseRows list={expensesInPeriod} />
        </>
      )}

      {tab === "reports" && (
        <>
          <Kpi hero label={"Ringkasan Penjualan · " + periodText} value={rupiah(sales)} sub={active.length + " transaksi · " + growthIcon(salesGrowth.direction) + " " + salesGrowth.pct.toFixed(0) + "% dari periode lalu (" + rupiah(prevSales) + ", " + prevActive.length + " trx)"} />
          <View style={{ flexDirection: "row", flexWrap: "wrap", gap: 10 }}>
            <Kpi label="Cash" value={rupiah(cash)} /><Kpi label="QRIS" value={rupiah(qris)} />
            <Kpi label="Void" value={rupiah(voidAmount)} color={c.alert} /><Kpi label="Refund" value={rupiah(refundAmount)} color={c.warn} />
            <Kpi label="Pengeluaran" value={rupiah(expenseInPeriod)} color={c.alert} /><Kpi label="Laba bersih" value={rupiah(net)} color={c.cash} />
          </View>
          <Card>
            <T weight="700" style={{ marginBottom: 10 }}>7 hari terakhir</T>
            <View style={{ flexDirection: "row", alignItems: "flex-end", height: 120, gap: 8 }}>
              {last7.map((d) => (
                <View key={d.date} style={{ flex: 1, alignItems: "center", height: "100%", justifyContent: "flex-end" }}>
                  <View style={{ width: "100%", height: Math.max(6, (d.total / maxDay) * 96), borderRadius: 4, backgroundColor: d.isToday ? c.primary : c.primarySoft, borderWidth: d.isToday ? 0 : 1, borderColor: c.primary }} />
                  <Muted size={11} style={{ marginTop: 4 }}>{d.label}</Muted>
                </View>
              ))}
            </View>
          </Card>
          <Card style={{ gap: 10 }}>
            <T weight="700">Metode pembayaran</T>
            {[["Cash", cash, c.cash], ["QRIS", qris, c.qris]].map(([name, val, col]) => (
              <View key={name} style={{ gap: 4 }}>
                <View style={{ flexDirection: "row", justifyContent: "space-between" }}><T weight="500">{name}</T><T weight="600">{rupiah(val)}</T></View>
                <View style={{ height: 8, borderRadius: 4, backgroundColor: c.surfaceAlt }}><View style={{ height: 8, borderRadius: 4, backgroundColor: col, width: (sales ? (val / sales) * 100 : 0) + "%" }} /></View>
              </View>
            ))}
          </Card>
          <Section title="Ringkasan keuangan" />
          <Card>
            <SummaryRow label="Gross sales" value={rupiah(gross)} />
            <SummaryRow label="Diskon" value={"- " + rupiah(discount)} color={c.alert} />
            <SummaryRow label="Refund" value={"- " + rupiah(refundAmount)} color={c.warn} />
            <SummaryRow label="Pajak (tercatat)" value={rupiah(tax)} />
            <SummaryRow strong label="Penjualan bersih" value={rupiah(sales)} />
            <SummaryRow label="Pengeluaran" value={"- " + rupiah(expenseInPeriod)} color={c.alert} />
            <SummaryRow strong label="Laba bersih" value={rupiah(net)} color={c.cash} />
          </Card>
          <Section title="Per outlet" />
          <Card>{Object.keys(byOutlet).length ? Object.entries(byOutlet).map(([k, v]) => <SummaryRow key={k} label={k} value={rupiah(v)} />) : <Muted>Belum ada data.</Muted>}</Card>
          <Section title="Per kasir" />
          <Card>{Object.keys(byCashier).length ? Object.entries(byCashier).map(([k, v]) => <SummaryRow key={k} label={k} value={rupiah(v)} />) : <Muted>Belum ada data.</Muted>}</Card>
          <Section title="Rekap void & refund per kasir" />
          <Card>
            {Object.keys(cashierRecap).length ? Object.entries(cashierRecap).map(([k, r]) => (
              <View key={k} style={{ paddingVertical: 6 }}>
                <T weight="600">{k}</T>
                <Muted>Void {r.voidCount}× ({rupiah(r.voidAmount)}) · Refund {r.refundCount}× ({rupiah(r.refundAmount)})</Muted>
              </View>
            )) : <Muted>Belum ada data.</Muted>}
          </Card>
        </>
      )}
    </Page>
  );
}
