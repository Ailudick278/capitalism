package com.ailudick.capitalismmod.company;

import com.ailudick.capitalismmod.market.InventoryOwner;
import com.ailudick.capitalismmod.market.WarehouseSavedData;
import com.ailudick.capitalismmod.market.CommoditySavedData;
import com.ailudick.capitalismmod.loan.CompanyLoan;
import com.ailudick.capitalismmod.loan.CompanyLoanSavedData;
import com.ailudick.capitalismmod.util.EconomyMath;
import net.minecraft.server.MinecraftServer;

import java.util.Map;

/**
 * Read-only balance-sheet snapshot derived from persisted company resources.
 * Values are major USD units and are intentionally not stored separately, so
 * the statement cannot drift away from the cash, warehouse or equipment data.
 */
public record CompanyFinancialSnapshot(long cash, long inventory, long equipment,
                                       long assets, long taxLiabilities, long loanLiabilities,
                                       long liabilities,
                                       long equity) {
    public static CompanyFinancialSnapshot from(MinecraftServer server, Company company) {
        if (server == null || company == null) {
            return new CompanyFinancialSnapshot(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L);
        }
        long cash = Math.max(0L, company.treasuryOf("usd"));
        CommoditySavedData market = CommoditySavedData.get(server);
        long inventory = 0L;
        for (Map.Entry<String, Integer> entry : WarehouseSavedData.get(server)
                .storage(InventoryOwner.company(company.companyId())).entrySet()) {
            long unitPrice = Math.max(0L, market.price(entry.getKey()));
            inventory = add(inventory, multiply(unitPrice, Math.max(0L, entry.getValue())));
        }

        long equipment = 0L;
        for (CompanyEquipmentSavedData.Equipment installed
                : CompanyEquipmentSavedData.get(server).all(company.companyId()).values()) {
            MachineType type = MachineType.parse(installed.machineType());
            if (type == null || installed.count() <= 0 || installed.condition() <= 0) continue;
            long gross = multiply(type.purchasePrice(), installed.count());
            equipment = add(equipment, EconomyMath.multiply(gross,
                    Math.min(100L, installed.condition())) / 100L);
        }

        long assets = add(add(cash, inventory), equipment);
        long taxLiabilities = Math.max(0L, company.taxOwed());
        long loanLiabilities = 0L;
        for (CompanyLoan loan : CompanyLoanSavedData.get(server).forCompany(company.companyId())) {
            loanLiabilities = add(loanLiabilities, loan.principal());
            loanLiabilities = add(loanLiabilities, loan.interestDue());
        }
        long liabilities = add(taxLiabilities, loanLiabilities);
        return new CompanyFinancialSnapshot(cash, inventory, equipment, assets,
                taxLiabilities, loanLiabilities, liabilities, assets - liabilities);
    }

    private static long add(long left, long right) {
        long result = EconomyMath.add(Math.max(0L, left), Math.max(0L, right));
        return result < 0L ? Long.MAX_VALUE : result;
    }

    private static long multiply(long left, long right) {
        if (left <= 0L || right <= 0L) return 0L;
        long result = EconomyMath.multiply(left, right);
        return result < 0L ? Long.MAX_VALUE : result;
    }
}
