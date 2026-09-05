# Land auction tax settlement progress log — 2026-09-05

## Implemented

- Added a central `TaxService.settleFromProceeds` path for taxes withheld from an external settlement, without charging a player twice.
- Land auction proceeds now settle the seller's land-tax bills in the unified tax ledger and record tax payments.
- Auction settlement uses minor-unit ledger amounts before converting the withheld amount back to whole auction currency units.
- Land tax enforcement now verifies that the bill taxpayer still owns the claim, preventing an old owner's residual liability from freezing the buyer's newly acquired land.
- External settlement source IDs are persisted in the tax ledger, preventing the same auction proceeds from being applied twice after a reload or repeated settlement attempt.

## Design boundary

- Auction pricing, bid rules, disposal timing, and transfer-tax assessment were not changed.
- If auction proceeds are insufficient, the remaining seller liability stays in the tax ledger; it is not silently transferred to the buyer.

## Follow-up

- Add integration tests for full and partial auction-tax settlement.
- Add a dedicated seller-liability/auction history view when the land UI is revisited.
