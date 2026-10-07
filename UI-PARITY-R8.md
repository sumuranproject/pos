# R8 — UI parity audit & navigation correction

Basis audit:
- `acuan.zip` (47 reference screenshots)
- `app.js` (behavior, page routing, structure)
- `style.css` (visual tokens and component styling)
- Claude Android source in this repository

R8 corrections:
- Produk, Kategori, Stok, Outlet, and Kasir remain standalone pages; Reports is the only tabbed report area.
- Owner/cashier secondary navigation no longer uses a sidebar/drawer. The SK header button opens a scrollable menu sheet instead.
- Owner menu keeps Katalog / Bisnis / Pengaturan accordion grouping.
- Cashier menu remains a flat permission-filtered list.
- All secondary routes map to dedicated Compose pages with `← Beranda` behavior matching the web router.
- Receipt, notification settings, sync, security, audit, theme, profile, about, QRIS, printer, and expenses retain dedicated screens.
- QRIS merchant image upload now works through the Android document picker and can be replaced/removed.
- Cashier login now checks account active state and password; worker password editing is validated and retained in the local worker model.
- Login/register/forgot validation follows the web reference flow more closely.
- System status/navigation bars follow the light/dark theme.
- Inter remains the default UI font; JetBrains Mono remains explicit for IDs, audit/receipt timestamps, and receipt preview.

Validation performed locally:
- Source structure/braces checked.
- No Kotlin parser-level `expecting` / `unexpected tokens` errors remain when parsed with standalone `kotlinc` (Android/Compose dependencies are intentionally unavailable in this environment, so unresolved Android references are expected).
- Gradle/Codemagic build has not been run in this environment.
