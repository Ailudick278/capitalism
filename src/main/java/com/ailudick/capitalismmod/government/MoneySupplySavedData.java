package com.ailudick.capitalismmod.government;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

/** Persistent daily monetary aggregates in the default currency. */
public final class MoneySupplySavedData extends SavedData {
    private static final String ID = "capitalismmod_money_supply";
    private static final int MAX_SNAPSHOTS = 1024;
    private final List<Snapshot> snapshots = new ArrayList<>();

    public record Snapshot(long day, long householdCashMinor, long companyCashMinor,
                           long bankDepositsMinor, long governmentTreasuryMinor,
                           long privateMoneyMinor, long bankCreditMinor) {}

    private MoneySupplySavedData() {}

    public static MoneySupplySavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(MoneySupplySavedData::new, MoneySupplySavedData::load), ID);
    }

    public Snapshot latest() { return snapshots.isEmpty() ? null : snapshots.get(snapshots.size() - 1); }
    public List<Snapshot> snapshots() { return List.copyOf(snapshots); }

    public void record(Snapshot snapshot) {
        if (snapshot == null || (!snapshots.isEmpty() && snapshots.get(snapshots.size() - 1).day() >= snapshot.day())) return;
        snapshots.add(snapshot);
        while (snapshots.size() > MAX_SNAPSHOTS) snapshots.remove(0);
        setDirty();
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Snapshot s : snapshots) {
            CompoundTag e = new CompoundTag();
            e.putLong("day", s.day()); e.putLong("householdCash", s.householdCashMinor());
            e.putLong("companyCash", s.companyCashMinor()); e.putLong("bankDeposits", s.bankDepositsMinor());
            e.putLong("governmentTreasury", s.governmentTreasuryMinor());
            e.putLong("privateMoney", s.privateMoneyMinor()); e.putLong("bankCredit", s.bankCreditMinor());
            list.add(e);
        }
        tag.put("snapshots", list);
        return tag;
    }

    public static MoneySupplySavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        MoneySupplySavedData data = new MoneySupplySavedData();
        ListTag list = tag.getList("snapshots", Tag.TAG_COMPOUND);
        for (int i = Math.max(0, list.size() - MAX_SNAPSHOTS); i < list.size(); i++) {
            CompoundTag e = list.getCompound(i);
            data.snapshots.add(new Snapshot(Math.max(0L, e.getLong("day")),
                    Math.max(0L, e.getLong("householdCash")), Math.max(0L, e.getLong("companyCash")),
                    Math.max(0L, e.getLong("bankDeposits")), Math.max(0L, e.getLong("governmentTreasury")),
                    Math.max(0L, e.getLong("privateMoney")), Math.max(0L, e.getLong("bankCredit"))));
        }
        return data;
    }
}
