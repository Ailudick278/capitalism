# Bank credit-risk model

## Implemented

- Added a deterministic `CreditAssessment` derived from existing bank-account data, so old saves need no migration.
- The assessment considers debt utilization, repayment entries, and overdue status.
- New bank loans are checked against the score-adjusted approved limit.
- An overdue credit account cannot take additional loans until its debt is cleared.

## Design basis

The model follows common real-world underwriting concepts: repayment history, amount owed relative to available credit, and the borrower’s capacity to repay. It is intentionally a game abstraction rather than a jurisdiction-specific legal credit score.

## Next direction

- Add stable income/cash-flow evidence from wallet and company distributions.
- Add collateral and structured default/recovery handling before introducing more complex loan products.
