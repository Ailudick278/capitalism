# Company loan payment allocation — 2026-09-06

## Implemented

- Extracted company-loan payment allocation into a pure rule object.
- Payments first settle accrued interest, then reduce principal.
- Full repayment leaves zero principal; overpayments and invalid amounts are rejected.
- Added unit tests for interest-first partial payment, full payment and overpayment rejection.

## Accounting rationale

Separating the payment allocation from the command/service layer makes the loan balance auditable and prevents a future change to interest calculation from silently producing an incorrect principal balance.

## Boundary

The loan still uses the existing game-scale daily interest and maturity model. Collateral enforcement, guarantors and restructuring remain separate future features.
