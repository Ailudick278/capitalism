package com.ailudick.capitalismmod.bank;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

/** Persistent bank-liquidity history and current crisis withdrawal usage. */
public final class BankLiquiditySavedData extends SavedData {
    private static final String ID = "capitalismmod_bank_liquidity";
    private static final int MAX_SNAPSHOTS = 1024;
    private final List<BankLiquiditySnapshot> snapshots = new ArrayList<>();
    private long currentDay = -1L;
    private long withdrawnToday;

    private BankLiquiditySavedData() {}
    public static BankLiquiditySavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(BankLiquiditySavedData::new, BankLiquiditySavedData::load), ID);
    }
    public List<BankLiquiditySnapshot> snapshots() { return List.copyOf(snapshots); }
    public BankLiquiditySnapshot latest() { return snapshots.isEmpty() ? null : snapshots.get(snapshots.size() - 1); }
    public long withdrawnToday(long day) { return currentDay == day ? withdrawnToday : 0L; }
    public boolean reserveWithdrawal(long day, long amount, long limit) {
        if (amount <= 0L || limit <= 0L || amount > limit) return false;
        if (currentDay != day) { currentDay = day; withdrawnToday = 0L; }
        if (amount > limit - withdrawnToday) return false;
        withdrawnToday += amount; setDirty(); return true;
    }
    public void record(BankLiquiditySnapshot snapshot) {
        if (snapshot == null || (!snapshots.isEmpty() && snapshots.get(snapshots.size() - 1).day() >= snapshot.day())) return;
        snapshots.add(snapshot); while (snapshots.size() > MAX_SNAPSHOTS) snapshots.remove(0); setDirty();
    }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putLong("currentDay", currentDay); tag.putLong("withdrawnToday", withdrawnToday);
        ListTag list = new ListTag();
        for (BankLiquiditySnapshot s : snapshots) { CompoundTag e = new CompoundTag(); e.putLong("day", s.day());
            e.putLong("deposits", s.depositsMinor()); e.putLong("loans", s.loanDebtMinor());
            e.putLong("withdrawn", s.withdrawnMinor()); e.putBoolean("limited", s.withdrawalLimitActive()); list.add(e); }
        tag.put("snapshots", list); return tag;
    }
    public static BankLiquiditySavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        BankLiquiditySavedData data = new BankLiquiditySavedData(); data.currentDay = tag.getLong("currentDay");
        data.withdrawnToday = Math.max(0L, tag.getLong("withdrawnToday"));
        ListTag list = tag.getList("snapshots", Tag.TAG_COMPOUND);
        for (int i = Math.max(0, list.size() - MAX_SNAPSHOTS); i < list.size(); i++) { CompoundTag e = list.getCompound(i);
            data.snapshots.add(new BankLiquiditySnapshot(Math.max(0L, e.getLong("day")), Math.max(0L, e.getLong("deposits")),
                    Math.max(0L, e.getLong("loans")), Math.max(0L, e.getLong("withdrawn")), e.getBoolean("limited"))); }
        return data;
    }
}
