# Company tax expense event identity — 2026-09-06

## Implemented

- Operating debits continue to enter deductible company expense ledgers.
- Equipment acquisition is now explicitly treated as a non-operating capital outflow; its tax effect comes through the existing depreciation path rather than an immediate expense deduction.
- Deductible cash expense source IDs include a UUID. Parallel production batches at the same game tick therefore create independent expense events instead of being collapsed by the tax ledger's idempotency guard.

## Accounting rationale

Routine production, maintenance, and rework costs are period operating expenses. Purchasing a durable machine creates a fixed asset and should not be deducted in full at acquisition. Separating the cash-flow label from the tax-expense event keeps the company ledger, corporate tax periods, and depreciation treatment consistent.

## Boundary

Jurisdiction-specific capital allowance schedules and tax-loss carryforwards are not introduced yet; depreciation remains the mod's game-scale asset-cost recovery model.
