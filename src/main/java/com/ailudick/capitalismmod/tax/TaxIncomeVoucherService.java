package com.ailudick.capitalismmod.tax;

import net.minecraft.server.MinecraftServer;
import java.util.UUID;

public final class TaxIncomeVoucherService {
    private TaxIncomeVoucherService() {}
    public static void record(MinecraftServer server, UUID taxpayerUuid, String subjectId, String category,
                              String currencyId, long amount, long occurredAt, String sourceId, String details) {
        TaxIncomeVoucherLedgerSavedData.get(server).add(new TaxIncomeVoucherLedgerSavedData.Voucher(
                UUID.randomUUID().toString(), taxpayerUuid, subjectId, category, currencyId, amount,
                occurredAt, sourceId, details == null ? "" : details));
    }
}
