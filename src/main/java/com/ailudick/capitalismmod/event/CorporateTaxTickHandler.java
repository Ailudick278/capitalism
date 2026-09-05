package com.ailudick.capitalismmod.event;

import com.ailudick.capitalismmod.CapitalismMod;
import com.ailudick.capitalismmod.Config;
import com.ailudick.capitalismmod.company.Company;
import com.ailudick.capitalismmod.company.CompanySavedData;
import com.ailudick.capitalismmod.currency.Money;
import com.ailudick.capitalismmod.tax.CorporateTaxPeriodSavedData;
import com.ailudick.capitalismmod.tax.CorporateTaxAnnualSavedData;
import com.ailudick.capitalismmod.tax.TaxPeriod;
import com.ailudick.capitalismmod.tax.TaxService;
import com.ailudick.capitalismmod.tax.TaxSubject;
import com.ailudick.capitalismmod.tax.TaxType;
import com.ailudick.capitalismmod.tax.TaxLedgerSavedData;
import com.ailudick.capitalismmod.tax.TaxCreditSavedData;
import com.ailudick.capitalismmod.tax.TaxRuleService;
import com.ailudick.capitalismmod.tax.TaxType;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Closes company quarterly income periods and creates prepayment bills. */
@EventBusSubscriber(modid = CapitalismMod.MODID)
public final class CorporateTaxTickHandler {
    private static final long TICKS_PER_DAY = 24000L;
    private CorporateTaxTickHandler() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        long now = server.overworld().getGameTime();
        if (now <= 0L || now % TICKS_PER_DAY != 0L) return;
        CorporateTaxPeriodSavedData data = CorporateTaxPeriodSavedData.get(server);
        CorporateTaxAnnualSavedData annual = CorporateTaxAnnualSavedData.get(server);
        TaxLedgerSavedData ledger = TaxLedgerSavedData.get(server);
        for (var entry : data.due(now)) {
            Company company = CompanySavedData.get(server).get(entry.companyId());
            if (company == null || entry.revenue() <= 0L) {
                data.remove(entry);
                continue;
            }
            long profit = Math.max(0L, entry.revenue() - Math.min(entry.revenue(), entry.expenses()));
            long taxAmount = TaxRuleService.taxMinor(server, TaxType.CORPORATE_INCOME,
                    Money.toMinorSaturated(profit), now);
            long taxMajor = Money.toMajorCeiling(taxAmount);
            long yearTicks = 360L * TICKS_PER_DAY;
            long annualEnd = ((Math.max(1L, entry.periodEnd()) - 1L) / yearTicks + 1L) * yearTicks;
            annual.recordPrepaid(entry.companyId(), annualEnd,
                    taxAmount);
            TaxPeriod period = new TaxPeriod(entry.periodStart(), entry.periodEnd(), entry.periodEnd(),
                    entry.periodEnd() + 15L * TICKS_PER_DAY);
            TaxSubject subject = new TaxSubject(TaxType.CORPORATE_INCOME, company.companyId(), company.ownerUuid());
            String source = "corporate-quarter:" + entry.companyId() + ":" + entry.periodEnd();
            taxAmount = TaxRuleService.taxMinor(server, TaxType.CORPORATE_INCOME,
                    Money.toMinorSaturated(profit), now);
            if (ledger.findBySourceEvent(source) == null) {
                taxAmount -= TaxCreditSavedData.get(server).consume(subject, entry.currencyId(), taxAmount);
            }
            if (taxAmount > 0L) {
                TaxService.createPeriodicBill(server, subject, entry.currencyId(), taxAmount, period,
                        Money.toMinorSaturated(profit),
                        TaxRuleService.rateBasisPoints(server, TaxType.CORPORATE_INCOME, now), source);
            }
            data.remove(entry);
        }
        settleAnnualTax(server, now);
    }

    private static void settleAnnualTax(MinecraftServer server, long now) {
        CorporateTaxPeriodSavedData periods = CorporateTaxPeriodSavedData.get(server);
        CorporateTaxAnnualSavedData annual = CorporateTaxAnnualSavedData.get(server);
        TaxLedgerSavedData ledger = TaxLedgerSavedData.get(server);
        for (var entry : annual.due(now)) {
            Company company = CompanySavedData.get(server).get(entry.companyId());
            if (company == null) continue;
            long profit = Math.max(0L, entry.revenue() - Math.min(entry.revenue(), entry.expenses()));
            long annualTax = Money.toMinorSaturated(Math.max(0L,
                    TaxRuleService.taxMinor(server, TaxType.CORPORATE_INCOME,
                            Money.toMinorSaturated(profit), now)));
            long prepaid = annual.prepaidFor(entry.companyId(), entry.yearEnd());
            long balance = annualTax - prepaid;
            long credit = balance < 0L ? -balance : 0L;
            TaxSubject subject = new TaxSubject(TaxType.CORPORATE_INCOME, company.companyId(), company.ownerUuid());
            String source = "corporate-annual:" + entry.companyId() + ":" + entry.yearEnd();
            if (balance > 0L) {
                if (ledger.findBySourceEvent(source) == null) {
                    long used = TaxCreditSavedData.get(server).consume(subject, entry.currencyId(), balance);
                    balance -= used;
                }
            }
            if (balance > 0L) {
                TaxPeriod period = new TaxPeriod(entry.yearStart(), entry.yearEnd(), entry.yearEnd(),
                        entry.yearEnd() + 15L * TICKS_PER_DAY);
                TaxService.createPeriodicBill(server,
                        subject, entry.currencyId(), balance, period, Money.toMinorSaturated(profit),
                        TaxRuleService.rateBasisPoints(server, TaxType.CORPORATE_INCOME, now),
                        source);
            }
            if (credit > 0L) TaxCreditSavedData.get(server).add(subject, entry.currencyId(), credit, source,
                    entry.yearStart(), entry.yearEnd(), now);
            annual.settle(entry, new CorporateTaxAnnualSavedData.Settlement(entry.companyId(), entry.yearEnd(),
                    annualTax, prepaid, Math.max(0L, balance), credit, now));
        }
    }

    private static long addSaturated(long left, long right) {
        return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
    }
}
