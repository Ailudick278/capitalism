package com.ailudick.capitalismmod.bank;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Durable intent for a physical-cash deposit into a bank account. */
public final class BankCashDepositIntentSavedData extends SavedData {
    private static final String ID = "capitalismmod_bank_cash_deposit_intents";
    private static final int MAX_INTENTS = 8192;
    private final List<Intent> intents = new ArrayList<>();

    public record Intent(String source, UUID playerUuid, String accountId, String currencyId,
                         long amount, long physicalBefore, boolean cashRemoved) {
        public Intent {
            source = source == null ? "" : source;
            accountId = accountId == null ? "" : accountId;
            currencyId = currencyId == null ? "" : currencyId;
            amount = Math.max(0L, amount);
            physicalBefore = Math.max(0L, physicalBefore);
        }

        public Intent withCashRemoved(boolean value) {
            return new Intent(source, playerUuid, accountId, currencyId, amount, physicalBefore, value);
        }
    }

    private BankCashDepositIntentSavedData() {}

    public static BankCashDepositIntentSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(BankCashDepositIntentSavedData::new,
                        BankCashDepositIntentSavedData::load), ID);
    }

    public List<Intent> intents() { return List.copyOf(intents); }

    public Intent find(String source) {
        if (source == null || source.isBlank()) return null;
        return intents.stream().filter(intent -> source.equals(intent.source())).findFirst().orElse(null);
    }

    public void add(Intent intent) {
        if (intent == null || intent.source().isBlank() || intent.playerUuid() == null
                || intent.accountId().isBlank() || intent.currencyId().isBlank()
                || intent.amount() <= 0L || find(intent.source()) != null) return;
        intents.add(intent);
        while (intents.size() > MAX_INTENTS) intents.remove(0);
        setDirty();
    }

    public void markCashRemoved(String source) {
        Intent current = find(source);
        if (current == null || current.cashRemoved()) return;
        intents.set(intents.indexOf(current), current.withCashRemoved(true));
        setDirty();
    }

    public void remove(String source) {
        if (source != null && intents.removeIf(intent -> source.equals(intent.source()))) setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Intent intent : intents) {
            CompoundTag value = new CompoundTag();
            value.putString("source", intent.source());
            value.putUUID("player", intent.playerUuid());
            value.putString("account", intent.accountId());
            value.putString("currency", intent.currencyId());
            value.putLong("amount", intent.amount());
            value.putLong("physicalBefore", intent.physicalBefore());
            value.putBoolean("cashRemoved", intent.cashRemoved());
            list.add(value);
        }
        tag.put("intents", list);
        return tag;
    }

    public static BankCashDepositIntentSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        BankCashDepositIntentSavedData data = new BankCashDepositIntentSavedData();
        ListTag list = tag.getList("intents", Tag.TAG_COMPOUND);
        for (int i = Math.max(0, list.size() - MAX_INTENTS); i < list.size(); i++) {
            CompoundTag value = list.getCompound(i);
            if (!value.hasUUID("player") || value.getString("source").isBlank()
                    || value.getString("account").isBlank() || value.getString("currency").isBlank()
                    || value.getLong("amount") <= 0L) continue;
            data.intents.add(new Intent(value.getString("source"), value.getUUID("player"),
                    value.getString("account"), value.getString("currency"), value.getLong("amount"),
                    value.getLong("physicalBefore"), value.getBoolean("cashRemoved")));
        }
        return data;
    }
}
