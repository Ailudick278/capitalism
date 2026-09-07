package com.ailudick.capitalismmod.population;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

/** Durable daily household cash-flow snapshots used to reconcile stock and flows. */
public final class HouseholdCashflowSavedData extends SavedData {
    private static final String ID = "capitalismmod_household_cashflow";
    private static final int MAX_SNAPSHOTS = 16384;
    private final List<Snapshot> snapshots = new ArrayList<>();

    public record Snapshot(String id, String householdId, long day, long openingCashMinor,
                           long wageIncomeMinor, long governmentIncomeMinor,
                           long consumptionMinor, long rentMinor, long closingCashMinor) {}

    private HouseholdCashflowSavedData() {}

    public static HouseholdCashflowSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(HouseholdCashflowSavedData::new, HouseholdCashflowSavedData::load), ID);
    }

    public List<Snapshot> snapshots() { return List.copyOf(snapshots); }
    public Snapshot find(String id) {
        return snapshots.stream().filter(snapshot -> snapshot.id().equals(id)).findFirst().orElse(null);
    }

    public boolean record(Snapshot snapshot) {
        if (snapshot == null || snapshot.id() == null || snapshot.id().isBlank()
                || snapshot.householdId() == null || snapshot.householdId().isBlank()
                || snapshot.day() < 0L || snapshot.openingCashMinor() < 0L
                || snapshot.wageIncomeMinor() < 0L || snapshot.governmentIncomeMinor() < 0L
                || snapshot.consumptionMinor() < 0L || snapshot.rentMinor() < 0L
                || snapshot.closingCashMinor() < 0L || find(snapshot.id()) != null) return false;
        snapshots.add(snapshot);
        while (snapshots.size() > MAX_SNAPSHOTS) snapshots.remove(0);
        setDirty();
        return true;
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Snapshot snapshot : snapshots) {
            CompoundTag entry = new CompoundTag();
            entry.putString("id", snapshot.id());
            entry.putString("household", snapshot.householdId());
            entry.putLong("day", snapshot.day());
            entry.putLong("opening", snapshot.openingCashMinor());
            entry.putLong("wages", snapshot.wageIncomeMinor());
            entry.putLong("government", snapshot.governmentIncomeMinor());
            entry.putLong("consumption", snapshot.consumptionMinor());
            entry.putLong("rent", snapshot.rentMinor());
            entry.putLong("closing", snapshot.closingCashMinor());
            list.add(entry);
        }
        tag.put("snapshots", list);
        return tag;
    }

    public static HouseholdCashflowSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        HouseholdCashflowSavedData data = new HouseholdCashflowSavedData();
        ListTag list = tag.getList("snapshots", Tag.TAG_COMPOUND);
        for (int i = Math.max(0, list.size() - MAX_SNAPSHOTS); i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (!entry.getString("id").isBlank() && !entry.getString("household").isBlank()
                    && entry.getLong("day") >= 0L && nonNegative(entry, "opening")
                    && nonNegative(entry, "wages") && nonNegative(entry, "government")
                    && nonNegative(entry, "consumption") && nonNegative(entry, "rent")
                    && nonNegative(entry, "closing")) {
                data.snapshots.add(new Snapshot(entry.getString("id"), entry.getString("household"),
                        entry.getLong("day"), entry.getLong("opening"), entry.getLong("wages"),
                        entry.getLong("government"), entry.getLong("consumption"), entry.getLong("rent"),
                        entry.getLong("closing")));
            }
        }
        return data;
    }

    private static boolean nonNegative(CompoundTag entry, String key) {
        return entry.getLong(key) >= 0L;
    }
}
