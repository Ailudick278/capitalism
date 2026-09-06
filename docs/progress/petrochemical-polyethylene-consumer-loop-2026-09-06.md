# Polyethylene consumer loop — 2026-09-06

## Implemented

- Added the consumer commodity `plastic_container`.
- Added an `injection_molder` recipe consuming two `polyethylene_pellets` and producing one plastic container.
- Added an `extrusion_line` and a packaging-film recipe consuming one `polyethylene_pellets` and producing four packaging-film units.
- Registered the item, market price, creative-tab entry, language names, and item model.
- Reused the existing injection-molding machine, which now serves phone casings, fan blades, and plastic containers; the new extrusion line models a different downstream process.

## Real-world basis

Polyethylene resin is commonly supplied as pellets to downstream processors. Injection molding heats and injects polymer melt into a mold, while extrusion melts and pushes resin through a die to form continuous film. The game recipes abstract grade selection, drying, die changes, and scrap while keeping the material and machine relationships visible.

Reference: U.S. Environmental Protection Agency, “Plastics Molding and Forming Effluent Guidelines”: https://www.epa.gov/eg/plastics-molding-and-forming-effluent-guidelines

## Follow-up

- Later connect container demand to food, chemical, and retail contracts instead of relying only on the commodity market.
