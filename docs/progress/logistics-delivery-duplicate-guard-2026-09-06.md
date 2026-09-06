# Logistics delivery duplicate guard — 2026-09-06

## Implemented

- Delivery processing now checks the persistent delivery ledger before crediting a warehouse.
- A shipment that has already been recorded as delivered is removed as a stale transport entry instead of being credited twice.
- Fuel planning records now reject a second plan for the same shipment ID.
- Fuel planning records reject invalid transport modes, missing fuel identifiers and negative prices/costs before they enter the persistent ledger.

## Accounting rationale

Delivery, freight capitalization and supply-order audit are separate persistent records. A retry can therefore encounter a completed delivery while the transport entry is still present. The shipment ID is the stable business key used to prevent duplicate inventory and duplicate fuel-plan records.

## Boundary

The guard reduces retry duplication across normal server restarts and stale entries. A fully atomic cross-SavedData transaction would require a broader persistence transaction layer and remains outside this phase.
