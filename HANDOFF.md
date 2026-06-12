# Dwell — Project Handoff

Complete state of the project as of 2026-06-12, for whoever picks it up next.

## What Dwell is

An Android phone app **+** Wear OS watch app. You pin a place on a map; when you
physically arrive there, a countdown timer starts automatically (default 4.5 h,
adjustable). Alerts reach your wrist. Everything runs **on-device** — no backend,
no accounts, no analytics.

- **Package (immutable):** `work.shreyaan.dwell`
- **Stack:** native Kotlin + Jetpack Compose (phone), Wear Compose (watch)
- **Maps:** osmdroid / OpenStreetMap (no API key)
- **Min/target SDK:** phone 26/35, wear 30/34 · AGP 8.7.3 · Kotlin 2.0.21

## Repos (GitHub owner: Shreyaanp)

- **Shreyaanp/maps** — the app (this folder, `/Users/ichiropractic/code/watch app`)
- **Shreyaanp/dwell-site** — privacy-policy website (`/Users/ichiropractic/code/dwell-site`)

Both pushed. Commit history scrubbed of any AI co-author trailer (user requirement:
the user must be the sole contributor — never add `Co-Authored-By` to commits).

## Module / file map

### `app/` (phone) — `app/src/main/java/work/shreyaan/dwell/`
- `MainActivity.kt` — Compose UI: map, long-press to drop pin, radius slider,
  duration field, Arm/Disarm, Start/Cancel timer, live countdown, permission flow
- `Prefs.kt` — SharedPreferences store (lat, lon, radius, duration_min, armed, timer_end)
- `GeofenceManager.kt` — arm/disarm geofence via Play Services
- `GeofenceReceiver.kt` — on ENTER → start timer; on EXIT → keep/cancel notification
- `TimerController.kt` — schedules exact alarm via AlarmManager; pushes state to watch
- `TimerAlarmReceiver.kt` — fires the "Time's up" alarm when the timer elapses
- `NotificationActionReceiver.kt` — handles Keep / Cancel notification buttons
- `BootReceiver.kt` — re-arms geofence + alarm after reboot
- `Notifications.kt` — channels + running / done / exit-question / cancelled notifications
- `WearSync.kt` — pushes timer state to the watch over the Wear Data Layer
- `PhoneDataService.kt` — receives the Cancel message sent from the watch

### `wear/` (watch) — `wear/src/main/java/work/shreyaan/dwell/`
- `MainActivity.kt` — Wear Compose: full-screen live countdown + Cancel chip
- `WatchDataService.kt` — receives timer state pushed from the phone

### Other
- `.github/workflows/build.yml` — builds both debug APKs on push/PR
- `.github/workflows/release.yml` — on `v*` tag: signed AABs → Google Play
- `README.md` — user-facing overview & build/run instructions

## Behavior verified

On the `Pixel8_API34` emulator (has Play services): pin drop, arm, geofence
ENTER auto-starts timer, EXIT shows keep/cancel question, alarm fires on time.
Also installed + launched on a real connected phone (model CPH2649) on 2026-06-12.
Geofence simulated with `adb emu geo fix <lon> <lat>`.

## Build & run

Default system JDK is 25 (too new for this toolchain) — **must use JDK 17**.
Android SDK lives at `/opt/homebrew/share/android-commandlinetools`.

```sh
cd "/Users/ichiropractic/code/watch app"
export JAVA_HOME=$(/usr/libexec/java_home -v 17)

# Debug build (both modules)
./gradlew assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk
# → wear/build/outputs/apk/debug/wear-debug.apk

# Install on connected phone
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Signed release bundles (CI does this; for local, set the keystore env vars)
export ANDROID_KEYSTORE_PATH=$HOME/.dwell-release/upload-keystore.jks
export ANDROID_KEYSTORE_PASSWORD=$(awk -F': ' '/^password/{print $2}' ~/.dwell-release/credentials.txt)
export ANDROID_KEY_ALIAS=upload
export ANDROID_KEY_PASSWORD=$ANDROID_KEYSTORE_PASSWORD
./gradlew :app:bundleRelease :wear:bundleRelease
```

Installing the **watch** app (Pixel Watch = no USB, use wireless adb):
enable Developer options + Wireless debugging on the watch, same Wi-Fi as the Mac,
then `adb connect <watch-ip:port>` and `adb install -r wear/build/outputs/apk/debug/wear-debug.apk`.

## Publishing pipeline — STATE

### Credentials — in `~/.dwell-release/` (⚠️ BACK THIS UP — not in git)
- `upload-keystore.jks` — the app signing/upload key (irreplaceable once published)
- `credentials.txt` — keystore password
- `play-service-account.json` — Play Developer API service-account key
- `gcp-project.txt` — `dwell-ci-14834`

### Google Cloud — DONE
- Project `dwell-ci-14834` (account shreyaan.work@gmail.com)
- Play Android Developer API enabled
- Service account `play-publisher@dwell-ci-14834.iam.gserviceaccount.com` created

### GitHub secrets on Shreyaanp/maps — DONE (all 5)
`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`,
`PLAY_SERVICE_ACCOUNT_JSON`

### Google Play Console — PARTIAL
- App "Dwell" created (Play app id 4974805763811621320) — DONE
- Service account invited with **release** permissions — DONE
- **PENDING:** "Set up your app" forms — privacy policy URL, app access (no login),
  ads (none), content rating questionnaire, target audience (18+), data safety
  (declare: precise location, app functionality, on-device, not shared)
- **PENDING:** store listing — icon 512×512, feature graphic 1024×500, ≥2 screenshots
- **PENDING:** the FIRST AAB upload must be done **manually** in the Console
  (Testing → Internal testing → Create release → upload
  `app/build/outputs/bundle/release/app-release.aab`). Accept Play App Signing.
  After this one manual upload, all future releases are automated by pushing a
  `v*` git tag.

### Pre-built artifacts ready to upload
- `app/build/outputs/bundle/release/app-release.aab` (~6.9 MB, signed)
- `wear/build/outputs/bundle/release/wear-release.aab` (~5.9 MB, signed)

## Privacy site (Shreyaanp/dwell-site)

Static HTML: `index.html` (landing), `privacy.html` (policy), `vercel.json` (clean URLs).
- **Host on Vercel:** import the repo, framework preset "Other", no build step, deploy.
- **Domain:** add `dwell.shreyaan.work` in Vercel → Settings → Domains.
- **DNS (GoDaddy):** the `dwell` CNAME was wrongly set to value `dwell.` — fix it to
  `cname.vercel-dns.com.` (matches the working `vois`/`www` records).
- **Privacy policy URL for Play:** `https://dwell.shreyaan.work/privacy`

## OPEN DECISION — Flutter rewrite (NOT STARTED)

User chose **"Hybrid: Flutter phone + native watch."** Flutter 3.38.9 is installed
at `~/flutter`. Nothing has been built yet. Intended approach:
- New Flutter project; **relocate the existing native Kotlin** (all receivers,
  services, `TimerController`, `Prefs`, `Notifications`, `WearSync`) into the Flutter
  project's `android/app/src/main/kotlin/...` — it's reused almost verbatim.
- Replace only the phone **UI** with Flutter/Dart + `flutter_map` (OSM tiles).
- Bridge Dart ↔ native with a `MethodChannel` (Dart calls arm/disarm/start/cancel;
  native reports timer state back for the countdown).
- Keep the **wear module native Kotlin** (Flutter's Wear OS support is weak) as a
  second Gradle module in the same project.
- Why hybrid: this app is ~80% OS integration (geofence, exact alarms, boot, Data
  Layer) that Flutter can't do in Dart anyway, so the native code stays; only the
  map screen becomes Flutter.

## Quick status board

| Item | State |
|---|---|
| Phone app (native) | ✅ built, runs on emulator + real phone |
| Watch app (native) | ✅ built (install via wireless adb to test) |
| GitHub repos | ✅ pushed, contributor-clean |
| CI build workflow | ✅ passing |
| Release workflow + secrets | ✅ wired |
| Google Cloud + service account | ✅ done |
| Play Console app + SA invite | ✅ done |
| Play "set up" forms + store listing | ⏳ pending (manual, browser) |
| First manual AAB upload | ⏳ pending |
| Privacy site hosting + DNS | ⏳ pending (Vercel import + 1 DNS fix) |
| Flutter hybrid rewrite | ⛔ chosen, not started |
