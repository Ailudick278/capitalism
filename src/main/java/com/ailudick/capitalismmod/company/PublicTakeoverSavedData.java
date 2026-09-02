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

/** Persists targeted public takeover offers for listed company shares. */
public final class PublicTakeoverSavedData extends SavedData {
    private static final String ID = "capitalismmod_public_takeovers";
    public static final long OFFER_TTL = 168000L;
    private final List<Offer> offers = new ArrayList<>();

    public record Offer(String id, UUID buyerUuid, UUID sellerUuid, String stockId,
                        long pricePerShare, int quantity, long createdTick) {
        private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);
        public static final Codec<Offer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("id").forGetter(Offer::id),
                UUID_CODEC.fieldOf("buyerUuid").forGetter(Offer::buyerUuid),
                UUID_CODEC.fieldOf("sellerUuid").forGetter(Offer::sellerUuid),
                Codec.STRING.fieldOf("stockId").forGetter(Offer::stockId),
                Codec.LONG.fieldOf("pricePerShare").forGetter(Offer::pricePerShare),
                Codec.INT.fieldOf("quantity").forGetter(Offer::quantity),
                Codec.LONG.fieldOf("createdTick").forGetter(Offer::createdTick)
        ).apply(instance, Offer::new));
    }

    private record State(List<Offer> offers) {
        private static final Codec<State> CODEC = Offer.CODEC.listOf().fieldOf("offers")
                .codec().xmap(State::new, State::offers);
    }

    private PublicTakeoverSavedData() {
    }

    public static PublicTakeoverSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(PublicTakeoverSavedData::new, PublicTakeoverSavedData::load), ID);
    }

    public List<Offer> offers() {
        return List.copyOf(offers);
    }

    public Offer find(String id) {
        for (Offer offer : offers) {
            if (offer.id().equals(id)) return offer;
        }
        return null;
    }

    public void add(Offer offer) {
        if (offer != null && offer.buyerUuid() != null && offer.sellerUuid() != null
                && !offer.stockId().isBlank() && offer.pricePerShare() > 0 && offer.quantity() > 0) {
            offers.add(offer);
            setDirty();
        }
    }

    public void remove(String id) {
        if (offers.removeIf(offer -> offer.id().equals(id))) setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        State.CODEC.encodeStart(NbtOps.INSTANCE, new State(offers)).result()
                .ifPresent(encoded -> tag.put("data", encoded));
        return tag;
    }

    public static PublicTakeoverSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        PublicTakeoverSavedData data = new PublicTakeoverSavedData();
        if (tag.contains("data")) {
            State.CODEC.parse(NbtOps.INSTANCE, tag.get("data")).result()
                    .ifPresent(state -> data.offers.addAll(state.offers()));
        }
        return data;
    }
}
