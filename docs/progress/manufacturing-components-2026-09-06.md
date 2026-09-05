# Manufacturing component chain

## Implemented

- Added machine frames and electric motors as reusable manufacturing intermediates.
- Machine frames use steel sheet and iron ingots on a rolling mill.
- Electric motors use copper wire, iron ingots, and redstone on a lathe.
- Assembly lines now require those components plus glass, connecting equipment construction to upstream manufacturing.
- Added commodity entries and item models for both components.

## Real-world alignment

Industrial equipment is assembled from structural frames and drive/control components rather than appearing as a single abstract purchase. The chain remains intentionally simplified but exposes meaningful upstream demand.

## Next direction

- Add explicit equipment supplier offers and machine delivery/installation lead time.
- Expand the same component pattern to transport, energy, and agricultural equipment.
