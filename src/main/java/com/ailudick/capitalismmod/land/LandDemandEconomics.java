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
        double populationPressure = Math.max(0.0, Math.min(0.50,
                Math.max(0, residents) / 10_000.0));
        double servicePremium = Math.max(-0.20, Math.min(0.20,
                (Math.max(0, Math.min(100, publicServiceScore)) - 50) / 250.0));
        return 1.0 + populationPressure + servicePremium;
    }
}
