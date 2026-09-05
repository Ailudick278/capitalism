# Company loan servicing

## Implemented

- Company loans now expose explicit due and overdue transition checks.
- Daily settlement sends the company owner a notice when a loan becomes due.
- Daily settlement sends a second notice when the loan becomes overdue and penalty interest begins.
- Notifications are deduplicated and delivered after login through the existing persistent loan-notification queue.

## Next direction

- Add a company debt-service ratio based on operating cash flow.
- Add collateral and a documented default/recovery process before introducing forced liquidation.
