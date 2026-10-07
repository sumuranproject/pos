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
- `kasir2` tersedia untuk pengujian permission. `kasir3` tersedia sebagai akun nonaktif untuk pengujian pembatasan login.

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

## Revision r8 UI parity and navigation fixes

- Audited against the supplied `acuan.zip`, `app.js`, and `style.css`.
- Kept Products, Categories, Stock, Outlets, and Cashiers as separate pages; Reports is the only tabbed area.
- Replaced the sidebar/drawer shell with a scrollable secondary menu sheet.
- Corrected secondary-page routing and back-to-home behavior.
- Added working QRIS merchant image selection/removal.
- Tightened cashier authentication/active-account/password handling and worker password validation.
- Synchronized light/dark system bar appearance with the app theme.

## Revision r5 audit fixes

- Replaced the Java-21 custom Gradle wrapper dependency with a Java-17-compatible launcher script. It delegates to an installed `gradle` binary or downloads Gradle 8.7 when needed.
- Kept the project toolchain on Java 17 and explicitly configured the Kotlin JVM toolchain to 17.
- Made the offline sync outbox persistent across process restarts using local SharedPreferences-backed storage.
- QRIS proof capture now uses the actual generated transaction ID, creates the camera file only when capture starts, honors configured QRIS retention, and rejects proof older than five minutes at checkout.
- QRIS proof expiry is evaluated from `expiredAt` when displayed instead of relying on a stale boolean.
- Bluetooth printer writes now fail when disconnected, flush output, handle Android 12+ permissions, and the printer screen can scan paired devices, connect, and test-print.
- Removed the Java-21 `gradle-wrapper.jar` that caused `UnsupportedClassVersionError` under Codemagic Java 17.

A real Firebase backend is intentionally not enabled in this revision because the bundled `google-services.json` is still a placeholder. Firebase SDK dependencies remain present for the later backend integration.
