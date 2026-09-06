package com.ailudick.capitalismmod.government;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

/** Persistent fiscal policy, reserve balance, and transfer audit trail. */
public final class GovernmentPolicySavedData extends SavedData {
    private static final String ID = "capitalismmod_government_policy";
    private static final int MAX_TRANSACTIONS = 8192;
    private long treasuryMinor;
    private long dailyBenefitMinor;
    private final List<Transaction> transactions = new ArrayList<>();

    public record Transaction(String id, long day, String householdId, long amount, long balanceAfter) {}

    private GovernmentPolicySavedData() {}

    public static GovernmentPolicySavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(GovernmentPolicySavedData::new, GovernmentPolicySavedData::load), ID);
    }

    public long treasuryMinor() { return treasuryMinor; }
    public long dailyBenefitMinor() { return dailyBenefitMinor; }
    public List<Transaction> transactions() { return List.copyOf(transactions); }

    public boolean setDailyBenefit(long amount) {
        if (amount < 0L || amount > 1_000_000_000L) return false;
        dailyBenefitMinor = amount; setDirty(); return true;
    }

    public boolean deposit(long amount) {
        if (amount <= 0L || treasuryMinor > Long.MAX_VALUE - amount) return false;
        treasuryMinor += amount; setDirty(); return true;
    }

    public boolean spend(String householdId, long day, long amount, String transactionId) {
        if (householdId == null || householdId.isBlank() || amount <= 0L || amount > treasuryMinor
                || transactionId == null || transactionId.isBlank()
                || transactions.stream().anyMatch(t -> transactionId.equals(t.id()))) return false;
        treasuryMinor -= amount;
        transactions.add(new Transaction(transactionId, day, householdId, amount, treasuryMinor));
        while (transactions.size() > MAX_TRANSACTIONS) transactions.remove(0);
        setDirty(); return true;
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putLong("treasury", treasuryMinor); tag.putLong("benefit", dailyBenefitMinor);
        ListTag list = new ListTag();
        for (Transaction t : transactions) {
            CompoundTag e = new CompoundTag(); e.putString("id", t.id()); e.putLong("day", t.day());
            e.putString("household", t.householdId()); e.putLong("amount", t.amount());
            e.putLong("balance", t.balanceAfter()); list.add(e);
        }
        tag.put("transactions", list); return tag;
    }

    public static GovernmentPolicySavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        GovernmentPolicySavedData data = new GovernmentPolicySavedData();
        data.treasuryMinor = Math.max(0L, tag.getLong("treasury"));
        data.dailyBenefitMinor = Math.max(0L, tag.getLong("benefit"));
        ListTag list = tag.getList("transactions", Tag.TAG_COMPOUND);
        for (int i = Math.max(0, list.size() - MAX_TRANSACTIONS); i < list.size(); i++) {
            CompoundTag e = list.getCompound(i);
            if (!e.getString("id").isBlank() && !e.getString("household").isBlank() && e.getLong("amount") > 0L) {
                data.transactions.add(new Transaction(e.getString("id"), e.getLong("day"),
                        e.getString("household"), e.getLong("amount"), Math.max(0L, e.getLong("balance"))));
            }
        }
        return data;
    }
}
