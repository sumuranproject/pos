#!/usr/bin/env bash
set -euo pipefail

FONT_DIR="app/src/main/res/font"
mkdir -p "$FONT_DIR"

# TTF files are bundled in the repository, so the normal Codemagic build path
# does not require network access. Downloads are only a fallback for missing
# or invalid files.
is_valid_ttf() {
  local file="$1"
  [ -s "$file" ] || return 1
  local magic
  magic="$(dd if="$file" bs=1 count=4 2>/dev/null | od -An -tx1 | tr -d ' \n')"
  [ "$magic" = "00010000" ]
}

font_url() {
  case "$1" in
    inter_regular.ttf) echo "https://raw.githubusercontent.com/google/fonts/main/ofl/inter/static/Inter_18pt-Regular.ttf" ;;
    inter_medium.ttf) echo "https://raw.githubusercontent.com/google/fonts/main/ofl/inter/static/Inter_18pt-Medium.ttf" ;;
    inter_semibold.ttf) echo "https://raw.githubusercontent.com/google/fonts/main/ofl/inter/static/Inter_18pt-SemiBold.ttf" ;;
    inter_bold.ttf) echo "https://raw.githubusercontent.com/google/fonts/main/ofl/inter/static/Inter_18pt-Bold.ttf" ;;
    jetbrains_mono_regular.ttf) echo "https://raw.githubusercontent.com/google/fonts/main/ofl/jetbrainsmono/static/JetBrainsMono-Regular.ttf" ;;
    jetbrains_mono_medium.ttf) echo "https://raw.githubusercontent.com/google/fonts/main/ofl/jetbrainsmono/static/JetBrainsMono-Medium.ttf" ;;
    *) echo "Unknown font: $1" >&2; return 1 ;;
  esac
}

fonts="inter_regular.ttf inter_medium.ttf inter_semibold.ttf inter_bold.ttf jetbrains_mono_regular.ttf jetbrains_mono_medium.ttf"

for font in $fonts; do
  target="$FONT_DIR/$font"
  if is_valid_ttf "$target"; then
    echo "Using bundled font: $font"
    continue
  fi

  url="$(font_url "$font")"
  echo "Bundled font missing/invalid, downloading: $font"
  rm -f "$target"
  curl -fL --retry 3 --retry-delay 2 -o "$target" "$url"

  if ! is_valid_ttf "$target"; then
    echo "Downloaded file is not a valid TrueType font: $font" >&2
    exit 1
  fi
 done

echo "Bundled fonts ready: Inter + JetBrains Mono"
