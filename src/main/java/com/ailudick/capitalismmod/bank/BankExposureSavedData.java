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
    private final Map<UUID, Map<String, AccountSnapshot>> accountSnapshots = new HashMap<>();

    public record Exposure(long depositsMinor, long loanDebtMinor, long overdueDebtMinor,
                           int overdueAccounts, long syncedAt) {}
    public record AccountSnapshot(String accountId, long depositsMinor, long loanDebtMinor,
                                  long overdueDebtMinor, int loanDaysRemaining, long syncedAt,
                                  int transactionCount, long lastTransactionAt) {
        public AccountSnapshot(String accountId, long depositsMinor, long loanDebtMinor,
                               long overdueDebtMinor, int loanDaysRemaining, long syncedAt) {
            this(accountId, depositsMinor, loanDebtMinor, overdueDebtMinor, loanDaysRemaining, syncedAt, 0, -1L);
        }
    }

    private BankExposureSavedData() {}

    public static BankExposureSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(BankExposureSavedData::new, BankExposureSavedData::load), ID);
    }

    public Map<UUID, Exposure> exposures() { return Map.copyOf(exposures); }
    public Map<UUID, Map<String, AccountSnapshot>> accountSnapshots() {
        Map<UUID, Map<String, AccountSnapshot>> copy = new HashMap<>();
        accountSnapshots.forEach((player, accounts) -> copy.put(player, Map.copyOf(accounts)));
        return Map.copyOf(copy);
    }

    public Exposure exposure(UUID playerId) {
        return playerId == null ? null : exposures.get(playerId);
    }

    /** Replaces one player's normalized exposure from their authoritative accounts. */
    public void sync(ServerPlayer player) {
        if (player == null) return;
        Map<String, AccountSnapshot> snapshots = expectedAccounts(player);
        accountSnapshots.put(player.getUUID(), snapshots);
        exposures.put(player.getUUID(), aggregate(snapshots, player.getServer().overworld().getGameTime()));
        setDirty();
    }

    /** Recomputes the normalized base-currency exposure from the player's accounts. */
    public Exposure expected(ServerPlayer player) {
        if (player == null) return null;
        return aggregate(expectedAccounts(player), player.getServer().overworld().getGameTime());
    }

    private static Map<String, AccountSnapshot> expectedAccounts(ServerPlayer player) {
        Map<String, AccountSnapshot> result = new HashMap<>();
        long syncedAt = player.getServer().overworld().getGameTime();
        for (BankAccount account : BankAccountHelper.getAccounts(player).values()) {
            long deposits = 0L, loans = 0L;
            for (var entry : account.balances().entrySet()) deposits = add(deposits, toBase(entry.getValue(), entry.getKey()));
            for (var entry : account.debts().entrySet()) loans = add(loans, toBase(entry.getValue(), entry.getKey()));
            long overdue = account.loanDaysRemaining() < 0 ? loans : 0L;
            result.put(account.id(), new AccountSnapshot(account.id(), deposits, loans, overdue,
                    account.loanDaysRemaining(), syncedAt, account.transactions().size(), lastTransactionAt(account)));
        }
        return result;
    }

    private static Exposure aggregate(Map<String, AccountSnapshot> snapshots, long syncedAt) {
        long deposits = 0L, loans = 0L, overdue = 0L;
        int overdueAccounts = 0;
        for (AccountSnapshot account : snapshots.values()) {
            deposits = add(deposits, account.depositsMinor());
            loans = add(loans, account.loanDebtMinor());
            overdue = add(overdue, account.overdueDebtMinor());
            if (account.loanDaysRemaining() < 0 && account.loanDebtMinor() > 0L) {
                overdueAccounts++;
            }
        }
        return new Exposure(deposits, loans, overdue, overdueAccounts, syncedAt);
    }

    private static long lastTransactionAt(BankAccount account) {
        return account.transactions().stream().mapToLong(BankTransaction::occurredAt).max().orElse(-1L);
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
        ListTag accounts = new ListTag();
        accountSnapshots.forEach((player, values) -> values.forEach((id, snapshot) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("player", player);
            entry.putString("account", id);
            entry.putLong("deposits", snapshot.depositsMinor());
            entry.putLong("loans", snapshot.loanDebtMinor());
            entry.putLong("overdue", snapshot.overdueDebtMinor());
            entry.putInt("loanDays", snapshot.loanDaysRemaining());
            entry.putLong("syncedAt", snapshot.syncedAt());
            entry.putInt("transactionCount", snapshot.transactionCount());
            entry.putLong("lastTransactionAt", snapshot.lastTransactionAt());
            accounts.add(entry);
        }));
        tag.put("accountSnapshots", accounts);
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
        ListTag accounts = tag.getList("accountSnapshots", Tag.TAG_COMPOUND);
        for (int i = 0; i < accounts.size(); i++) {
            CompoundTag entry = accounts.getCompound(i);
            if (!entry.hasUUID("player") || entry.getString("account").isBlank()) continue;
            AccountSnapshot snapshot = new AccountSnapshot(entry.getString("account"),
                    Math.max(0L, entry.getLong("deposits")), Math.max(0L, entry.getLong("loans")),
                    Math.max(0L, entry.getLong("overdue")), entry.getInt("loanDays"),
                    Math.max(0L, entry.getLong("syncedAt")), Math.max(0, entry.getInt("transactionCount")),
                    entry.contains("lastTransactionAt") ? entry.getLong("lastTransactionAt") : -1L);
            data.accountSnapshots.computeIfAbsent(entry.getUUID("player"), ignored -> new HashMap<>())
                    .putIfAbsent(snapshot.accountId(), snapshot);
        }
        return data;
    }
}
