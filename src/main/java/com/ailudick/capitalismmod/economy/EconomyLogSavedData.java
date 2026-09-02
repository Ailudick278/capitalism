package com.ailudick.capitalismmod.economy;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** A bounded, world-persisted audit trail for successful wallet settlements. */
public final class EconomyLogSavedData extends SavedData {
    private static final String ID = "capitalismmod_economy_log";
    private static final int MAX_ENTRIES = 2000;
    private final List<Entry> entries = new ArrayList<>();

    public record Entry(long gameTime, UUID playerId, String action, String currencyId, long amount) {
    }

    private EconomyLogSavedData() {
    }

    public static EconomyLogSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(EconomyLogSavedData::new, EconomyLogSavedData::load), ID);
    }

    public List<Entry> entries() {
        return List.copyOf(entries);
    }

    public void append(long gameTime, UUID playerId, String action, String currencyId, long amount) {
        if (playerId == null || action == null || currencyId == null || amount <= 0) {
            return;
        }
        entries.add(new Entry(gameTime, playerId, action, currencyId, amount));
        if (entries.size() > MAX_ENTRIES) {
            entries.subList(0, entries.size() - MAX_ENTRIES).clear();
        }
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Entry entry : entries) {
            CompoundTag nbt = new CompoundTag();
            nbt.putLong("time", entry.gameTime());
            nbt.putUUID("player", entry.playerId());
            nbt.putString("action", entry.action());
            nbt.putString("currency", entry.currencyId());
            nbt.putLong("amount", entry.amount());
            list.add(nbt);
        }
        tag.put("entries", list);
        return tag;
    }

    public static EconomyLogSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        EconomyLogSavedData data = new EconomyLogSavedData();
        ListTag list = tag.getList("entries", Tag.TAG_COMPOUND);
        for (int i = Math.max(0, list.size() - MAX_ENTRIES); i < list.size(); i++) {
            CompoundTag nbt = list.getCompound(i);
            if (nbt.hasUUID("player") && nbt.getLong("amount") > 0) {
                data.entries.add(new Entry(nbt.getLong("time"), nbt.getUUID("player"),
                        nbt.getString("action"), nbt.getString("currency"), nbt.getLong("amount")));
            }
        }
        return data;
    }
}
