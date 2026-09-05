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
import java.util.UUID;

/** Accumulates sole-proprietor revenue and deductible costs by tax period. */
public final class IndividualTaxPeriodSavedData extends SavedData {
    private static final String ID = "capitalismmod_individual_tax_periods";
    private final Map<String, Entry> entries = new HashMap<>();
    private final Set<String> eventIds = new HashSet<>();
    private final Set<String> closedPeriods = new HashSet<>();

    public record Entry(String businessId, UUID ownerUuid, String currencyId, long revenue, long expenses,
                        long periodStart, long periodEnd) {}

    private IndividualTaxPeriodSavedData() {}

    public static IndividualTaxPeriodSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(IndividualTaxPeriodSavedData::new, IndividualTaxPeriodSavedData::load), ID);
    }

    public void recordIncome(String eventId, String businessId, UUID ownerUuid, String currencyId, long amount,
                             long periodStart, long periodEnd) {
        record(eventId, businessId, ownerUuid, currencyId, amount, 0L, periodStart, periodEnd);
    }

    public boolean isClosed(String businessId, long periodEnd) {
        return closedPeriods.contains(businessId + ":" + periodEnd);
    }

    public void close(String businessId, long periodEnd) {
        if (businessId == null || businessId.isBlank() || !closedPeriods.add(businessId + ":" + periodEnd)) return;
        setDirty();
    }

    public boolean correct(String businessId, long periodEnd, long revenue, long expenses) {
        String key = businessId + ":" + periodEnd;
        Entry old = entries.get(key);
        if (old == null || revenue < 0L || expenses < 0L) return false;
        entries.put(key, new Entry(old.businessId(), old.ownerUuid(), old.currencyId(), revenue, expenses,
                old.periodStart(), old.periodEnd()));
        setDirty();
        return true;
    }

    public void recordExpense(String eventId, String businessId, UUID ownerUuid, String currencyId, long amount,
                              long periodStart, long periodEnd) {
        record(eventId, businessId, ownerUuid, currencyId, 0L, amount, periodStart, periodEnd);
    }

    private void record(String eventId, String businessId, UUID ownerUuid, String currencyId, long revenue,
                        long expenses, long periodStart, long periodEnd) {
        if (eventId == null || eventIds.contains(eventId) || businessId == null || ownerUuid == null
                || (revenue <= 0L && expenses <= 0L) || isClosed(businessId, periodEnd)) return;
        String key = businessId + ":" + periodEnd;
        Entry old = entries.get(key);
        long nextRevenue = add(old == null ? 0L : old.revenue(), revenue);
        long nextExpenses = add(old == null ? 0L : old.expenses(), expenses);
        entries.put(key, new Entry(businessId, ownerUuid, currencyId, nextRevenue, nextExpenses,
                periodStart, periodEnd));
        eventIds.add(eventId);
        setDirty();
    }

    public java.util.List<Entry> due(long now) {
        return entries.values().stream().filter(entry -> entry.periodEnd() <= now).toList();
    }

    public java.util.List<Entry> forBusiness(String businessId) {
        return entries.values().stream().filter(entry -> entry.businessId().equals(businessId)).toList();
    }

    public void remove(Entry entry) {
        entries.remove(entry.businessId() + ":" + entry.periodEnd());
        setDirty();
    }

    private static long add(long left, long right) {
        return right > 0L && left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Entry entry : entries.values()) {
            CompoundTag value = new CompoundTag();
            value.putString("businessId", entry.businessId());
            value.putUUID("owner", entry.ownerUuid());
            value.putString("currency", entry.currencyId());
            value.putLong("revenue", entry.revenue());
            value.putLong("expenses", entry.expenses());
            value.putLong("periodStart", entry.periodStart());
            value.putLong("periodEnd", entry.periodEnd());
            list.add(value);
        }
        tag.put("entries", list);
        ListTag events = new ListTag();
        for (String eventId : eventIds) {
            CompoundTag value = new CompoundTag();
            value.putString("id", eventId);
            events.add(value);
        }
        tag.put("events", events);
        ListTag closed = new ListTag();
        for (String period : closedPeriods) { CompoundTag value = new CompoundTag(); value.putString("period", period); closed.add(value); }
        tag.put("closedPeriods", closed);
        return tag;
    }

    public static IndividualTaxPeriodSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        IndividualTaxPeriodSavedData data = new IndividualTaxPeriodSavedData();
        ListTag entries = tag.getList("entries", 10);
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag value = entries.getCompound(i);
            if (!value.hasUUID("owner")) continue;
            Entry entry = new Entry(value.getString("businessId"), value.getUUID("owner"),
                    value.getString("currency"), value.getLong("revenue"), value.getLong("expenses"),
                    value.getLong("periodStart"), value.getLong("periodEnd"));
            if (!entry.businessId().isBlank()) data.entries.put(entry.businessId() + ":" + entry.periodEnd(), entry);
        }
        ListTag events = tag.getList("events", 10);
        for (int i = 0; i < events.size(); i++) {
            String id = events.getCompound(i).getString("id");
            if (!id.isBlank()) data.eventIds.add(id);
        }
        ListTag closed = tag.getList("closedPeriods", 10);
        for (int i = 0; i < closed.size(); i++) {
            String period = closed.getCompound(i).getString("period");
            if (!period.isBlank()) data.closedPeriods.add(period);
        }
        return data;
    }
}
