#!/usr/bin/env bash
set -euo pipefail

PHONE_SERIAL="${PHONE_SERIAL:-}"
TEST_CLASS="${TEST_CLASS:-work.shreyaan.dwell.OnDeviceHomeOfficeGymLogicTest}"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

cd "$ROOT_DIR"

if [[ -z "${JAVA_HOME:-}" ]]; then
  if command -v /usr/libexec/java_home >/dev/null 2>&1; then
    export JAVA_HOME="$(/usr/libexec/java_home -v 17)"
  fi
fi

die() {
  echo "error: $*" >&2
  exit 1
}

if ! command -v adb >/dev/null 2>&1; then
  die "adb not found; install Android platform-tools or add adb to PATH"
fi

detect_phone() {
  local line serial
  while IFS= read -r line; do
    [[ "$line" == List* || -z "$line" ]] && continue
    [[ "$line" == *$'\t'offline* || "$line" == *" offline "* ]] && continue
    [[ "$line" == *$'\t'unauthorized* || "$line" == *" unauthorized "* ]] && continue
    [[ "$line" != *$'\t'device* && "$line" != *" device "* ]] && continue

    serial="${line%%[[:space:]]*}"
    if [[ "$line" == *"model:Pixel_Watch"* || "$line" == *"device:meridian"* || "$serial" == adb-* ]]; then
      continue
    fi

    PHONE_SERIAL="$serial"
    return
  done < <(adb devices -l)
}

if [[ -z "$PHONE_SERIAL" ]]; then
  detect_phone
fi

if [[ -z "$PHONE_SERIAL" ]]; then
  adb devices -l
  die "no authorized phone found; connect a phone or set PHONE_SERIAL=..."
fi

echo "Phone: $PHONE_SERIAL"
echo "Running on-device flow test: $TEST_CLASS"
echo

ANDROID_SERIAL="$PHONE_SERIAL" ./gradlew \
  --console=plain \
  :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class="$TEST_CLASS"
