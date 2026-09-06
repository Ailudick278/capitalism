# Logistics loss accounting idempotency — 2026-09-06

## Implemented

- Added a persistent inventory-loss source ledger keyed by shipment ID.
- Repeated loss processing cannot record the same inventory impairment twice.
- Loss accounting is performed before the durable loss notification is written, so a retry can complete accounting after an interrupted attempt.
- Existing loss notifications remain deduplicated by shipment ID.

## Accounting rationale

Cargo loss recognition and the user-facing loss notification are separate records. The shipment ID is treated as the stable business event key; accounting can therefore be retried safely without creating a second impairment charge.

## Boundary

Supply-order audit events still use their existing append-only model; a future event-key layer can make every order event idempotent across persistence boundaries.
