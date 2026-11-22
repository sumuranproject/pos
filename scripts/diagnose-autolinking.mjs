// Mencetak modul autolinking Android yang publication.version-nya kosong + cuplikan build.gradle-nya.
import { execSync } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';
let j;
try { j = JSON.parse(execSync('npx expo-modules-autolinking resolve --platform android --json', { encoding: 'utf8', maxBuffer: 1 << 26 })); }
catch (e) { console.log('resolve gagal:', String(e.message).slice(0, 400)); process.exit(0); }
console.log('expo', JSON.parse(fs.readFileSync('node_modules/expo/package.json', 'utf8')).version,
  '| autolinking', JSON.parse(fs.readFileSync('node_modules/expo-modules-autolinking/package.json', 'utf8')).version);
(j.modules || []).forEach((m, i) => (m.projects || []).forEach((p) => {
  const ok = p.publication && p.publication.version;
  if (ok) return;
  console.log(`TANPA VERSION: modules[${i}] ${m.packageName} / ${p.name} @ ${m.packageVersion || '?'}`);
  const g = ['build.gradle', 'build.gradle.kts'].map((f) => path.join(m.sourceDir || p.sourceDir || '', f)).find((f) => fs.existsSync(f));
  if (g) console.log(fs.readFileSync(g, 'utf8').split('\n').slice(0, 12).map((l) => '   | ' + l).join('\n'));
}));
