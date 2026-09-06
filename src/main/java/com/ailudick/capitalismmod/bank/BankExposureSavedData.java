package com.ailudick.capitalismmod.bank;

import com.ailudick.capitalismmod.Config;
import com.ailudick.capitalismmod.currency.Currencies;
import com.ailudick.capitalismmod.currency.ExchangeRates;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Persistent base-currency bank exposure, including players who are offline. */
public final class BankExposureSavedData extends SavedData {
    private static final String ID = "capitalismmod_bank_exposure";
    private final Map<UUID, Exposure> exposures = new HashMap<>();

    public record Exposure(long depositsMinor, long loanDebtMinor, long overdueDebtMinor,
                           int overdueAccounts, long syncedAt) {}

    private BankExposureSavedData() {}

    public static BankExposureSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(BankExposureSavedData::new, BankExposureSavedData::load), ID);
    }

    public Map<UUID, Exposure> exposures() { return Map.copyOf(exposures); }

    /** Replaces one player's normalized exposure from their authoritative accounts. */
    public void sync(ServerPlayer player) {
        if (player == null) return;
        long deposits = 0L, loans = 0L, overdue = 0L;
        int overdueAccounts = 0;
        for (BankAccount account : BankAccountHelper.getAccounts(player).values()) {
            for (var entry : account.balances().entrySet()) {
                deposits = add(deposits, toBase(entry.getValue(), entry.getKey()));
            }
            long accountDebt = 0L;
            for (var entry : account.debts().entrySet()) {
                accountDebt = add(accountDebt, toBase(entry.getValue(), entry.getKey()));
            }
            loans = add(loans, accountDebt);
            if (account.loanDaysRemaining() < 0 && accountDebt > 0L) {
                overdue = add(overdue, accountDebt);
                overdueAccounts++;
            }
        }
        exposures.put(player.getUUID(), new Exposure(deposits, loans, overdue, overdueAccounts,
                player.getServer().overworld().getGameTime()));
        setDirty();
    }

    private static long toBase(long amount, String currencyId) {
        if (amount <= 0L || !Currencies.exists(currencyId)) return 0L;
        return ExchangeRates.convert(amount, Currencies.byId(currencyId), Config.defaultCurrency());
    }

    private static long add(long left, long right) {
        return right > Long.MAX_VALUE - left ? Long.MAX_VALUE : left + right;
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        exposures.forEach((uuid, exposure) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("player", uuid);
            entry.putLong("deposits", exposure.depositsMinor());
            entry.putLong("loans", exposure.loanDebtMinor());
            entry.putLong("overdue", exposure.overdueDebtMinor());
            entry.putInt("overdueAccounts", exposure.overdueAccounts());
            entry.putLong("syncedAt", exposure.syncedAt());
            list.add(entry);
        });
        tag.put("exposures", list);
        return tag;
    }

    public static BankExposureSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        BankExposureSavedData data = new BankExposureSavedData();
        ListTag list = tag.getList("exposures", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (!entry.hasUUID("player")) continue;
            data.exposures.put(entry.getUUID("player"), new Exposure(
                    Math.max(0L, entry.getLong("deposits")), Math.max(0L, entry.getLong("loans")),
                    Math.max(0L, entry.getLong("overdue")), Math.max(0, entry.getInt("overdueAccounts")),
                    Math.max(0L, entry.getLong("syncedAt"))));
        }
        return data;
    }
}
