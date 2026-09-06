package com.ailudick.capitalismmod.economy.labor;

import java.util.Objects;

/** A posted position; hiring creates a separate employment contract. */
public record JobOffer(String id, String employerId, String role, int vacancies,
                       long dailyWageMinor, LaborSkill requiredSkill, int minimumSkill,
                       long postedAt, long closesAt, String region) {
    public JobOffer(String id, String employerId, String role, int vacancies, long dailyWageMinor,
                    LaborSkill requiredSkill, int minimumSkill, long postedAt, long closesAt) {
        this(id, employerId, role, vacancies, dailyWageMinor, requiredSkill, minimumSkill, postedAt, closesAt, "spawn");
    }
    public JobOffer {
        id = Objects.requireNonNullElse(id, "").trim(); employerId = Objects.requireNonNullElse(employerId, "").trim();
        role = Objects.requireNonNullElse(role, "").trim(); requiredSkill = requiredSkill == null ? LaborSkill.FOUNDATION : requiredSkill;
        region = Objects.requireNonNullElse(region, "spawn").trim();
        // Older world saves did not persist a region field; treat a missing or
        // blank value as the legacy default instead of failing world loading.
        if (region.isEmpty()) region = "spawn";
        if (id.isEmpty() || employerId.isEmpty() || role.isEmpty() || vacancies <= 0 || dailyWageMinor < 0L
                || minimumSkill < 0 || minimumSkill > 100 || postedAt < 0L || closesAt < postedAt) {
            throw new IllegalArgumentException("Invalid job offer");
        }
    }

    public JobOffer withVacancies(int value) {
        return new JobOffer(id, employerId, role, value, dailyWageMinor, requiredSkill, minimumSkill, postedAt, closesAt, region);
    }
}
