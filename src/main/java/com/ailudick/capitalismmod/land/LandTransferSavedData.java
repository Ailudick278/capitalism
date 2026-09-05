package com.ailudick.capitalismmod.land;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Pending two-party land transfers. */
public final class LandTransferSavedData extends SavedData {
    private static final String ID = "capitalismmod_land_transfers";
    private final Map<UUID, Pending> pending = new HashMap<>();
    public record Pending(UUID from, UUID to, String dimension, int chunkX, int chunkZ, long price, long expiresAt) {}
    public static LandTransferSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(LandTransferSavedData::new, LandTransferSavedData::load), ID);
    }
    public Pending get(UUID target) { return pending.get(target); }
    public java.util.Collection<Pending> all() { return java.util.List.copyOf(pending.values()); }
    public Pending findForLand(String dimension, int chunkX, int chunkZ) {
        return pending.values().stream().filter(p -> p.dimension().equals(dimension)
                && p.chunkX() == chunkX && p.chunkZ() == chunkZ).findFirst().orElse(null);
    }
    public void put(Pending value) { pending.put(value.to(), value); setDirty(); }
    public void remove(UUID target) { if (pending.remove(target) != null) setDirty(); }
    public boolean removeForLand(UUID from, String dimension, int chunkX, int chunkZ) {
        UUID target = pending.values().stream().filter(p -> p.from().equals(from) && p.dimension().equals(dimension)
                && p.chunkX() == chunkX && p.chunkZ() == chunkZ).map(Pending::to).findFirst().orElse(null);
        if (target == null) return false;
        pending.remove(target); setDirty(); return true;
    }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Pending p : pending.values()) { CompoundTag n = new CompoundTag(); n.putUUID("from", p.from()); n.putUUID("to", p.to());
            n.putString("dimension", p.dimension()); n.putInt("x", p.chunkX()); n.putInt("z", p.chunkZ());
            n.putLong("price", p.price()); n.putLong("expires", p.expiresAt()); list.add(n); }
        tag.put("pending", list); return tag;
    }
    public static LandTransferSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        LandTransferSavedData data = new LandTransferSavedData(); ListTag list = tag.getList("pending", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) { CompoundTag n = list.getCompound(i);
            if (n.hasUUID("from") && n.hasUUID("to")) data.pending.put(n.getUUID("to"), new Pending(n.getUUID("from"), n.getUUID("to"),
                    n.getString("dimension"), n.getInt("x"), n.getInt("z"), n.getLong("price"), n.getLong("expires"))); }
        return data;
    }
}
