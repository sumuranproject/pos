#!/usr/bin/env bash
set -euo pipefail

python3 - <<'PY'
from pathlib import Path
import re

root = Path('.')
required = [
    Path('settings.gradle.kts'),
    Path('build.gradle.kts'),
    Path('app/build.gradle.kts'),
    Path('app/google-services.json'),
    Path('app/src/main/AndroidManifest.xml'),
    Path('app/src/main/java/com/pentolrebus/kasir/MainActivity.kt'),
    Path('app/src/main/java/com/pentolrebus/kasir/ui/PosViewModel.kt'),
    Path('app/src/main/java/com/pentolrebus/kasir/ui/Theme.kt'),
    Path('app/src/main/java/com/pentolrebus/kasir/data/PosRepository.kt'),
    Path('app/src/main/java/com/pentolrebus/kasir/data/LocalPosRepository.kt'),
]
missing = [str(p) for p in required if not p.exists()]
if missing:
    raise SystemExit('Missing required files: ' + ', '.join(missing))

if Path('app/google-services.json.example').exists():
    raise SystemExit('Forbidden placeholder Firebase config exists')

files = list(root.glob('app/src/main/java/**/*.kt'))
if not files:
    raise SystemExit('No Kotlin source files found')

bad_patterns = [
    r'android\.webkit', r'\bWebView\b', r'\bloadUrl\s*\(',
    r'\bGlobalScope\b', r'\brunBlocking\b',
    r'<<<<<<<|=======|>>>>>>>',
]
for path in files:
    text = path.read_text(encoding='utf-8')
    for pattern in bad_patterns:
        if re.search(pattern, text):
            raise SystemExit(f'Forbidden/corrupt source pattern {pattern!r} in {path}')
    if not text.lstrip().startswith('package '):
        raise SystemExit(f'Missing package declaration: {path}')

# Lightweight delimiter check after removing comments and quoted literals.
def stripped(s: str) -> str:
    s = re.sub(r'/\*.*?\*/', '', s, flags=re.S)
    s = re.sub(r'//.*', '', s)
    s = re.sub(r'"(?:\\.|[^"\\])*"', '""', s)
    s = re.sub(r"'(?:\\.|[^'\\])*'", "''", s)
    return s

pairs = {'(': ')', '[': ']', '{': '}'}
for path in files:
    stack = []
    for i, ch in enumerate(stripped(path.read_text(encoding='utf-8')), 1):
        if ch in pairs:
            stack.append((ch, i))
        elif ch in pairs.values():
            if not stack or pairs[stack[-1][0]] != ch:
                raise SystemExit(f'Delimiter mismatch in {path} near character {i}')
            stack.pop()
    if stack:
        raise SystemExit(f'Unclosed delimiter in {path}: {stack[-5:]}')

# Compose API import checks for APIs used by this project.
main = Path('app/src/main/java/com/pentolrebus/kasir/MainActivity.kt').read_text(encoding='utf-8')
expected_imports = {
    'KeyboardOptions': 'import androidx.compose.foundation.text.KeyboardOptions',
    'KeyboardType': 'import androidx.compose.ui.text.input.KeyboardType',
    'PasswordVisualTransformation': 'import androidx.compose.ui.text.input.PasswordVisualTransformation',
    'VisualTransformation': 'import androidx.compose.ui.text.input.VisualTransformation',
}
for symbol, expected in expected_imports.items():
    if symbol in main and expected not in main:
        raise SystemExit(f'Invalid Compose import for {symbol}; expected {expected}')

# Contract checks for the repository implementations.
repo = Path('app/src/main/java/com/pentolrebus/kasir/data/PosRepository.kt').read_text()
local = Path('app/src/main/java/com/pentolrebus/kasir/data/LocalPosRepository.kt').read_text()
for name in ('saveProduct', 'saveTransaction'):
    if not re.search(rf'override\s+suspend\s+fun\s+{name}\b[^\n]*:\s*Result<Unit>', repo):
        raise SystemExit(f'Firebase repository contract missing Result<Unit> for {name}')
    if not re.search(rf'override\s+suspend\s+fun\s+{name}\b[^\n]*:\s*Result<Unit>', local):
        raise SystemExit(f'Local repository contract missing Result<Unit> for {name}')

# Credential safety checks.
profile = Path('app/src/main/java/com/pentolrebus/kasir/data/PosRepository.kt').read_text()
if re.search(r'put\("(?:password|pin)"', profile, re.I):
    raise SystemExit('Plain password/PIN appears to be written to RTDB profile map')

print(f'Source preflight passed: {len(files)} Kotlin files checked.')
PY

test "$(grep -c 'sourceCompatibility = JavaVersion.VERSION_21' app/build.gradle.kts)" -eq 1
test "$(grep -c 'targetCompatibility = JavaVersion.VERSION_21' app/build.gradle.kts)" -eq 1
grep -F 'jvmToolchain(21)' app/build.gradle.kts >/dev/null
grep -F 'androidx.compose:compose-bom:2025.08.00' app/build.gradle.kts >/dev/null
grep -F 'com.google.firebase:firebase-bom:34.15.0' app/build.gradle.kts >/dev/null
grep -F 'instance_type: mac_mini_m2' codemagic.yaml >/dev/null

echo 'Gradle/Firebase/Codemagic configuration preflight passed.'
