# Logistics disruption limits

## Implemented

- Shipments now persist their disruption count.
- Uninsured shipments are retried only up to the configurable `logisticsMaxDisruptions` limit (default 3).
- Insured shipments continue to settle through the declared-value payout path on a disruption.
- Old shipment records load with a disruption count of zero.
- This prevents permanently delayed cargo from accumulating in the logistics queue.

## Next direction

- Add a persistent cargo-loss/claims ledger and buyer notifications.
- Add transport-specific risk and insurance pricing after route and infrastructure data are richer.
