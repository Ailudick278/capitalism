# Polyethylene consumer loop — 2026-09-06

## Implemented

- Added the consumer commodity `plastic_container`.
- Added an `injection_molder` recipe consuming two `polyethylene_pellets` and producing one plastic container.
- Registered the item, market price, creative-tab entry, language names, and item model.
- Reused the existing injection-molding machine, which now serves phone casings, fan blades, and plastic containers.

## Real-world basis

Polyethylene resin is commonly supplied as pellets to downstream processors. Injection molding heats and injects polymer melt into a mold to make repeatable consumer and packaging articles. The game recipe abstracts grade selection, drying, mold changeover, and scrap while keeping the material and machine relationship visible.

Reference: U.S. Environmental Protection Agency, “Plastics Molding and Forming Effluent Guidelines”: https://www.epa.gov/eg/plastics-molding-and-forming-effluent-guidelines

## Follow-up

- Add packaging film through an extrusion-line abstraction if packaging logistics needs a second polyethylene outlet.
- Later connect container demand to food, chemical, and retail contracts instead of relying only on the commodity market.
