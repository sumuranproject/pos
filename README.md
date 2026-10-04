# SakuKasir POS — Status Proyek

## Posisi Saat Ini

**Versi:** V27 — V10 Build Fix  
**Basis:** V25 Build-Fix / Offline Checkout  
**Status:** 🟡 **Siap diuji di Codemagic — belum dinyatakan build sukses**

V27 adalah patch untuk memperbaiki error compile V10 pada bottom navigation tanpa mengubah backend Firebase maupun fitur transaksi/offline yang sudah ada.

---

## Progress Utama

| Area | Status | Keterangan |
|---|---|---|
| UI dasar SakuKasir | ✅ Selesai | Native Jetpack Compose, tema biru |
| Bottom Navigation | ✅ Diperbaiki | 4 menu: Kasir, Checkout, Laporan, Pengaturan |
| Firebase Auth | ✅ Ada | Email + Password dan alur login yang sudah ada |
| Firebase RTDB | ✅ Ada | Repository/backend dipertahankan |
| FCM | ✅ Ada | Owner messaging service dipertahankan |
| Shift | ✅ Ada | Dukungan shift aktif/offline dipertahankan |
| Checkout | ✅ Ada | Alur checkout/payment dipertahankan |
| Cash / QRIS | ✅ Ada | Payment flow dipertahankan |
| Offline Transaction ID | ✅ Ada | ID dibuat lokal sebelum sinkronisasi |
| `PENDING_SYNC` | ✅ Ada | Queue/status sinkronisasi dipertahankan |
| Sync tanpa duplikasi | ✅ Ada | Menggunakan Transaction ID sebagai identitas transaksi |
| Offline reprint | ✅ Ada | Riwayat lokal dapat digunakan untuk cetak ulang |
| Bluetooth printer | ✅ Ada | ESC/POS dan printer paired dipertahankan |
| Riwayat transaksi | ✅ Ada | Data transaksi yang tersedia dipertahankan |
| Pengaturan | 🟡 Bertahap | Struktur dasar sudah ada; UI detail mengikuti mockup |
| Produk | 🟡 Bertahap | Fungsi dasar ada; UI lengkap masih dikembangkan |
| Kategori | 🟡 Bertahap | UI/fungsi lengkap masih dikembangkan |
| Stok | 🟡 Bertahap | UI/fungsi lengkap masih dikembangkan |
| Laporan Owner | 🟡 Bertahap | Dasar laporan ada; detail mockup masih dikembangkan |
| Pengeluaran | 🟡 Bertahap | Belum seluruh UI mockup selesai |
| Outlet / Pekerja / Owner | 🟡 Bertahap | Pengembangan UI masih berjalan |
| Tema Light/Dark | 🟡 Bertahap | Sistem tema ada; penyempurnaan UI mengikuti mockup |
| UI 60+ screen/state mockup | 🟡 Bertahap | Mockup menjadi source of truth; belum seluruhnya native Compose |
| Build Codemagic V27 | ⏳ Menunggu | Harus diverifikasi dari hasil build Codemagic |

---

## Error Terakhir yang Sedang Diperbaiki

### V9

Codemagic berhenti pada `:app:compileDebugKotlin` dengan error:

- `MainActivity.kt:704` — `NavigationBarItem` tidak ditemukan.
- `MainActivity.kt:707` — error context `@Composable`.
- `MainActivity.kt:708` — error context `@Composable`.

V26 menghilangkan penggunaan langsung `NavigationBarItem` dan menggantinya dengan komponen bottom navigation Compose yang lebih sederhana, tetapi tetap mempertahankan 4 menu yang sama.

**Catatan:** error V9 sudah diperbaiki pada source V26, lalu V10 menemukan error baru pada modifier `weight`, tetapi status final compile belum boleh disebut sukses sebelum Codemagic mengonfirmasi.

---

## Yang Sudah Ada Sebelum V26

### Transaksi & Offline

- Offline Transaction ID.
- Status `PENDING_SYNC`, `SYNCED`, dan `SYNC_ERROR`.
- Queue sinkronisasi.
- Sinkronisasi menggunakan ID transaksi yang sama sehingga tidak membuat transaksi baru saat retry.
- Riwayat transaksi lokal untuk kebutuhan offline.
- Reprint transaksi yang sudah tersimpan lokal.
- Shift dapat bekerja dengan data lokal.
- Checkout Cash dan QRIS.
- Transaction ID dapat tersedia sebelum Firebase berhasil diakses.

### Printer

- Bluetooth printer paired.
- Android 12+ Bluetooth permission handling yang sudah ada.
- ESC/POS receipt.
- Cetak ulang struk dari detail transaksi.

### Backend

- Firebase Authentication.
- Firebase Realtime Database.
- Firebase Cloud Messaging.
- Firebase rules tetap dipertahankan.
- Tidak menggunakan Firestore.
- Tidak menggunakan Firebase Storage untuk bukti QRIS.
- Tidak menambahkan Cloud Functions.

---

## Arah UI Berikutnya

Mockup `Mockup_UI_POS_SakuKasir_Lengkap (1).html` menjadi **source of truth UI/UX**.

Urutan kerja berikutnya:

1. Pastikan **V27 lolos Codemagic**.
2. Jangan mengubah backend jika pekerjaan hanya UI.
3. Lanjutkan UI native Compose berdasarkan mockup.
4. Setiap perubahan UI diaudit terhadap compile-risk sebelum dikirim.
5. Hindari perubahan dependency jika tidak diperlukan.
6. Setelah satu kelompok UI selesai, baru lanjut kelompok berikutnya.

### Prioritas UI

- Kasir / POS.
- Checkout.
- Pembayaran.
- Laporan.
- Pengaturan.
- Produk, Kategori, dan Stok.
- Owner Dashboard dan laporan Owner.
- Outlet, Pekerja/Kasir, Bisnis, QRIS, Printer, Tema, Sinkronisasi.
- Penyempurnaan seluruh state/detail dari mockup.

---

## Aturan Patch

- Native Android Kotlin + Jetpack Compose.
- Firebase/backend yang sudah ada dipertahankan.
- Jangan mengubah schema Firebase hanya untuk perubahan UI.
- Jangan menambahkan dependency baru tanpa alasan yang jelas.
- Perubahan UI tidak boleh merusak logic transaksi/offline.
- Audit import, Composable context, parameter, state, navigation, dan dependency setelah perubahan.
- Hanya **satu file Markdown: `README.md`**.
- Jangan mengklaim Gradle/Codemagic berhasil jika belum benar-benar diverifikasi.

---

## Status Build

**V27:** source/static audit ✅  
**V27:** ZIP integrity ✅  
**V27:** full Gradle build lokal ⏳ tidak tersedia di environment ini  
**V27:** Codemagic ⏳ menunggu hasil build

### Cara membaca status

- ✅ **Selesai** — fitur/source sudah tersedia dan dipertahankan.
- 🟡 **Bertahap** — sudah ada sebagian, masih perlu penyempurnaan.
- ⏳ **Menunggu** — belum diverifikasi/masih menunggu tahap berikutnya.
- 🔴 **Error** — ada masalah yang harus diperbaiki.

---

## Setelah Codemagic V27

Jika **V26 berhasil build**, lanjut ke pekerjaan UI berikutnya tanpa mengulang pekerjaan offline/backend.

Jika **V26 gagal**, gunakan error terbaru Codemagic sebagai dasar patch berikutnya. Jangan melakukan perubahan acak pada dependency atau Firebase sebelum akar masalah diketahui.
