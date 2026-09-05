package com.ailudick.capitalismmod.tax;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Accumulates company income for quarterly corporate-tax prepayments. */
public final class CorporateTaxPeriodSavedData extends SavedData {
    private static final String ID = "capitalismmod_corporate_tax_periods";
    private final Map<String, Entry> entries = new HashMap<>();
    private final Set<String> eventIds = new HashSet<>();
    public record Entry(String companyId, String currencyId, long revenue, long expenses, long periodStart, long periodEnd) {
        public Entry(String companyId, String currencyId, long revenue, long periodStart, long periodEnd) {
            this(companyId, currencyId, revenue, 0L, periodStart, periodEnd);
        }
    }
    private CorporateTaxPeriodSavedData() {}

    public static CorporateTaxPeriodSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(CorporateTaxPeriodSavedData::new, CorporateTaxPeriodSavedData::load), ID);
    }

    public boolean containsEvent(String eventId) { return eventIds.contains(eventId); }

    public void record(String eventId, String companyId, String currencyId, long revenue,
                       long periodStart, long periodEnd) {
        if (eventIds.contains(eventId) || revenue <= 0L) return;
        String key = companyId + ":" + periodEnd;
        Entry old = entries.get(key);
        long total = old == null ? revenue : old.revenue() > Long.MAX_VALUE - revenue
                ? Long.MAX_VALUE : old.revenue() + revenue;
        entries.put(key, new Entry(companyId, currencyId, total, old == null ? 0L : old.expenses(), periodStart, periodEnd));
        eventIds.add(eventId);
        setDirty();
    }

    public void recordExpense(String eventId, String companyId, String currencyId, long expense,
                              long periodStart, long periodEnd) {
        if (eventIds.contains(eventId) || expense <= 0L) return;
        String key = companyId + ":" + periodEnd;
        Entry old = entries.get(key);
        long revenue = old == null ? 0L : old.revenue();
        long expenses = old == null ? expense : old.expenses() > Long.MAX_VALUE - expense
                ? Long.MAX_VALUE : old.expenses() + expense;
        entries.put(key, new Entry(companyId, currencyId, revenue, expenses, periodStart, periodEnd));
        eventIds.add(eventId);
        setDirty();
    }

    public java.util.List<Entry> due(long now) {
        return entries.values().stream().filter(entry -> entry.periodEnd() <= now).toList();
    }

    public void remove(Entry entry) {
        entries.remove(entry.companyId() + ":" + entry.periodEnd());
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag entriesTag = new ListTag();
        entries.forEach((key, entry) -> {
            CompoundTag value = new CompoundTag();
            value.putString("companyId", entry.companyId()); value.putString("currency", entry.currencyId());
            value.putLong("revenue", entry.revenue()); value.putLong("expenses", entry.expenses());
            value.putLong("periodStart", entry.periodStart());
            value.putLong("periodEnd", entry.periodEnd()); entriesTag.add(value);
        });
        tag.put("entries", entriesTag);
        ListTag eventsTag = new ListTag();
        eventIds.forEach(eventId -> { CompoundTag value = new CompoundTag(); value.putString("id", eventId); eventsTag.add(value); });
        tag.put("events", eventsTag);
        return tag;
    }

    public static CorporateTaxPeriodSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        CorporateTaxPeriodSavedData data = new CorporateTaxPeriodSavedData();
        ListTag entries = tag.getList("entries", 10);
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag value = entries.getCompound(i);
            Entry entry = new Entry(value.getString("companyId"), value.getString("currency"), value.getLong("revenue"),
                    value.getLong("expenses"), value.getLong("periodStart"), value.getLong("periodEnd"));
            if (!entry.companyId().isBlank()) data.entries.put(entry.companyId() + ":" + entry.periodEnd(), entry);
        }
        ListTag events = tag.getList("events", 10);
        for (int i = 0; i < events.size(); i++) {
            String id = events.getCompound(i).getString("id");
            if (!id.isBlank()) data.eventIds.add(id);
        }
        return data;
    }
}
