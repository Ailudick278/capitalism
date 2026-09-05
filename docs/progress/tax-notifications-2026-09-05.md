# Tax notification progress log — 2026-09-05

## Implemented

- Added a dedicated persistent tax-notification store instead of reusing the refund-notification store.
- Delinquency notices now use a common tax notification service.
- Online players still receive immediate notices; offline players receive a queued notice when they next log in.
- Notice delivery is deduplicated by notice ID and capped at 1024 records.

## Design boundary

- The change affects notification delivery only. Tax rates, declaration deadlines, grace periods, and land auction rules are unchanged.
- The tax ledger remains the source of truth for liabilities; notifications are informational and cannot settle a bill.

## Follow-up

- Add a client-side tax-notification panel when the tax UI is revisited.
- Add focused tests for offline delivery, deduplication, and persistence migration.
