package com.ailudick.capitalismmod.auction;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Durable journal for bids whose money movement completed before auction state was saved. */
public final class AuctionBidSavedData extends SavedData {
    private static final String ID = "capitalismmod_auction_bids";
    private static final int MAX_RECORDS = 8192;
    private final List<Bid> bids = new ArrayList<>();

    public record Bid(String auctionId, UUID bidder, long amount) {
    }

    private AuctionBidSavedData() {
    }

    public static AuctionBidSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(AuctionBidSavedData::new, AuctionBidSavedData::load), ID);
    }

    public Bid find(String auctionId, UUID bidder, long amount) {
        return bids.stream().filter(bid -> bid.auctionId().equals(auctionId)
                && bid.bidder().equals(bidder) && bid.amount() == amount).findFirst().orElse(null);
    }

    public List<Bid> bids() {
        return List.copyOf(bids);
    }

    public void record(Bid bid) {
        if (bid == null || bid.auctionId() == null || bid.auctionId().isBlank()
                || bid.bidder() == null || bid.amount() <= 0L || find(bid.auctionId(), bid.bidder(), bid.amount()) != null) {
            return;
        }
        bids.add(bid);
        while (bids.size() > MAX_RECORDS) bids.remove(0);
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Bid bid : bids) {
            CompoundTag entry = new CompoundTag();
            entry.putString("auctionId", bid.auctionId());
            entry.putUUID("bidder", bid.bidder());
            entry.putLong("amount", bid.amount());
            list.add(entry);
        }
        tag.put("bids", list);
        return tag;
    }

    public static AuctionBidSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        AuctionBidSavedData data = new AuctionBidSavedData();
        ListTag list = tag.getList("bids", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (!entry.getString("auctionId").isBlank() && entry.hasUUID("bidder") && entry.getLong("amount") > 0L) {
                data.bids.add(new Bid(entry.getString("auctionId"), entry.getUUID("bidder"), entry.getLong("amount")));
            }
        }
        while (data.bids.size() > MAX_RECORDS) data.bids.remove(0);
        return data;
    }
}
