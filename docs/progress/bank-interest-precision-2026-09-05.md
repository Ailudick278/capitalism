# Bank interest precision progress log — 2026-09-05

## Implemented

- Bank accounts now persist fractional interest remainders for both deposits and credit debt.
- Daily settlement carries values smaller than one minor currency unit into later days instead of discarding them.
- Remainders are cleared when the corresponding balance or debt reaches zero, preventing old accruals from leaking into a later position.
- The new fields are optional in the codec, so existing bank-account data loads with zero remainders.

## Design boundary

- Annual rates, overdue multipliers, settlement cadence, and term-deposit rules were not changed.
- Account balances and debts remain integer minor units; only the unposted fractional interest is persisted separately.

## Follow-up

- Add direct bank-settlement tests covering small balances, partial repayment, and save/load round trips.
- Review whether bank transaction history should expose accumulated-but-not-yet-posted interest.
