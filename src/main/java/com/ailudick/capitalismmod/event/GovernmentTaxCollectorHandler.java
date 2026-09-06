package com.ailudick.capitalismmod.event;

import com.ailudick.capitalismmod.CapitalismMod;
import com.ailudick.capitalismmod.Config;
import com.ailudick.capitalismmod.calendar.PerpetualCalendar;
import com.ailudick.capitalismmod.currency.Currencies;
import com.ailudick.capitalismmod.currency.ExchangeRates;
import com.ailudick.capitalismmod.government.GovernmentPolicySavedData;
import com.ailudick.capitalismmod.tax.TaxSettledEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Converts fully settled tax bills into durable government revenue. */
@EventBusSubscriber(modid = CapitalismMod.MODID)
public final class GovernmentTaxCollectorHandler {
    private GovernmentTaxCollectorHandler() {}

    @SubscribeEvent
    public static void onTaxSettled(TaxSettledEvent event) {
        var bill = event.bill();
        if (bill == null || !bill.paid() || !Currencies.exists(bill.currencyId())) return;
        long converted = ExchangeRates.convert(bill.totalDue(),
                Currencies.byId(bill.currencyId()), Config.defaultCurrency());
        if (converted <= 0L) return;
        GovernmentPolicySavedData.get(event.server()).collectTax(
                bill.id(), event.server().overworld().getGameTime() / PerpetualCalendar.TICKS_PER_DAY,
                bill.subject().subjectId(), bill.subject().type().name(), bill.currencyId(),
                bill.totalDue(), converted);
    }
}
