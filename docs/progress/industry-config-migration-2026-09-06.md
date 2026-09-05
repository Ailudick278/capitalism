# Industry recipe configuration migration

## Implemented

- Existing `industries.json` files now receive missing maintained recipe IDs when the mod loads them.
- Player or pack-author recipes with the same ID remain authoritative and are not overwritten.
- Newly added staged mining and smelting recipes therefore become visible without deleting or manually rebuilding an existing configuration file.
- The merge is performed in memory, so unrelated custom industries and recipes remain unchanged on disk.

## Compatibility boundary

This is additive migration only. It does not remove recipes, rewrite prices, or silently change an existing recipe with a matching ID.

## Next direction

- Add an explicit data version if future migrations need to rename or remove recipes.
- Continue expanding manufacturing intermediates while keeping recipe IDs stable.
