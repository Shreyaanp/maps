#!/usr/bin/env bash
set -euo pipefail

PKG="work.shreyaan.dwell"
PHONE_SERIAL="${PHONE_SERIAL:-}"
WATCH_SERIAL="${WATCH_SERIAL:-}"

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

adb_shell() {
  local serial="$1"
  shift
  adb -s "$serial" shell "$@"
}

detect_devices() {
  local line serial
  while IFS= read -r line; do
    [[ "$line" == List* || -z "$line" ]] && continue
    [[ "$line" != *$'\t'device* && "$line" != *" device "* ]] && continue
    serial="${line%%[[:space:]]*}"

    if [[ -z "$WATCH_SERIAL" && ( "$line" == *"model:Pixel_Watch"* || "$line" == *"device:meridian"* || "$serial" == adb-* ) ]]; then
      # Prefer the direct tcp serial over the mDNS alias when both are present.
      if [[ "$serial" != adb-* || -z "$WATCH_SERIAL" ]]; then
        WATCH_SERIAL="$serial"
      fi
    elif [[ -z "$PHONE_SERIAL" ]]; then
      PHONE_SERIAL="$serial"
    fi
  done < <(adb devices -l)

  [[ -n "$PHONE_SERIAL" ]] || die "no phone device found; set PHONE_SERIAL=..."
  [[ -n "$WATCH_SERIAL" ]] || die "no Wear OS device found; set WATCH_SERIAL=..."
}

current_user() {
  adb_shell "$1" am get-current-user | tr -d '\r'
}

device_sdk() {
  adb_shell "$1" getprop ro.build.version.sdk | tr -d '\r'
}

phone_permissions_for_sdk() {
  local sdk="$1"

  echo android.permission.ACCESS_FINE_LOCATION
  echo android.permission.ACCESS_COARSE_LOCATION

  if (( sdk >= 29 )); then
    echo android.permission.ACCESS_BACKGROUND_LOCATION
    echo android.permission.ACTIVITY_RECOGNITION
  fi

  if (( sdk >= 33 )); then
    echo android.permission.POST_NOTIFICATIONS
  fi
}

package_installed_for_user() {
  local serial="$1"
  local user="$2"
  adb_shell "$serial" dumpsys package "$PKG" |
    awk -v user="User $user:" '
      $0 ~ user { in_user=1 }
      in_user && /installed=true/ { found=1 }
      in_user && $0 ~ /User [0-9]+:/ && $0 !~ user { in_user=0 }
      END { exit found ? 0 : 1 }
    '
}

ensure_installed_for_user() {
  local serial="$1"
  local user="$2"
  if package_installed_for_user "$serial" "$user"; then
    return
  fi

  echo "Package exists but is not installed for user $user on $serial; enabling it there."
  adb_shell "$serial" cmd package install-existing --user "$user" "$PKG" >/dev/null
}

grant_or_note() {
  local serial="$1"
  local user="$2"
  local perm="$3"

  if runtime_permission_granted "$serial" "$user" "$perm"; then
    echo "already granted $perm"
    return 0
  fi

  if adb_shell "$serial" pm grant --user "$user" "$PKG" "$perm" >/tmp/dwell-grant.err 2>&1; then
    echo "granted $perm"
  else
    echo "manual permission needed for $perm"
    sed 's/^/  /' /tmp/dwell-grant.err | head -20
    return 1
  fi
}

runtime_permission_granted() {
  local serial="$1"
  local user="$2"
  local perm="$3"
  adb_shell "$serial" dumpsys package "$PKG" |
    sed -n "/User $user:/,/User /p" |
    rg "$perm: granted=true" >/dev/null
}

missing_phone_permissions() {
  local serial="$1"
  local user="$2"
  local sdk="$3"
  local missing=0
  local perm
  while IFS= read -r perm; do
    if ! runtime_permission_granted "$serial" "$user" "$perm"; then
      echo "$perm"
      missing=1
    fi
  done < <(phone_permissions_for_sdk "$sdk")
  return "$missing"
}

open_app_settings() {
  local serial="$1"
  local user="$2"
  adb_shell "$serial" am start --user "$user" \
    -a android.settings.APPLICATION_DETAILS_SETTINGS \
    -d "package:$PKG" >/dev/null
}

permission_summary() {
  local serial="$1"
  local user="$2"
  adb_shell "$serial" dumpsys package "$PKG" |
    sed -n "/User $user:/,/User /p" |
    rg "installed=|POST_NOTIFICATIONS|ACCESS_FINE_LOCATION|ACCESS_COARSE_LOCATION|ACCESS_BACKGROUND_LOCATION|ACTIVITY_RECOGNITION|granted=" || true
}

launch_and_scan() {
  local serial="$1"
  local label="$2"
  local user="${3:-}"

  adb -s "$serial" logcat -c
  if [[ -n "$user" ]]; then
    adb_shell "$serial" am start --user "$user" -n "$PKG/.MainActivity" >/dev/null
  else
    adb_shell "$serial" monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/dev/null
  fi

  sleep 5
  echo "$label pid: $(adb_shell "$serial" pidof "$PKG" || true)"
  if adb -s "$serial" logcat -d -t 600 | rg "FATAL EXCEPTION|AndroidRuntime.*$PKG|$PKG.*FATAL" >/tmp/dwell-crashes.txt; then
    echo "$label crash scan found possible fatal logs:"
    cat /tmp/dwell-crashes.txt
    return 1
  fi
  echo "$label crash scan: clean"
}

detect_devices

PHONE_USER="$(current_user "$PHONE_SERIAL")"
PHONE_SDK="$(device_sdk "$PHONE_SERIAL")"
echo "Phone: $PHONE_SERIAL user $PHONE_USER sdk $PHONE_SDK"
echo "Watch: $WATCH_SERIAL"

echo
echo "Installing debug builds..."
ANDROID_SERIAL="$PHONE_SERIAL" ./gradlew :app:installDebug
ANDROID_SERIAL="$WATCH_SERIAL" ./gradlew :wear:installDebug

echo
echo "Checking install user/profile state..."
ensure_installed_for_user "$PHONE_SERIAL" "$PHONE_USER"
ensure_installed_for_user "$WATCH_SERIAL" 0

echo
echo "Granting permissions where the device allows ADB grants..."
while IFS= read -r perm; do
  grant_or_note "$PHONE_SERIAL" "$PHONE_USER" "$perm" || true
done < <(phone_permissions_for_sdk "$PHONE_SDK")
grant_or_note "$WATCH_SERIAL" 0 android.permission.POST_NOTIFICATIONS || true

echo
echo "Phone permissions:"
permission_summary "$PHONE_SERIAL" "$PHONE_USER"

MISSING_PHONE_PERMISSIONS="$(missing_phone_permissions "$PHONE_SERIAL" "$PHONE_USER" "$PHONE_SDK" || true)"
if [[ -n "$MISSING_PHONE_PERMISSIONS" ]]; then
  echo
  echo "The phone still needs these permissions:"
  echo "$MISSING_PHONE_PERMISSIONS" | sed 's/^/  /'
  echo
  echo "Set Permissions to allow Location all the time, Physical activity, and Notifications, then rerun this script."
  open_app_settings "$PHONE_SERIAL" "$PHONE_USER"
  exit 2
fi

echo
echo "Launching apps and checking immediate crashes..."
launch_and_scan "$PHONE_SERIAL" "phone" "$PHONE_USER"
launch_and_scan "$WATCH_SERIAL" "watch"

echo
echo "Ready for testing."
echo "Next: open Dwell on the phone, clear diagnostics, monitor a place, lock the phone, and run FIELD_TEST.md."
