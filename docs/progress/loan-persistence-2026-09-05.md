# Loan persistence progress log — 2026-09-05

## Implemented

- `PeerLoanSavedData.replaceLoan` now marks the world data dirty whenever a loan is replaced.
- Loan updates are now persistence-safe even when called outside the daily economy settlement path.

## Design boundary

- No loan rates, maturity rules, repayment amounts, or overdue penalties were changed.
- This is a SavedData lifecycle fix; the existing daily settlement remains compatible.

## Follow-up

- Add explicit peer-loan repayment receipts and overdue notices.
- Separate principal, accrued interest, and overdue penalty into auditable ledger fields before expanding loan gameplay.
