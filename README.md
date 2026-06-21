# Dwell

An Android phone app + Wear OS watch app that automatically starts a countdown
timer (default **4.5 hours**) when you arrive at a saved place, and alerts you
on the phone and watch when the time is up.

Modules:

- `app/` — the phone app: MapLibre map, saved places, geofences, confidence
  engine, timer, notifications.
- `wear/` — the Wear OS companion app: swipeable glance/focus/action screens,
  a Tile, local countdown continuity, notifications, and quick keep/cancel/extend
  actions synced from the phone over the Data Layer API.

## How it works

For a user-facing walkthrough, start with [APP_TUTORIAL.md](APP_TUTORIAL.md).

1. Open the app and choose a place by searching, using your current location,
   or long-pressing the map to drop a pin.
2. Complete the one-time setup for location, background location,
   notifications, physical activity, and battery reliability.
3. Adjust the **radius** slider, default **duration**, and arrival mode.
4. Tap **Save this place**, then **Monitor**.
5. Dwell registers a local inner arrival geofence plus a larger approach ring.
   The approach ring wakes a lightweight confidence check; precise location is
   requested only when needed to start or ask.
6. When confidence is high, the timer starts automatically. Medium confidence
   asks on the phone/watch. Low confidence keeps observing.
7. You get an ongoing notification with a live countdown; Wear OS receives the
   same absolute end time so it can keep counting if the phone is briefly away.
8. When the duration elapses, a loud alarm-style notification fires on the
   phone and buzzes the watch.
9. If you **leave the place early**, a notification asks whether to keep or
   cancel the timer — the Keep/Cancel buttons work from the watch too.
10. **Start now** runs the countdown manually without waiting for arrival
   (also handy for testing).

Timers and monitored places survive phone reboots when permissions remain
available.

## Map and location stack

- Rendering: MapLibre.
- Tiles/style: OpenFreeMap by default, with OSM attribution.
- Search: Nominatim-compatible search with cache and manual submit behavior.
- Arrival: Android geofencing + fused location + activity recognition.
- Privacy: arrival decisions happen on-device. Dwell stores saved places, timer
  state, account/session data when used, and local diagnostics; it does not store
  continuous location history.

## Pixel Watch

Two layers of watch support:

1. **Notification mirroring (no install needed):** Wear OS mirrors all phone
   notifications to the watch — the live countdown, the keep/cancel question,
   and the final alarm. Make sure notifications from Dwell are not muted in
   the Pixel Watch app's "Watch notifications" settings.
2. **The `wear/` watch app:** shows setup, monitoring, active timer,
   leave-early, and time-up states on compact swipeable screens. The active
   timer syncs an absolute end time, so the watch can keep counting down if the
   phone is briefly not nearby.
   Install it on the watch via `adb install` over the watch's Wireless debugging,
   or distribute both apps through Google Play (phone track + Wear OS track).

## CI/CD

GitHub Actions workflows in `.github/workflows/`:

- `build.yml` — builds both debug APKs on every push/PR and uploads them as
  artifacts.
- `release.yml` — on a `v*` tag, builds signed release bundles and publishes
  them to Google Play (internal track). Requires the secrets listed at the top
  of that file: an upload keystore (base64 + passwords) and a Play service
  account JSON.

## Build & install

Requires JDK 17 and the Android SDK (`local.properties` points at it).

```sh
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

To install over Wi-Fi: enable Wireless debugging on the phone, then
`adb pair <ip:port>` / `adb connect <ip:port>` and run the install command.

For phone + watch local testing:

```sh
./scripts/setup-local-testing.sh
```

For the phone-side on-device Home/Office/Gym logic gate:

```sh
./scripts/run-device-flow-tests.sh
```

Then follow [FIELD_TEST.md](FIELD_TEST.md). To capture a privacy-safe field
run while testing, use:

```sh
DURATION_SECONDS=900 ./scripts/capture-field-logs.sh
```

## Testing the geofence without driving anywhere

- Use **Start now** with a small duration (e.g. `0.05` hours = 3 min) to
  see the full notification → watch → alarm flow.
- Or run the app in an Android emulator and simulate GPS positions from the
  emulator's location controls to cross the geofence boundary.
- For release confidence, use a real phone/watch and complete `FIELD_TEST.md`.

## Known limitations

- Geofence entry can lag real arrival by ~1–5 minutes (Android batches
  location checks to save battery).
- If you deny "Allow all the time" location, arrival detection only works
  while the app is open.
- OEM battery modes can delay background detection; use unrestricted battery
  settings for the most reliable field test.
