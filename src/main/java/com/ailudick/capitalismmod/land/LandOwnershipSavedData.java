package com.ailudick.capitalismmod.land;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Persistent ownership chain for each land claim. */
public final class LandOwnershipSavedData extends SavedData {
    private static final String ID = "capitalismmod_land_ownership";
    private final Map<String, List<OwnershipEvent>> owners = new HashMap<>();
    public record OwnershipEvent(UUID owner, long time, String reason) {}
    private LandOwnershipSavedData() {}

    public static LandOwnershipSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(LandOwnershipSavedData::new, LandOwnershipSavedData::load), ID);
    }

    public List<OwnershipEvent> history(String claimId) { return List.copyOf(owners.getOrDefault(claimId, List.of())); }
    public boolean wasOwner(String claimId, UUID owner) {
        return owners.getOrDefault(claimId, List.of()).stream().anyMatch(event -> event.owner().equals(owner));
    }
    public void record(String claimId, UUID owner) { record(claimId, owner, 0L, "历史记录"); }
    public void record(String claimId, UUID owner, long time, String reason) {
        List<OwnershipEvent> history = new ArrayList<>(owners.getOrDefault(claimId, List.of()));
        if (history.stream().noneMatch(event -> event.owner().equals(owner) && event.reason().equals(reason))) {
            history.add(new OwnershipEvent(owner, time, reason)); owners.put(claimId, history); setDirty();
        }
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        owners.forEach((claimId, history) -> {
            CompoundTag entry = new CompoundTag(); entry.putString("claimId", claimId);
            ListTag people = new ListTag();
            history.forEach(event -> { CompoundTag person = new CompoundTag(); person.putUUID("uuid", event.owner());
                person.putLong("time", event.time()); person.putString("reason", event.reason()); people.add(person); });
            entry.put("owners", people); list.add(entry);
        });
        tag.put("history", list); return tag;
    }

    public static LandOwnershipSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        LandOwnershipSavedData data = new LandOwnershipSavedData();
        ListTag list = tag.getList("history", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i); String claimId = entry.getString("claimId");
            if (claimId.isBlank()) continue;
            List<OwnershipEvent> history = new ArrayList<>(); ListTag people = entry.getList("owners", Tag.TAG_COMPOUND);
            for (int j = 0; j < people.size(); j++) if (people.getCompound(j).hasUUID("uuid")) {
                CompoundTag person = people.getCompound(j);
                history.add(new OwnershipEvent(person.getUUID("uuid"), person.getLong("time"), person.getString("reason")));
            }
            data.owners.put(claimId, history);
        }
        return data;
    }
}
