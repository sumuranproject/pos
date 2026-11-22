import React from 'react';
import { Pressable, View } from 'react-native';
import { SafeAreaView, useSafeAreaInsets } from 'react-native-safe-area-context';
import {
  closeSheet, commit, currentTab, doLogout, goRoute, goTab, hasPerm, isOwner, openSheet, state, toast, useApp,
} from '../lib/store';
import { signOut } from '../lib/auth';
import { syncNow } from '../lib/sync';
import { Btn, Dot, Icon, IconBtn, ListRow, Muted, SummaryRow, T, Empty, useC, useWide, useSidebarW } from '../ui/kit';
import { Image } from 'react-native';
import { goToManage, goToPrinterSettings } from '../lib/store';
import { confirmModal } from '../ui/overlays';
import POSPage from './POS';
import { CheckoutPage, SuccessScreen } from './Checkout';
import { TransactionsPage } from './Transactions';
import { ShiftPage } from './Shift';
import { DashboardPage, ReportsPage } from './Reports';
import ManagePage from './Manage';
import { ProfilePage } from './Settings';

// ---------- sheet header ----------
function SyncSheet() {
  const app = useApp();
  return (
    <View style={{ gap: 10 }}>
      <T size={17} weight="700">Sinkronisasi</T>
      <Muted size={13}>Status: {app.online ? 'Online' : 'Offline'}</Muted>
      <SummaryRow label="Pending" value={String(app.syncQueue.length)} />
      <Btn title={app.online ? 'Sync sekarang' : 'Offline'} block disabled={!app.online} onPress={() => syncNow(true)} />
    </View>
  );
}
export const openSyncSheet = () => openSheet(<SyncSheet />);

const NOTIF_META = (c) => ({
  stock_low: { color: c.warn, label: '⚠ STOK MENIPIS' }, out_of_stock: { color: c.alert, label: '⚠ STOK HABIS' },
  void: { color: c.alert, label: '⚠ VOID TRANSAKSI' }, refund: { color: c.warn, label: '↩ REFUND' },
});
function NotifSheet() {
  const { notifications: list } = useApp();
  const c = useC();
  const meta = NOTIF_META(c);
  return (
    <View style={{ gap: 4 }}>
      <T size={17} weight="700" style={{ marginBottom: 8 }}>Notifikasi</T>
      {list.length ? list.map((n) => {
        const m = meta[n.type] || { color: c.primary, label: 'ℹ SISTEM' };
        return (
          <View key={n.id} style={{ paddingVertical: 10, borderBottomWidth: 0.5, borderBottomColor: c.border }}>
            <T size={11} weight="700" color={m.color}>{m.label}</T>
            <T weight="500">{n.title}</T>
            <Muted>{n.body} · {n.time}</Muted>
          </View>
        );
      }) : <Empty icon="notifications-off-outline" title="Tidak ada notifikasi" />}
    </View>
  );
}
export function openNotifSheet() {
  openSheet(<NotifSheet />);
  state.notifications.forEach((n) => { n.read = true; });
  commit();
}

function OutletSheet() {
  const app = useApp();
  const c = useC();
  return (
    <View>
      <T size={17} weight="700" style={{ marginBottom: 8 }}>Pilih outlet</T>
      {app.outlets.filter((o) => o.active).map((o) => (
        <ListRow key={o.id} title={o.name} sub={o.address}
          right={app.outlet.name === o.name ? <T weight="600" color={c.primary}>Aktif</T> : null}
          onPress={() => { state.outlet = o; state.settings.receipt.bizName = o.name; closeSheet(); toast('Outlet diganti: ' + o.name); }} />
      ))}
    </View>
  );
}
export function openOutletSheet() { if (state.user && isOwner()) openSheet(<OutletSheet />); }

export function handleLogout() {
  if (state.user?.role === 'cashier' && state.activeShift) {
    return confirmModal('Shift masih aktif', 'Anda harus menutup shift dulu sebelum bisa keluar. Buka halaman Kasir, tap banner shift di atas, lalu tutup shift.', 'OK, buka Kasir', () => { state.activeView = 'pos'; commit(); }, false);
  }
  if (state.activeShift) {
    return confirmModal('Shift masih aktif', 'Masih ada shift kasir yang belum ditutup. Pastikan semua shift sudah tutup sebelum keluar.', 'OK', () => {}, false);
  }
  confirmModal('Keluar dari SakuKasir?', 'Kamu perlu login lagi untuk masuk.', 'Keluar', async () => { await signOut(); doLogout(); }, false);
}

function AccountSheet() {
  const app = useApp();
  const c = useC();
  const owner = app.user.role === 'owner';
  const outlet = app.user.outlet || app.outlet.name;
  return (
    <View style={{ gap: 6 }}>
      <View style={{ flexDirection: 'row', alignItems: 'center', gap: 12, marginBottom: 8 }}>
        <View style={{ width: 48, height: 48, borderRadius: 24, backgroundColor: c.primary, alignItems: 'center', justifyContent: 'center' }}>
          <T size={20} weight="700" color="#fff">{app.user.displayName.charAt(0).toUpperCase()}</T>
        </View>
        <View><T weight="700" size={16}>{app.user.displayName}</T><Muted size={13}>{owner ? 'Owner' : 'Kasir'} · {outlet}</Muted></View>
      </View>
      <ListRow title="Profil" sub="Info akun kamu" onPress={() => { closeSheet(); goRoute(owner ? 'profile' : 'cashier-profile'); }} />
      {owner && <ListRow title="Ganti outlet" sub={'Saat ini: ' + app.outlet.name} onPress={() => { closeSheet(); setTimeout(openOutletSheet, 80); }} />}
      <Btn kind="danger" block title="Keluar" style={{ marginTop: 14 }} onPress={() => { closeSheet(); setTimeout(handleLogout, 80); }} />
    </View>
  );
}
export const openAccountSheet = () => { if (state.user) openSheet(<AccountSheet />); };

// ---------- kerangka ----------
const PAGE_TITLES = { checkout: 'Checkout', reports: 'Laporan', dashboard: 'Laporan', transactions: 'Transaksi', shift: 'Shift', manage: 'Kelola' };
const ROUTE_TITLES = {
  products: 'Produk', categories: 'Kategori', inventory: 'Stok', outlets: 'Outlet', workers: 'Kasir', 'shift-history': 'Riwayat Shift',
  qris: 'QRIS', printer: 'Printer', receipt: 'Struk', 'notif-settings': 'Notifikasi', 'sync-settings': 'Sinkronisasi',
  'security-settings': 'Keamanan', 'audit-log': 'Audit Log', 'theme-settings': 'Tema', profile: 'Profil', about: 'Tentang',
  'cashier-profile': 'Profil', 'cashier-printer': 'Printer', 'cashier-sync': 'Sinkronisasi', 'cashier-theme': 'Tema', 'cashier-expenses': 'Pengeluaran',
};
const NAV_ICON = { pos: 'grid-outline', checkout: 'cart-outline', reports: 'bar-chart-outline', dashboard: 'bar-chart-outline', manage: 'settings-outline' };

function navItemsFor(app) {
  if (app.user.role === 'owner') return [{ id: 'pos', label: 'Kasir' }, { id: 'checkout', label: 'Checkout' }, { id: 'reports', label: 'Laporan' }];
  return [
    hasPerm('pos') && { id: 'pos', label: 'Kasir' },
    hasPerm('pos') && { id: 'checkout', label: 'Checkout' },
    hasPerm('dashboard') && { id: 'dashboard', label: 'Laporan' },
  ].filter(Boolean);
}

function Header() {
  const app = useApp();
  const c = useC();
  const pending = !app.online || app.syncQueue.length > 0;
  const unread = app.notifications.some((n) => !n.read);
  return (
    <View style={{ flexDirection: 'row', alignItems: 'center', paddingHorizontal: 16, paddingTop: 10, paddingBottom: 6, gap: 8, backgroundColor: c.card }}>
      <View style={{ flex: 1, flexDirection: 'row', alignItems: 'center', gap: 8 }}>
        <Image source={require('../../assets/icon.png')} style={{ width: 30, height: 30, borderRadius: 8 }} />
        <T size={22} weight="700" numberOfLines={1}>SakuKasir</T>
      </View>
      <IconBtn name="sync-outline" size={19} onPress={openSyncSheet} badge={pending} badgeColor={c.warn} />
      <IconBtn name="notifications-outline" size={19} onPress={openNotifSheet} badge={unread} />
      <Pressable onPress={openAccountSheet} style={{ width: 38, height: 38, borderRadius: 19, backgroundColor: c.primary, alignItems: 'center', justifyContent: 'center' }}>
        <T weight="700" color="#fff">{app.user.displayName.charAt(0).toUpperCase()}</T>
      </Pressable>
    </View>
  );
}


// ---------- tablet: sidebar kiri (sama dengan desain web tablet) ----------
function SideItem({ icon, label, active, onPress, right }) {
  const c = useC();
  return (
    <Pressable onPress={onPress} style={{ flexDirection: 'row', alignItems: 'center', gap: 14, paddingHorizontal: 14, height: 46, borderRadius: 12, backgroundColor: active ? c.primarySoft : 'transparent', marginHorizontal: 10 }}>
      {active ? <View style={{ position: 'absolute', left: -10, top: 8, bottom: 8, width: 3, borderRadius: 2, backgroundColor: c.primary }} /> : null}
      {typeof icon === 'string' ? <Icon name={icon} size={22} color={active ? c.primary : c.muted} /> : icon}
      <T weight={active ? '700' : '500'} color={active ? c.primary : c.muted} style={{ flex: 1 }}>{label}</T>
      {right}
    </Pressable>
  );
}

function Sidebar() {
  const app = useApp();
  const c = useC();
  const w = useSidebarW();
  const tab = currentTab();
  const owner = app.user.role === 'owner';
  const unread = app.notifications.some((n) => !n.read);
  const syncStatus = !app.online || app.syncQueue.length > 0 ? 'pending' : 'synced';
  const items = navItemsFor(app);
  return (
    <View style={{ width: w, backgroundColor: c.card, borderRightWidth: 0.5, borderRightColor: c.border, paddingBottom: 8 }}>
      <View style={{ flexDirection: 'row', alignItems: 'center', gap: 12, padding: 18, borderBottomWidth: 0.5, borderBottomColor: c.border, marginBottom: 12 }}>
        <Image source={require('../../assets/icon.png')} style={{ width: 40, height: 40, borderRadius: 10 }} />
        <View style={{ flex: 1 }}>
          <T weight="700" size={16}>SakuKasir</T>
          <Pressable onPress={openOutletSheet} disabled={!owner} style={{ flexDirection: 'row', alignItems: 'center', gap: 4 }}>
            <Muted size={13} numberOfLines={1} style={{ flexShrink: 1 }}>{app.outlet.name || '-'}</Muted>
            {owner ? <Icon name="chevron-down" size={14} color={c.muted} /> : null}
          </Pressable>
        </View>
      </View>
      {items.map((it) => <SideItem key={it.id} icon={NAV_ICON[it.id]} label={it.label} active={!app.manageRoute && tab === it.id} onPress={() => goTab(it.id)} />)}
      <T size={11} weight="700" color={c.faint} style={{ marginHorizontal: 24, marginTop: 20, marginBottom: 6, letterSpacing: 0.8 }}>AKSI CEPAT</T>
      <SideItem icon={<Dot status={syncStatus} />} label="Sinkron" onPress={openSyncSheet} />
      <SideItem icon="print-outline" label="Printer" onPress={goToPrinterSettings} />
      <SideItem icon="notifications-outline" label="Notifikasi" onPress={openNotifSheet} right={unread ? <View style={{ width: 8, height: 8, borderRadius: 4, backgroundColor: c.alert }} /> : null} />
      <SideItem icon="settings-outline" label="Kelola" active={tab === 'manage'} onPress={goToManage} />
      <View style={{ flex: 1 }} />
      <Pressable onPress={openAccountSheet} style={{ flexDirection: 'row', alignItems: 'center', gap: 12, padding: 14, borderTopWidth: 0.5, borderTopColor: c.border }}>
        <View style={{ width: 38, height: 38, borderRadius: 19, backgroundColor: c.primary, alignItems: 'center', justifyContent: 'center' }}>
          <T weight="700" color="#fff">{app.user.displayName.charAt(0).toUpperCase()}</T>
        </View>
        <View style={{ flex: 1 }}><T weight="600" numberOfLines={1}>{app.user.displayName}</T><Muted>{owner ? 'Owner' : 'Kasir'}</Muted></View>
        <IconBtn name="log-out-outline" size={20} onPress={handleLogout} style={{ backgroundColor: 'transparent' }} />
      </Pressable>
    </View>
  );
}

function BottomNav() {
  const app = useApp();
  const c = useC();
  const insets = useSafeAreaInsets();
  const tab = currentTab();
  if (app.hideNav) return null;
  const items = [...navItemsFor(app), { id: 'manage', label: 'Kelola' }];
  return (
    <View style={{ flexDirection: 'row', backgroundColor: c.card, borderTopWidth: 1, borderTopColor: c.border, paddingBottom: insets.bottom }}>
      {items.map((it) => {
        const active = tab === it.id;
        return (
          <Pressable key={it.id} onPress={() => goTab(it.id)} style={{ flex: 1, alignItems: 'center', paddingVertical: 8, gap: 2 }}>
            <Icon name={NAV_ICON[it.id]} size={22} color={active ? c.primary : c.muted} />
            <T size={11} weight={active ? '700' : '500'} color={active ? c.primary : c.muted}>{it.label}</T>
          </Pressable>
        );
      })}
    </View>
  );
}

function MainRouter() {
  const app = useApp();
  if (!app.user) return null;
  if (app.manageRoute === 'profile' || app.manageRoute === 'cashier-profile') return <ProfilePage />;
  if (app.success) return <SuccessScreen tx={app.success} />;
  switch (currentTab()) {
    case 'manage': return <ManagePage />;
    case 'pos': return <POSPage />;
    case 'checkout': return <CheckoutPage />;
    case 'dashboard': return <DashboardPage />;
    case 'transactions': return <TransactionsPage />;
    case 'shift': return <ShiftPage />;
    case 'reports': return <ReportsPage />;
    default: return null;
  }
}

export default function Shell() {
  const app = useApp();
  const c = useC();
  const wide = useWide();
  if (!app.user) return null;
  const tab = currentTab();
  const showHeader = tab === 'pos' && !app.manageRoute && !app.success;
  const pending = app.syncQueue.length;
  const showBanner = !app.online || pending > 0;
  const banner = showBanner && (
    <Pressable onPress={() => syncNow()} style={{ flexDirection: 'row', alignItems: 'center', gap: 8, paddingHorizontal: 16, paddingVertical: 8, backgroundColor: c.warnSoft }}>
      <Dot status="pending" />
      <T size={12} color={c.warn} style={{ flex: 1 }}>{!app.online ? `Mode offline · ${pending} perubahan menunggu` : `${pending} perubahan menunggu sinkron`}</T>
      <T size={12} weight="700" color={c.primary}>Sync sekarang</T>
    </Pressable>
  );
  if (wide) {
    // Tablet: sidebar kiri, tanpa header atas & bottom-nav (judul halaman sudah ada di tiap halaman).
    return (
      <SafeAreaView edges={['top', 'bottom']} style={{ flex: 1, flexDirection: 'row', backgroundColor: c.card }}>
        <Sidebar />
        <View style={{ flex: 1, backgroundColor: c.surface }}>
          {banner}
          <MainRouter />
        </View>
      </SafeAreaView>
    );
  }
  return (
    <SafeAreaView edges={['top']} style={{ flex: 1, backgroundColor: c.card }}>
      {showHeader ? <Header /> : null}
      {banner}
      <View style={{ flex: 1, backgroundColor: c.surface }}><MainRouter /></View>
      <BottomNav />
    </SafeAreaView>
  );
}
