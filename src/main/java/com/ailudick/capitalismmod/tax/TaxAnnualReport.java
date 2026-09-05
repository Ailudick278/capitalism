package com.ailudick.capitalismmod.tax;

import java.util.UUID;

/** Read-only annual tax summary derived from the persistent tax ledger and audit trail. */
public record TaxAnnualReport(long year, long yearStart, long yearEnd, long taxableBase,
                              long assessedTax, long paidTax, long refunds, long outstandingTax,
                              long currentCreditBalance, long refundActions) {
    public static TaxAnnualReport calculate(net.minecraft.server.MinecraftServer server, UUID taxpayerUuid, long now) {
        long yearTicks = 360L * 24000L;
        long year = Math.floorDiv(now, yearTicks);
        long start = year * yearTicks;
        long end = start + yearTicks;
        TaxLedgerSavedData ledger = TaxLedgerSavedData.get(server);
        long base = 0L, assessed = 0L, outstanding = 0L;
        for (TaxBill bill : ledger.bills()) {
            if (!bill.subject().taxpayerUuid().equals(taxpayerUuid) || !inYear(bill.createdAt(), bill.periodStart(), bill.periodEnd(), start, end)) continue;
            base = add(base, bill.taxableBase()); assessed = add(assessed, bill.amount()); outstanding = add(outstanding, bill.outstanding());
        }
        long paid = ledger.paymentsFor(taxpayerUuid).stream().filter(payment -> payment.paidAt() >= start && payment.paidAt() < end).mapToLong(TaxPayment::amount).reduce(0L, TaxAnnualReport::add);
        long refunds = 0L, refundActions = 0L;
        for (TaxRefundAuditSavedData.Event event : TaxRefundAuditSavedData.get(server).all()) {
            if (!event.taxpayerUuid().equals(taxpayerUuid) || event.time() < start || event.time() >= end || !"APPROVE".equals(event.action())) continue;
            refunds = add(refunds, event.amount()); refundActions++;
        }
        return new TaxAnnualReport(year, start, end, base, assessed, paid, refunds, outstanding,
                TaxCreditSavedData.get(server).totalFor(taxpayerUuid), refundActions);
    }
    private static boolean inYear(long createdAt, long periodStart, long periodEnd, long start, long end) {
        return (createdAt >= start && createdAt < end) || (periodEnd > start && periodStart < end);
    }
    private static long add(long left, long right) { return right > 0L && left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + Math.max(0L, right); }
}
