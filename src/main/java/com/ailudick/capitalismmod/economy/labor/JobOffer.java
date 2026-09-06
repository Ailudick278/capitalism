package com.ailudick.capitalismmod.economy.labor;

import java.util.Objects;

/** A posted position; hiring creates a separate employment contract. */
public record JobOffer(String id, String employerId, String role, int vacancies,
                       long dailyWageMinor, LaborSkill requiredSkill, int minimumSkill,
                       long postedAt, long closesAt) {
    public JobOffer {
        id = Objects.requireNonNullElse(id, "").trim(); employerId = Objects.requireNonNullElse(employerId, "").trim();
        role = Objects.requireNonNullElse(role, "").trim(); requiredSkill = requiredSkill == null ? LaborSkill.FOUNDATION : requiredSkill;
        if (id.isEmpty() || employerId.isEmpty() || role.isEmpty() || vacancies <= 0 || dailyWageMinor < 0L
                || minimumSkill < 0 || minimumSkill > 100 || postedAt < 0L || closesAt < postedAt) {
            throw new IllegalArgumentException("Invalid job offer");
        }
    }
}
