# Saku Kasir — V44 Visual/Parity Revision

V44 is the native Jetpack Compose revision built from the V43 successful-build baseline, using the 48-screen screenshot reference and the fixed `saku-kasir-dpi-reference-fixed` HTML/CSS/JS package as the design/behavior references.

The implementation remains native Android. Firebase configuration, repository/offline state, authentication, reports, QRIS, Bluetooth printer, Dropbox integration shell, notifications and existing POS behavior are preserved.

This package has passed the repository's static source preflight and additional vector/delimiter/config audits, including checkout tax persistence and the native receipt preview path. A full APK build must still be run in Codemagic or an Android SDK build environment.
