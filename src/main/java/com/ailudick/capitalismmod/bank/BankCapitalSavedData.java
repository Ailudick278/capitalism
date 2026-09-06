package com.ailudick.capitalismmod.bank;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Persistent bank equity ledger in default-currency minor units. */
public final class BankCapitalSavedData extends SavedData {
    private static final String ID = "capitalismmod_bank_capital";
    private long capitalMinor;
    private long cumulativeProfitLossMinor;
    private long lastSettlementDay = -1L;
    private boolean initialized;

    private BankCapitalSavedData() {}

    public static BankCapitalSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(BankCapitalSavedData::new, BankCapitalSavedData::load), ID);
    }

    public long capitalMinor() { return capitalMinor; }
    public long cumulativeProfitLossMinor() { return cumulativeProfitLossMinor; }
    public long lastSettlementDay() { return lastSettlementDay; }
    public boolean initialized() { return initialized; }

    public void initialize(long openingCapitalMinor) {
        if (initialized) return;
        capitalMinor = Math.max(0L, openingCapitalMinor);
        initialized = true;
        setDirty();
    }

    public boolean applyDailyResult(long day, long incomeMinor, long expenseMinor) {
        if (!initialized || day <= lastSettlementDay) return false;
        long result;
        try { result = Math.subtractExact(incomeMinor, expenseMinor); }
        catch (ArithmeticException exception) { result = incomeMinor >= 0L ? Long.MAX_VALUE : Long.MIN_VALUE; }
        capitalMinor = saturatingAdd(capitalMinor, result);
        cumulativeProfitLossMinor = saturatingAdd(cumulativeProfitLossMinor, result);
        lastSettlementDay = day;
        setDirty();
        return true;
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putLong("capitalMinor", capitalMinor);
        tag.putLong("cumulativeProfitLossMinor", cumulativeProfitLossMinor);
        tag.putLong("lastSettlementDay", lastSettlementDay);
        tag.putBoolean("initialized", initialized);
        return tag;
    }

    public static BankCapitalSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        BankCapitalSavedData data = new BankCapitalSavedData();
        data.capitalMinor = tag.getLong("capitalMinor");
        data.cumulativeProfitLossMinor = tag.getLong("cumulativeProfitLossMinor");
        data.lastSettlementDay = tag.getLong("lastSettlementDay");
        data.initialized = tag.getBoolean("initialized") || tag.contains("capitalMinor");
        return data;
    }

    private static long saturatingAdd(long left, long right) {
        try { return Math.addExact(left, right); }
        catch (ArithmeticException exception) { return right >= 0L ? Long.MAX_VALUE : Long.MIN_VALUE; }
    }
}
