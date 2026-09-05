package com.ailudick.capitalismmod.company;

import com.ailudick.capitalismmod.Config;
import com.ailudick.capitalismmod.util.EconomyMath;
import net.minecraft.world.entity.player.Player;

import java.util.Map;

/**
 * Data-driven company economics. Income and input/output recipes come from
 * {@link Industries} (config), except finance which earns interest on its treasury.
 */
public final class CompanyEconomy {
    private CompanyEconomy() {
    }

    /** USD income for one tick. Production recipes describe one batch per cycle. */
    public static long incomePerTick(Company company, Player owner) {
        if (company == null || company.registeredCapital() <= 0) {
            return 0L;
        }
        if ("finance".equals(company.type())) {
            return financeIncome(company);
        }
        IndustrySpec spec = Industries.byId(company.type());
        if (spec == null) {
            return 0L;
        }
        return recipe(company).income();
    }

    /** Commodities produced by one production batch. */
    public static Map<String, Integer> outputs(Company company) {
        return recipe(company).outputs();
    }

    /** Commodities consumed by one production batch. */
    public static Map<String, Integer> inputs(Company company) {
        return recipe(company).inputs();
    }

    public static ProductionRecipe recipe(Company company) {
        IndustrySpec spec = company == null ? null : Industries.byId(company.type());
        return spec == null ? new ProductionRecipe("default", Map.of(), Map.of(), 0L) : spec.recipe(company.productionRecipe());
    }

    private static long financeIncome(Company company) {
        long treasury = company.treasuryOf("usd");
        if (treasury <= 0) {
            return 0L;
        }
        double cyclesPerDay = Math.max(1.0, 24000.0 / Config.COMPANY_PRODUCTION_CYCLE_TICKS.get());
        double income = treasury * Config.FINANCE_RATE_PER_YEAR.get() / 365.0 / cyclesPerDay;
        if (!Double.isFinite(income) || income >= Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }
        return Math.max(0L, (long) income);
    }
}
