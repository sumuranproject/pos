#!/bin/sh
set -eu
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
GRADLE_VERSION=8.7
DIST_DIR="$APP_HOME/.gradle-local/gradle-$GRADLE_VERSION"
DIST_ZIP="$APP_HOME/.gradle-local/gradle-$GRADLE_VERSION-bin.zip"

if command -v gradle >/dev/null 2>&1; then
  exec gradle "$@"
fi

if [ ! -x "$DIST_DIR/bin/gradle" ]; then
  mkdir -p "$APP_HOME/.gradle-local"
  if [ ! -f "$DIST_ZIP" ]; then
    command -v curl >/dev/null 2>&1 || { echo "Gradle tidak tersedia dan curl tidak ditemukan." >&2; exit 1; }
    echo "Mengunduh Gradle $GRADLE_VERSION..." >&2
    curl -fL --retry 3 --retry-delay 2 -o "$DIST_ZIP" "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"
  fi
  command -v unzip >/dev/null 2>&1 || { echo "Gradle tidak tersedia dan unzip tidak ditemukan." >&2; exit 1; }
  rm -rf "$DIST_DIR" "$APP_HOME/.gradle-local/gradle-$GRADLE_VERSION"
  unzip -q "$DIST_ZIP" -d "$APP_HOME/.gradle-local"
fi

exec "$DIST_DIR/bin/gradle" "$@"
