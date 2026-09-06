package com.ailudick.capitalismmod.economy;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashMap;
import java.util.List;

/** Durable phase journal shared by financial instruments and their cross-ledger settlements. */
public final class FinancialSettlementJournalSavedData extends SavedData {
    private static final String ID = "capitalismmod_financial_settlement_journal";
    private static final int MAX_ENTRIES = 32768;
    private final LinkedHashMap<String, Entry> entries = new LinkedHashMap<>();

    public record Entry(String transactionId, String instrument, String phase, String status,
                        long amountMinor, long gameTime) {}

    private FinancialSettlementJournalSavedData() {}

    public static FinancialSettlementJournalSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(FinancialSettlementJournalSavedData::new,
                        FinancialSettlementJournalSavedData::load), ID);
    }

    public static String key(String transactionId, String phase) {
        return transactionId + "|" + phase;
    }

    public Entry entry(String transactionId, String phase) {
        return entries.get(key(transactionId, phase));
    }

    public boolean isCompleted(String transactionId, String phase) {
        Entry entry = entry(transactionId, phase);
        return entry != null && "completed".equals(entry.status());
    }

    public void markStarted(String transactionId, String instrument, String phase,
                            long amountMinor, long gameTime) {
        put(new Entry(transactionId, instrument, phase, "started", Math.max(0L, amountMinor), gameTime));
    }

    public void markCompleted(String transactionId, String instrument, String phase,
                              long amountMinor, long gameTime) {
        put(new Entry(transactionId, instrument, phase, "completed", Math.max(0L, amountMinor), gameTime));
    }

    public List<Entry> entries() { return List.copyOf(entries.values()); }

    /** Returns phases that were started but have not yet been durably completed. */
    public List<Entry> pendingEntries() {
        return entries.values().stream().filter(entry -> "started".equals(entry.status())).toList();
    }

    private void put(Entry entry) {
        if (entry == null || entry.transactionId() == null || entry.transactionId().isBlank()
                || entry.instrument() == null || entry.instrument().isBlank()
                || entry.phase() == null || entry.phase().isBlank()) return;
        entries.put(key(entry.transactionId(), entry.phase()), entry);
        while (entries.size() > MAX_ENTRIES) entries.remove(entries.keySet().iterator().next());
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Entry entry : entries.values()) {
            CompoundTag value = new CompoundTag();
            value.putString("transactionId", entry.transactionId());
            value.putString("instrument", entry.instrument());
            value.putString("phase", entry.phase());
            value.putString("status", entry.status());
            value.putLong("amountMinor", entry.amountMinor());
            value.putLong("gameTime", entry.gameTime());
            list.add(value);
        }
        tag.put("entries", list);
        return tag;
    }

    public static FinancialSettlementJournalSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        FinancialSettlementJournalSavedData data = new FinancialSettlementJournalSavedData();
        ListTag list = tag.getList("entries", Tag.TAG_COMPOUND);
        for (int i = Math.max(0, list.size() - MAX_ENTRIES); i < list.size(); i++) {
            CompoundTag value = list.getCompound(i);
            String transactionId = value.getString("transactionId");
            String instrument = value.getString("instrument");
            String phase = value.getString("phase");
            String status = value.getString("status");
            if (!transactionId.isBlank() && !instrument.isBlank() && !phase.isBlank()
                    && ("started".equals(status) || "completed".equals(status))) {
                data.entries.put(key(transactionId, phase), new Entry(transactionId, instrument, phase,
                        status, Math.max(0L, value.getLong("amountMinor")), value.getLong("gameTime")));
            }
        }
        return data;
    }
}
