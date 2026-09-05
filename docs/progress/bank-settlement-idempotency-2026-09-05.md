# Bank settlement idempotency

## Implemented

- Bank interest and term-deposit settlement now receives an explicit Minecraft settlement day.
- The player's persisted settlement-day attachment prevents duplicate interest or duplicate term-deposit ticks when the same day is requested more than once.
- Offline catch-up still applies each missed day in order when the player logs in.
- Players with no bank accounts still advance their settlement marker, avoiding repeated empty settlement work.

## Real-world alignment

An account statement should not contain two daily accruals merely because two application events were delivered. The settlement day is therefore treated as an idempotency key, while the existing transaction list remains the user-visible audit trail.

## Next direction

- Continue reviewing transfer, repayment, and company-treasury paths for atomic debit/credit behavior.
- Add a proper bank statement/reporting layer before introducing more complex financial products.
