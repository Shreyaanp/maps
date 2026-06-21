# Dwell Location Flow Test Matrix

This matrix defines the full location-setting contract for Dwell. A place must
always be in exactly one user-visible compartment:

- Default settings: settings used only for the next new place.
- Unsaved place: a pending map/search/current-location selection that is not in Places yet.
- Editing place: one saved place whose name, radius, timer, and arrival mode can change.
- Viewing place: a saved place shown on the map without editable place controls.
- Runtime place: the place that owns a timer, arrival prompt, switch prompt, leave prompt, or done prompt.

## Location Set Entry Points

| Starting state | User action | Expected result | Settings target |
| --- | --- | --- | --- |
| No saved places | Use current location | Unsaved place on map; Save this place creates first place | Unsaved place |
| No saved places | Search result | Unsaved place on map; Save this place creates first place | Unsaved place |
| No saved places | Long-press map | Unsaved place on map; Save this place creates first place | Unsaved place |
| Viewing saved place | Use current location | Center map on phone; viewed place is unchanged | Viewing place |
| Viewing saved place | Search result | Unsaved new place; viewed place is unchanged | Unsaved place |
| Viewing saved place | Long-press map | Unsaved new place; viewed place is unchanged | Unsaved place |
| Editing saved place | Use current location | Unsaved move for that saved place | Unsaved move |
| Editing saved place | Search result | Unsaved move for that saved place | Unsaved move |
| Editing saved place | Long-press map | Unsaved new place, not a move | Unsaved place |
| Places screen | Add place | Opens map search in Add place mode | Unsaved place after selection |
| Places screen | View map | Opens saved place read-only | Viewing place |
| Places screen | Edit settings | Opens saved place controls | Editing place |

Use current location is mode-aware everywhere: in Add place it creates or
updates the unsaved preview, in Edit settings it creates or updates the move
preview, and in View map it only centers the map on the phone.

## Pending Unsaved Place Rules

| User action while unsaved place exists | Expected result |
| --- | --- |
| Change name | Changes only the unsaved place |
| Change radius | Changes only the unsaved place |
| Change timer length | Changes only the unsaved place |
| Change arrival mode | Changes only the unsaved place |
| Save this place | Creates a new saved place with reviewed settings |
| Move `<place>` | Updates only that saved place |
| Cancel preview / Cancel move | Clears the unsaved pin or returns to the previous saved place |
| Open Settings or Insights | Stay on map; ask to save/cancel a new preview or move/cancel a move preview first |
| Monitor, Pause monitoring, Start now, or row mutation | Block; ask to save/cancel a new preview or move/cancel a move preview first |
| Open Places | Show unsaved banner with Review on map and Cancel preview / Cancel move |

New unsaved-place previews start from the current default radius, timer, and
arrival mode settings. If the user is already editing the same unsaved preview
and only changes the point, the draft name, radius, timer, and arrival mode stay
with that draft. Switching from moving a saved place to creating a new unsaved
place does not carry the saved place's settings into the new place.

## Saved Place Compartment Rules

| Flow | Expected result |
| --- | --- |
| Edit Home radius | Only Home radius changes |
| Edit Office timer | Only Office timer changes |
| Edit Gym arrival mode | Only Gym arrival mode changes |
| Rename timer place | Phone, watch, tile, and notification labels refresh |
| View Home while Office timer runs | Dock names Office timer, not Home settings |
| Start Gym from prompt while viewing Home | Timer belongs to Gym; viewed Home stays viewed |
| Start Gym while Office timer runs | Gym Start now is disabled and names the Office timer to cancel first |
| Pause Office monitoring | Only Office monitoring pauses |
| Enable Gym monitoring | Only Gym monitoring enables |
| Delete non-viewed place | Stay in Places; current viewed/editing map state stays put |
| Delete viewed/editing place | Stay in Places; map focuses the next saved place if one exists |
| Delete last saved place | Stay in Places; empty state shows Add place |
| Undo delete | Restores saved place and monitoring flag; does not resurrect stale prompts |

## Duplicate And Nearby Matrix

| Existing place | Candidate | Expected result |
| --- | --- | --- |
| Office | Same point, any label | Select existing Office; do not duplicate |
| Office | Same label within duplicate distance | Select existing Office; do not mutate Office settings |
| Office | Different label nearby | Create separate place |
| Office | Same label far away | Create separate place |
| Office + Home duplicate groups | Duplicate both groups at once | Each removed ID remaps to its own survivor |

## Runtime Place Matrix

| Runtime state | Location event | Expected result |
| --- | --- | --- |
| No timer | Enter monitored place | Prompt/start uses entered place settings |
| Timer at A | Enter B | Switch prompt names B and current timer A |
| Timer at A | Enter overlap A+B | No switch while A is still triggered |
| Timer at A | Leave A | Leave prompt names A |
| Timer at A expires | Done prompt names A |
| Prompt for deleted A | Delete A | Prompt clears; B runtime state survives |
| Prompt for B | Delete A | Prompt for B survives |

## Automated Coverage

The unit test matrix lives in:

- `app/src/test/java/work/shreyaan/dwell/LocationUserFlowMatrixTest.kt`

It covers:

- 15 location source/start-state combinations.
- 12 settings persistence target combinations.
- Per-place mutation isolation across Home, Office, and Gym.
- New-place creation from search/current-location/long-press without saved-place leakage.
- Duplicate and nearby-place permutations.
- Monitored-place limit behavior without losing saved-place settings.
- Delete transitions for non-open, open-with-next, and open-last-place flows.
