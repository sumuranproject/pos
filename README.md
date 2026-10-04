# SakuKasir V34

V34 is the native Jetpack Compose UI implementation pass based on the supplied Saku Kasir HTML mockup.

## Baseline

- Base source: `KASIR-PATCH-20261004-V33.zip`
- V32 remains the frozen baseline.
- V34 changes the native UI layer only; the existing backend/data architecture is retained.

## UI scope

- Mockup-aligned light/dark visual system
- Role selection and authentication flow
- Compact app header and bottom navigation
- Shift status strip
- Product search and category chips
- Product grid and cart summary
- Cash/QRIS checkout
- Transaction success and printing entry point
- Reports and transaction history
- Owner/cashier settings and management entry points
- Product/category/stock/business/outlet/worker management views
- Sync, QRIS, theme and printer entry points

## Firebase protection

The following V32 Firebase-related files were left unchanged:

- `app/google-services.json`
- `app/src/main/java/com/pentolrebus/kasir/data/PosRepository.kt`
- `app/src/main/java/com/pentolrebus/kasir/data/RepositoryProvider.kt`
- `app/src/main/java/com/pentolrebus/kasir/util/OwnerMessagingService.kt`
- `database.rules.json`
- Firebase dependencies and Gradle configuration

## Validation

`bash scripts/preflight.sh` passes.

A full Gradle/Codemagic build was not run in this environment because no Gradle executable or wrapper is available locally. The final visual fidelity and device behavior should be verified with the Codemagic build/device or emulator.
