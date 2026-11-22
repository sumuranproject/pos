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
      {g.items.map((it) => <ListRow key={it.label} title={it.label} sub={it.sub} onPress={it.onPress || (() => goRoute(it.route))} />)}
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
      { title: "Perangkat", items: [hasPerm("printer") && { label: "Printer", sub: p.deviceId ? p.device : "Printer Bluetooth struk", route: "cashier-printer" }, hasPerm("sync") && { label: "Sinkronisasi", sub: "Status & antrean", route: "cashier-sync" }].filter(Boolean) },
      { title: "Akun", items: [hasPerm("theme") && { label: "Tema", sub: app.theme === "dark" ? "Mode gelap" : "Mode terang", route: "cashier-theme" }].filter(Boolean) },
    ].filter((g) => g.items.length);
    return <Page><PageHead title="Kelola" />{groups.length ? <Groups groups={groups} /> : null}</Page>;
  }
  const groups = [
    { title: "Katalog", items: [{ label: "Produk", sub: app.products.length + " produk", route: "products" }, { label: "Kategori", sub: app.categories.length + " kategori", route: "categories" }, { label: "Stok", sub: "Pantau & sesuaikan stok", route: "inventory" }] },
    { title: "Bisnis", items: [{ label: "Outlet", sub: app.outlets.length + " outlet", route: "outlets" }, { label: "Kasir", sub: app.workers.length + " akun", route: "workers" }, { label: "Riwayat Shift", sub: app.shiftHistory.length + " shift", route: "shift-history" }] },
    { title: "Pembayaran & Struk", items: [{ label: "QRIS", sub: app.settings.qris.image ? "Gambar terpasang" : "Belum ada gambar", route: "qris" }, { label: "Printer", sub: p.deviceId ? p.device : "Bluetooth BLE / Classic", route: "printer" }, { label: "Struk", sub: "Format & konten", route: "receipt" }] },
    { title: "Sistem", items: [{ label: "Notifikasi", sub: "Preferensi", route: "notif-settings" }, { label: "Sinkronisasi", sub: "Status & antrean", route: "sync-settings" }, { label: "Keamanan", sub: "PIN & limit refund", route: "security-settings" }, { label: "Audit Log", sub: app.auditLog.length + " catatan", route: "audit-log" }, { label: "Tema", sub: app.theme === "dark" ? "Mode gelap" : "Mode terang", route: "theme-settings" }, { label: "Tentang", sub: "SakuKasir", route: "about" }] },
  ];
  return <Page><PageHead title="Kelola" /><Groups groups={groups} /></Page>;
}
