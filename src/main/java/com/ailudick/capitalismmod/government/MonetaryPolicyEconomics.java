package com.ailudick.capitalismmod.government;

/** Central-policy rate arithmetic shared by deposits and loans. */
public final class MonetaryPolicyEconomics {
    private MonetaryPolicyEconomics() {}

    public static double adjustedAnnualRate(double configuredRate, int policyBasisPoints) {
        if (!Double.isFinite(configuredRate)) return 0.0;
        double adjusted = configuredRate + Math.max(-10000, Math.min(20000, policyBasisPoints)) / 10000.0;
        return Math.min(2.0, Math.max(0.0, adjusted));
    }
}
