# Equipment material supply chain

## Implemented

- Each industrial machine now declares a material bill of materials in `MachineType`.
- Installing equipment requires both the existing cash purchase price and the corresponding materials in the company's warehouse.
- Basic machines use vanilla materials so a new company can bootstrap; advanced machines consume steel sheet, copper wire, glass, and electric lamps from the existing industrial chain.
- Equipment material consumption is atomic and reduces commodity supply after the machine is installed.
- Existing installed equipment remains valid because the new requirement applies only to future purchases.
- If the material reservation fails after the cash debit, the cash purchase is rolled back as a non-operating ledger entry.

## Real-world alignment

Industrial equipment requires both capital expenditure and physical components. The model keeps the game abstraction manageable while making machinery demand visible to upstream steel, electrical, glass, and assembly industries.

## Next direction

- Add explicit equipment manufacturing recipes and supplier orders so companies can sell machines rather than only consume warehouse materials.
- Add machine-specific spare parts and maintenance material requirements after the supplier path exists.
