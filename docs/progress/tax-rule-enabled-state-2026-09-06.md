# Tax rule enabled-state enforcement

## Implemented

- Tax calculation now ignores tax-rule versions whose `enabled` flag is false.
- Disabled rules remain in history and can still be inspected for audit purposes.
- If no enabled rule is effective at the requested time, the existing configured default fallback remains in use.

## Real-world alignment

An annulled or disabled tax rule may remain in the legal/audit history, but it must not be used to assess new liabilities. The effective-rule query now separates those two concerns.

## Next direction

- Add regression coverage for future-effective, disabled, and re-enabled rule versions.
- Continue reviewing administrator operations for auditability and effective-date behavior.
