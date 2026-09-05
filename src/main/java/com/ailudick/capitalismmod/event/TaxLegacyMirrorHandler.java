package com.ailudick.capitalismmod.event;

import com.ailudick.capitalismmod.CapitalismMod;
import com.ailudick.capitalismmod.business.IndividualBusiness;
import com.ailudick.capitalismmod.business.IndividualBusinessSavedData;
import com.ailudick.capitalismmod.company.Company;
import com.ailudick.capitalismmod.company.CompanySavedData;
import com.ailudick.capitalismmod.currency.Money;
import com.ailudick.capitalismmod.tax.TaxDelinquentEvent;
import com.ailudick.capitalismmod.tax.TaxService;
import com.ailudick.capitalismmod.tax.TaxSettledEvent;
import com.ailudick.capitalismmod.tax.TaxSubject;
import com.ailudick.capitalismmod.tax.TaxType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Keeps legacy company/business mirrors derived from the unified tax ledger. */
@EventBusSubscriber(modid = CapitalismMod.MODID)
public final class TaxLegacyMirrorHandler {
    private TaxLegacyMirrorHandler() {}

    @SubscribeEvent
    public static void onDelinquent(TaxDelinquentEvent event) {
        sync(event.server(), event.bill().subject());
    }

    @SubscribeEvent
    public static void onSettled(TaxSettledEvent event) {
        sync(event.server(), event.bill().subject());
    }

    private static void sync(net.minecraft.server.MinecraftServer server, TaxSubject subject) {
        long outstandingMajor = Money.toMajorCeiling(TaxService.outstanding(server, subject));
        if (subject.type() == TaxType.CORPORATE_INCOME) {
            Company company = CompanySavedData.get(server).get(subject.subjectId());
            if (company != null && subject.taxpayerUuid().equals(company.ownerUuid())) {
                CompanySavedData.get(server).put(company.withTaxOwed(outstandingMajor));
            }
        } else if (subject.type() == TaxType.INDIVIDUAL_BUSINESS_INCOME) {
            IndividualBusiness business = IndividualBusinessSavedData.get(server).findByBusinessId(subject.subjectId());
            if (business != null && subject.taxpayerUuid().equals(business.ownerUuid())) {
                IndividualBusinessSavedData.get(server).put(
                        business.afterSettlement(business.account(), outstandingMajor));
            }
        }
    }
}
