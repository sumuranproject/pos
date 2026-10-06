# SakuKasir Android

Native Android POS untuk `com.sakukasir.pos`.

## Teknologi
- Kotlin 1.9.24
- Jetpack Compose + Material 3, BOM 2024.09.00
- AGP 8.5.2 / Gradle 8.7 / Java 17
- MVVM + StateFlow + Coroutines
- Room offline cache + OfflineSyncQueue
- DataStore secure local session/theme
- Firebase SDK siap untuk tahap backend
- Supabase integration contract disiapkan di util layer
- Bluetooth Classic SPP + BLE-ready permission boundary
- Kamera QRIS memakai `ActivityResultContracts.TakePicture()`

## Demo login
- `owner` — Budi Santoso (OWNER)
- `kasir` — Andi Wijaya (CASHIER), password `kasir123`
- `kasir2` dan `kasir3` juga tersedia untuk pengujian permission.

## Build
```bash
./gradlew assembleDebug --no-daemon
```

Codemagic memakai `codemagic.yaml` dan menghasilkan:
`app/build/outputs/**/*.apk`

Wrapper Gradle disertakan. Jika distribusi Gradle belum tersedia di cache build, wrapper akan mengambil Gradle 8.7 dari distribution URL.

## Firebase
`google-services.json` yang disertakan adalah placeholder yang sengaja belum berisi project Firebase nyata. MVP memakai `LocalPosRepository`, sehingga project dapat dikembangkan tanpa kredensial backend.

Saat Firebase benar-benar diaktifkan:
1. Ganti `google-services.json` dengan file project Firebase nyata.
2. Aktifkan Google Services Gradle plugin.
3. Sambungkan `RepositoryProvider` ke repository Firebase tanpa mengubah UI.

## Supabase
Credential sensitif tidak disimpan di APK.
- anon key: dapat digunakan di client dengan RLS yang benar.
- service role key: hanya di Edge Function.
- `QrisStore` adalah boundary untuk upload `uploadQrisProof`.
- Retensi QRIS dirancang 35 hari dan backend dapat menandai bukti expired.

## Catatan
UI Android mengikuti struktur dan token visual dari prototipe SakuKasir yang diberikan: Inter, JetBrains Mono, hijau sebagai primary, ungu QRIS, semantic alert/warn, light/dark theme, bottom navigation, dan drawer menu sekunder.

## Bundled fonts

SakuKasir bundles Inter and JetBrains Mono inside `app/src/main/res/font/`; no runtime font download or Google Fonts Android library is required.

- Inter: `inter_regular.ttf`, `inter_medium.ttf`, `inter_semibold.ttf`, `inter_bold.ttf`
- JetBrains Mono: `jetbrains_mono_regular.ttf`, `jetbrains_mono_medium.ttf`

`download_fonts.sh` first validates the TTF files already bundled in the repository. This is the normal build path and avoids a network dependency. If a TTF is missing or invalid, the script falls back to the Google Fonts repository URLs and validates the downloaded TrueType header before continuing. Codemagic runs this script before the Android build.

Inter is the default Material 3 UI typeface. JetBrains Mono is used explicitly for TRX IDs and receipt/audit timestamp-style content.
