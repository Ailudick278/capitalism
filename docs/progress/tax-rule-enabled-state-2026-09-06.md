# Tax rule enabled-state enforcement

## Implemented

- The latest effective rule remains authoritative even when its `enabled` flag is false; `TaxRule.taxableBase()` then produces zero taxable base.
- Disabled rules therefore act as an explicit tax pause instead of silently falling back to an older enabled version.
- Disabled rules remain in history and can still be inspected for audit purposes.

## Real-world alignment

An annulled or disabled tax rule may remain in the legal/audit history, but it must not be used to assess new liabilities. The effective-rule query now separates those two concerns.

## Next direction

- Add regression coverage for future-effective, disabled, and re-enabled rule versions.
- Continue reviewing administrator operations for auditability and effective-date behavior.
