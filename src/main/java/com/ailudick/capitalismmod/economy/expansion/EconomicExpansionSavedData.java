package com.ailudick.capitalismmod.economy.expansion;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

/** Shared bounded persistence for the first-stage economic expansion framework. */
public final class EconomicExpansionSavedData extends SavedData {
    private static final String ID = "capitalismmod_economic_expansion";
    private static final int MAX_EVENTS = 4096;
    private final List<EconomicEvent> events = new ArrayList<>();

    private EconomicExpansionSavedData() {
    }

    public static EconomicExpansionSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(EconomicExpansionSavedData::new, EconomicExpansionSavedData::load), ID);
    }

    public List<EconomicEvent> events() {
        return List.copyOf(events);
    }

    public List<EconomicEvent> eventsFor(ExpansionSystem system) {
        return events.stream().filter(event -> event.system() == system).toList();
    }

    public boolean contains(String id) {
        return id != null && events.stream().anyMatch(event -> event.id().equals(id));
    }

    public boolean addOnce(EconomicEvent event) {
        if (event == null || contains(event.id())) return false;
        events.add(event);
        while (events.size() > MAX_EVENTS) events.remove(0);
        setDirty();
        return true;
    }

    public boolean replace(EconomicEvent event) {
        if (event == null) return false;
        for (int i = 0; i < events.size(); i++) {
            if (events.get(i).id().equals(event.id())) {
                events.set(i, event);
                setDirty();
                return true;
            }
        }
        return false;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (EconomicEvent event : events) {
            CompoundTag entry = new CompoundTag();
            entry.putString("id", event.id());
            entry.putString("system", event.system().id());
            entry.putString("type", event.type());
            entry.putLong("createdAt", event.createdAt());
            entry.putLong("startsAt", event.startsAt());
            entry.putLong("endsAt", event.endsAt());
            if (event.source() != null) putActor(entry, "source", event.source());
            if (event.target() != null) putActor(entry, "target", event.target());
            entry.putLong("amountMinor", event.amountMinor());
            entry.putString("currency", event.currencyId());
            entry.putString("status", event.status());
            list.add(entry);
        }
        tag.put("events", list);
        return tag;
    }

    private static void putActor(CompoundTag parent, String key, EconomicActorRef actor) {
        CompoundTag entry = new CompoundTag();
        entry.putString("type", actor.type());
        entry.putString("id", actor.id());
        parent.put(key, entry);
    }

    private static EconomicActorRef readActor(CompoundTag parent, String key) {
        if (!parent.contains(key, Tag.TAG_COMPOUND)) return null;
        CompoundTag entry = parent.getCompound(key);
        try {
            return new EconomicActorRef(entry.getString("type"), entry.getString("id"));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static EconomicExpansionSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        EconomicExpansionSavedData data = new EconomicExpansionSavedData();
        ListTag list = tag.getList("events", Tag.TAG_COMPOUND);
        for (int i = Math.max(0, list.size() - MAX_EVENTS); i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            try {
                ExpansionSystem system = java.util.Arrays.stream(ExpansionSystem.values())
                        .filter(value -> value.id().equals(entry.getString("system"))).findFirst().orElse(null);
                if (system == null || entry.getString("id").isBlank() || entry.getString("type").isBlank()) continue;
                data.events.add(new EconomicEvent(entry.getString("id"), system, entry.getString("type"),
                        entry.getLong("createdAt"), entry.getLong("startsAt"), entry.getLong("endsAt"),
                        readActor(entry, "source"), readActor(entry, "target"), Math.max(0L, entry.getLong("amountMinor")),
                        entry.getString("currency"), entry.getString("status")));
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed expansion records so a bad optional record cannot block world loading.
            }
        }
        return data;
    }
}
