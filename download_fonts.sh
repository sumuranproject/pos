#!/usr/bin/env bash
set -euo pipefail

FONT_DIR="app/src/main/res/font"
mkdir -p "$FONT_DIR"

curl -fL --retry 3 --retry-delay 2 -o "$FONT_DIR/inter_regular.ttf" \
  "https://github.com/rsms/inter/raw/master/docs/font-files/Inter-Regular.ttf"
curl -fL --retry 3 --retry-delay 2 -o "$FONT_DIR/inter_medium.ttf" \
  "https://github.com/rsms/inter/raw/master/docs/font-files/Inter-Medium.ttf"
curl -fL --retry 3 --retry-delay 2 -o "$FONT_DIR/inter_semibold.ttf" \
  "https://github.com/rsms/inter/raw/master/docs/font-files/Inter-SemiBold.ttf"
curl -fL --retry 3 --retry-delay 2 -o "$FONT_DIR/inter_bold.ttf" \
  "https://github.com/rsms/inter/raw/master/docs/font-files/Inter-Bold.ttf"

curl -fL --retry 3 --retry-delay 2 -o "$FONT_DIR/jetbrains_mono_regular.ttf" \
  "https://github.com/JetBrains/JetBrainsMono/raw/master/fonts/ttf/JetBrainsMono-Regular.ttf"
curl -fL --retry 3 --retry-delay 2 -o "$FONT_DIR/jetbrains_mono_medium.ttf" \
  "https://github.com/JetBrains/JetBrainsMono/raw/master/fonts/ttf/JetBrainsMono-Medium.ttf"

for f in \
  inter_regular.ttf inter_medium.ttf inter_semibold.ttf inter_bold.ttf \
  jetbrains_mono_regular.ttf jetbrains_mono_medium.ttf; do
  test -s "$FONT_DIR/$f" || { echo "Font download failed or file is empty: $f" >&2; exit 1; }
done

echo "Bundled fonts ready: Inter + JetBrains Mono"
