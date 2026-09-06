# Company inventory sale idempotency — 2026-09-06

## Implemented

- Persisted inventory-sale source IDs alongside company cost layers.
- Replayed delivery events with an already-settled source ID now skip cost-layer and quality-ledger consumption.
- Zero-cost legacy inventory sales are also marked as settled, preventing repeated quality consumption on retries.
- The new field is optional in the saved-data codec, so older worlds load with an empty source set.

## Why this matters

Supply settlement already protects payment and delivery journals from retries. Cost-of-goods recognition must use the same source identity: otherwise a crash or repeated settlement could remove the same weighted-average cost layer and quality quantity more than once, distorting profit and inventory records.

## Boundary

The source ID must be stable across retries. Existing supply delivery keys provide that identity; future sales channels should use the same pattern before calling the cost-transfer helper.
