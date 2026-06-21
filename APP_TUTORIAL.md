# Dwell App Tutorial

Dwell watches real places and starts the right timer when you arrive.

Save places like Home, Office, Gym, Library, or a client site. Each saved place
keeps its own radius, timer duration, arrival behavior, and monitoring state.
When you enter a monitored place, Dwell either starts the timer automatically
or asks first, depending on that place's settings.

Use this guide as the user story for the app: from fresh install, to adding
multiple places, to trusting the phone and watch in daily use.

## User Story

A person installs Dwell because they want timers to follow real life places
instead of starting every timer by hand.

They set up the app once, save Home, Office, and Gym as separate places, then
turn on Monitor for each place they want watched live. Home can have a short
timer and a 50 m radius. Office can have a work timer. Gym can ask before
starting if it sits near other places.

After that, daily use should be simple:

1. Open **Places** to see every saved place.
2. Turn **Monitor** on or off per row.
3. Use **Start now** when testing or starting manually.
4. Use **Edit settings** only when one saved row should change.
5. Close the app and let Dwell detect arrivals in the background.

When they arrive, Dwell should use the place they actually entered. The phone
notification, watch app, and watch Tile should all name that same place. If
setup breaks, Dwell should tell them exactly which setup step to finish.

## Where The Tutorial Lives

Dwell teaches the flow in three places:

1. **First run** shows a short guided setup after the user grants the core
   permissions.
2. **Settings > How to use Dwell** opens the in-app tutorial any time.
3. This file is the full product tutorial and QA contract for the phone and
   watch flows.

All three should tell the same story: set up once, add a place, save the
preview, turn on Monitor, then repeat for every place the user wants live.

## The Short Version

1. Open Dwell.
2. Tap **Finish setup** and complete Setup checks once.
3. Tap **Add place**.
4. Pick the spot with search, current location, or a long-press on the map.
5. Review the unsaved preview in the bottom dock.
6. Name the place and choose radius, timer, and arrival mode.
7. Tap **Save this place**.
8. Turn on **Monitor** for that saved row.
9. Repeat for Home, Office, Gym, or any other place you want live.

That is the core loop: pick a spot, review it, save it, monitor it, repeat.

## In-App Tutorial Script

The in-app tutorial should teach this order:

1. **Set up once** so background arrivals can work after the app is closed.
2. **Add a place** with search, current location, or one long-press on the map.
3. **Save the preview** after checking the name, radius, timer, and arrival mode.
4. **Monitor that row** so Dwell can watch it live.
5. **Repeat for more places** because every saved place keeps separate settings.
6. **Use Places daily** to monitor, pause, start now, cancel timers, edit, view, or remove rows.
7. **Use Finish setup** when a permission, alarm, battery, or geofence issue
   blocks monitoring.

## First Session Walkthrough

Use this as the exact first-run tutorial.

1. Open Dwell and finish the intro.
2. Tap **Finish setup**.
3. Allow location, notifications, physical activity, background location,
   exact alarms, and unrestricted battery when Android offers them.
4. Tap **Add first place** at the end of onboarding, or **Add place** from
   Places later.
5. Pick Home with current location, search, or a long-press on the map.
6. Confirm the bottom dock says this is an unsaved preview.
7. Name it `Home`.
8. Set radius to **50 m** if you want precise testing.
9. Pick a timer duration and choose **Confirm first** for the first field run.
10. Tap **Save this place**.
11. Open **Places** and turn on **Monitor** for Home.
12. Add Office and Gym the same way, with different radius and timer settings.
13. Turn on **Monitor** for each row you want live.
14. Close the app and arrive at a monitored place.
15. Respond to the arrival prompt or let auto-start begin the correct timer.

The expected result: Home, Office, and Gym each stay separate. The active
prompt, notification, watch app, and watch Tile should all name the same place.

## The Mental Model

Dwell has three rules.

1. A picked spot is only a preview until you tap **Save this place**.
2. Every saved place owns its own settings.
3. Multiple saved places can be monitored live at the same time.

This means Home, Office, and Gym are separate compartments. Editing Gym should
not change Office. Starting an Office timer should not depend on which place is
currently open on the map.

## Main Screens

| Screen | Use it for | What should happen |
| --- | --- | --- |
| Home | Check whether Dwell is ready | Shows setup, live monitoring, active timer, and quick recovery |
| Map | Pick or inspect a location | Search, current location, and long-press create previews when adding |
| Places | Daily control center | Monitor, pause, start now, cancel timers, edit, view, or remove one saved row |
| Tutorial | Learn the flow | Explains setup, adding places, multiple places, and recovery |
| Settings | App-level setup | Account, setup checks, tutorial, battery, and reliability actions |
| Insights | Review usage | Shows saved-place and timer history signals when available |

For daily use, start in **Places**. For setup or recovery, start with
**Finish setup**. For adding a new location, use **Add place**.

## First-Time Setup

Setup exists so Dwell can keep working after the app is closed.

When Android asks, allow:

- **Location** so Dwell can find the phone.
- **Background location** so arrivals work after the app is closed.
- **Notifications** so Dwell can ask, count down, and alert when time is up.
- **Physical activity** so Dwell can avoid starting timers when you only pass by.
- **Exact alarms**, if Android asks, so the time-up alert fires on time.
- **Unrestricted battery**, when available, for better background reliability.

Dwell should not keep asking for the same permission during normal use. If a
permission or battery setting breaks later, tap **Finish setup** to open Setup
checks from Home, Places, Tutorial, or Settings.

## Add Your First Place

Use this flow when setting up Home or any first place.

1. Tap **Add place**.
2. Choose the spot:
   - Search for an address, business, or landmark.
   - Use current location if you are standing there.
   - Long-press the map for one exact point.
3. Check that the bottom dock says the place is unsaved.
4. Enter a name, for example `Home`.
5. Choose the radius.
6. Choose the timer duration.
7. Choose the arrival mode.
8. Tap **Save this place**.
9. Turn on **Monitor** for that saved place.

Until you tap **Save this place**, the spot is only a preview. It should not
appear as a permanent row in Places yet.

## Pick The Spot

Dwell gives you three ways to choose a location.

| Method | Use it when | Result |
| --- | --- | --- |
| Search | You know the address or place name | Creates an unsaved preview |
| Current location | You are physically at the place | Creates or updates the relevant preview |
| Long-press map | You want one exact point | Drops a new unsaved preview at that point |

Current location is mode-aware:

- In **Add place**, it creates or updates the new unsaved place.
- In **Edit settings**, it creates or updates the move preview for that saved place.
- In **View map**, it only centers the map on the phone.

Long-press always creates a new unsaved place. It does not silently move an
existing saved place.

## Choose Place Settings

Each saved place has its own settings.

### Radius

Radius is the area around the saved place that Dwell watches.

- Use **50 m** for precise places in dense areas.
- Use **100-150 m** for larger buildings, campuses, or weaker GPS.
- Use smaller radii or **Confirm first** when two places are close together.

Changing Gym radius should not change Home or Office.

### Timer Duration

Pick how long you want to stay at that place.

Examples:

- Home: 45 min.
- Office: 4.5 h.
- Gym: 1 h.

When you arrive at Office, Dwell uses Office's duration. When you arrive at
Gym, Dwell uses Gym's duration.

### Arrival Mode

Each place decides what happens when you arrive.

- **Auto-start** starts the timer when confidence is high.
- **Confirm first** asks before starting.

Use Confirm first for overlapping places, places near your commute, or places
where you often pass nearby without stopping.

## Add Home, Office, And Gym

This is the main product story.

1. Save **Home** with a 50 m radius, a short timer, and Confirm first.
2. Save **Office** with its own radius and work timer.
3. Save **Gym** with a separate radius and workout timer.
4. Open **Places**.
5. Turn on **Monitor** for all three rows.
6. Arrive at Gym.
7. Dwell starts or asks about the Gym timer.
8. Later, arrive at Office.
9. Dwell uses Office settings, not Gym settings.
10. The phone notification, watch app, and watch Tile name the same active place.

The promise is simple: the entered place wins, and each place keeps its own
settings.

## Places Is The Daily Dashboard

After setup, most daily use happens in **Places**.

Use Places to:

- See every saved place.
- Add another place.
- Turn **Monitor** on for one row, then confirm its radius, timer, and arrival mode.
- Pause monitoring for one row.
- Use **Start now** for a manual timer.
- Use **Cancel timer** on the row marked **Timer here**.
- Use **View map** to inspect a saved place.
- Use **Edit settings** to change one saved place.
- Remove a place without getting kicked back to the main map, then use **Undo**
  if it was a mistake.
- Tap **Finish setup** if monitoring needs help, or **Fix setup** on a row
  that needs recovery.

If you remove the last saved place, Places should stay open, show **Add
place**, and still offer **Undo**.

## Daily Use Stories

### I Want To Add Another Place

1. Open **Places**.
2. Tap **Add place**.
3. Pick the exact spot.
4. Save it with its own name, radius, timer, and arrival mode.
5. Return to **Places**.
6. Turn on **Monitor** only for the rows you want live.

Adding Office should not duplicate Home. Adding Gym should not overwrite
Office. A long-press should create one new unsaved preview at the pressed point.

### I Want To Edit One Saved Place

1. Open **Places**.
2. Tap **Edit settings** on exactly one row.
3. Change that row's name, radius, timer, arrival mode, or map position.
4. Save the edit.
5. Check the other rows did not change.

The row marked **Editing** is the only place that should mutate.

### I Want To Start A Timer Manually

1. Open **Places**.
2. Tap **Start now** on the saved row.
3. Confirm the notification and watch show that row's place name.
4. Stop, extend, keep, or cancel from the phone or watch when prompted.

Use **Start now** for quick testing without physically crossing a geofence.
If another row already has **Timer here**, Dwell names that active place. Cancel
that timer before starting a different saved row.

### I Want To Pause Or Remove A Place

1. Open **Places**.
2. Tap **Pause monitoring** to keep the saved place but stop watching it.
3. Tap **Monitor** again when you want it live.
4. Remove a place only when you no longer want the saved row.

After removing a place, Dwell should keep you in **Places** so you can continue
working through the list.

## View, Edit, And Add Are Different

This rule prevents accidental changes.

| Mode | What it means | What can change |
| --- | --- | --- |
| Add place | You are creating a new saved place | Only the unsaved preview |
| View map | You are inspecting a saved place | Nothing about the saved place |
| Edit settings | You are changing one saved place | Only the row marked Editing |

If you want to move a saved place:

1. Open **Places**.
2. Tap **Edit settings** on that place.
3. Search for the new spot or use current location.
4. Review the move preview.
5. Tap **Move `<place name>`**.

Canceling a move leaves the original saved place unchanged.
Use **Cancel preview** for a new unsaved place and **Cancel move** when moving
an existing saved place.

## Place States

Dwell uses place states to show what is happening.

| State | Meaning |
| --- | --- |
| Unsaved | Picked on the map, not saved yet |
| Viewing | Saved place open read-only |
| Editing | This saved row can change |
| Monitoring live | Dwell is watching this place |
| Paused | Saved, but not watched |
| Needs setup | Permission, alarm, battery, or geofence recovery is needed |
| Timer here | The running timer belongs to this place |

If the app shows an unsaved preview, save it, move it, or cancel it before
using runtime actions like Monitor, Pause monitoring, Remove, or Start now.

## What Happens When You Arrive

When Dwell detects that you entered a monitored place:

- Auto-start places can start the timer automatically.
- Confirm-first places ask before starting.
- If another timer is already running, Dwell asks whether to switch.
- The prompt names the place you entered.
- The phone notification, watch app, and Tile should agree on the place name.

If you are viewing Home while an Office timer starts, the map can keep showing
Home, but the active timer must belong to Office.

## Leaving Early

If you leave before the timer ends, Dwell may ask whether to keep or cancel the
timer.

- **Keep timer** is for GPS jumps, short walks, or stepping outside briefly.
- **Cancel timer** stops the active place's timer.

The prompt should name the place that owns the running timer.

## Use The Watch

The watch mirrors important phone state.

It can show:

- Needs setup.
- Monitoring live or paused.
- Number of live monitored places.
- Arrival prompt.
- Switch-place prompt.
- Running timer.
- Leave-early prompt.
- Time-up alert.

For best results, keep Dwell notifications enabled in the watch companion app
and open the watch app once after installing it.

## Common Problems

### Dwell Did Not Start When I Arrived

Check:

- The place is saved.
- Monitor is on for that place.
- Background location is allowed.
- Notifications are allowed.
- Exact alarms are allowed if Android requires them.
- Battery use is unrestricted.
- The radius is not too small for the area.

### The Wrong Place Started

Check:

- Nearby radii may overlap.
- The prompt should name the entered place.
- Use Confirm first for close or overlapping places.
- Reduce radius for dense places.

### I Changed The Wrong Settings

Open Places and check the row label.

- **Viewing** means read-only map inspection.
- **Editing** means this row can change.
- **Timer here** means the running timer belongs to this row.

Only Editing should receive settings changes.

### Search Suggestions Will Not Close

Choose a result, clear the text, tap close, tap outside the panel, or press
Back.

### I See An Unsaved Place And Cannot Monitor

Save this place, move the edited place, or cancel the preview. Dwell blocks
runtime actions until the preview is resolved.

### The Watch Looks Stale

Open the phone app once and confirm the watch is connected. The watch receives
state from the phone, and the running timer uses an absolute end time so it can
continue briefly even if the phone is away.

## The Product Promise

A polished Dwell flow must satisfy these promises:

- Multiple saved places can be monitored live.
- Each saved place has isolated name, radius, timer, and arrival mode settings.
- Search, current location, and long-press create previews before saving.
- Long-press creates a new place, not a silent move.
- View map is read-only.
- Edit settings mutates exactly one saved place.
- Runtime prompts and notifications name the place that owns the timer.
- Removing a place keeps the user in Places.
- Missing permissions, alarms, and battery restrictions show recovery actions.
- The phone and watch tell the same story.

## Manual Tutorial Test

Run this once before calling the main flow polished.

1. Fresh install the phone app.
2. Tap Finish setup and complete setup once.
3. Save Home using current location.
4. Save Office using search.
5. Save Gym using a long-press.
6. Give each place a different radius and timer duration.
7. Turn on Monitor for all three places.
8. Open View map for Home and confirm it is read-only.
9. Open Edit settings for Office and change only Office.
10. Remove Gym and confirm you stay in Places.
11. Confirm phone and watch prompts name the same active place.
