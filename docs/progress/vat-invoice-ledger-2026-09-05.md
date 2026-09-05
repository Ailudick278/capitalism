# VAT invoice ledger

## Implemented

- Added a persistent VAT invoice-like ledger for output and input transactions.
- Each record stores gross amount, assessed VAT, input credit applied, source event and direction.
- `/taxinvoices` displays the player's latest VAT records for audit.
- Fully credit-offset output transactions now still leave an auditable invoice record.
- Source-event deduplication is retained across reloads.

## Design boundary

- These are simplified game invoices, not a full legal invoice system. Supplier identity, item tax classes and invoice transfer are not yet modeled.
