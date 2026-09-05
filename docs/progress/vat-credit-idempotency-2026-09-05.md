# VAT credit idempotency

## Implemented

- Tax-credit storage now persists processed VAT source-event IDs.
- A VAT output event that is fully offset by input credit cannot consume the same credit again if the transaction is retried.
- Input-credit creation is also idempotent by source event.
- The event marker list is bounded to 8192 entries to prevent unbounded growth.
- Older worlds without the new list load normally.

## Next direction

- Add explicit invoice records and tax-category metadata so input eligibility can be audited by item and supplier.
