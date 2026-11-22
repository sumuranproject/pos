# Integrasi Supabase — panduan dari HP

Semua langkah lewat browser HP (supabase.com) dan Codemagic. Tidak butuh PC.

## 1. Buat proyek
1. Masuk ke supabase.com › **New project**. Isi nama (mis. `sakukasir`), password database (simpan), region **Southeast Asia (Singapore)**.
2. Tunggu ±2 menit sampai proyek siap.

## 2. Pasang database (satu kali tempel)
1. Menu kiri › **SQL Editor** › **New query**.
2. Buka file `supabase/setup-all.sql` dari zip (lewat aplikasi pengelola file / editor teks), salin **seluruh isinya**, tempel, tekan **Run**.
3. Harus muncul "Success". Cek: menu **Table Editor** berisi tabel `businesses`, `profiles`, `products`, `transactions`, dst.; menu **Storage** berisi bucket `qris-proofs`.
   Bila ada error "already exists", berarti sebagian sudah pernah dijalankan: abaikan atau buat proyek baru.

## 3. Pengaturan Auth (penting)
Menu **Authentication › Sign In / Providers › Email**:
- Untuk uji coba cepat: matikan **Confirm email** (supaya daftar owner langsung bisa masuk).
- Bila tetap dinyalakan, setelah daftar aplikasi akan meminta Anda konfirmasi lewat email dulu.
Menu **Authentication › URL Configuration** tidak perlu diubah.

## 4. Pasang fungsi akun kasir
Dipakai owner saat membuat/mengubah akun kasir.
1. Menu **Edge Functions › Deploy a new function › Via Editor**.
2. Nama fungsi: `manage-worker` (harus persis).
3. Hapus isi contoh, tempel seluruh isi `supabase/functions/manage-worker/index.ts`, tekan **Deploy**.
Tidak perlu mengisi secret: `SUPABASE_URL`, `SUPABASE_ANON_KEY`, `SUPABASE_SERVICE_ROLE_KEY` sudah tersedia otomatis.

## 5. Ambil URL dan anon key
**Project Settings › API** (atau **Connect**):
- **Project URL** → `https://xxxx.supabase.co`
- **anon / public key** (panjang, diawali `eyJ`) → pakai ini. **Jangan** pakai `service_role`.

## 6. Isi di Codemagic
App › **Environment variables**, grup `supabase`, centang Secret:
| Name | Value |
|---|---|
| `EXPO_PUBLIC_SUPABASE_URL` | Project URL |
| `EXPO_PUBLIC_SUPABASE_ANON_KEY` | anon key |

Lalu **Start new build** (variabel hanya terbaca saat build baru).

## 7. Coba di aplikasi
1. Pasang APK hasil build di HP.
2. Layar login › **Daftar**: isi data owner → aplikasi membawa ke **Setup bisnis** (nama, alamat, jenis) → selesai, masuk sebagai owner.
3. Tambah produk di Kelola › Produk. Di Supabase **Table Editor › products** data harus muncul dalam beberapa detik (atau tekan ikon sinkron › Sync sekarang).
4. Kelola › Kasir › Tambah → buat akun kasir; login sebagai kasir dengan username & password tersebut.
5. Uji offline: matikan data HP, buat transaksi, nyalakan lagi: data terkirim otomatis.

## Masalah umum
| Gejala | Penyebab / solusi |
|---|---|
| Aplikasi langsung ke login tanpa bisa daftar / "Supabase belum dikonfigurasi" | Variabel Codemagic belum terisi atau build dibuat sebelum diisi → isi lalu build ulang |
| "Invalid API key" | Salah tempel anon key (ada spasi/terpotong) |
| Daftar berhasil tapi tidak bisa masuk | Confirm email masih aktif → konfirmasi lewat email atau matikan (langkah 3) |
| Tambah kasir gagal | Fungsi `manage-worker` belum di-deploy atau nama salah |
| Data tidak muncul di Table Editor | SQL langkah 2 belum sukses; cek **Logs** › Postgres |
| Foto bukti QRIS tidak terbuka | Bucket `qris-proofs` belum ada (jalankan bagian 0002 dari `setup-all.sql`) |
