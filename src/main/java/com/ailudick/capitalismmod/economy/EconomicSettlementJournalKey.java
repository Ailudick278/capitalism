package com.ailudick.capitalismmod.economy;

/** Stable identifier format shared by settlement journal writers and readers. */
public final class EconomicSettlementJournalKey {
    private EconomicSettlementJournalKey() {}

    public static String of(long day, String phase) {
        return day + ":" + (phase == null ? "" : phase.trim());
    }
}
