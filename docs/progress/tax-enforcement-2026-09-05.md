# Tax enforcement progress log — 2026-09-05

## Implemented

- Added `TaxEnforcementTickHandler`.
- Every 1200 server ticks, the server scans unpaid tax bills, updates late fees, and dispatches delinquency events through the central tax service.
- Tax enforcement no longer depends on a player opening the tax screen; offline taxpayers continue to advance through the same tax lifecycle.
- Land tax enforcement therefore remains connected to the unified tax ledger even when no land or tax screen is open.

## Design boundary

- No tax rates, grace periods, or land disposal thresholds were changed.
- This is a server-side lifecycle correction, not a new player-facing gameplay rule.

## Follow-up

- Add focused tests for scheduled late-fee updates and idempotent enforcement notices.
- Review whether player notifications should be persisted as mailbox notices for offline players.
