#!/usr/bin/env sh
set -eu
GRADLE_VERSION="8.14.3"
if command -v gradle >/dev/null 2>&1; then
  exec gradle "$@"
fi
BASE="${GRADLE_USER_HOME:-$HOME/.gradle}/wrapper/dists/gamopanel-gradle-${GRADLE_VERSION}"
BIN="$BASE/gradle-${GRADLE_VERSION}/bin/gradle"
if [ ! -x "$BIN" ]; then
  mkdir -p "$BASE"
  ARCHIVE="$BASE/gradle-${GRADLE_VERSION}-bin.zip"
  if command -v curl >/dev/null 2>&1; then
    curl -fL "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" -o "$ARCHIVE"
  elif command -v wget >/dev/null 2>&1; then
    wget -O "$ARCHIVE" "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"
  else
    echo "Instala Gradle ${GRADLE_VERSION} o curl/wget para descargarlo." >&2
    exit 1
  fi
  unzip -q -o "$ARCHIVE" -d "$BASE"
fi
exec "$BIN" "$@"
