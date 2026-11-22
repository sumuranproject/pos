// Expo 54 (autolinking Gradle) mewajibkan `version = '...'` di android/build.gradle tiap MODUL EXPO
// (paket yang punya expo-module.config.json), termasuk paket turunan (expo-font, expo-asset, dst.), bukan hanya
// dependensi langsung. Skrip ini memindai SELURUH node_modules (termasuk salinan bersarang) dan menambahkan baris itu
// bila belum ada. Jalankan setelah `npm install` / `expo install --fix`, sebelum `expo prebuild`.
import fs from 'node:fs';
import path from 'node:path';

const VERSION_RE = /^\s*version\s*=\s*['"][^'"]+['"]/m;
const found = [];

function walk(dir, depth = 0) {
  if (depth > 6) return;
  let entries;
  try { entries = fs.readdirSync(dir, { withFileTypes: true }); } catch { return; }
  for (const e of entries) {
    if (!e.isDirectory() && !e.isSymbolicLink()) continue;
    const p = path.join(dir, e.name);
    if (e.name.startsWith('@')) { walk(p, depth + 1); continue; }
    if (fs.existsSync(path.join(p, 'package.json'))) found.push(p);
    const nested = path.join(p, 'node_modules');
    if (fs.existsSync(nested)) walk(nested, depth + 1);
  }
}
walk('node_modules');

let patched = 0; let checked = 0;
for (const dir of new Set(found)) {
  const isExpoModule = fs.existsSync(path.join(dir, 'expo-module.config.json'));
  const hasAndroid = ['build.gradle', 'build.gradle.kts'].some((g) => fs.existsSync(path.join(dir, 'android', g)));
  if (!hasAndroid) continue;
  // modul Expo selalu; library RN biasa hanya bila bukan modul expo tetapi di daftar dependensi langsung (kompatibel skrip lama)
  const root = JSON.parse(fs.readFileSync('package.json', 'utf8'));
  const direct = Object.keys({ ...root.dependencies, ...root.devDependencies });
  const pkg = JSON.parse(fs.readFileSync(path.join(dir, 'package.json'), 'utf8'));
  if (!isExpoModule && !direct.includes(pkg.name)) continue;
  checked++;
  for (const gradle of ['build.gradle', 'build.gradle.kts']) {
    const file = path.join(dir, 'android', gradle);
    if (!fs.existsSync(file)) continue;
    const src = fs.readFileSync(file, 'utf8');
    if (VERSION_RE.test(src)) continue;
    const kts = gradle.endsWith('.kts');
    const line = kts ? `version = "${pkg.version}"` : `version = '${pkg.version}'`;
    const groupRe = /^(\s*group\s*=?\s*['"].*)$/m;
    const out = groupRe.test(src) ? src.replace(groupRe, `$1\n${line}`) : `${line}\n${src}`;
    fs.writeFileSync(file, out);
    console.log(`patched ${pkg.name}@${pkg.version} (${path.relative('.', file)})`);
    patched++;
  }
}
console.log(`fix-autolinking-version: ${checked} paket native diperiksa, ${patched} dipatch`);
