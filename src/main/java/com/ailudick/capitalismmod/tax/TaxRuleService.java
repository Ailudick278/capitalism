package com.ailudick.capitalismmod.tax;

import com.ailudick.capitalismmod.Config;
import net.minecraft.server.MinecraftServer;

/** Single access point for tax rates and deductions. */
public final class TaxRuleService {
    private TaxRuleService() {}

    public static TaxRule current(MinecraftServer server, TaxType type, long now) {
        TaxRule configured = TaxRuleSavedData.get(server).effective(type, now);
        if (configured != null) return configured;
        return defaults(type, now);
    }

    public static double rate(MinecraftServer server, TaxType type, long now) {
        return current(server, type, now).rate();
    }

    public static long taxMinor(MinecraftServer server, TaxType type, long baseMinor, long now) {
        TaxRule rule = current(server, type, now);
        long taxable = rule.taxableBase(baseMinor);
        if (taxable <= 0L || rule.rateBasisPoints() <= 0) return 0L;
        return taxable > Long.MAX_VALUE / rule.rateBasisPoints()
                ? Long.MAX_VALUE : (taxable * rule.rateBasisPoints()) / 10_000L;
    }

    public static TaxRule defaults(TaxType type, long now) {
        double rate = switch (type) {
            case LAND -> Config.LAND_TAX_RATE_PER_YEAR.get();
            case CORPORATE_INCOME -> Config.INCOME_TAX_RATE.get();
            case INDIVIDUAL_BUSINESS_INCOME -> Config.INDIVIDUAL_INCOME_TAX_RATE.get();
            default -> 0.0;
        };
        return new TaxRule(type, (int) Math.min(Integer.MAX_VALUE, Math.round(rate * 10_000.0)),
                0L, 0L, now, true);
    }

    public static int rateBasisPoints(MinecraftServer server, TaxType type, long now) {
        return current(server, type, now).rateBasisPoints();
    }
}
