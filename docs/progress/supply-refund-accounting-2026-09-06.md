# Supply order refund accounting

## Implemented

- Expired backorders now refund the original payer's account type.
- Orders paid from a company treasury are refunded to that company's treasury.
- Personal orders and legacy orders without a valid company continue to refund the buyer's mailbox.
- The supply-order audit distinguishes company-treasury refunds from personal mailbox refunds.
- Unused VAT input credit is proportionally reversed when an undelivered part of a prepaid order expires, with a separate tax-invoice audit entry.

## Real-world alignment

A business refund returns to the business bank account or ledger that paid the invoice; it should not become a personal payment to the owner merely because the owner placed the order.

## Follow-up

- Add an explicit tax-credit reversal record when a prepaid business order expires before delivery.
- Continue auditing shipment loss, insurance payout, and company treasury ownership transitions.
