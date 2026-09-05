# Commodity configuration migration

## Implemented

- Existing `commodities.json` files now receive missing maintained commodity IDs when the mod loads them.
- Existing custom initial prices remain unchanged.
- Newly introduced manufacturing components, including machine frames and electric motors, therefore become available to the commodity exchange in existing worlds.
- Invalid or custom commodity entries are preserved rather than replaced.

## Next direction

- Add explicit data versions for future removals or renames.
- Continue auditing shop, stock, and industry configuration loaders for the same additive-migration guarantee.
