package com.ailudick.capitalismmod.company;

import com.ailudick.capitalismmod.Config;
import com.ailudick.capitalismmod.util.EconomyMath;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Data-driven company economics. Income and input/output recipes come from
 * {@link Industries} (config), except finance which earns interest on its treasury.
 */
public final class CompanyEconomy {
    /** One production cycle is 30 seconds, or 1/40 of a Minecraft day. */
    private static final double PRODUCTION_CYCLES_PER_DAY = 40.0;

    private CompanyEconomy() {
    }

    /** USD income for one tick. Finance uses treasury interest; others use data-driven income × level. */
    public static long incomePerTick(Company company, Player owner) {
        if (company.level() <= 0) {
            return 0L;
        }
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
        if (level <= 0) {
            return -1L;
        }
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
        if (level <= 0) {
            return result;
        }
        for (Map.Entry<String, Integer> entry : perLevel.entrySet()) {
            int amount = entry.getValue();
            if (amount <= 0) {
                continue;
            }
            long scaled = (long) amount * level;
            result.put(entry.getKey(), (int) Math.min(Integer.MAX_VALUE, scaled));
        }
        return result;
    }

    private static long financeIncome(Company company) {
        long treasury = company.treasuryOf("usd");
        if (treasury <= 0) {
            return 0L;
        }
        double income = treasury * Config.FINANCE_RATE_PER_YEAR.get() / 365.0
                / PRODUCTION_CYCLES_PER_DAY * company.level();
        if (!Double.isFinite(income) || income >= Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }
        return Math.max(0L, (long) income);
    }
}
