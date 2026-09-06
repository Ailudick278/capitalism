package com.ailudick.capitalismmod.loan;

import com.ailudick.capitalismmod.company.Company;
import com.ailudick.capitalismmod.company.CompanyFinancialSnapshot;
import com.ailudick.capitalismmod.util.EconomyMath;
import net.minecraft.server.MinecraftServer;

/** Read-only conservative collateral view used to prepare secured lending. */
public record CompanyCollateralAssessment(long inventoryValue, long equipmentValue,
                                          long eligibleInventory, long eligibleEquipment,
                                          long eligibleCollateral, long existingDebt,
                                          long indicativeHeadroom) {
    private static final long INVENTORY_HAIRCUT_PERCENT = 50L;
    private static final long EQUIPMENT_HAIRCUT_PERCENT = 60L;

    public static CompanyCollateralAssessment from(MinecraftServer server, Company company) {
        CompanyFinancialSnapshot snapshot = CompanyFinancialSnapshot.from(server, company);
        long eligibleInventory = percentage(snapshot.inventory(), INVENTORY_HAIRCUT_PERCENT);
        long eligibleEquipment = percentage(snapshot.equipment(), EQUIPMENT_HAIRCUT_PERCENT);
        long eligible = add(eligibleInventory, eligibleEquipment);
        long debt = Math.max(0L, snapshot.loanLiabilities());
        long headroom = Math.max(0L, eligible - debt);
        return new CompanyCollateralAssessment(snapshot.inventory(), snapshot.equipment(),
                eligibleInventory, eligibleEquipment, eligible, debt, headroom);
    }

    private static long percentage(long amount, long percent) {
        if (amount <= 0L || percent <= 0L) return 0L;
        if (amount > Long.MAX_VALUE / percent) return Long.MAX_VALUE;
        return amount * percent / 100L;
    }

    private static long add(long left, long right) {
        long result = EconomyMath.add(Math.max(0L, left), Math.max(0L, right));
        return result < 0L ? Long.MAX_VALUE : result;
    }
}
