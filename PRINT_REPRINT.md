# SakuKasir V22 — Printer & Reprint

This phase keeps the V21 Firebase/RTDB architecture and adds native Bluetooth thermal-printer support plus reprint from transaction history.

- Paired Bluetooth printers are discovered from Android's paired-device list.
- Android 12+ requests BLUETOOTH_CONNECT at runtime.
- ESC/POS receipt bytes are generated locally from the stored Transaction.
- Laporan now exposes transaction history and a Detail Transaksi dialog with CETAK ULANG.
- Printer settings opens the paired-printer list.
- Reprinting does not create a new transaction and does not modify Firebase data.
- No printer credentials/secrets are stored.

Limitation for this phase: printer selection is not yet persisted as a preferred device. That can be added with the full Printer Settings screen in a later phase.
