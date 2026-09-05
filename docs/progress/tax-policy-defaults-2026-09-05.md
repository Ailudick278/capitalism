# Tax policy defaults

## Implemented

- Added configurable defaults for every active transaction-linked tax category.
- VAT, general transaction tax, land-transfer tax, securities stamp duty, capital gains, dividends, resources and customs now have non-zero baseline rules.
- Inheritance/gift tax remains disabled by default because it requires a more complete estate and beneficiary model.
- Existing operator commands remain authoritative: `/taxrule set`, `/taxrule schedule`, `/taxrule reset` and `/taxrule list` can override or inspect the defaults.

## Policy boundary

These are gameplay defaults inspired by common tax categories and publicly documented reference rates; they are not a claim that one jurisdiction's complete tax law is being reproduced. Tax rates, thresholds, exemptions and effective dates remain server-configurable.

## Next direction

- Add tax-class distinctions for goods/services and input-tax credits instead of applying one VAT rate to every transaction.
- Add taxpayer registration and filing records before enabling more complex exemptions or inheritance tax.
