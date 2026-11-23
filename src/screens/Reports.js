import React, { useState } from "react";
import { Pressable, ScrollView, View } from "react-native";
import { closeSheet, commit, openSheet, state, toast, useApp } from "../lib/store";
import { computeGrowth, dateStr, getPrevPeriodRange, growthIcon, netOf, nowDateTime, periodLabel, periodRange, rupiah, todayStr, uid } from "../lib/format";
import { Btn, Card, Chip, Empty, Field, Icon, IconBtn, Kpi, ListRow, Muted, Page, PageHead, Row2, RupiahInput, Section, Select, SummaryRow, T, useC, useWide, R } from "../ui/kit";
import { confirmModal } from "../ui/overlays";
import { TxItem } from "./Transactions";

export const EXPENSE_CATS = ["Bahan", "Operasional", "Gaji", "Lainnya"];
const sum = (list, fn) => list.reduce((s, t) => s + fn(t), 0);

// Kartu ringkasan: label, angka besar, sub hijau, garis, lalu baris pembanding (sesuai screenshot).
function HeroCard({ label, value, valueColor, sub, growth, growthBad, rows = [], style }) {
  const c = useC();
  const gcol = growthBad ? c.alert : c.cash;
  return (
    <View style={[{ backgroundColor: c.card, borderRadius: R.lg, borderWidth: 1, borderColor: c.border, padding: 14 }, style]}>
      <T size={12} color={c.muted}>{label}</T>
      <T size={28} weight="700" color={valueColor || c.text} style={{ marginTop: 2 }}>{value}</T>
      {sub ? <T size={13} weight="600" color={c.cash}>{sub}</T> : null}
      <View style={{ height: 1, backgroundColor: c.border, marginVertical: 12 }} />
      {growth ? <T size={12} weight="700" color={gcol} style={{ marginBottom: 6 }}>{growth}</T> : null}
      {rows.map(([k, v], i) => (
        <View key={k + i} style={{ flexDirection: "row", justifyContent: "space-between", paddingVertical: 3 }}>
          <T size={13} color={c.muted}>{k}</T><T size={13} weight="600">{v}</T>
        </View>
      ))}
    </View>
  );
}

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
      <HeroCard label="Ringkasan Penjualan · Hari ini" value={rupiah(totalSales)} sub={active.length + " transaksi"}
        growth={growthIcon(growth.direction) + " " + growth.pct.toFixed(0) + "% dari kemarin"} growthBad={growth.direction === "down"}
        rows={[["Kemarin", rupiah(ySales)]]} />
      <View style={{ flexDirection: "row", flexWrap: "wrap", gap: 10 }}>
        <Kpi label="Cash" value={rupiah(cash)} /><Kpi label="QRIS" value={rupiah(qris)} />
        <Kpi label="Void" value={rupiah(voidAmount)} color={c.alert} /><Kpi label="Refund" value={rupiah(refundAmount)} color={c.warn} />
      </View>
      {sh && (
        <Card style={{ gap: 4, padding: 16 }}>
          <View style={{ flexDirection: "row", alignItems: "center", gap: 8 }}><View style={{ width: 8, height: 8, borderRadius: 4, backgroundColor: c.cash }} /><T size={12} weight="700" color={c.cash} style={{ letterSpacing: 0.6 }}>SHIFT AKTIF</T></View>
          <Muted size={13} style={{ marginVertical: 6 }}>Mulai {sh.startTime}</Muted>
          <SummaryRow line label="Transaksi shift ini" value={String(sh.transactions)} />
          <SummaryRow line label="Cash" value={rupiah(sh.cashSales)} /><SummaryRow line label="QRIS" value={rupiah(sh.qrisSales)} />
          <SummaryRow strong label="Total" value={rupiah(sh.cashSales + sh.qrisSales)} color={c.primary} />
        </Card>
      )}
      <Section title={"Riwayat Transaksi Hari Ini (" + todayTx.length + ")"} style={{ marginTop: 14 }} />
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
      right={<View style={{ flexDirection: "row", alignItems: "center", gap: 12 }}>
        <T weight="700" color={c.alert}>-{rupiah(e.amount)}</T>
        <Pressable onPress={() => openExpenseForm(e.id)} hitSlop={10}><Icon name="create-outline" size={17} /></Pressable>
        <Pressable onPress={() => deleteExpense(e)} hitSlop={10}><Icon name="trash-outline" size={17} /></Pressable>
      </View>} />
  )) : <Empty title="Belum ada pengeluaran di periode ini" />;
}

// ---------------- Laporan owner ----------------
const TABS = [["transactions", "Transaksi", "document-text-outline"], ["expenses", "Pengeluaran", "wallet-outline"], ["reports", "Laporan", "bar-chart-outline"]];
export function ReportsPage() {
  const app = useApp();
  const c = useC();
  const wide = useWide();
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

  const tabsEl = (
<View style={{ flexDirection: "row", backgroundColor: wide ? c.surfaceAlt : "transparent", borderRadius: R.lg, padding: wide ? 4 : 0, gap: wide ? 0 : 8 }}>
      {TABS.map(([id, label, icon]) => {
        const on = tab === id;
        return (
          <Pressable key={id} onPress={() => setTab(id)} style={{ flex: 1, height: 40, borderRadius: 12, flexDirection: "row", gap: 8, alignItems: "center", justifyContent: "center", backgroundColor: on ? c.primary : wide ? "transparent" : c.card, borderWidth: !wide && !on ? 1 : 0, borderColor: c.border }}>
            <Icon name={icon} size={17} color={on ? "#fff" : c.muted} />
            {wide ? <T size={13} weight={on ? "700" : "500"} color={on ? "#fff" : c.muted}>{label}</T> : null}
          </Pressable>
        );
      })}
    </View>
  );
  return (
    <Page>
      <PageHead title="Laporan" sub={periodText + " · " + userText} right={<View style={{ flexDirection: "row", gap: 8 }}><Btn sm kind="ghost" icon="filter" title="Filter" onPress={openFilterSheet} /><Btn sm kind="ghost" title="Export" onPress={() => toast("Export belum tersedia di versi ini")} /></View>} footer={wide ? undefined : tabsEl} />
      {wide ? tabsEl : null}

      {tab === "transactions" && (userList.length ? userList.map((tx) => <TxItem key={tx.id} tx={tx} />) : <Empty title="Belum ada transaksi" sub="Coba ubah filter atau periode." />)}

      {tab === "expenses" && (
        <>
          <HeroCard label={"Total Pengeluaran · " + periodLabel(rf.period)} value={rupiah(expenseInPeriod)} valueColor={c.alert} sub={expensesInPeriod.length + " catatan"}
            growth={growthIcon(expenseGrowth.direction) + " " + expenseGrowth.pct.toFixed(0) + "% dari periode lalu"} growthBad
            rows={expenseByCat.map((row) => [row.cat, rupiah(row.total)])} />
          <View style={{ flexDirection: "row", alignItems: "center", justifyContent: "space-between", marginTop: 6 }}>
            <View><T weight="700" size={15}>Daftar pengeluaran</T><Muted>{expensesInPeriod.length} catatan di periode ini</Muted></View>
            <Btn sm icon="add" title="Tambah" onPress={() => openExpenseForm(null)} />
          </View>
          <ExpenseRows list={expensesInPeriod} />
        </>
      )}

      {tab === "reports" && (
        <>
          <View style={{ flexDirection: wide ? "row" : "column", gap: 10, alignItems: "stretch" }}>
            <HeroCard style={wide ? { flex: 1 } : undefined} label={"Ringkasan Penjualan · " + periodText.replace(/^\w/, (m) => m.toUpperCase()).split(" · ")[0]} value={rupiah(sales)} sub={active.length + " transaksi"}
              growth={growthIcon(salesGrowth.direction) + " " + salesGrowth.pct.toFixed(0) + "% dari periode lalu"} growthBad={salesGrowth.direction === "down"}
              rows={[["Periode lalu", rupiah(prevSales)], ["Transaksi lalu", String(prevActive.length)]]} />
            <View style={{ flexDirection: "row", flexWrap: "wrap", gap: 10, flex: wide ? 1.1 : undefined }}>
              <Kpi label="Cash" value={rupiah(cash)} /><Kpi label="QRIS" value={rupiah(qris)} />
              <Kpi label="Void" value={rupiah(voidAmount)} color={c.alert} /><Kpi label="Refund" value={rupiah(refundAmount)} color={c.warn} />
              <Kpi label="Pengeluaran" value={rupiah(expenseInPeriod)} color={c.alert} /><Kpi label="Laba bersih" value={rupiah(net)} color={net < 0 ? c.alert : c.cash} />
            </View>
          </View>
          <View style={{ flexDirection: wide ? "row" : "column", gap: 10, alignItems: "stretch" }}>
          <Card style={wide ? { flex: 1 } : undefined}>
            <T weight="500" color={c.muted} style={{ marginBottom: 10 }}>7 hari terakhir</T>
            <View style={{ flexDirection: "row", alignItems: "flex-end", height: 120, gap: 8 }}>
              {last7.map((d) => (
                <View key={d.date} style={{ flex: 1, alignItems: "center", height: "100%", justifyContent: "flex-end" }}>
                  <View style={{ width: "100%", height: d.total ? Math.max(6, (d.total / maxDay) * 96) : 5, borderRadius: 3, backgroundColor: d.isToday ? c.primary : c.surfaceAlt }} />
                  <Muted size={11} style={{ marginTop: 6 }}>{d.label}</Muted>
                </View>
              ))}
            </View>
          </Card>
          <Card style={[{ gap: 10 }, wide ? { flex: 1 } : null]}>
            <T weight="500" color={c.muted}>Metode pembayaran</T>
            {[["Cash", cash, c.cash], ["QRIS", qris, c.qris]].map(([name, val, col]) => (
              <View key={name} style={{ gap: 4 }}>
                <View style={{ flexDirection: "row", justifyContent: "space-between" }}><T weight="700">{name}</T><T weight="700">{rupiah(val)}</T></View>
                <View style={{ height: 8, borderRadius: 4, backgroundColor: c.surfaceAlt }}><View style={{ height: 8, borderRadius: 4, backgroundColor: col, width: (sales ? (val / sales) * 100 : 0) + "%" }} /></View>
              </View>
            ))}
          </Card>
          </View>
          <Section title="Ringkasan keuangan" />
          <Card>
            <SummaryRow line label="Gross sales" value={rupiah(gross)} />
            <SummaryRow line label="Diskon" value={"- " + rupiah(discount)} color={c.alert} />
            <SummaryRow line label="Pajak" value={"- " + rupiah(tax)} color={c.alert} />
            <SummaryRow line label="Net sales" value={rupiah(sales)} />
            <SummaryRow line label="Pengeluaran" value={"- " + rupiah(expenseInPeriod)} color={c.alert} />
            <SummaryRow strong label="Net profit" value={rupiah(net)} color={net < 0 ? c.alert : c.primary} />
          </Card>
          <Section title="Rekap void/refund per kasir" />
          {Object.keys(cashierRecap).length ? Object.entries(cashierRecap).map(([k, r]) => (
            <Card key={k} style={{ flexDirection: "row", alignItems: "center", justifyContent: "space-between", paddingVertical: 10 }}>
              <View><T weight="600">{k}</T><Muted size={11}>Void {r.voidCount}× · Refund {r.refundCount}×</Muted></View>
              <T weight="700" color={c.alert}>{rupiah(r.voidAmount + r.refundAmount)}</T>
            </Card>
          )) : <Card><Muted>Belum ada data.</Muted></Card>}
          <Section title="Penjualan per outlet" />
          {Object.keys(byOutlet).length ? Object.entries(byOutlet).map(([k, v]) => <Card key={k} style={{ flexDirection: "row", justifyContent: "space-between", paddingVertical: 12 }}><T weight="500">{k}</T><T weight="700">{rupiah(v)}</T></Card>) : <Card><Muted>Belum ada data.</Muted></Card>}
          <Section title="Penjualan per kasir" />
          {Object.keys(byCashier).length ? Object.entries(byCashier).map(([k, v]) => <Card key={k} style={{ flexDirection: "row", justifyContent: "space-between", paddingVertical: 12 }}><T weight="500">{k}</T><T weight="700">{rupiah(v)}</T></Card>) : <Card><Muted>Belum ada data.</Muted></Card>}
        </>
      )}
    </Page>
  );
}
