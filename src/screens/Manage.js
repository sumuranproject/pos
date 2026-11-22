import React from "react";
import { goRoute, goTab, hasPerm, useApp } from "../lib/store";
import { Icon, ListRow, Page, PageHead, Section, useC } from "../ui/kit";
import { CategoriesPage, InventoryPage, ProductsPage } from "./Catalog";
import { OutletsPage, ShiftHistoryPage, WorkersPage } from "./Business";
import { AboutPage, AuditLogPage, ExpensesPage, NotifSettingsPage, PrinterPage, QrisPage, ReceiptPage, SecurityPage, SyncSettingsPage, ThemePage } from "./Settings";

const ROUTES = {
  products: ProductsPage, categories: CategoriesPage, inventory: InventoryPage, outlets: OutletsPage, workers: WorkersPage, "shift-history": ShiftHistoryPage,
  qris: QrisPage, printer: PrinterPage, receipt: ReceiptPage, "notif-settings": NotifSettingsPage, "sync-settings": SyncSettingsPage,
  "security-settings": SecurityPage, "audit-log": AuditLogPage, "theme-settings": ThemePage, about: AboutPage,
  "cashier-printer": PrinterPage, "cashier-sync": SyncSettingsPage, "cashier-theme": ThemePage, "cashier-expenses": ExpensesPage,
};

function Groups({ groups }) {
  return groups.map((g) => (
    <React.Fragment key={g.title}>
      <Section title={g.title} />
      {g.items.map((it) => <ListRow key={it.label} title={it.label} sub={it.sub} badge={it.badge} badgeTone={it.tone} onPress={it.onPress || (() => goRoute(it.route))} />)}
    </React.Fragment>
  ));
}

export default function ManagePage() {
  const app = useApp();
  if (app.manageRoute) { const Page_ = ROUTES[app.manageRoute]; return Page_ ? <Page_ /> : null; }
  const p = app.settings.printer;
  if (app.user.role === "cashier") {
    const groups = [
      { title: "Aktivitas", items: [hasPerm("shift") && { label: "Shift", sub: "Status & riwayat shift", onPress: () => goTab("shift") }, hasPerm("transactions") && { label: "Transaksi", sub: "Riwayat transaksi saya", onPress: () => goTab("transactions") }].filter(Boolean) },
      { title: "Keuangan", items: [hasPerm("expense") && { label: "Pengeluaran", sub: "Catat pengeluaran harian", route: "cashier-expenses" }].filter(Boolean) },
      { title: "Perangkat", items: [hasPerm("printer") && { label: "Printer", sub: p.deviceId ? p.device : "Printer Bluetooth struk", route: "cashier-printer" }, hasPerm("sync") && { label: "Sinkronisasi", sub: "Status & queue", route: "cashier-sync" }].filter(Boolean) },
      { title: "Akun", items: [hasPerm("theme") && { label: "Tema", sub: app.theme === "dark" ? "Mode gelap" : "Mode terang", route: "cashier-theme" }].filter(Boolean) },
    ].filter((g) => g.items.length);
    return <Page><PageHead title="Kelola" sub="Menu kasir" />{groups.length ? <Groups groups={groups} /> : null}</Page>;
  }
  const lowCount = app.products.filter((x) => x.active && x.track && x.stock <= x.low).length;
  const groups = [
    { title: "Katalog", items: [
      { label: "Produk", sub: "Kelola daftar produk", badge: app.products.length, route: "products" },
      { label: "Kategori", sub: "Kelompok produk", badge: app.categories.length, route: "categories" },
      { label: "Stok", sub: "Pantau & sesuaikan stok", badge: lowCount ? lowCount + " low" : undefined, tone: "warn", route: "inventory" }] },
    { title: "Bisnis", items: [
      { label: "Outlet", sub: "Cabang & lokasi toko", badge: app.outlets.length, route: "outlets" },
      { label: "Kasir", sub: "Akun & hak akses", badge: app.workers.length, route: "workers" },
      { label: "Shift", sub: "Riwayat shift karyawan", badge: app.shiftHistory.length, route: "shift-history" }] },
    { title: "Pengaturan", items: [
      { label: "QRIS", sub: "Metode pembayaran QR", badge: app.settings.qris.image ? "Aktif" : undefined, tone: "cash", route: "qris" },
      { label: "Printer", sub: "Printer Bluetooth struk", badge: p.deviceId ? "On" : "Off", tone: p.deviceId ? "cash" : "muted", route: "printer" },
      { label: "Struk", sub: "Format & konten struk", route: "receipt" },
      { label: "Notifikasi", sub: "Preferensi notifikasi", route: "notif-settings" },
      { label: "Sinkronisasi", sub: "Status & queue", route: "sync-settings" },
      { label: "Keamanan", sub: "Aturan void & refund", route: "security-settings" },
      { label: "Audit Log", sub: "Riwayat aktivitas", route: "audit-log" },
      { label: "Tema", sub: app.theme === "dark" ? "Mode gelap" : "Mode terang", route: "theme-settings" },
      { label: "Tentang", sub: "Info aplikasi", route: "about" }] },
  ];
  return <Page><PageHead title="Kelola" sub="Menu owner" /><Groups groups={groups} /></Page>;
}
