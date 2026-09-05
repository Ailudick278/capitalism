package com.ailudick.capitalismmod.business;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Server-level registry for sole proprietor registrations. */
public final class IndividualBusinessSavedData extends SavedData {
    private static final String ID = "capitalismmod_individual_businesses";
    private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);
    private final Map<UUID, IndividualBusiness> businesses = new HashMap<>();

    private record State(Map<UUID, IndividualBusiness> businesses) {
        private static final Codec<State> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.unboundedMap(UUID_CODEC, IndividualBusiness.CODEC).fieldOf("businesses")
                        .forGetter(State::businesses)
        ).apply(instance, State::new));
    }

    private IndividualBusinessSavedData() {
    }

    public static IndividualBusinessSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(IndividualBusinessSavedData::new, IndividualBusinessSavedData::load), ID);
    }

    public IndividualBusiness get(UUID ownerUuid) {
        return businesses.get(ownerUuid);
    }

    public IndividualBusiness findByBusinessId(String businessId) {
        return businesses.values().stream().filter(business -> business.businessId().equals(businessId)).findFirst().orElse(null);
    }

    public void put(IndividualBusiness business) {
        if (business != null && business.ownerUuid() != null) {
            businesses.put(business.ownerUuid(), business);
            setDirty();
        }
    }

    public void remove(UUID ownerUuid) {
        if (ownerUuid != null && businesses.remove(ownerUuid) != null) {
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        State.CODEC.encodeStart(NbtOps.INSTANCE, new State(businesses)).result()
                .ifPresent(encoded -> tag.put("data", encoded));
        return tag;
    }

    public static IndividualBusinessSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        IndividualBusinessSavedData data = new IndividualBusinessSavedData();
        if (tag.contains("data")) {
            State.CODEC.parse(NbtOps.INSTANCE, tag.get("data")).result()
                    .ifPresent(state -> data.businesses.putAll(state.businesses()));
        }
        return data;
    }
}
