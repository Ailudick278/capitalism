package com.ailudick.capitalismmod.land;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.UUID;
import java.util.List;

/** Persistent bounded audit trail for land operations. */
public final class LandOperationLogSavedData extends SavedData {
    private static final String ID = "capitalismmod_land_operation_logs";
    private static final int MAX_ENTRIES = 512;
    private final Deque<Entry> entries = new ArrayDeque<>();
    public record Entry(long time, UUID actor, String action, String dimension, int chunkX, int chunkZ) {}

    public static LandOperationLogSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(LandOperationLogSavedData::new, LandOperationLogSavedData::load), ID);
    }

    public void record(long time, UUID actor, String action, String dimension, int chunkX, int chunkZ) {
        Entry last = entries.peekLast();
        if (last != null && last.actor().equals(actor) && last.action().equals(action)
                && last.dimension().equals(dimension) && last.chunkX() == chunkX && last.chunkZ() == chunkZ
                && time - last.time() < 10L) return;
        entries.addLast(new Entry(time, actor, action, dimension, chunkX, chunkZ));
        while (entries.size() > MAX_ENTRIES) entries.removeFirst();
        setDirty();
    }

    public java.util.List<Entry> recentFor(UUID owner, java.util.Map<String, LandClaim> claims, int limit) {
        return recentFor(owner, claims, limit, null);
    }

    public java.util.List<Entry> recentFor(UUID owner, java.util.Map<String, LandClaim> claims, int limit,
                                           LandOwnershipSavedData ownership) {
        ArrayList<Entry> result = new ArrayList<>();
        var copy = entries.descendingIterator();
        while (copy.hasNext() && result.size() < limit) {
            Entry entry = copy.next();
            LandClaim claim = claims.get(entry.dimension() + ":" + entry.chunkX() + ":" + entry.chunkZ());
            boolean historicalOwner = ownership != null && ownership.wasOwner(
                    entry.dimension() + ":" + entry.chunkX() + ":" + entry.chunkZ(), owner);
            if ((claim != null && claim.ownerUuid().equals(owner)) || historicalOwner || entry.actor().equals(owner)) result.add(entry);
        }
        return List.copyOf(result);
    }

    public java.util.List<Entry> forLand(String dimension, int chunkX, int chunkZ, int offset, int limit) {
        ArrayList<Entry> result = new ArrayList<>();
        var copy = entries.descendingIterator();
        int skipped = 0;
        while (copy.hasNext() && result.size() < limit) {
            Entry entry = copy.next();
            if (!entry.dimension().equals(dimension) || entry.chunkX() != chunkX || entry.chunkZ() != chunkZ) continue;
            if (skipped++ < offset) continue;
            result.add(entry);
        }
        return List.copyOf(result);
    }

    public void clearFor(UUID owner, java.util.Map<String, LandClaim> claims) {
        entries.removeIf(entry -> {
            LandClaim claim = claims.get(entry.dimension() + ":" + entry.chunkX() + ":" + entry.chunkZ());
            return entry.actor().equals(owner) || (claim != null && claim.ownerUuid().equals(owner));
        });
        setDirty();
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Entry entry : entries) {
            CompoundTag nbt = new CompoundTag(); nbt.putLong("time", entry.time()); nbt.putUUID("actor", entry.actor());
            nbt.putString("action", entry.action()); nbt.putString("dimension", entry.dimension());
            nbt.putInt("chunkX", entry.chunkX()); nbt.putInt("chunkZ", entry.chunkZ()); list.add(nbt);
        }
        tag.put("entries", list); return tag;
    }

    public static LandOperationLogSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        LandOperationLogSavedData data = new LandOperationLogSavedData();
        ListTag list = tag.getList("entries", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag nbt = list.getCompound(i);
            if (nbt.hasUUID("actor")) data.entries.addLast(new Entry(nbt.getLong("time"), nbt.getUUID("actor"),
                    nbt.getString("action"), nbt.getString("dimension"), nbt.getInt("chunkX"), nbt.getInt("chunkZ")));
        }
        while (data.entries.size() > MAX_ENTRIES) data.entries.removeFirst();
        return data;
    }
}
