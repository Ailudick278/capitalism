package com.ailudick.capitalismmod.auction;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

/**
 * World-persisted registry of active auctions.
 */
public final class AuctionSavedData extends SavedData {
    private static final String ID = "capitalismmod_auctions";

    private final List<Auction> auctions = new ArrayList<>();

    private record State(List<Auction> auctions) {
        static final Codec<State> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Auction.CODEC.listOf().fieldOf("auctions").forGetter(State::auctions)
        ).apply(instance, State::new));
    }

    private AuctionSavedData() {
    }

    public static AuctionSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(AuctionSavedData::new, AuctionSavedData::load), ID);
    }

    public List<Auction> auctions() {
        return auctions;
    }

    public void addAuction(Auction auction) {
        auctions.add(auction);
        setDirty();
    }

    public void removeAuction(String auctionId) {
        auctions.removeIf(auction -> auction.id().equals(auctionId));
        setDirty();
    }

    public Auction findAuction(String auctionId) {
        for (Auction auction : auctions) {
            if (auction.id().equals(auctionId)) {
                return auction;
            }
        }
        return null;
    }

    public void replaceAuction(Auction auction) {
        for (int i = 0; i < auctions.size(); i++) {
            if (auctions.get(i).id().equals(auction.id())) {
                auctions.set(i, auction);
                setDirty();
                return;
            }
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        State state = new State(new ArrayList<>(auctions));
        State.CODEC.encodeStart(NbtOps.INSTANCE, state).result()
                .ifPresent(encoded -> tag.put("data", encoded));
        return tag;
    }

    public static AuctionSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        AuctionSavedData data = new AuctionSavedData();
        if (tag.contains("data")) {
            State.CODEC.parse(NbtOps.INSTANCE, tag.get("data")).result()
                    .ifPresent(state -> data.auctions.addAll(state.auctions()));
        }
        return data;
    }
}
