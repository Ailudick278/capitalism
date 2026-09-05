package com.ailudick.capitalismmod.event;

import com.ailudick.capitalismmod.CapitalismMod;
import com.ailudick.capitalismmod.land.LandClaim;
import com.ailudick.capitalismmod.land.LandOperationLogSavedData;
import com.ailudick.capitalismmod.land.LandSavedData;
import com.ailudick.capitalismmod.tax.TaxDelinquentEvent;
import com.ailudick.capitalismmod.tax.TaxSettledEvent;
import com.ailudick.capitalismmod.tax.TaxType;
import com.ailudick.capitalismmod.tax.TaxService;
import com.ailudick.capitalismmod.currency.Money;
import com.ailudick.capitalismmod.land.LandAuctionSavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Adapter between tax outcomes and land enforcement. It does not calculate tax. */
@EventBusSubscriber(modid = CapitalismMod.MODID)
public final class LandTaxEnforcementHandler {
    private LandTaxEnforcementHandler() {}

    @SubscribeEvent
    public static void onDelinquent(TaxDelinquentEvent event) {
        if (event.bill().subject().type() != TaxType.LAND) return;
        LandClaim claim = LandSavedData.get(event.server()).get(event.bill().subject().subjectId());
        if (claim == null) return;
        long outstanding = TaxService.outstanding(event.server(), event.bill().subject());
        LandSavedData.get(event.server()).put(claim.withTaxSchedule(
                Money.toMajorCeiling(outstanding), claim.taxDueAt(), claim.taxGraceUntil()));
        LandOperationLogSavedData.get(event.server()).record(event.gameTime(), claim.ownerUuid(),
                "税务系统确认土地欠税，土地系统进入执行流程", claim.dimension(), claim.chunkX(), claim.chunkZ());
    }

    @SubscribeEvent
    public static void onSettled(TaxSettledEvent event) {
        if (event.bill().subject().type() != TaxType.LAND) return;
        LandClaim claim = LandSavedData.get(event.server()).get(event.bill().subject().subjectId());
        if (claim == null) return;
        if (TaxService.outstanding(event.server(), event.bill().subject()) <= 0L
                && LandAuctionSavedData.get(event.server()).get(claim.id()) == null) {
            LandSavedData.get(event.server()).put(claim.withTaxSchedule(0L, 0L, 0L));
        }
        LandOperationLogSavedData.get(event.server()).record(event.server().overworld().getGameTime(), claim.ownerUuid(),
                "土地税务欠款已结清", claim.dimension(), claim.chunkX(), claim.chunkZ());
    }
}
