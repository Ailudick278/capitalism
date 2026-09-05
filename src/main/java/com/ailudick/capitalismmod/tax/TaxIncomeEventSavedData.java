package com.ailudick.capitalismmod.tax;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;

/** Persistent idempotency registry for income-tax assessment events. */
public final class TaxIncomeEventSavedData extends SavedData {
    private static final String ID = "capitalismmod_tax_income_events";
    private final Set<String> assessedEvents = new HashSet<>();
    private TaxIncomeEventSavedData() {}

    public static TaxIncomeEventSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(TaxIncomeEventSavedData::new, TaxIncomeEventSavedData::load), ID);
    }

    public boolean contains(String eventId) { return assessedEvents.contains(eventId); }
    public void add(String eventId) { assessedEvents.add(eventId); setDirty(); }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        var list = new net.minecraft.nbt.ListTag();
        assessedEvents.forEach(id -> list.add(net.minecraft.nbt.StringTag.valueOf(id)));
        tag.put("events", list);
        return tag;
    }

    public static TaxIncomeEventSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        TaxIncomeEventSavedData data = new TaxIncomeEventSavedData();
        var list = tag.getList("events", 8);
        for (int i = 0; i < list.size(); i++) data.assessedEvents.add(list.getString(i));
        return data;
    }
}
