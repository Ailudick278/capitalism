package com.ailudick.capitalismmod.economy.labor;

/** A durable match between one worker actor and one employer. */
public record EmploymentRecord(String id, String workerId, String employerId, String role,
                               long dailyWageMinor, long startedAt, long endedAt, boolean active) {
    public EmploymentRecord {
        if (id == null || id.isBlank() || workerId == null || workerId.isBlank()
                || employerId == null || employerId.isBlank() || role == null || role.isBlank()
                || dailyWageMinor < 0L || startedAt < 0L || endedAt < 0L) {
            throw new IllegalArgumentException("Invalid employment record");
        }
    }

    public EmploymentRecord end(long at) {
        return new EmploymentRecord(id, workerId, employerId, role, dailyWageMinor, startedAt, Math.max(startedAt, at), false);
    }
}
