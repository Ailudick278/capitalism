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
        int foundation = Math.min(100, Math.max(skill(LaborSkill.FOUNDATION), 45 + Math.max(0, education) / 2));
        next.put(LaborSkill.FOUNDATION, foundation);
        return new LaborProfile(actorId, next, Math.max(0, Math.min(100, health)), reservationWageMinor);
    }
}
