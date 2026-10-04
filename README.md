# SakuKasir V29

V29 is the UI-breadth pass after the V28 build baseline.

## Current status

- UI implementation: expanded from preview pages to real Compose management screens.
- Build verification: **NOT RUN** in this environment.
- Firebase/Auth/RTDB architecture: retained.
- Offline transaction/shift queue: retained.
- Bluetooth printer/reprint: retained.
- SVG/vector UI assets: retained.

## V29 UI scope

- POS product grid and dynamic category chips
- Checkout Cash/QRIS, discount, cash received and change
- Transaction success/detail/history and filters
- Product list/grid, search, categories, stock, two-step editor and local photo picker
- Category list/add/edit/empty
- Stock ON/OFF and low-stock threshold
- Outlet list/form
- Worker list/form
- Owner/business profile and business form
- QRIS configuration and proof picker
- Printer settings/dialog
- Reports: Ringkasan, Outlet, Kasir, Harian, Bulanan
- Expenses and financial summary
- Theme, sync and about
- Responsive Compose layout foundation
- Native vector assets for the UI icon set

## Validation

`bash scripts/preflight.sh` passes.

The final visual fidelity claim must be verified with an actual Codemagic build and device/emulator screenshots against the mockup. V29 therefore does **not** mark itself as build-success or pixel-perfect.
