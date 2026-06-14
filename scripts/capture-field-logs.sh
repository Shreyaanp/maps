#!/usr/bin/env bash
set -euo pipefail

PHONE_SERIAL="${PHONE_SERIAL:-}"
WATCH_SERIAL="${WATCH_SERIAL:-}"
DURATION_SECONDS="${DURATION_SECONDS:-900}"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT_DIR="${OUT_DIR:-$ROOT_DIR/field-logs/$(date -u +%Y%m%dT%H%M%SZ)}"

is_watch_device() {
  local serial="$1"
  local line_hint="${2:-}"
  local characteristics model signature

  characteristics="$(adb -s "$serial" shell getprop ro.build.characteristics 2>/dev/null | tr -d '\r' || true)"
  model="$(adb -s "$serial" shell getprop ro.product.model 2>/dev/null | tr -d '\r' || true)"
  signature="$(printf '%s %s %s' "$characteristics" "$model" "$line_hint" | tr '[:upper:]' '[:lower:]')"

  [[ "$signature" == *watch* || "$signature" == *wear* || "$signature" == *meridian* || "$signature" == *pixel_watch* ]]
}

detect_devices() {
  local line serial
  while IFS= read -r line; do
    [[ "$line" == List* || -z "$line" ]] && continue
    [[ "$line" != *$'\t'device* && "$line" != *" device "* ]] && continue
    serial="${line%%[[:space:]]*}"
    [[ "$serial" == "$PHONE_SERIAL" || "$serial" == "$WATCH_SERIAL" ]] && continue

    if [[ -z "$WATCH_SERIAL" ]] && is_watch_device "$serial" "$line"; then
      WATCH_SERIAL="$serial"
    elif [[ -z "$PHONE_SERIAL" ]]; then
      PHONE_SERIAL="$serial"
    fi
  done < <(adb devices -l)
}

capture_device() {
  local serial="$1"
  local label="$2"
  local output="$OUT_DIR/$label.log"

  adb -s "$serial" logcat -c || true
  adb -s "$serial" logcat -v time \
    DwellDiagnostics:I \
    DwellWearSync:I \
    DwellWatchData:I \
    DwellTile:I \
    DwellBackend:W \
    DwellLocation:W \
    DwellArrival:W \
    DwellActivity:W \
    DwellMapCache:W \
    AndroidRuntime:E \
    '*:S' >"$output" &
  echo "$!"
}

detect_devices
mkdir -p "$OUT_DIR"

echo "Writing field logs to: $OUT_DIR"
echo "Duration: ${DURATION_SECONDS}s"
echo "Privacy: this captures Dwell diagnostic buckets and sync/runtime errors only."

pids=()
if [[ -n "$PHONE_SERIAL" ]]; then
  echo "Phone: $PHONE_SERIAL"
  pids+=("$(capture_device "$PHONE_SERIAL" phone)")
else
  echo "Phone: not connected"
fi

if [[ -n "$WATCH_SERIAL" ]]; then
  echo "Watch: $WATCH_SERIAL"
  pids+=("$(capture_device "$WATCH_SERIAL" watch)")
else
  echo "Watch: not connected (set WATCH_SERIAL=... if Wireless debugging hides it)"
fi

if [[ "${#pids[@]}" -eq 0 ]]; then
  echo "error: no ADB devices found; set PHONE_SERIAL/WATCH_SERIAL or connect devices" >&2
  exit 1
fi

cleanup() {
  for pid in "${pids[@]}"; do
    kill "$pid" >/dev/null 2>&1 || true
  done
  echo
  echo "Saved logs:"
  find "$OUT_DIR" -maxdepth 1 -type f -print
}
trap cleanup EXIT INT TERM

sleep "$DURATION_SECONDS"
