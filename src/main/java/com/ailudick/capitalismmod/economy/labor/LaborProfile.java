package com.ailudick.capitalismmod.economy.labor;

import java.util.EnumMap;
import java.util.Map;

/** Portable skill and participation profile for a resident or player household. */
public record LaborProfile(String actorId, Map<LaborSkill, Integer> skills,
                           int participation, long reservationWageMinor) {
    public LaborProfile {
        actorId = actorId == null ? "" : actorId.trim();
        if (actorId.isEmpty() || participation < 0 || participation > 100 || reservationWageMinor < 0L) {
            throw new IllegalArgumentException("Invalid labor profile");
        }
        EnumMap<LaborSkill, Integer> copy = new EnumMap<>(LaborSkill.class);
        if (skills != null) skills.forEach((skill, value) -> {
            if (skill != null) copy.put(skill, Math.max(0, Math.min(100, value == null ? 0 : value)));
        });
        skills = Map.copyOf(copy);
    }

    public int skill(LaborSkill skill) { return skills.getOrDefault(skill, 0); }

    public int averageSkill() {
        return skills.isEmpty() ? 0 : (int) skills.values().stream().mapToInt(Integer::intValue).average().orElse(0);
    }

    public LaborProfile withHealthAndEducation(int health, int education) {
        EnumMap<LaborSkill, Integer> next = new EnumMap<>(LaborSkill.class);
        next.putAll(skills);
        int boundedHealth = Math.max(0, Math.min(100, health));
        int boundedEducation = Math.max(0, Math.min(100, education));
        int foundation = Math.min(100, Math.max(skill(LaborSkill.FOUNDATION), 45 + boundedEducation / 2));
        next.put(LaborSkill.FOUNDATION, foundation);
        return new LaborProfile(actorId, next, boundedHealth, reservationWageMinor);
    }

    /** Reservation wage adjusted at the hiring decision without mutating the stored baseline. */
    public long effectiveReservationWageMinor() {
        if (reservationWageMinor <= 0L) return 0L;
        long education = Math.max(0L, Math.min(100L, (long) (skill(LaborSkill.FOUNDATION) - 45) * 2L));
        long multiplier = 70L + participation * 20L / 100L + education * 30L / 100L;
        return reservationWageMinor > Long.MAX_VALUE / multiplier
                ? Long.MAX_VALUE : reservationWageMinor * multiplier / 100L;
    }
}
