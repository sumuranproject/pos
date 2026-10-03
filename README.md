# SakuKasir — UI/UX Patch V19

Patch ini merapikan UI native Jetpack Compose berdasarkan mockup POS yang diberikan pengguna. Mockup dipakai sebagai referensi struktur, hierarchy, flow, spacing, dan pola komponen; HTML/CSS tidak digunakan sebagai source.

## UI direction
- SakuKasir blue, bukan purple mockup.
- Flat surfaces, minimal elevation, radius 12–16dp.
- Bottom navigation tepat 4 item: **Kasir · Checkout · Laporan · Pengaturan**.
- Owner dan Cashier memakai navbar yang sama.
- Owner dapat bertransaksi melalui Kasir.
- Laporan menjadi tempat seluruh hasil/rekap; Pengaturan menjadi tempat konfigurasi/manajemen.
- Phone, tablet, dan landscape memakai layout yang dapat beradaptasi.

## Mockup-derived flows
### Cashier
Login → Mulai Shift → Kasir → Checkout → Cash/QRIS → berhasil → Laporan/Detail Shift → Tutup Shift → Keluar.

### Owner
Login → Laporan/Ringkasan → filter laporan (Outlet/Kasir/Harian/Bulanan) → Pengaturan → halaman manajemen terpisah.

## Important product UI decisions
- Kategori dibuat sendiri oleh Owner.
- Produk dapat memiliki area foto/placeholder.
- Stok dapat ON/OFF per produk.
- Produk/Kategori/Stok berada di Pengaturan, bukan Laporan.
- Pengeluaran berada di Laporan sebagai hasil/rekap keuangan, bukan tab navbar baru.

## Backend boundary
Patch V19 hanya mengubah UI. Firebase/Auth/RTDB/FCM/rules/repository tidak disentuh.
