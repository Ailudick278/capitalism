package com.ailudick.capitalismmod.currency;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Durable two-phase record for a player currency conversion. */
public final class CurrencyExchangeIntentSavedData extends SavedData {
    private static final String ID = "capitalismmod_currency_exchange_intents";
    private static final int MAX_INTENTS = 8192;
    private final List<Intent> intents = new ArrayList<>();

    public record Intent(String id, UUID player, String from, String to, long amount, long converted, boolean paid) {
        public Intent {
            id = id == null ? "" : id;
            from = from == null ? "" : from;
            to = to == null ? "" : to;
            amount = Math.max(0L, amount);
            converted = Math.max(0L, converted);
        }
        public Intent withPaid(boolean value) { return new Intent(id, player, from, to, amount, converted, value); }
    }

    private CurrencyExchangeIntentSavedData() {}

    public static CurrencyExchangeIntentSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(CurrencyExchangeIntentSavedData::new, CurrencyExchangeIntentSavedData::load), ID);
    }

    public List<Intent> intents() { return List.copyOf(intents); }
    public Intent find(String id) { return intents.stream().filter(i -> id != null && id.equals(i.id())).findFirst().orElse(null); }

    public void add(Intent intent) {
        if (intent == null || intent.id().isBlank() || intent.player() == null || intent.from().isBlank()
                || intent.to().isBlank() || intent.amount() <= 0L || intent.converted() <= 0L || find(intent.id()) != null) return;
        intents.add(intent);
        while (intents.size() > MAX_INTENTS) intents.remove(0);
        setDirty();
    }

    public void markPaid(String id) {
        Intent current = find(id);
        if (current != null && !current.paid()) {
            intents.set(intents.indexOf(current), current.withPaid(true));
            setDirty();
        }
    }

    public void remove(String id) { if (id != null && intents.removeIf(i -> id.equals(i.id()))) setDirty(); }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Intent i : intents) {
            CompoundTag e = new CompoundTag(); e.putString("id", i.id()); e.putUUID("player", i.player());
            e.putString("from", i.from()); e.putString("to", i.to()); e.putLong("amount", i.amount());
            e.putLong("converted", i.converted()); e.putBoolean("paid", i.paid()); list.add(e);
        }
        tag.put("intents", list); return tag;
    }

    public static CurrencyExchangeIntentSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        CurrencyExchangeIntentSavedData data = new CurrencyExchangeIntentSavedData();
        ListTag list = tag.getList("intents", Tag.TAG_COMPOUND);
        for (int i = Math.max(0, list.size() - MAX_INTENTS); i < list.size(); i++) {
            CompoundTag e = list.getCompound(i);
            if (!e.hasUUID("player") || e.getString("id").isBlank() || e.getString("from").isBlank()
                    || e.getString("to").isBlank() || e.getLong("amount") <= 0L || e.getLong("converted") <= 0L) continue;
            data.intents.add(new Intent(e.getString("id"), e.getUUID("player"), e.getString("from"), e.getString("to"),
                    e.getLong("amount"), e.getLong("converted"), e.getBoolean("paid")));
        }
        return data;
    }
}
