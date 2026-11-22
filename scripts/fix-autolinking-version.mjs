// Expo 54 (autolinking Gradle) mewajibkan `version` di android/build.gradle tiap paket native.
// Beberapa library (mis. Bluetooth) tidak menuliskannya → "Field 'version' is required ... Publication".
// Skrip ini menambahkan `version = "<versi package.json>"` pada paket dependensi yang belum punya. Jalankan setelah npm install.
import fs from 'node:fs';
import path from 'node:path';

const root = JSON.parse(fs.readFileSync('package.json', 'utf8'));
const names = Object.keys({ ...root.dependencies, ...root.devDependencies });
let patched = 0;
for (const name of names) {
  const dir = path.join('node_modules', name);
  const pkgFile = path.join(dir, 'package.json');
  if (!fs.existsSync(pkgFile)) continue;
  const version = JSON.parse(fs.readFileSync(pkgFile, 'utf8')).version;
  for (const gradle of ['build.gradle', 'build.gradle.kts']) {
    const file = path.join(dir, 'android', gradle);
    if (!fs.existsSync(file)) continue;
    const src = fs.readFileSync(file, 'utf8');
    if (/^\s*version\s*=\s*['"]/m.test(src) || /^\s*version\s+['"]/m.test(src)) continue;
    const kts = gradle.endsWith('.kts');
    const line = kts ? `version = "${version}"` : `version = '${version}'`;
    // sisipkan tepat setelah baris group (bila ada), selain itu di baris pertama
    const out = /^\s*group\s*=?\s*['"].*$/m.test(src)
      ? src.replace(/^(\s*group\s*=?\s*['"].*)$/m, `$1\n${line}`)
      : `${line}\n${src}`;
    fs.writeFileSync(file, out);
    console.log(`patched ${name}@${version} (${gradle})`);
    patched++;
  }
}
console.log(`fix-autolinking-version: ${patched} paket dipatch`);
