# Production batch atomicity

## Implemented

- Added an atomic warehouse batch-consumption operation for production bills of materials.
- All input item IDs, positive quantities, and available stock are validated before any input is removed.
- Production now uses that operation instead of consuming inputs one by one, preventing partial material consumption when a recipe is incomplete.
- Commodity supply statistics are updated only after the complete batch has been removed.

## Real-world alignment

A manufacturing work order consumes a defined bill of materials as one committed batch. A missing component should leave the work order unstarted rather than consuming only the components that happened to be available.

## Next direction

- Connect machine purchases and maintenance to industrial goods instead of treating all equipment as abstract balance-sheet entries.
- Add service contracts and customer orders for industries whose outputs are services rather than warehouse items.
