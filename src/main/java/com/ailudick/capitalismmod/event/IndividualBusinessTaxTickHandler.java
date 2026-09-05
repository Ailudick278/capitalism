package com.ailudick.capitalismmod.event;

import com.ailudick.capitalismmod.CapitalismMod;
import com.ailudick.capitalismmod.business.IndividualBusiness;
import com.ailudick.capitalismmod.business.IndividualBusinessSavedData;
import com.ailudick.capitalismmod.currency.Money;
import com.ailudick.capitalismmod.tax.IndividualTaxPeriodSavedData;
import com.ailudick.capitalismmod.tax.TaxLedgerSavedData;
import com.ailudick.capitalismmod.tax.TaxPeriod;
import com.ailudick.capitalismmod.tax.TaxRuleService;
import com.ailudick.capitalismmod.tax.TaxService;
import com.ailudick.capitalismmod.tax.TaxSubject;
import com.ailudick.capitalismmod.tax.TaxType;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Closes individual-business periods and taxes profit after deductible costs. */
@EventBusSubscriber(modid = CapitalismMod.MODID)
public final class IndividualBusinessTaxTickHandler {
    private static final long TICKS_PER_DAY = 24000L;
    private IndividualBusinessTaxTickHandler() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        long now = server.overworld().getGameTime();
        if (now <= 0L || now % TICKS_PER_DAY != 0L) return;

        IndividualTaxPeriodSavedData periods = IndividualTaxPeriodSavedData.get(server);
        TaxLedgerSavedData ledger = TaxLedgerSavedData.get(server);
        for (var entry : periods.due(now)) {
            IndividualBusiness business = IndividualBusinessSavedData.get(server).get(entry.ownerUuid());
            if (business == null || !business.businessId().equals(entry.businessId())) {
                periods.remove(entry);
                continue;
            }
            long profit = Math.max(0L, entry.revenue() - Math.min(entry.revenue(), entry.expenses()));
            long taxableBase = Money.toMinorSaturated(profit);
            long taxAmount = TaxRuleService.taxMinor(server, TaxType.INDIVIDUAL_BUSINESS_INCOME,
                    taxableBase, now);
            String source = "individual-quarter:" + entry.businessId() + ":" + entry.periodEnd();
            if (taxAmount > 0L && ledger.findBySourceEvent(source) == null) {
                TaxPeriod period = new TaxPeriod(entry.periodStart(), entry.periodEnd(), entry.periodEnd(),
                        entry.periodEnd() + 15L * TICKS_PER_DAY);
                TaxSubject subject = new TaxSubject(TaxType.INDIVIDUAL_BUSINESS_INCOME,
                        entry.businessId(), entry.ownerUuid());
                TaxService.createPeriodicBill(server, subject, entry.currencyId(), taxAmount, period,
                        taxableBase, TaxRuleService.rateBasisPoints(server,
                        TaxType.INDIVIDUAL_BUSINESS_INCOME, now), source);
            }
            periods.close(entry.businessId(), entry.periodEnd());
        }
    }
}
