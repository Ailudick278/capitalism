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

/** Persistent list of land claims awaiting a future auction or recovery decision. */
public final class LandAuctionSavedData extends SavedData {
    private static final String ID = "capitalismmod_land_auctions";
    private final Map<String, Auction> auctions = new HashMap<>();

    public record Auction(String claimId, UUID ownerUuid, String dimension, int chunkX, int chunkZ,
                          long listedAt, long taxOwed, long startPrice, long highestBid,
                          UUID highestBidder, long endsAt) {
        public Auction withBid(UUID bidder, long bid) {
            return new Auction(claimId, ownerUuid, dimension, chunkX, chunkZ, listedAt, taxOwed,
                    startPrice, bid, bidder, endsAt);
        }
    }

    private LandAuctionSavedData() {}

    public static LandAuctionSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(LandAuctionSavedData::new, LandAuctionSavedData::load), ID);
    }

    public java.util.Collection<Auction> all() { return java.util.List.copyOf(auctions.values()); }
    public Auction get(String claimId) { return auctions.get(claimId); }
    public void put(Auction auction) { auctions.put(auction.claimId(), auction); setDirty(); }
    public void remove(String claimId) { if (auctions.remove(claimId) != null) setDirty(); }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Auction auction : auctions.values()) {
            CompoundTag entry = new CompoundTag();
            entry.putString("claimId", auction.claimId());
            entry.putUUID("owner", auction.ownerUuid());
            entry.putString("dimension", auction.dimension());
            entry.putInt("x", auction.chunkX());
            entry.putInt("z", auction.chunkZ());
            entry.putLong("listedAt", auction.listedAt());
            entry.putLong("taxOwed", auction.taxOwed());
            entry.putLong("startPrice", auction.startPrice());
            entry.putLong("highestBid", auction.highestBid());
            if (auction.highestBidder() != null) entry.putUUID("highestBidder", auction.highestBidder());
            entry.putLong("endsAt", auction.endsAt());
            list.add(entry);
        }
        tag.put("auctions", list);
        return tag;
    }

    public static LandAuctionSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        LandAuctionSavedData data = new LandAuctionSavedData();
        ListTag list = tag.getList("auctions", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (entry.hasUUID("owner") && entry.contains("claimId")) {
                Auction auction = new Auction(entry.getString("claimId"), entry.getUUID("owner"),
                        entry.getString("dimension"), entry.getInt("x"), entry.getInt("z"),
                        entry.getLong("listedAt"), entry.getLong("taxOwed"), entry.getLong("startPrice"),
                        entry.getLong("highestBid"), entry.hasUUID("highestBidder") ? entry.getUUID("highestBidder") : null,
                        entry.getLong("endsAt"));
                data.auctions.put(auction.claimId(), auction);
            }
        }
        return data;
    }
}
