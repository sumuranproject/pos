# SakuKasir V37

V36 baseline completed the missing page/state coverage identified by the audit against `SakuKasir-UI-FIX.html` and removes the remaining fake/prototype implementations.

## Page coverage

Implemented native page/state coverage includes:

- Role selection / Owner login / Kasir login / Owner registration
- Kasir / product search / category chips / cart
- Checkout / Cash / QRIS / transaction success
- Laporan Kasir: Hari ini / Riwayat shift
- Laporan Owner: Ringkasan / Outlet / Kasir / Harian / Bulanan
- Riwayat transaksi / detail transaksi
- Detail shift / tutup shift / shift selesai
- Laporan keuangan with real period filtering
- Pengeluaran / detail pengeluaran / tambah / ubah / hapus
- Pengaturan Owner and role-specific Pengaturan Kasir
- Profil / Outlet read-only for Kasir / Produk / tambah-ubah / detail
- Kategori / Stok
- Outlet / detail outlet
- Kasir / Pekerja / detail pekerja
- Bisnis / Profil Owner
- QRIS settings with user-supplied QR image
- Bluetooth printer discovery / connect / disconnect / test print
- Edit Struk
- Tema: Terang / Gelap / Sistem
- Sinkronisasi with actual loaded sync-status counts
- Notifikasi stok menipis
- Tentang

## Real asset behavior

### QRIS

There is no generated/sample QR code anymore. The Owner must select an actual QRIS image from the device in **Pengaturan → QRIS**. The selected image is copied into app-private storage and reused by the QRIS checkout screen.

### Bluetooth printer

There are no hardcoded printer names or fake connection states. The printer page reads Android's paired Bluetooth devices and uses a real RFCOMM SPP connection for connect/disconnect/test print.

The printer must first be paired in Android Bluetooth settings. Android 12+ Bluetooth Connect permission is requested before accessing paired devices.

### QRIS payment proof

A proof image selected during QRIS checkout is copied to app-private storage and attached to the transaction as its local proof path. Google Drive upload is not fabricated here; that remains a separate backend/storage integration step because the current repository contains no Drive uploader implementation.

## Backend protection

The following existing Firebase/data architecture was preserved:

- Firebase Authentication
- Firebase Realtime Database
- `PosRepository`
- `RepositoryProvider`
- `OwnerMessagingService` / FCM
- `OfflineStore` / offline sync queue
- `database.rules.json`
- existing Firebase dependencies and Gradle/Codemagic configuration

No WebView was introduced. The UI remains native Jetpack Compose.

## Validation

`bash scripts/preflight.sh` passes.

A full Gradle/Codemagic build was not run in this environment because no Gradle executable or wrapper is available locally. Codemagic/device validation is still required before treating V36 as build-verified.

## Dropbox UI

V37 adds the native UI shell for per-business Dropbox connection:

- Owner → Pengaturan → Penyimpanan → Dropbox
- Explicit “Belum terhubung” state
- Storage categories: Foto Produk, QRIS, Logo Bisnis
- OAuth explanation dialog without fake connection state
- Dropbox is not exposed as an account-management feature to Kasir

OAuth PKCE, token handling, upload, and persistent shared-link creation are deliberately not simulated in this UI-only version. They will be wired to the Dropbox App configuration in a later integration pass.
