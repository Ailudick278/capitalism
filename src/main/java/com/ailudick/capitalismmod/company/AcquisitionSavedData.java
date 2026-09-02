package com.ailudick.capitalismmod.company;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** World-persisted company acquisition offers awaiting seller approval. */
public final class AcquisitionSavedData extends SavedData {
    private static final String ID = "capitalismmod_acquisitions";
    private final List<Offer> offers = new ArrayList<>();

    public record Offer(String id, UUID buyerUuid, UUID sellerUuid, String companyName, long price, long createdTick) {
        private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);
        public static final Codec<Offer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("id").forGetter(Offer::id),
                UUID_CODEC.fieldOf("buyerUuid").forGetter(Offer::buyerUuid),
                UUID_CODEC.fieldOf("sellerUuid").forGetter(Offer::sellerUuid),
                Codec.STRING.fieldOf("companyName").forGetter(Offer::companyName),
                Codec.LONG.fieldOf("price").forGetter(Offer::price),
                Codec.LONG.fieldOf("createdTick").forGetter(Offer::createdTick)
        ).apply(instance, Offer::new));
    }

    private record State(List<Offer> offers) {
        private static final Codec<State> CODEC = Offer.CODEC.listOf().fieldOf("offers")
                .codec().xmap(State::new, State::offers);
    }

    private AcquisitionSavedData() {
    }

    public static AcquisitionSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(AcquisitionSavedData::new, AcquisitionSavedData::load), ID);
    }

    public List<Offer> offers() {
        return List.copyOf(offers);
    }

    public void add(Offer offer) {
        if (offer != null && offer.buyerUuid() != null && offer.sellerUuid() != null
                && offer.price() > 0 && !offer.companyName().isBlank()) {
            offers.add(offer);
            setDirty();
        }
    }

    public Offer find(String id) {
        for (Offer offer : offers) {
            if (offer.id().equals(id)) {
                return offer;
            }
        }
        return null;
    }

    public void remove(String id) {
        if (offers.removeIf(offer -> offer.id().equals(id))) {
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        State.CODEC.encodeStart(NbtOps.INSTANCE, new State(offers)).result()
                .ifPresent(encoded -> tag.put("data", encoded));
        return tag;
    }

    public static AcquisitionSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        AcquisitionSavedData data = new AcquisitionSavedData();
        if (tag.contains("data")) {
            State.CODEC.parse(NbtOps.INSTANCE, tag.get("data")).result()
                    .ifPresent(state -> data.offers.addAll(state.offers()));
        }
        return data;
    }
}
