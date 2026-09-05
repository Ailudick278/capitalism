package com.ailudick.capitalismmod.tax;

import net.minecraft.server.MinecraftServer;

import java.util.UUID;

/** Creates an auditable transaction-tax bill after a completed sale. */
public final class TaxTransactionService {
    private TaxTransactionService() {}

    public static TaxBill assess(MinecraftServer server, UUID taxpayerUuid, String currencyId,
                                 long grossAmountMinor, String sourceEventId, long now) {
        return assess(server, TaxType.TRANSACTION, taxpayerUuid, currencyId, grossAmountMinor, sourceEventId, now);
    }

    public static TaxBill assess(MinecraftServer server, TaxType type, UUID taxpayerUuid, String currencyId,
                                 long grossAmountMinor, String sourceEventId, long now) {
        if (taxpayerUuid == null || grossAmountMinor <= 0L || sourceEventId == null || sourceEventId.isBlank()) return null;
        TaxLedgerSavedData ledger = TaxLedgerSavedData.get(server);
        if (ledger.findBySourceEvent(sourceEventId) != null) return ledger.findBySourceEvent(sourceEventId);
        TaxRule rule = TaxRuleService.current(server, type, now);
        long taxAmount = TaxRuleService.taxMinor(server, type, grossAmountMinor, now);
        if (taxAmount <= 0L) return null;
        TaxSubject subject = new TaxSubject(type, sourceEventId, taxpayerUuid);
        return TaxService.createBill(server, subject, currencyId, taxAmount, now, now, now,
                sourceEventId, now, now, grossAmountMinor, rule.rateBasisPoints());
    }
}
