# Company debt service underwriting

## Implemented

- Company loan approval now considers loan term and annual interest, not only total debt and recent cash flow.
- Existing and proposed loans are converted to a conservative annual debt-service estimate.
- Companies with operating history need an estimated debt-service coverage ratio of at least 1.25.
- The calculation treats current company loans as balloon-style obligations, matching the current repayment model without changing saved loan records.
- New companies without operating history retain the startup-financing path and remain constrained by registered capital.

## Design basis

Debt-service coverage compares cash available for debt service with required debt service. The 1.25 threshold is a game rule inspired by common commercial underwriting practice, not a universal legal requirement.

## Next direction

- Replace the conservative balloon estimate with explicit amortization schedules when the loan UI supports installment payments.
- Add collateral valuation and loan-to-value limits after company equipment and warehouse valuation are made more comprehensive.
