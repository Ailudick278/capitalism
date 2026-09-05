# Market order priority progress log — 2026-09-05

## Implemented

- Commodity orders now persist their creation game time.
- Matching uses price priority first, then time priority, then order ID as a deterministic tie-breaker.
- Existing orders without the new field load with `createdAt = 0`, preserving them as older orders.
- Partial fills retain the original creation time.

## Design boundary

- No prices, fees, escrow amounts, or order expiration rules were changed.
- This only makes the existing limit-order matching rule deterministic and closer to a normal price-time order book.

## Follow-up

- Add explicit order expiration and an auditable cancellation/refund event stream.
- Add market tests for same-price FIFO matching and legacy order decoding.
