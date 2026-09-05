# Enterprise scale model - 2026-09-05

## Change

The company `level` mechanic has been removed. Real companies do not normally
advance through a universal level ladder. Their scale is observable through
separate accounting and operating metrics.

## Implemented

- `registeredCapital` replaces the persisted company level field.
- `/company contribute <name> <amount>` records paid-in capital and places the
  contribution in the company treasury.
- Production recipes now represent one batch. Output, input and recipe income
  are no longer multiplied by a fictitious level.
- A production batch requires the appropriate functioning machine, workers and
  treasury funding. Parallel capacity will come from more equipment and labor.
- IPO share count and fundamental value are derived from registered capital.
- Mergers add registered capital instead of adding levels.
- The company and securities screens show capital rather than `Lv.`.
- Added `/company statement`, a read-only balance-sheet snapshot showing cash,
  inventory, equipment, liabilities and derived equity from the persisted
  company resources.
- Old serialized companies without `registeredCapital` load with the safe
  default of 1000 major currency units; new saves never write `level`.

## Why this is closer to reality

Capital is not the same thing as revenue, profit, assets, workforce or output
capacity. The model therefore keeps these dimensions separate: capital belongs
to the balance sheet, treasury is liquid cash, labor is represented by worker
contracts, equipment is represented by maintained machines, and production is
represented by recipe batches. Future enterprise size indicators should use
revenue, assets, employees, utilization and profitability rather than restoring
a universal level.

## Next direction

- Add capital changes through retained earnings, dividends and formal equity
  issuance rather than treating every funding event as an upgrade.
- Add assets and liabilities so capital can be reconciled with a balance sheet.
- Add production scheduling so multiple machines and workers create parallel
  batches without changing recipe quantities.
- Add revenue, employee-count and asset-based reports for company comparison.

## Reference material

- [IFRS Conceptual Framework](https://www.ifrs.org/issued-standards/list-of-standards/conceptual-framework/)
  separates assets, liabilities, equity, income and expenses, and treats owner
  contributions as distinct from income.
- [SEC Beginners' Guide to Financial Statements](https://www.sec.gov/about/reports-publications/beginners-guide-financial-statements)
  explains the balance-sheet equation and the distinction between cash,
  inventory, fixed assets and liabilities.
