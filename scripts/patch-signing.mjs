// Dijalankan di CI setelah `npx expo prebuild --platform android`: signing release (env Codemagic) + versionCode.
import fs from "node:fs";

const gradlePath = "android/app/build.gradle";
let g = fs.readFileSync(gradlePath, "utf8");

if (!g.includes("CM_KEYSTORE_PATH")) {
  g = g.replace(/signingConfigs\s*\{/, `signingConfigs {
        release {
            if (System.getenv("CM_KEYSTORE_PATH")) {
                storeFile file(System.getenv("CM_KEYSTORE_PATH"))
                storePassword System.getenv("CM_KEYSTORE_PASSWORD")
                keyAlias System.getenv("CM_KEY_ALIAS")
                keyPassword System.getenv("CM_KEY_PASSWORD")
            }
        }`);
  // template Expo: buildTypes.release memakai signingConfigs.debug -> ganti ke release
  g = g.replace(/(buildTypes\s*\{[\s\S]*?release\s*\{[\s\S]*?)signingConfig signingConfigs\.debug/, "$1signingConfig signingConfigs.release");
}
if (process.env.BUILD_NUMBER) g = g.replace(/versionCode\s+\d+/, `versionCode ${process.env.BUILD_NUMBER}`);
fs.writeFileSync(gradlePath, g);

const m = fs.readFileSync("android/app/src/main/AndroidManifest.xml", "utf8");
for (const p of ["BLUETOOTH_SCAN", "BLUETOOTH_CONNECT", "BLUETOOTH_ADMIN"]) {
  if (!m.includes(p)) console.warn("PERINGATAN: izin " + p + " tidak ada di AndroidManifest.xml");
}
console.log("Android dipatch: signing release + versionCode; izin Bluetooth diperiksa");
