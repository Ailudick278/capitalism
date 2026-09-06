# Corporate tax loss carryforward — 2026-09-06

## Implemented

- Annual corporate tax now calculates signed operating result before applying tax.
- A company loss is saved as a tax-loss carryforward instead of being discarded.
- A later profitable year uses the carryforward to reduce taxable profit.
- Unused losses continue to carry forward after a partial offset.
- Quarterly prepayments remain based on the current quarter; the annual settlement reconciles the final taxable result.
- Existing overpayment handling still turns excess prepaid tax into a tax credit.

## Accounting rationale

Real corporate income-tax systems commonly distinguish accounting profit from taxable profit and allow qualifying losses to offset future taxable income, subject to jurisdiction-specific limits. The mod now models the core mechanism without introducing a jurisdiction-specific expiry or ownership-change rule yet.

## Future direction

- Add configurable carryforward expiry/limits if the tax rules become jurisdiction-specific.
- Separate tax adjustments from accounting expenses when the company accounting model is expanded.
