# Shop and stock configuration migration

## Implemented

- Existing shop configuration files now receive missing maintained offers keyed by item and currency.
- Existing custom shop prices and quantities remain unchanged.
- Existing stock configuration files now receive missing maintained stock IDs without replacing custom listings.
- The migration keeps reserved/removed stock filtering in place.

## Next direction

- Add explicit data versions if a future release needs to remove or rename configured entries.
- Continue the same compatibility audit for tax rules and other data-driven systems.
