package com.ailudick.capitalismmod.economy;

/** Pure validation rules for durable settlement journal entries. */
public final class SettlementJournalRules {
    private SettlementJournalRules() {
    }

    public static boolean validFinancial(String transactionId, String instrument, String phase,
                                         String status, long amountMinor, long gameTime) {
        return nonBlank(transactionId) && nonBlank(instrument) && nonBlank(phase)
                && validStatus(status) && amountMinor >= 0L && gameTime >= 0L;
    }

    public static boolean validDaily(long day, String phase, String status, long gameTime) {
        return day >= 0L && nonBlank(phase) && validStatus(status) && gameTime >= 0L;
    }

    private static boolean validStatus(String status) {
        return "started".equals(status) || "completed".equals(status);
    }

    private static boolean nonBlank(String value) {
        return value != null && !value.isBlank();
    }
}
