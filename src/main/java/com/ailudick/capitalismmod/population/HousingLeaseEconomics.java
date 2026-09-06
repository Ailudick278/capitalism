package com.ailudick.capitalismmod.population;

/** Pure rent settlement and arrears state transitions used by the persistent lease store. */
public final class HousingLeaseEconomics {
    public record Settlement(long totalDue, long paid, long arrears, int missedDays, long noticeDay) {
        public String status() {
            if (missedDays >= 90) return "termination_eligible";
            if (missedDays >= 30) return "notice";
            if (missedDays > 0) return "grace";
            return "current";
        }
    }

    private HousingLeaseEconomics() {}

    public static Settlement settle(long priorArrears, int priorMissedDays, long priorNoticeDay,
                                    long day, long currentDue, long available) {
        long totalDue = add(Math.max(0L, priorArrears), Math.max(0L, currentDue));
        long paid = Math.max(0L, Math.min(Math.max(0L, available), totalDue));
        long arrears = totalDue - paid;
        int missedDays = paid >= totalDue ? 0 : Math.min(10000, Math.max(0, priorMissedDays) + 1);
        long noticeDay = missedDays >= 30
                ? (priorNoticeDay >= 0L ? priorNoticeDay : day) : -1L;
        return new Settlement(totalDue, paid, arrears, missedDays, noticeDay);
    }

    private static long add(long left, long right) {
        try { return Math.addExact(left, right); } catch (ArithmeticException e) { return Long.MAX_VALUE; }
    }
}
