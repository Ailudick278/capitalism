package com.ailudick.capitalismmod.land;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

/** Optional per-claim member permissions, kept separate for old-save compatibility. */
public final class LandPermissionSavedData extends SavedData {
    private static final String ID = "capitalismmod_land_permissions";
    private final Map<String, Integer> permissions = new HashMap<>();

    private record State(Map<String, Integer> permissions) {
        private static final Codec<State> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("permissions").forGetter(State::permissions)
        ).apply(instance, State::new));
    }

    private LandPermissionSavedData() {}

    public static LandPermissionSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(LandPermissionSavedData::new, LandPermissionSavedData::load), ID);
    }

    public boolean canBuild(String claimId) { return permissions.getOrDefault(claimId, 3) % 2 == 1; }
    public boolean canInteract(String claimId) { return permissions.getOrDefault(claimId, 3) >= 2; }
    public boolean canContainer(String claimId) { return (permissions.getOrDefault(claimId, 3) & 4) != 0; }
    public boolean canRedstone(String claimId) { return (permissions.getOrDefault(claimId, 3) & 8) != 0; }

    public void set(String claimId, boolean build, boolean interact, boolean container, boolean redstone) {
        permissions.put(claimId, (build ? 1 : 0) + (interact ? 2 : 0)
                + (container ? 4 : 0) + (redstone ? 8 : 0));
        setDirty();
    }

    public void remove(String claimId) {
        if (permissions.remove(claimId) != null) setDirty();
    }

    public void removeOrphans(Map<String, LandClaim> claims) {
        if (permissions.keySet().removeIf(id -> !claims.containsKey(id))) setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        State.CODEC.encodeStart(NbtOps.INSTANCE, new State(permissions)).result()
                .ifPresent(encoded -> tag.put("data", encoded));
        return tag;
    }

    public static LandPermissionSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        LandPermissionSavedData data = new LandPermissionSavedData();
        if (tag.contains("data")) State.CODEC.parse(NbtOps.INSTANCE, tag.get("data")).result()
                .ifPresent(state -> data.permissions.putAll(state.permissions()));
        return data;
    }
}
