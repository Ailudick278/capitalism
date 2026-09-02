package com.ailudick.capitalismmod.event;

/**
 * Legacy marker retained for source compatibility. Peer-loan maturities are
 * settled by {@link EconomySettlementTickHandler}; this class must not be
 * registered as an empty event subscriber.
 */
@Deprecated(forRemoval = false)
public final class PeerLoanTickHandler {
    private PeerLoanTickHandler() {
    }
}
