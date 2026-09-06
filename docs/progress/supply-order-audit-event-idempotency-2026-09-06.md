# Supply-order audit event idempotency — 2026-09-06

## Implemented

- Supply-order audit events can now carry an optional stable event key.
- Delivery and loss events use the transport shipment ID as that key.
- Retrying the same shipment cannot add a second `DELIVERED` or `LOST` event to the order history.
- Existing audit records without event keys remain readable and valid.

## Accounting rationale

An order may be fulfilled through multiple shipments, so deduplicating only by order ID would incorrectly discard legitimate partial deliveries. The shipment ID distinguishes legitimate batches while making retries idempotent.
