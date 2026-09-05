package com.ailudick.capitalismmod.company;

import com.ailudick.capitalismmod.market.InventoryOwner;
import com.ailudick.capitalismmod.market.WarehouseSavedData;
import com.ailudick.capitalismmod.market.CommoditySavedData;
import com.ailudick.capitalismmod.loan.CompanyLoan;
import com.ailudick.capitalismmod.loan.CompanyLoanSavedData;
import com.ailudick.capitalismmod.currency.Money;
import com.ailudick.capitalismmod.tax.TaxService;
import com.ailudick.capitalismmod.tax.TaxSubject;
import com.ailudick.capitalismmod.tax.TaxType;
import com.ailudick.capitalismmod.util.EconomyMath;
import net.minecraft.server.MinecraftServer;

import java.util.Map;

/**
 * Read-only balance-sheet snapshot derived from persisted company resources.
 * Values are major USD units and are intentionally not stored separately, so
 * the statement cannot drift away from the cash, warehouse or equipment data.
 */
public record CompanyFinancialSnapshot(long cash, long inventory, long equipment,
                                       long assets, long taxLiabilities, long loanLiabilities, long payrollLiabilities,
                                       long liabilities,
                                       long equity) {
    public static CompanyFinancialSnapshot from(MinecraftServer server, Company company) {
        if (server == null || company == null) {
            return new CompanyFinancialSnapshot(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L);
        }
        long cash = Math.max(0L, company.treasuryOf("usd"));
        CommoditySavedData market = CommoditySavedData.get(server);
        long inventory = 0L;
        CompanyInventoryCostSavedData inventoryCosts = CompanyInventoryCostSavedData.get(server);
        for (Map.Entry<String, Integer> entry : WarehouseSavedData.get(server)
                .storage(InventoryOwner.company(company.companyId())).entrySet()) {
            int quantity = Math.max(0, entry.getValue());
            long unitPrice = Math.max(0L, market.price(entry.getKey()));
            CompanyInventoryCostSavedData.CostLayer layer = inventoryCosts.layer(company.companyId(), entry.getKey());
            int tracked = layer == null ? 0 : Math.min(quantity, Math.max(0, layer.quantity()));
            long trackedCost = layer == null ? 0L : proportionalCost(layer.totalCost(), tracked, layer.quantity());
            long trackedMarketValue = multiply(unitPrice, tracked);
            inventory = add(inventory, Math.min(trackedCost, trackedMarketValue));
            inventory = add(inventory, multiply(unitPrice, quantity - tracked));
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
        long ledgerTax = Money.toMajorCeiling(TaxService.outstanding(server,
                new TaxSubject(TaxType.CORPORATE_INCOME, company.companyId(), company.ownerUuid())));
        // Keep the legacy mirror as a compatibility floor for old saves while
        // using the unified tax ledger as the authoritative current balance.
        long taxLiabilities = Math.max(Math.max(0L, company.taxOwed()), ledgerTax);
        long loanLiabilities = 0L;
        for (CompanyLoan loan : CompanyLoanSavedData.get(server).forCompany(company.companyId())) {
            loanLiabilities = add(loanLiabilities, loan.principal());
            loanLiabilities = add(loanLiabilities, loan.interestDue());
        }
        long payrollLiabilities = CompanyPayrollSavedData.get(server).unpaid(company.companyId());
        long liabilities = add(add(taxLiabilities, loanLiabilities), payrollLiabilities);
        return new CompanyFinancialSnapshot(cash, inventory, equipment, assets,
                taxLiabilities, loanLiabilities, payrollLiabilities, liabilities, assets - liabilities);
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

    private static long proportionalCost(long total, int quantity, int denominator) {
        if (total <= 0L || quantity <= 0 || denominator <= 0) return 0L;
        if (total == Long.MAX_VALUE) return Long.MAX_VALUE;
        long whole = total / denominator;
        long remainder = total % denominator;
        long result = EconomyMath.multiply(whole, quantity);
        result = EconomyMath.add(result, EconomyMath.multiply(remainder, quantity) / denominator);
        return result < 0L ? Long.MAX_VALUE : result;
    }
}
