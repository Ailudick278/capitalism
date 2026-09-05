package com.ailudick.capitalismmod.company;

/**
 * A read-only statistical size profile, not a company level or upgrade path.
 * The thresholds mirror the widely used EU SME bands, while the game's USD
 * values are treated as a reporting proxy rather than a legal determination.
 */
public enum CompanySizeProfile {
    MICRO("micro"),
    SMALL("small"),
    MEDIUM("medium"),
    LARGE("large");

    private static final long MILLION = 1_000_000L;
    private final String label;

    CompanySizeProfile(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /**
     * Classifies an individual company using employees and either annual
     * turnover or total assets. This method has no gameplay side effects.
     */
    public static CompanySizeProfile classify(int employees, long annualRevenue, long assets) {
        long revenue = Math.max(0L, annualRevenue);
        long balanceSheet = Math.max(0L, assets);
        int staff = Math.max(0, employees);
        if (staff < 10 && within(revenue, balanceSheet, 2L * MILLION)) return MICRO;
        if (staff < 50 && within(revenue, balanceSheet, 10L * MILLION)) return SMALL;
        if (staff < 250 && within(revenue, balanceSheet, 50L * MILLION, 43L * MILLION)) return MEDIUM;
        return LARGE;
    }

    private static boolean within(long revenue, long assets, long turnoverLimit) {
        return revenue <= turnoverLimit || assets <= turnoverLimit;
    }

    private static boolean within(long revenue, long assets, long turnoverLimit, long assetLimit) {
        return revenue <= turnoverLimit || assets <= assetLimit;
    }
}
