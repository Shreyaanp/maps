# Dwell Field Test Checklist

Use this checklist on a real Android phone plus a paired Wear OS watch. The goal is
to prove the on-device engine, not the backend.

For the full location setup permutation matrix, use `LOCATION_FLOW_TEST_MATRIX.md`.

## Setup

1. Connect the phone and watch with ADB.
2. Run:

   ```sh
   ./scripts/setup-local-testing.sh
   ```

3. Run the on-device Home/Office/Gym logic gate:

   ```sh
   ./scripts/run-device-flow-tests.sh
   ```

   This installs and runs the phone instrumentation test for duplicate places,
   per-place settings, runtime ownership, overlap handling, stale watch commands,
   and prompted-place timer duration. It is not a replacement for the walking
   field cases below.

4. In a second terminal, start privacy-safe field logs:

   ```sh
   DURATION_SECONDS=900 ./scripts/capture-field-logs.sh
   ```

   Logs are written under `field-logs/`. They include Dwell diagnostic buckets,
   sync status, and runtime errors, but no exact coordinates.

5. On the phone, open Dwell.
6. Continue locally or sign in.
7. On a fresh install, confirm Dwell does not show Android permission prompts before you tap current location, Monitor, Start now, or Finish setup.
8. With a draft or paused place selected, confirm the home dock says ready/viewing/editing instead of setup needed until that place is monitored.
9. Open Settings.
10. Clear Diagnostics.
11. Confirm these are allowed:
   - Location
   - Background location
   - Notifications
   - Physical activity
12. Confirm battery settings are not blocking background work.
13. Open the watch app once and confirm it does not request notification permission immediately.
14. Tap `Allow watch alerts` on the watch, then grant notification permission.
15. Before the first phone sync, the watch and Tile lead with sync/open-phone copy rather than claiming monitoring is ready.
16. After phone sync, before any place is saved, the watch and Tile say no place is selected, not that monitoring setup is broken.
17. If a saved place is restored from the backend on a fresh install, the map opens it as Viewing, not Add place.

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
   - Phone notifications, watch notifications, watch app, and Tile use the same place label.
   - The watch active-timer glance leads with the timer place when known, not only generic `Timer active` copy.
   - When one place is paused on the phone but another is monitoring, the watch app and Tile name the monitored place or show the live-place count, not the paused/viewed place.
   - When multiple places are live, the watch app and Tile lead with the live-place count instead of a single viewed/selected place.
   - Other saved places stay available.
   - Places shows a Monitoring health card with `Healthy`, `Partial`, `Needs setup`, `Timer risk`, or `Battery risk`.
   - `Timer risk` uses an `Allow alarms` action that opens Android's exact-alarm permission screen.
   - Places shows distinct row states: `Viewing`, `Editing`, `Timer here`, `Monitoring live`, `Paused`, or `Needs setup`.
   - Turning Monitor on from Places asks for confirmation and explains the place, radius, timer, and arrival mode.
   - Pausing monitoring from Places is one tap and confirms with paused feedback.
   - Pausing a monitored place clears any arrival, switch, or leave-place prompt for that place on phone and watch.
   - If you turn Monitor on and immediately pause it before setup finishes, no already-inside timer or prompt starts for the paused place.
   - If Android delivers a delayed geofence event after pausing a place, Dwell ignores that paused place and does not start, switch, or leave-prompt for it.
   - View map centers the place without entering move/edit mode.
   - Reopening Dwell with an existing saved place starts in `Viewing`, not `Editing`.
   - On the map, viewing a place does not show Add place as the selected mode; only Add place or Move `<name>` mode should be selected when explicitly chosen.
   - Edit settings opens the place controls and marks that row as `Editing`.
   - Name, radius, timer length, and arrival mode edits apply only to the place marked `Editing`, not a previously active or viewed place.
   - Renaming a place that owns the running timer, arrival prompt, switch prompt, leave prompt, or time-up prompt refreshes phone notifications, watch app, Tile, and watch notifications to the new label.
   - Monitor from the map acts on the place currently viewed or edited, not a previously active place.
   - Canceling a running timer from the app asks for confirmation and names the timer place when known.
   - Removing a place asks for confirmation and explains whether monitoring stops or the current timer is canceled.
   - Removing the place currently being edited does not mark the next place as `Editing`; the next place opens as `Viewing`.
   - Removing a different saved place does not move the map away from the place currently viewed or edited, even if the removed place was previously active.
   - Removing the last saved place keeps you in Places and shows the empty `Add place` action instead of jumping to the main map.
   - After removal, Dwell shows Undo without leaving Places; tapping it restores the place and its monitoring setting.
   - View map is read-only: expanding the dock shows `Viewing only`, not radius/timer/mode controls.
   - While View map is selected, tapping the selected saved-place chip does not switch into move/edit mode; use Edit settings for changes.
   - Tapping Edit from that viewing state opens the editable controls for the same place, not a previously active place.
   - If search returns no results, the dropdown offers Use current location with add-place or move-place wording instead of dead-ending.
   - The floating current-location button creates/updates the preview in Add place or Edit settings, but only centers the map while viewing a saved place.
   - If a timer is running for another place while you view the map, the dock names the timer place and shows timer actions, not the viewed place's settings.
   - Starting or confirming a timer for another place does not change the place currently viewed or edited on the map; Places marks only the timer row as `Timer here`.
   - The `Timer here` row offers `Cancel timer` from Places and asks for confirmation before stopping it.
   - Other saved rows name the active timer place and explain that it must be canceled before `Start now` can run there.
   - Background monitoring refreshes, permission recovery, and app-open self-heal do not change the place currently viewed or edited on the map.
   - If the monitored-place limit is reached, enabling another place explains that another monitored place must be paused first.

## Case 6: Unsaved Map Selection

1. Tap Add place, choose a search result, current location, or long-press the map.
2. Without tapping Save this place, leave the map or open Places.
3. Repeat by choosing Edit for an existing place, then pick a new map/search location.
4. Expected:
   - The dock says the location is unsaved and not saved yet.
   - The dock shows a `Place name` field before radius, timer, and arrival mode.
   - The primary action is Save this place for a new place or Move `<name>` for an existing place.
   - While Add place is selected, the saved-place mode chip still names the saved place to move, e.g. `Move Home`, not the unsaved pin.
   - Places should not gain a new row until Save this place is tapped.
   - Opening Places before saving shows an unsaved banner with Review on map and Cancel preview or Cancel move actions.
   - Tapping Settings or Insights from the map rail while an unsaved place is pending stays on the map and asks you to save, move, or cancel the preview first.
   - With the dock collapsed, tapping the floating current-location button updates the unsaved preview instead of merely centering the map.
   - Android Back closes search first, collapses the dock second, and when an unsaved place is pending it opens the dock to show Save this place/Cancel preview or Move `<name>`/Cancel move instead of exiting immediately.
   - An existing place should not move until Move `<name>` is tapped.
   - Moving `Home` with search or current location still leaves the place named `Home` unless the `Place name` field is changed.
   - Changing radius, timer length, or arrival mode while unsaved is saved only after Save this place or Move `<name>`.
   - Changing radius or timer while in Add place/default mode does not mutate an existing saved place; only the place marked `Editing` can receive those edits.
   - Tapping Add place, Move `<name>`, View map, or Edit settings while an unsaved place is pending does not discard it; Dwell asks to save, move, or cancel the preview first.
   - Saved rows explain that a new preview must be saved or canceled before opening or changing saved rows.
   - Saved rows explain that a move preview must be moved or canceled before opening or changing saved rows.
   - While an unsaved place is pending, Places does not allow Monitor, Pause monitoring, or Remove on saved rows until the preview is saved, moved, or canceled.
   - While an unsaved place is pending, Places blocks Monitoring health recovery actions such as Finish setup, Allow alarms, or Review battery and explains whether to save/cancel or move/cancel the preview first.
   - While an unsaved place is pending, row-level Fix setup is disabled until the preview is saved, moved, or canceled.
   - After Save this place or Move `<name>`, the saved place opens as `Viewing`/ready; radius, timer, name, and arrival mode controls stay hidden until Edit settings is tapped.
   - Cancel preview clears the unsaved place; Cancel move returns to the previous saved place.
   - Monitor and Start now do not run against an unsaved place; Dwell asks you to save, move, or cancel the preview first.

## Case 7: Duplicate And Nearby Places

1. Save a place named `Office`.
2. Try saving `Office` again within roughly 25 m.
3. Save a different nearby place, such as `Gym`, with an overlapping radius.
4. Save another `Office` far away.
5. Expected:
   - The nearby duplicate `Office` is selected instead of added twice.
   - `Gym` remains separate even if the radius overlaps.
   - The far `Office` remains separate.
   - If a duplicate merge removes one saved-place ID, any timer, prompt, watch state, live monitoring reference, arrival debounce, leave-early keep choice, or switch-place keep-current choice moves to that duplicate group's surviving place instead of becoming orphaned.
   - If two duplicate groups are cleaned up at the same time, each removed ID maps to its own surviving place, not to whichever place was edited most recently.
   - The retained auto-start or confirm-first policy is visible before monitoring.

## Case 8: Delete Place Cleanup

1. Create two monitored places.
2. Trigger an arrival, switch, or leave prompt for place A.
3. Delete place A from Places.
4. Repeat while place B has its own timer or prompt.
5. Expected:
   - Phone, watch, Tile, and notifications stop naming deleted place A.
   - Place B's timer or prompt is not cleared by deleting A.
   - Any kept leave/switch suppression for A is forgotten.
   - If A has a time-up prompt after its timer ends, deleting A clears that time-up prompt and done notification.
   - Undo restores A as a saved place, but does not resurrect an old prompt.

## Case 9: Overlapping Places

1. Save and monitor two nearby places with overlapping radii.
2. Enter the overlap from a point closer to place A.
3. Repeat after reversing the order in which the places were created or enabled.
4. Expected:
   - Dwell chooses the same nearest place both times.
   - If only the outer approach rings trigger first, the fresh location check still chooses the nearest triggered place rather than the first geofence event.
   - If a timer is already running at place A and a refresh enter event includes both A and nearby B, Dwell does not ask to switch away from A.
   - If Android omits triggered geofence IDs and Dwell has to infer from the event location, it still treats every containing overlapping place as triggered and does not ask to switch away from A while you are still inside A.
   - Timer/prompt label, duration, and auto-start policy belong to that chosen place.
   - Watch app, Tile, and notification show the same place.

## Case 10: Already Inside Enable

1. Stand inside a saved place that is currently paused.
2. Enable Monitor from the Places screen.
3. Repeat with a confirm-first place.
4. Expected:
   - Dwell produces exactly one timer or one start prompt.
   - Phone and watch labels match the enabled place.
   - Diagnostics include `armed-inside` and do not show duplicate start prompts.
   - If the geofence initial trigger wins the race, the phone says the timer or prompt is already active instead of saying it is merely monitoring.
   - Legacy or unscoped follow-up probes do not borrow the currently viewed/active place; they are ignored unless they name a saved monitored place.
   - Pausing and later resuming a place does not reuse stale inside/near evidence from before the pause.

## Case 11: Switch Place Conflict

1. Start a timer at monitored place A.
2. Enter monitored place B before A's timer ends.
3. Expected:
   - Phone asks whether to switch to B or keep A.
   - Phone notification, watch app, Tile, and watch notification all name B as the switch target, e.g. `Switch to B?`, not as an already-running timer.
   - Keep leaves A running and clears the prompt.
   - Keep or Not now suppresses the same switch-to-B prompt for the rest of A's timer.
   - Extending A's timer keeps that switch-to-B suppression until the extended timer ends.
   - Switch cancels A and starts B.
   - Stale actions from the old prompt are ignored.
   - If a leave, arrival, or done notification was still visible, the switch prompt replaces it instead of leaving two competing prompts.
   - If the phone dock is still showing an older prompt when a newer prompt/timer replaces it, tapping the old dock action refreshes instead of starting, switching, keeping, or canceling the wrong place.
   - Legacy or blank-place timer starts fall back to the prompt or active saved place instead of creating an unscoped timer.

## Case 12: Leave Early

1. Start a timer at a monitored place.
2. Leave the radius before time is up.
3. Expected:
   - Phone asks whether to keep or cancel.
   - Watch asks the same question.
   - Phone and watch prompts name the timer place.
   - Keep is available and should not cancel the timer.
   - After Keep from either phone or watch, GPS jitter should not show the same leave prompt again for that timer.
   - If Android still exposes the old leave notification after Keep, tapping its Cancel timer action does not cancel the kept timer.
   - If a leave-place location check started for an older timer, it does not show a leave prompt after a newer place timer starts.
   - The leave prompt replaces any stale arrival, switch, or done notification instead of leaving two competing prompts.

## Case 13: Permission Recovery

1. Monitor at least one place.
2. Revoke background location, notifications, or physical activity permission.
3. Open Dwell and the watch app.
4. Grant the missing permission again.
5. Expected:
   - Phone and watch show `Needs setup` while permission is missing.
   - Watch and Tile stop showing live/ready copy after permission loss, even if the place was live before permission was revoked.
   - Places shows `Monitoring needs setup` with a `Finish setup` action.
   - Returning to Dwell re-registers geofences.
   - Setup notification clears after recovery.
   - Watch `needs_setup` clears and live monitoring count matches the phone.

## Case 14: Reboot And App-Open Self-Heal

1. Monitor a place while outside its radius.
2. Reboot the phone.
3. Open Dwell after reboot.
4. Repeat while already inside the place.
5. Expected:
   - Monitoring refreshes after boot or app open.
   - Diagnostics show the refresh source.
   - Already-inside behavior still produces only one timer or prompt.
   - If a leave or switch prompt was live before reboot, phone notification and watch restore that same prompt instead of showing only a generic running timer.
   - If the stored prompt is stale or no longer names the timer/switch place correctly, reboot clears it and shows the running timer.
   - Watch state converges after opening the watch app.

## Case 15: Time Up And Disconnected Watch

1. Start a short timer.
2. Move the phone away or disable connection briefly.
3. Expected:
   - Watch keeps counting down from the absolute end time.
   - If stale, watch says `Phone not nearby, still counting`.
   - If no timer is active and phone state is stale, the Tile leads with phone-away/open-phone copy instead of stale live/ready copy or stale prompt actions.
   - When synced again, watch receives time-up/done/extend state.
   - The phone done notification names the timer place when known, e.g. `Time's up at Office`.
   - The watch time-up notification also names the timer place when known.
   - The watch app time-up screen names the timer place in its main title when known.
   - The phone home dock also shows `Time's up` for the timer place, with Extend and Done actions.
   - Tapping Extend from either a local expired watch timer or a phone-fired time-up prompt starts a same-place extension, not a no-op.
   - Tapping Extend or Done from a stale phone dock time-up prompt refreshes instead of mutating a newer timer/place.
   - A late alarm from an older timer does not mark a newer running timer as time-up.
   - Out-of-order older phone sync messages or cached watch app launches do not resurrect an old timer, prompt, or place on the watch.
   - Starting or extending a timer clears stale done, leave, arrival, and switch notifications.

## Case 16: Stale Watch Command Guard

1. Start a timer at place A and let the watch receive it.
2. Disconnect the watch.
3. Cancel or finish the timer on the phone, then start a new timer at place B.
4. Reconnect the watch and tap Cancel, Extend, or Done from the stale A screen before it refreshes.
5. Repeat with a stale watch prompt: show `Switch to B?`, `Leaving A?`, or `Time's up`, replace the prompt/timer on the phone, then tap the old watch action before the watch refreshes.
6. Expected:
   - The phone does not cancel or extend the B timer.
   - The watch is pushed the current B timer state.
   - Diagnostics or logs show no stale command mutating the active timer.
   - If the timer has no saved place, watch Cancel/Extend/Done still works by timer token and does not borrow the currently displayed place.
   - Extending a no-place timer from phone or watch keeps it as a no-place timer; it does not relabel itself as the currently viewed or active saved place.
   - Stale watch Start, Switch, Not now, Keep, or Done prompt actions are ignored unless their prompt type, prompt place, and timer identity still match the phone.
   - After the prompt is stale for 2+ minutes, the full watch app and Tile both show phone-away/open-phone copy instead of actionable prompt buttons.

## Case 17: Stale Phone Notification Guard

1. Start a timer at place A and leave its notification visible.
2. Finish or cancel that timer, then start a new timer at place A or place B.
3. Tap a stale Cancel, Keep, or Switch action from the old phone notification if Android still exposes it.
4. Expected:
   - The stale action does not cancel, extend, switch, or clear prompts for the newer timer.
   - A stale Start, Not now, Keep, or Keep current action for the same place is ignored if a newer prompt replaced it.
   - A stale leave-prompt Cancel timer action is ignored after the prompt was kept or replaced, even if the underlying timer is still the same.
   - Legacy prompt-only Keep or Keep current actions are ignored unless they also match the running timer identity.
   - The same stale-action guard applies to phone dock prompt buttons, not only notification buttons.
   - If a Cancel timer confirmation was opened for an older timer, confirming it after a newer timer starts refreshes the dock instead of canceling the newer timer.
   - Legacy or unscoped notification actions are ignored instead of borrowing the currently visible prompt place.
   - Running, done, and insight history for a no-place timer stay generic even if another saved place is active or viewed.
   - Watch state for a no-place timer also stays generic and uses default timer/radius metadata rather than another saved place's settings.
   - New timer actions still work from the current notification.
   - Time-up clears any old leave, arrival, or switch prompts.

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
