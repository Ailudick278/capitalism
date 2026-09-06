package com.ailudick.capitalismmod.economy;

/** Pure state-transition rules shared by persistent settlement journals. */
public final class SettlementPhaseState {
    private SettlementPhaseState() {}

    /** Completed is terminal; a recovery pass must not downgrade it to started. */
    public static boolean preservesCompleted(String previousStatus, String nextStatus) {
        return "completed".equals(previousStatus) && "started".equals(nextStatus);
    }
}
