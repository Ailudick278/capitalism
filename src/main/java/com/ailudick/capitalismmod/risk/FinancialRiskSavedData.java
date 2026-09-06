package com.ailudick.capitalismmod.risk;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

/** Persistent daily financial-risk history for dashboards and future crisis policies. */
public final class FinancialRiskSavedData extends SavedData {
    private static final String ID = "capitalismmod_financial_risk";
    private static final int MAX_SNAPSHOTS = 1024;
    private final List<FinancialRiskSnapshot> snapshots = new ArrayList<>();

    private FinancialRiskSavedData() {}

    public static FinancialRiskSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(FinancialRiskSavedData::new, FinancialRiskSavedData::load), ID);
    }

    public List<FinancialRiskSnapshot> snapshots() { return List.copyOf(snapshots); }
    public FinancialRiskSnapshot latest() { return snapshots.isEmpty() ? null : snapshots.get(snapshots.size() - 1); }

    public void record(FinancialRiskSnapshot snapshot) {
        if (snapshot == null || (!snapshots.isEmpty() && snapshots.get(snapshots.size() - 1).day() >= snapshot.day())) return;
        snapshots.add(snapshot);
        while (snapshots.size() > MAX_SNAPSHOTS) snapshots.remove(0);
        setDirty();
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (FinancialRiskSnapshot snapshot : snapshots) {
            CompoundTag e = new CompoundTag();
            e.putLong("day", snapshot.day()); e.putLong("company", snapshot.companyDebtMinor());
            e.putLong("peer", snapshot.peerDebtMinor()); e.putLong("bonds", snapshot.bondLiabilityMinor());
            e.putLong("overdue", snapshot.overdueDebtMinor()); e.putInt("overdueCount", snapshot.overdueLoanCount());
            e.putInt("overdueShareBps", snapshot.overdueShareBasisPoints()); list.add(e);
        }
        tag.put("snapshots", list); return tag;
    }

    public static FinancialRiskSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        FinancialRiskSavedData data = new FinancialRiskSavedData();
        ListTag list = tag.getList("snapshots", Tag.TAG_COMPOUND);
        for (int i = Math.max(0, list.size() - MAX_SNAPSHOTS); i < list.size(); i++) {
            CompoundTag e = list.getCompound(i);
            data.snapshots.add(new FinancialRiskSnapshot(Math.max(0L, e.getLong("day")),
                    Math.max(0L, e.getLong("company")), Math.max(0L, e.getLong("peer")),
                    Math.max(0L, e.getLong("bonds")), Math.max(0L, e.getLong("overdue")),
                    Math.max(0, e.getInt("overdueCount")), Math.max(0, Math.min(10000, e.getInt("overdueShareBps")))));
        }
        return data;
    }
}
