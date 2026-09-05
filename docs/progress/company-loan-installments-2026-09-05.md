# Company loan installments

## Implemented

- Added a scheduled-payment estimate based on the remaining principal, accrued interest and remaining days.
- Added `/company repayinstallment <name> <loanId>` for one scheduled payment.
- `/company companyloans <name>` now displays the current installment estimate.
- Overdue loans have one immediate payment equal to the current outstanding principal and interest.
- Manual `/company repayloan` remains available for arbitrary partial or full payments.

## Limitation

- The first version is an equal-payment estimate rather than a separately persisted amortization calendar. Daily interest accrual and payment dates can be made more precise in a later loan-contract revision.
