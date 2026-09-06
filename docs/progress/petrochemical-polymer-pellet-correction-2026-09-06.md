# Polymer pellet route correction — 2026-09-06

## Implemented

- Corrected the default ethylene route to produce `polyethylene_pellets`.
- Corrected the default propylene route to produce `polypropylene_pellets`.
- Kept generic `plastic_pellets` as a separate simplified route for legacy and general-purpose recipes.
- Added a compatibility migration for old `industries.json` entries that used the two specific recipe IDs but incorrectly produced generic plastic pellets.
- The migration only changes the known old default output; deliberately customized recipe outputs remain unchanged.

## Why this matches industry practice

Ethylene and propylene are petrochemical feedstocks for distinct polymer families. Polyethylene and polypropylene are supplied as resin/pellet materials to downstream molding, extrusion, compounding, and battery-separator processes; they should not collapse into one generic commodity when the recipe explicitly represents a specific polymer.

## References

- U.S. Energy Information Administration, “How much oil is used to make plastic?”: https://www.eia.gov/tools/faqs/faq.php?id=34
- U.S. Environmental Protection Agency, “Plastics Molding and Forming Effluent Guidelines”: https://www.epa.gov/eg/plastics-molding-and-forming-effluent-guidelines
- U.S. EPA, “Polymer Manufacturing Industry” archive: https://archive.epa.gov/compliance/resources/publications/assistance/sectors/web/pdf/resfibsn.pdf

## Follow-up

- When quality batches become available for polymer production, add grade-specific yields and scrap rather than introducing more generic resin IDs.
- Later, connect pellets to injection molding, extrusion, and battery-separator capacity without forcing every downstream recipe to use the same polymer grade.
