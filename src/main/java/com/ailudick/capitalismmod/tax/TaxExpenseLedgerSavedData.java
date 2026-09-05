package com.ailudick.capitalismmod.tax;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Persistent, auditable ledger for deductible and non-deductible business expenses. */
public final class TaxExpenseLedgerSavedData extends SavedData {
    private static final String ID = "capitalismmod_tax_expenses";
    public record Expense(String id, UUID taxpayerUuid, String subjectId, String category,
                          String currencyId, long amount, long occurredAt, String sourceId,
                          boolean deductible, String details) {
        public Expense(String id, UUID taxpayerUuid, String subjectId, String category,
                       String currencyId, long amount, long occurredAt, String sourceId,
                       boolean deductible) {
            this(id, taxpayerUuid, subjectId, category, currencyId, amount, occurredAt, sourceId, deductible, "");
        }
    }
    private final List<Expense> expenses = new ArrayList<>();

    private TaxExpenseLedgerSavedData() {}

    public static TaxExpenseLedgerSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(TaxExpenseLedgerSavedData::new, TaxExpenseLedgerSavedData::load), ID);
    }

    public List<Expense> all() { return List.copyOf(expenses); }
    public List<Expense> forTaxpayer(UUID uuid) {
        return expenses.stream().filter(expense -> expense.taxpayerUuid().equals(uuid)).toList();
    }
    public boolean containsSource(String sourceId) {
        return expenses.stream().anyMatch(expense -> expense.sourceId().equals(sourceId));
    }
    public void add(Expense expense) {
        if (expense == null || expense.amount() <= 0L || containsSource(expense.sourceId())) return;
        expenses.add(expense);
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Expense expense : expenses) {
            CompoundTag entry = new CompoundTag();
            entry.putString("id", expense.id());
            entry.putUUID("taxpayer", expense.taxpayerUuid());
            entry.putString("subject", expense.subjectId());
            entry.putString("category", expense.category());
            entry.putString("currency", expense.currencyId());
            entry.putLong("amount", expense.amount());
            entry.putLong("occurredAt", expense.occurredAt());
            entry.putString("source", expense.sourceId());
            entry.putBoolean("deductible", expense.deductible());
            entry.putString("details", expense.details());
            list.add(entry);
        }
        tag.put("expenses", list);
        return tag;
    }

    public static TaxExpenseLedgerSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        TaxExpenseLedgerSavedData data = new TaxExpenseLedgerSavedData();
        ListTag list = tag.getList("expenses", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (!entry.hasUUID("taxpayer") || entry.getLong("amount") <= 0L) continue;
            data.expenses.add(new Expense(entry.getString("id"), entry.getUUID("taxpayer"),
                    entry.getString("subject"), entry.getString("category"), entry.getString("currency"),
                    entry.getLong("amount"), entry.getLong("occurredAt"), entry.getString("source"),
                    entry.getBoolean("deductible"), entry.getString("details")));
        }
        return data;
    }
}
