# Company loan repayment

## Implemented

- Partial company-loan payments are now supported instead of requiring either interest-only payment or full payoff.
- Payments apply to accrued interest first, then reduce outstanding principal.
- Full payoff still removes the loan contract; partial payments preserve the same saved loan record and update its principal.
- Existing saves and command syntax remain compatible: `/company repayloan <name> <loanId> [amount]`.

## Limitation

- The current contract remains a balloon-style loan. Interest is calculated from the current outstanding principal using the existing model; a future installment schedule can make daily accrual and amortization more precise.
