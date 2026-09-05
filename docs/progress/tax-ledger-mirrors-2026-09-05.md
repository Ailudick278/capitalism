# Tax ledger mirror progress log — 2026-09-05

## Implemented

- Added a unified event listener for corporate and individual-business tax settlement/delinquency events.
- Legacy `taxOwed` fields are now refreshed from the central tax ledger when those events occur, including when the owner is offline.
- Mirror conversion now uses major-unit ceiling conversion instead of integer truncation, so a non-zero minor-unit liability cannot appear as zero.

## Design boundary

- The tax ledger remains the only source of truth for liability amount, payment, and late fees.
- The company/business fields remain compatibility and gameplay-state mirrors; they do not create additional tax bills.

## Follow-up

- Audit all remaining legacy fields that represent balances already owned by a central ledger.
- Add direct tests for mirror synchronization with sub-unit liabilities and offline owners.
