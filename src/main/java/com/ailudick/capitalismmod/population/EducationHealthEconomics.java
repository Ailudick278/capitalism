package com.ailudick.capitalismmod.population;

/** Deterministic human-capital rules for the regional simulation. */
public final class EducationHealthEconomics {
    private EducationHealthEconomics() {}

    public static int nextEducation(int current, int schoolCoverage, int workingAge) {
        int gain = workingAge <= 0 ? schoolCoverage / 50 : schoolCoverage / 25;
        return clamp(current + Math.max(0, gain), 0, 100);
    }

    public static int nextHealth(int current, int clinicCoverage, int spendingWelfare) {
        return nextHealth(current, clinicCoverage, spendingWelfare, 100);
    }

    public static int nextHealth(int current, int clinicCoverage, int spendingWelfare, int foodSecurity) {
        int change = clinicCoverage / 25 + spendingWelfare / 50 + Math.max(0, Math.min(100, foodSecurity)) / 50 - 4;
        return clamp(current + change, 0, 100);
    }

    public static int laborParticipation(int health, int workingAge) {
        return workingAge <= 0 ? 0 : clamp(health, 0, 100);
    }

    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
}
