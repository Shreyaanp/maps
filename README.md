# Dwell

An Android phone app + Wear OS watch app that automatically starts a countdown
timer (default **4.5 hours**) when you arrive at a place you've pinned on a map,
and alerts you — including on your **Pixel Watch** — when the time is up.

Modules:

- `app/` — the phone app: map, geofence, timer, notifications.
- `wear/` — the Wear OS companion app: live countdown screen on the watch with
  a Cancel button, synced from the phone over the Data Layer API.

## How it works

1. Open the app and **long-press the map** to drop a pin on your place
   (parking spot, office, etc.).
2. Adjust the **radius** slider (how close counts as "arrived") and the
   **duration** in hours (default 4.5).
3. Tap **Arm geofence**. Grant the permissions it asks for — location must be
   set to **"Allow all the time"** so arrival detection works with the app closed.
4. When you physically enter the zone, the timer starts automatically. You get
   an ongoing notification with a **live countdown** — Wear OS mirrors it to
   your Pixel Watch, ticking included.
5. When the duration elapses, a loud alarm-style notification fires on the
   phone and buzzes the watch.
6. If you **leave the zone early**, a notification asks whether to keep or
   cancel the timer — the Keep/Cancel buttons work from the watch too.
7. **Start timer now** runs the countdown manually without waiting for arrival
   (also handy for testing).

Timers and the armed geofence survive phone reboots.

## Pixel Watch

Two layers of watch support:

1. **Notification mirroring (no install needed):** Wear OS mirrors all phone
   notifications to the watch — the live countdown, the keep/cancel question,
   and the final alarm. Make sure notifications from Dwell are not muted in
   the Pixel Watch app's "Watch notifications" settings.
2. **The `wear/` watch app:** shows the running countdown full-screen on the
   watch with a Cancel button that also cancels on the phone. Install it on
   the watch via `adb install` over the watch's Wireless debugging, or
   distribute both apps through Google Play (phone track + Wear OS track).

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

## Testing the geofence without driving anywhere

- Use **Start timer now** with a small duration (e.g. `0.05` hours = 3 min) to
  see the full notification → watch → alarm flow.
- Or run the app in an Android emulator and simulate GPS positions from the
  emulator's location controls to cross the geofence boundary.

## Known limitations

- Geofence entry can lag real arrival by ~1–5 minutes (Android batches
  location checks to save battery).
- If you deny "Allow all the time" location, arrival detection only works
  while the app is open.
- One place at a time by design; re-pin and re-arm to change it.
