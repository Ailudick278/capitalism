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

/** Server-level registry of claimed chunks. */
public final class LandSavedData extends SavedData {
    private static final String ID = "capitalismmod_land_claims";
    private final Map<String, LandClaim> claims = new HashMap<>();

    private record State(Map<String, LandClaim> claims) {
        private static final Codec<State> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.unboundedMap(Codec.STRING, LandClaim.CODEC).fieldOf("claims").forGetter(State::claims)
        ).apply(instance, State::new));
    }

    private LandSavedData() {}

    public static LandSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(LandSavedData::new, LandSavedData::load), ID);
    }

    public Map<String, LandClaim> claims() { return Map.copyOf(claims); }
    public LandClaim get(String id) { return claims.get(id); }
    public void put(LandClaim claim) { claims.put(claim.id(), claim); setDirty(); }
    public void remove(String id) { if (claims.remove(id) != null) setDirty(); }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        State.CODEC.encodeStart(NbtOps.INSTANCE, new State(claims)).result()
                .ifPresent(encoded -> tag.put("data", encoded));
        return tag;
    }

    public static LandSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        LandSavedData data = new LandSavedData();
        if (tag.contains("data")) State.CODEC.parse(NbtOps.INSTANCE, tag.get("data")).result()
                .ifPresent(state -> data.claims.putAll(state.claims()));
        return data;
    }
}
