package com.ailudick.capitalismmod.bank;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;

/** Persistent bank equity ledger in default-currency minor units. */
public final class BankCapitalSavedData extends SavedData {
    private static final String ID = "capitalismmod_bank_capital";
    private long capitalMinor;
    private long cumulativeProfitLossMinor;
    private long lossProvisionMinor;
    private long lastSettlementDay = -1L;
    private boolean initialized;
    private final Set<String> injectionReceipts = new HashSet<>();
    private static final int MAX_INJECTION_RECEIPTS = 4096;

    private BankCapitalSavedData() {}

    public static BankCapitalSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(BankCapitalSavedData::new, BankCapitalSavedData::load), ID);
    }

    public long capitalMinor() { return capitalMinor; }
    public long cumulativeProfitLossMinor() { return cumulativeProfitLossMinor; }
    public long lastSettlementDay() { return lastSettlementDay; }
    public long lossProvisionMinor() { return lossProvisionMinor; }
    public boolean initialized() { return initialized; }

    /** Applies a government capital injection once, using a durable source receipt. */
    public boolean injectOnce(long amountMinor, String sourceId) {
        if (!initialized || amountMinor <= 0L || sourceId == null || sourceId.isBlank()
                || injectionReceipts.contains(sourceId)) return false;
        capitalMinor = saturatingAdd(capitalMinor, amountMinor);
        injectionReceipts.add(sourceId);
        while (injectionReceipts.size() > MAX_INJECTION_RECEIPTS) {
            injectionReceipts.remove(injectionReceipts.iterator().next());
        }
        setDirty();
        return true;
    }

    public boolean hasInjection(String sourceId) {
        return sourceId != null && !sourceId.isBlank() && injectionReceipts.contains(sourceId);
    }

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

    /** Moves the reserve toward a target and returns the signed P/L impact. */
    public long adjustLossProvision(long targetMinor) {
        long target = Math.max(0L, targetMinor);
        long delta;
        try { delta = Math.subtractExact(target, lossProvisionMinor); }
        catch (ArithmeticException exception) { delta = target >= 0L ? Long.MAX_VALUE : Long.MIN_VALUE; }
        if (delta != 0L) {
            lossProvisionMinor = target;
            setDirty();
        }
        return delta;
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putLong("capitalMinor", capitalMinor);
        tag.putLong("cumulativeProfitLossMinor", cumulativeProfitLossMinor);
        tag.putLong("lossProvisionMinor", lossProvisionMinor);
        tag.putLong("lastSettlementDay", lastSettlementDay);
        tag.putBoolean("initialized", initialized);
        net.minecraft.nbt.ListTag injections = new net.minecraft.nbt.ListTag();
        for (String source : injectionReceipts) {
            CompoundTag entry = new CompoundTag();
            entry.putString("source", source);
            injections.add(entry);
        }
        tag.put("injectionReceipts", injections);
        return tag;
    }

    public static BankCapitalSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        BankCapitalSavedData data = new BankCapitalSavedData();
        data.capitalMinor = tag.getLong("capitalMinor");
        data.cumulativeProfitLossMinor = tag.getLong("cumulativeProfitLossMinor");
        data.lossProvisionMinor = Math.max(0L, tag.getLong("lossProvisionMinor"));
        data.lastSettlementDay = tag.getLong("lastSettlementDay");
        data.initialized = tag.getBoolean("initialized") || tag.contains("capitalMinor");
        net.minecraft.nbt.ListTag injections = tag.getList("injectionReceipts", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int i = Math.max(0, injections.size() - MAX_INJECTION_RECEIPTS); i < injections.size(); i++) {
            String source = injections.getCompound(i).getString("source");
            if (!source.isBlank()) data.injectionReceipts.add(source);
        }
        return data;
    }

    private static long saturatingAdd(long left, long right) {
        try { return Math.addExact(left, right); }
        catch (ArithmeticException exception) { return right >= 0L ? Long.MAX_VALUE : Long.MIN_VALUE; }
    }
}
