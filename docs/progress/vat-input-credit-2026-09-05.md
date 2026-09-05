# VAT input credit

## Implemented

- Added a stable VAT tax subject per taxpayer, so multiple sales share one output-tax account instead of creating unrelated accounts.
- Supply-market sales create output VAT bills for the supplier.
- Company and active sole-proprietor purchases through the supply market create input-tax credits.
- Later output VAT consumes the seller's available input credits before creating a payable bill.
- Ordinary player purchases do not create deductible VAT credits.
- Existing tax-credit storage and refund allocation records are reused; no migration is required.

## Design boundary

- This is a simplified invoice/input-credit model: it does not yet model tax invoices, different VAT classes, exempt goods, or cross-border zero-rating.
- Input credits are currently limited to registered company and active sole-proprietor supply purchases.
