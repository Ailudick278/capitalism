package com.ailudick.capitalismmod.land;

/** Deterministic regional land-demand multiplier used by reference valuation. */
public final class LandDemandEconomics {
    private LandDemandEconomics() {}

    /**
     * Population creates demand for scarce locations; public services make
     * that location more productive. Both effects are bounded so a single
     * region cannot make prices overflow or become unplayable.
     */
    public static double multiplier(int residents, int publicServiceScore) {
        return multiplier(residents, publicServiceScore, 0);
    }

    public static double multiplier(int residents, int publicServiceScore, int businessSites) {
        return multiplier(residents, publicServiceScore, businessSites, 50);
    }

    public static double multiplier(int residents, int publicServiceScore, int businessSites,
                                    int accessScore) {
        double populationPressure = Math.max(0.0, Math.min(0.50,
                Math.max(0, residents) / 10_000.0));
        double servicePremium = Math.max(-0.20, Math.min(0.20,
                (Math.max(0, Math.min(100, publicServiceScore)) - 50) / 250.0));
        double businessPremium = Math.max(0.0, Math.min(0.25, Math.max(0, businessSites) * 0.05));
        double accessPremium = Math.max(-0.20, Math.min(0.20,
                (Math.max(0, Math.min(100, accessScore)) - 50) / 250.0));
        return 1.0 + populationPressure + servicePremium + businessPremium + accessPremium;
    }

    /** Smoothly incorporates observed market prices without allowing a spike to dominate. */
    public static double marketMultiplier(long marketAverage, double baselineValue) {
        if (marketAverage <= 0L || !Double.isFinite(baselineValue) || baselineValue <= 0.0) return 1.0;
        return Math.max(0.75, Math.min(1.50, 0.75 + marketAverage / baselineValue * 0.25));
    }
}
