# Mining and metal-processing chain

## Implemented

- Added an optional ore-concentration stage for iron, copper, and gold: two ore blocks become one raw-metal unit.
- Added an optional blast-furnace smelting stage: raw metal plus coal becomes an ingot.
- Added the `blast_furnace` industrial machine with a raw-material equipment BOM.
- Kept the former direct ore-to-ingot recipes so existing company saves and simple starter play remain compatible.

## Real-world alignment

Mining, beneficiation/concentration, and metallurgical smelting are separate industrial stages. The simplified chain now represents that separation without requiring a full chemical-process simulation.

## Next direction

- Add by-products and quality/yield differences for ore grades.
- Split manufacturing into reusable intermediate components and final assembly orders.
