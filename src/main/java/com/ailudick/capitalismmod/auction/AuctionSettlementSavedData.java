package com.ailudick.capitalismmod.auction;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;

/** Durable idempotency ledger for auction escrow settlement and release. */
public final class AuctionSettlementSavedData extends SavedData {
    private static final String ID = "capitalismmod_auction_settlements";
    private static final int MAX_RECORDS = 8192;
    private final Set<String> settled = new HashSet<>();

    private AuctionSettlementSavedData() {
    }

    public static AuctionSettlementSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(AuctionSettlementSavedData::new, AuctionSettlementSavedData::load), ID);
    }

    public boolean has(String auctionId) {
        return auctionId != null && !auctionId.isBlank() && settled.contains(auctionId);
    }

    public Set<String> settledIds() {
        return Set.copyOf(settled);
    }

    public void record(String auctionId) {
        if (auctionId == null || auctionId.isBlank() || !settled.add(auctionId)) return;
        while (settled.size() > MAX_RECORDS) settled.remove(settled.iterator().next());
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        settled.forEach(id -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("id", id);
            list.add(entry);
        });
        tag.put("settled", list);
        return tag;
    }

    public static AuctionSettlementSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        AuctionSettlementSavedData data = new AuctionSettlementSavedData();
        ListTag list = tag.getList("settled", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            String id = list.getCompound(i).getString("id");
            if (!id.isBlank()) data.settled.add(id);
        }
        while (data.settled.size() > MAX_RECORDS) data.settled.remove(data.settled.iterator().next());
        return data;
    }
}
