package com.ailudick.capitalismmod.economy.contract;

/** Pure lifecycle rules for the shared economic contract state machine. */
public final class ContractEconomics {
    private ContractEconomics() {}

    public static boolean canTransition(ContractStatus from, ContractStatus to) {
        if (from == null || to == null || from == to) return false;
        return switch (from) {
            case DRAFT -> to == ContractStatus.OFFERED || to == ContractStatus.CANCELLED;
            case OFFERED -> to == ContractStatus.ACTIVE || to == ContractStatus.CANCELLED
                    || to == ContractStatus.BREACHED || to == ContractStatus.EXPIRED;
            case ACTIVE -> to == ContractStatus.COMPLETED || to == ContractStatus.CANCELLED
                    || to == ContractStatus.BREACHED || to == ContractStatus.EXPIRED;
            case COMPLETED, CANCELLED, BREACHED, EXPIRED -> false;
        };
    }
}
