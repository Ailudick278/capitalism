package com.ailudick.capitalismmod.economy;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashMap;
import java.util.List;

/** Durable phase journal for daily settlement recovery and diagnostics. */
public final class EconomicSettlementJournalSavedData extends SavedData {
    private static final String ID = "capitalismmod_economic_settlement_journal";
    private static final int MAX_ENTRIES = 16384;
    private final LinkedHashMap<String, Entry> entries = new LinkedHashMap<>();

    public record Entry(long day, String phase, String status, long gameTime) {}

    private EconomicSettlementJournalSavedData() {}

    public static EconomicSettlementJournalSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(EconomicSettlementJournalSavedData::new, EconomicSettlementJournalSavedData::load), ID);
    }

    public static String key(long day, String phase) {
        return EconomicSettlementJournalKey.of(day, phase);
    }

    public Entry entry(long day, String phase) { return entries.get(key(day, phase)); }

    public boolean isCompleted(long day, String phase) {
        Entry entry = entry(day, phase);
        return entry != null && "completed".equals(entry.status());
    }

    public void markStarted(long day, String phase, long gameTime) {
        put(new Entry(day, phase, "started", gameTime));
    }

    public void markCompleted(long day, String phase, long gameTime) {
        put(new Entry(day, phase, "completed", gameTime));
    }

    public List<Entry> entries() { return List.copyOf(entries.values()); }

    private void put(Entry entry) {
        if (entry == null || entry.phase() == null || entry.phase().isBlank()) return;
        Entry previous = entries.get(key(entry.day(), entry.phase()));
        // A completed phase is terminal. A repeated tick or recovery pass may
        // observe it again, but must never make diagnostics report it as started.
        if (previous != null && SettlementPhaseState.preservesCompleted(previous.status(), entry.status())) return;
        entries.put(key(entry.day(), entry.phase()), entry);
        while (entries.size() > MAX_ENTRIES) entries.remove(entries.keySet().iterator().next());
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Entry entry : entries.values()) {
            CompoundTag value = new CompoundTag();
            value.putLong("day", entry.day());
            value.putString("phase", entry.phase());
            value.putString("status", entry.status());
            value.putLong("gameTime", entry.gameTime());
            list.add(value);
        }
        tag.put("entries", list);
        return tag;
    }

    public static EconomicSettlementJournalSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        EconomicSettlementJournalSavedData data = new EconomicSettlementJournalSavedData();
        ListTag list = tag.getList("entries", Tag.TAG_COMPOUND);
        for (int i = Math.max(0, list.size() - MAX_ENTRIES); i < list.size(); i++) {
            CompoundTag value = list.getCompound(i);
            String phase = value.getString("phase");
            String status = value.getString("status");
            if (!phase.isBlank() && ("started".equals(status) || "completed".equals(status))) {
                Entry entry = new Entry(value.getLong("day"), phase, status, value.getLong("gameTime"));
                data.entries.put(key(entry.day(), phase), entry);
            }
        }
        while (data.entries.size() > MAX_ENTRIES) data.entries.remove(data.entries.keySet().iterator().next());
        return data;
    }
}
