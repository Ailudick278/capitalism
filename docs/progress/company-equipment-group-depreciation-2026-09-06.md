# Equipment group usage depreciation — 2026-09-06

## Implemented

- Equipment records now persist a small `usageRemainder` counter.
- A group of N identical machines receives one condition-point of wear after N production batches.
- Old saves without the field load with a zero remainder.
- Maintenance resets the remainder because the equipment has been restored to a new service interval.
- Merger, installation, removal, and repair paths preserve or normalize the counter.

## Why this is more realistic

Parallel capacity represents multiple machines performing separate batches. Applying a full group-wide wear step to every batch made a company with four machines consume four times the useful life per cycle compared with a company using one machine. The new counter treats condition as a group-level average and counts actual batch uses before applying the next wear step.

## Boundary

This remains a game-scale condition model: it does not yet track machine-by-machine failure, spare parts, planned downtime, or different maintenance schedules.
