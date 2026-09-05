package com.ailudick.capitalismmod.tax;

import net.minecraft.server.MinecraftServer;

import java.util.UUID;

/** Single entry point for recording deductible business costs. */
public final class TaxExpenseService {
    private TaxExpenseService() {}

    public static void record(MinecraftServer server, UUID taxpayerUuid, String subjectId,
                              String category, String currencyId, long amount, long occurredAt,
                              String sourceId, boolean deductible) {
        record(server, taxpayerUuid, subjectId, category, currencyId, amount, occurredAt, sourceId, deductible, "");
    }

    public static void record(MinecraftServer server, UUID taxpayerUuid, String subjectId,
                              String category, String currencyId, long amount, long occurredAt,
                              String sourceId, boolean deductible, String details) {
        TaxExpenseLedgerSavedData.get(server).add(new TaxExpenseLedgerSavedData.Expense(
                UUID.randomUUID().toString(), taxpayerUuid, subjectId, category, currencyId,
                amount, occurredAt, sourceId, deductible, details == null ? "" : details));
    }
}
