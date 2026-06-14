# Dwell Field Test Checklist

Use this checklist on a real Android phone plus a paired Wear OS watch. The goal is
to prove the on-device engine, not the backend.

## Setup

1. Connect the phone and watch with ADB.
2. Run:

   ```sh
   ./scripts/setup-local-testing.sh
   ```

3. In a second terminal, start privacy-safe field logs:

   ```sh
   DURATION_SECONDS=900 ./scripts/capture-field-logs.sh
   ```

   Logs are written under `field-logs/`. They include Dwell diagnostic buckets,
   sync status, and runtime errors, but no exact coordinates.

4. On the phone, open Dwell.
5. Continue locally or sign in.
6. Open Settings.
7. Clear Diagnostics.
8. Confirm these are allowed:
   - Location
   - Background location
   - Notifications
   - Motion / Physical activity
9. Confirm battery settings are not blocking background work.
10. Open the watch app once and allow notifications.

## Case 1: Current Location

1. On the phone map, tap current location.
2. Expected:
   - Map moves to your real current area.
   - It must not jump to the Android emulator default near North Shoreline.
   - A user position marker appears.

## Case 2: Already Inside

1. Stand inside the place you want to monitor.
2. Save the place with a 100-150 m radius.
3. Tap Monitor.
4. Expected:
   - Dwell checks a fresh location.
   - High confidence starts the timer automatically.
   - Medium confidence asks to start.
   - Watch shows the same timer or prompt.
5. Diagnostics should include `armed-inside`.

## Case 3: Screen-Off Arrival

1. Save and monitor a place while outside the radius.
2. Lock the phone.
3. Walk into the place and stay there for several minutes.
4. Expected:
   - Outer approach ring may wake Dwell first.
   - Inner geofence or confidence probe starts/asks for the timer.
   - Watch updates without opening the phone app.
5. Diagnostics should include one or more of:
   - `approach - probe`
   - `approach - precise-requested`
   - `geofence`
   - `approach-precise`

## Case 4: Pass-Through Guard

1. Monitor a place you can pass through without stopping.
2. Drive, bike, or walk through without dwelling.
3. Expected:
   - Dwell should not auto-start from one fast pass-through.
   - It may ask if evidence is medium confidence.
   - Fresh vehicle/bike/run motion should wait instead of auto-starting.

## Case 5: Multiple Places

1. Save at least two places.
2. Enable Monitor on both.
3. Confirm Settings or Diagnostics shows monitoring live.
4. Visit one place.
5. Expected:
   - Timer/prompt uses the correct place label.
   - Watch uses the same place label.
   - Other saved places stay available.

## Case 6: Leave Early

1. Start a timer at a monitored place.
2. Leave the radius before time is up.
3. Expected:
   - Phone asks whether to keep or cancel.
   - Watch asks the same question.
   - Keep is available and should not cancel the timer.

## Case 7: Time Up And Disconnected Watch

1. Start a short timer.
2. Move the phone away or disable connection briefly.
3. Expected:
   - Watch keeps counting down from the absolute end time.
   - If stale, watch says `Phone not nearby, still counting`.
   - When synced again, watch receives time-up/done/extend state.

## Evidence To Capture

For each run, record:

- phone model and Android version
- watch model and Wear OS version
- battery mode
- whether the phone screen was on or off
- whether Dwell auto-started, asked, waited, or missed
- last 3-5 Diagnostics rows
- the generated `field-logs/` folder for the run
- whether the watch app, Tile, and notification matched the phone

Do not record exact coordinates.
