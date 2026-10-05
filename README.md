# Saku Kasir — V39

V39 adalah **UI Fidelity Patch** di atas V38. Base arsitektur modular V29/V38 dipertahankan; sumber visual utama adalah `SakuKasir-UI-FIX.html`.

## Fokus V39
- Palet light/dark diselaraskan dengan mockup.
- Typography Compose diselaraskan dengan ukuran/weight mockup.
- Common UI primitives dibuat lebih dekat dengan CSS HTML.
- Search bar, chips, segmented control, card/list row, tombol dan bottom navigation diperbaiki.
- Kasir, checkout, pembayaran, auth, laporan dan management memakai primitive UI yang sama.

## Fitur V38 yang tetap ada
- Dropbox UI shell untuk Owner.
- Notifikasi stok menipis.
- Edit Struk.
- QRIS upload lokal V29.
- Bluetooth printer paired-device/RFCOMM V29.
- Firebase Authentication, Realtime Database, FCM, repository dan offline store.

Dropbox pada V39 **belum melakukan OAuth nyata** dan tidak membuat status koneksi palsu.

## Verifikasi
- `scripts/preflight.sh`: PASS
- Delimiter/source sanity check: PASS
- Full Gradle/Codemagic build: belum dijalankan karena environment tidak menyediakan Gradle wrapper/executable.
- Firebase-sensitive files identik dengan V38.
