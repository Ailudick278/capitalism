# Electronics recycling loop — 2026-09-06

## Implemented

- Added a dedicated `electronics_recycler` machine.
- Added recycling recipes for smartphones, televisions, laptops and wireless routers.
- Recycling returns copper wire and plastic pellets as secondary raw materials.
- The recycling output is intentionally lower than the original bill of materials; it represents dismantling loss, contamination and material separation rather than duplicating a finished product.

## Industry basis

The model follows the basic real-world hierarchy of used-electronics management: reuse or refurbishment should be considered before material recycling; recycling facilities then collect, sort, dismantle and mechanically separate devices to recover materials such as copper, plastics, glass and other metals.

## Future direction

- Add a separate refurbishment route that consumes repair parts and returns a discounted used product.
- Add battery/data-security and hazardous-material handling when the end-of-life system becomes more detailed.
