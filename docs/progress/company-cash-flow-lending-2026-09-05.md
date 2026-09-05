# Company cash-flow lending

## Implemented

- Company loan underwriting now reads the existing company ledger for the latest 90 game days.
- Operating cash flow is separated from financing and investment flows such as loan proceeds, capital contributions, dividends, owner withdrawals and equipment purchases.
- Companies with recorded operating activity must have positive cash flow and keep total debt within three times the recent operating cash flow.
- Companies without operating history retain the existing registered-capital rule, so newly created companies are not permanently unable to obtain startup financing.
- No new persistent fields were added; old worlds use the same ledger and loan saves.

## Design basis

This is a game-scale approximation of cash-flow underwriting and debt-service coverage. Real lenders assess expected cash flow, repayment capacity, loan structure and collateral rather than relying on registered capital alone.

## Next direction

- Add scheduled amortization and a debt-service ratio that considers each loan's term and interest.
- Add collateral and default recovery only after the company asset valuation is reliable.
