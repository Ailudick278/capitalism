# Calendar time-source progress log — 2026-09-05

## Implemented

- Perpetual-calendar dates now derive from monotonic `gameTime`, matching the time source used by economic settlement systems.
- `/time set` can still change the visual Minecraft clock (`dayTime`) without changing the economic calendar date.
- Added regression tests for Gregorian day mapping and Minecraft tick-to-clock conversion.
- Calendar command day counters now use the same `gameTime` source as the displayed date.
- Added a shared overflow-safe `ticksForDays` conversion utility for future economic periods.
- Company, individual-business, and annual tax-report periods now use that shared conversion utility without changing their existing 90/360-day values.

## Design boundary

- The stable epoch remains 2000-01-01.
- Financial period lengths such as 90-day quarters and 360-day configured years were not changed in this phase.

## Follow-up

- Update the calendar command text so every displayed day counter explicitly uses the same game-time source.
- Add a shared period utility for systems that currently repeat raw `24000` conversions.
