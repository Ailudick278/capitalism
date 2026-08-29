package com.ailudick.capitalismmod.company;

import com.ailudick.capitalismmod.util.EconomyMath;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Data-driven company economics. Income and input/output recipes come from
 * {@link Industries} (config), except finance which earns interest on its treasury.
 */
public final class CompanyEconomy {
    /** Finance interest: treasury(usd) * level / 200 = 0.5% * level per tick. */
    private static final long FINANCE_INTEREST_DIVISOR = 200L;

    private CompanyEconomy() {
    }

    /** USD income for one tick. Finance uses treasury interest; others use data-driven income × level. */
    public static long incomePerTick(Company company, Player owner) {
        if ("finance".equals(company.type())) {
            return financeIncome(company);
        }
        IndustrySpec spec = Industries.byId(company.type());
        if (spec == null) {
            return 0L;
        }
        return EconomyMath.multiply(spec.income(), company.level());
    }

    /** Cost (USD) to upgrade from {@code level} to {@code level + 1}: 1000 * level^2. */
    public static long upgradeCost(int level) {
        long squared = EconomyMath.multiply(level, level);
        if (squared < 0) {
            return Long.MAX_VALUE;
        }
        return EconomyMath.multiply(1000L, squared);
    }

    /** Commodities produced per tick (item id -> count), scaled by level. */
    public static Map<String, Integer> outputs(Company company) {
        IndustrySpec spec = Industries.byId(company.type());
        return spec == null ? Map.of() : scale(spec.outputs(), company.level());
    }

    /** Commodities consumed per tick (item id -> count), scaled by level. */
    public static Map<String, Integer> inputs(Company company) {
        IndustrySpec spec = Industries.byId(company.type());
        return spec == null ? Map.of() : scale(spec.inputs(), company.level());
    }

    private static Map<String, Integer> scale(Map<String, Integer> perLevel, int level) {
        Map<String, Integer> result = new HashMap<>();
        for (Map.Entry<String, Integer> entry : perLevel.entrySet()) {
            result.put(entry.getKey(), entry.getValue() * level);
        }
        return result;
    }

    private static long financeIncome(Company company) {
        long scaled = EconomyMath.multiply(company.treasuryOf("usd"), company.level());
        if (scaled <= 0) {
            return 0L;
        }
        return scaled / FINANCE_INTEREST_DIVISOR;
    }
}
