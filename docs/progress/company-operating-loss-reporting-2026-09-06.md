# Company operating loss reporting — 2026-09-06

## Implemented

- Company operating snapshots now preserve negative gross profit and operating profit.
- Other operating expenses remain a non-negative subtotal after cost of sales is separated.
- Saturating subtraction protects reports from long-integer overflow.

## Accounting rationale

Financial statements must distinguish a loss from zero activity. If cost of sales exceeds revenue, gross profit is negative; if other operating costs exceed gross profit, operating profit is negative. Flooring both values to zero hid underperforming companies and made size/credit analysis less informative.

## Boundary

The snapshot is still a read-only game-scale report. It does not yet include a separate other-income/expense section, deferred tax, or comprehensive-income adjustments.
