# SakuKasir (React Native + Expo + Supabase)

Port dari versi web/Capacitor. **Backend Supabase tidak berubah** (migrasi `0001_init.sql`, `0002_storage.sql`, edge function `manage-worker` disalin apa adanya di `supabase/`). Logika bisnis (store, sync offline-first, auth, void/refund, shift, struk ESC/POS) dipertahankan; UI ditulis ulang dengan komponen React Native. Tidak ada mode demo/data contoh: semua data dari Supabase.

## Jalankan
1. Backend: ikuti README lama (jalankan kedua migrasi, `supabase functions deploy manage-worker`).
2. `cp .env.example .env` lalu isi URL & anon key.
3. `npm install` lalu `npx expo install --fix` (WAJIB, lalu commit package.json + package-lock.json) (menyelaraskan versi dengan SDK Expo yang terpasang).
4. **Wajib development build** (bukan Expo Go) karena Bluetooth memakai modul native:
   `npx expo run:android` (perangkat fisik; emulator tidak punya Bluetooth).
5. `npm test` menjalankan uji diff/outbox dan ESC/POS.

## Bluetooth
- **BLE** (`react-native-ble-plx`, Android & iOS): Kelola › Printer › Cari printer BLE. Mencari karakteristik tulis otomatis (urutan UUID layanan umum 18F0/FF00/FFE0/49535343), chunk mengikuti MTU.
- **Classic SPP** (`react-native-bluetooth-classic`, Android saja): Kelola › Printer › Bluetooth Classic. Pilih printer terpasang, atau cari & pairing dari dalam app. Mencoba socket aman, lalu non-aman bila gagal.
- iOS tidak mendukung SPP untuk printer umum; hanya BLE.
- Cetak diserialkan lewat antrean dan otomatis menyambung ulang ke printer tersimpan.

## Perbedaan dari versi web
- ID transaksi kini `TRX-YYYYMMDD-seq-XXXX` (4 karakter acak) agar dua kasir tidak bertabrakan (versi lama memakai hitungan lokal saja).
- Shift aktif disimpan di perangkat sehingga tidak hilang saat app ditutup.
- Status online dari NetInfo; foto bukti QRIS dari kamera (`expo-image-picker`), diperkecil 900px dan diunggah ke Storage saat sync.
- Filter tanggal custom memakai input teks `YYYY-MM-DD`; tombol Export XLSX (stub di web) tidak dibawa. Layout tablet (sisi terpendek ≥ 600dp, portrait & landscape): sidebar kiri (nav, aksi cepat, outlet, akun) tanpa header/bottom-nav, grid produk 4–5 kolom; ponsel tetap header + bottom-nav.

## Build Codemagic
`codemagic.yaml` (grup env `supabase` dengan `EXPO_PUBLIC_*`, keystore `sakukasir_keystore`). `scripts/patch-signing.mjs` memasang signing release & versionCode setelah `expo prebuild`.
