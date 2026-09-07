package com.ailudick.capitalismmod.population;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

/** Persistent bank accounts for virtual NPC households. All values use the default currency. */
public final class NpcBankingSavedData extends SavedData {
    private static final String ID = "capitalismmod_npc_banking";
    private static final int MAX_ACCOUNTS = 16384;
    private static final int MAX_TRANSACTIONS = 64;
    private final List<Account> accounts = new ArrayList<>();
    private long lastInterestDay = -1L;

    public record Transaction(String id, long day, String type, long amountMinor,
                              long balanceAfterMinor, long debtAfterMinor) {}
    public record Account(String id, String householdId, long balanceMinor, long debtMinor,
                          int loanDaysRemaining, List<Transaction> transactions) {
        public Account {
            transactions = transactions == null ? List.of() : List.copyOf(transactions);
        }
    }

    private NpcBankingSavedData() {}
    public static NpcBankingSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(NpcBankingSavedData::new, NpcBankingSavedData::load), ID);
    }
    public List<Account> accounts() { return List.copyOf(accounts); }
    public long lastInterestDay() { return lastInterestDay; }
    public boolean markInterestDay(long day) {
        if (day < 0L || lastInterestDay >= day) return false;
        lastInterestDay = day; setDirty(); return true;
    }
    public long totalDebt() { return accounts.stream().mapToLong(Account::debtMinor).reduce(0L, NpcBankingSavedData::safeAdd); }
    public long totalDeposits() { return accounts.stream().mapToLong(Account::balanceMinor).reduce(0L, NpcBankingSavedData::safeAdd); }
    public long overdueDebt() { return accounts.stream().filter(a -> a.loanDaysRemaining() < 0 && a.debtMinor() > 0L)
            .mapToLong(Account::debtMinor).reduce(0L, NpcBankingSavedData::safeAdd); }
    public Account find(String householdId) { return accounts.stream()
            .filter(a -> a.householdId().equals(householdId)).findFirst().orElse(null); }
    public Account ensure(String householdId) {
        Account existing = find(householdId);
        if (existing != null) return existing;
        Account created = new Account("npc-account:" + householdId, householdId, 0L, 0L, 0, List.of());
        accounts.add(created); setDirty(); return created;
    }
    public Account advanceLoanDay(String householdId) {
        Account old = ensure(householdId);
        if (old.debtMinor() <= 0L) return old;
        Account updated = new Account(old.id(), old.householdId(), old.balanceMinor(), old.debtMinor(),
                old.loanDaysRemaining() - 1, old.transactions());
        accounts.removeIf(a -> a.householdId().equals(householdId)); accounts.add(updated); setDirty(); return updated;
    }
    public Account transact(String householdId, long day, String type, long balanceDelta,
                            long debtDelta, int loanDaysRemaining) {
        Account old = ensure(householdId);
        long balance = safeAdd(old.balanceMinor(), balanceDelta);
        long debt = safeAdd(old.debtMinor(), debtDelta);
        if (balance < 0L || debt < 0L) return null;
        List<Transaction> txs = new ArrayList<>(old.transactions());
        txs.add(new Transaction(old.id() + ":" + day + ":" + txs.size(), day, type,
                balanceDelta + debtDelta, balance, debt));
        while (txs.size() > MAX_TRANSACTIONS) txs.remove(0);
        Account updated = new Account(old.id(), old.householdId(), balance, debt,
                debt == 0L ? 0 : loanDaysRemaining, txs);
        accounts.removeIf(a -> a.householdId().equals(householdId)); accounts.add(updated); setDirty();
        return updated;
    }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putLong("lastInterestDay", lastInterestDay);
        ListTag list = new ListTag();
        for (Account account : accounts) {
            CompoundTag e = new CompoundTag(); e.putString("id", account.id()); e.putString("household", account.householdId());
            e.putLong("balance", account.balanceMinor()); e.putLong("debt", account.debtMinor());
            e.putInt("loanDays", account.loanDaysRemaining());
            ListTag txs = new ListTag();
            for (Transaction tx : account.transactions()) {
                CompoundTag t = new CompoundTag(); t.putString("id", tx.id()); t.putLong("day", tx.day());
                t.putString("type", tx.type()); t.putLong("amount", tx.amountMinor());
                t.putLong("balance", tx.balanceAfterMinor()); t.putLong("debt", tx.debtAfterMinor()); txs.add(t);
            }
            e.put("transactions", txs); list.add(e);
        }
        tag.put("accounts", list); return tag;
    }
    public static NpcBankingSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        NpcBankingSavedData data = new NpcBankingSavedData();
        data.lastInterestDay = tag.getLong("lastInterestDay");
        ListTag list = tag.getList("accounts", Tag.TAG_COMPOUND);
        for (int i = Math.max(0, list.size() - MAX_ACCOUNTS); i < list.size(); i++) {
            CompoundTag e = list.getCompound(i);
            if (e.getString("id").isBlank() || e.getString("household").isBlank()
                    || e.getLong("balance") < 0L || e.getLong("debt") < 0L || e.getInt("loanDays") < -1) continue;
            List<Transaction> txs = new ArrayList<>(); ListTag txList = e.getList("transactions", Tag.TAG_COMPOUND);
            for (int j = Math.max(0, txList.size() - MAX_TRANSACTIONS); j < txList.size(); j++) {
                CompoundTag t = txList.getCompound(j);
                if (!t.getString("id").isBlank() && !t.getString("type").isBlank() && t.getLong("day") >= 0L
                        && t.getLong("balance") >= 0L && t.getLong("debt") >= 0L)
                    txs.add(new Transaction(t.getString("id"), t.getLong("day"), t.getString("type"),
                            t.getLong("amount"), t.getLong("balance"), t.getLong("debt")));
            }
            data.accounts.add(new Account(e.getString("id"), e.getString("household"), e.getLong("balance"),
                    e.getLong("debt"), e.getInt("loanDays"), txs));
        }
        return data;
    }
    private static long safeAdd(long a, long b) {
        try { return Math.addExact(a, b); } catch (ArithmeticException e) { return Long.MAX_VALUE; }
    }
}
