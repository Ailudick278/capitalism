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

/** Durable intent for an auction listing while its goods are being escrowed. */
public final class AuctionListingIntentSavedData extends SavedData {
    private static final String ID = "capitalismmod_auction_listing_intents";
    private static final int MAX_INTENTS = 8192;
    private final List<Intent> intents = new ArrayList<>();

    public record Intent(String auctionId, UUID seller, String itemId, int quantity, long startingPrice,
                         long endTick, long warehouseBefore, boolean escrowed) {
        public Intent {
            auctionId = auctionId == null ? "" : auctionId;
            itemId = itemId == null ? "" : itemId;
            quantity = Math.max(0, quantity);
            startingPrice = Math.max(0L, startingPrice);
            warehouseBefore = Math.max(0L, warehouseBefore);
        }
        public Intent withEscrowed(boolean value) {
            return new Intent(auctionId, seller, itemId, quantity, startingPrice,
                    endTick, warehouseBefore, value);
        }
    }

    private AuctionListingIntentSavedData() {}

    public static AuctionListingIntentSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(AuctionListingIntentSavedData::new, AuctionListingIntentSavedData::load), ID);
    }
    public List<Intent> intents() { return List.copyOf(intents); }
    public void add(Intent intent) {
        if (intent == null || intent.auctionId().isBlank() || intent.seller() == null
                || intent.itemId().isBlank() || intent.quantity() <= 0 || intent.startingPrice() <= 0
                || find(intent.auctionId()) != null) return;
        intents.add(intent);
        while (intents.size() > MAX_INTENTS) intents.remove(0);
        setDirty();
    }
    public Intent find(String auctionId) {
        if (auctionId == null || auctionId.isBlank()) return null;
        return intents.stream().filter(intent -> auctionId.equals(intent.auctionId())).findFirst().orElse(null);
    }
    public void markEscrowed(String auctionId) {
        Intent current = find(auctionId);
        if (current == null || current.escrowed()) return;
        intents.set(intents.indexOf(current), current.withEscrowed(true));
        setDirty();
    }
    public void remove(String auctionId) {
        if (auctionId != null && intents.removeIf(intent -> auctionId.equals(intent.auctionId()))) setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Intent intent : intents) {
            CompoundTag value = new CompoundTag();
            value.putString("auctionId", intent.auctionId());
            value.putUUID("seller", intent.seller());
            value.putString("itemId", intent.itemId());
            value.putInt("quantity", intent.quantity());
            value.putLong("startingPrice", intent.startingPrice());
            value.putLong("endTick", intent.endTick());
            value.putLong("warehouseBefore", intent.warehouseBefore());
            value.putBoolean("escrowed", intent.escrowed());
            list.add(value);
        }
        tag.put("intents", list);
        return tag;
    }
    public static AuctionListingIntentSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        AuctionListingIntentSavedData data = new AuctionListingIntentSavedData();
        ListTag list = tag.getList("intents", Tag.TAG_COMPOUND);
        for (int i = Math.max(0, list.size() - MAX_INTENTS); i < list.size(); i++) {
            CompoundTag value = list.getCompound(i);
            if (!value.hasUUID("seller") || value.getString("auctionId").isBlank()
                    || value.getString("itemId").isBlank() || value.getInt("quantity") <= 0
                    || value.getLong("startingPrice") <= 0L) continue;
            data.intents.add(new Intent(value.getString("auctionId"), value.getUUID("seller"),
                    value.getString("itemId"), value.getInt("quantity"), value.getLong("startingPrice"),
                    value.getLong("endTick"), value.getLong("warehouseBefore"), value.getBoolean("escrowed")));
        }
        return data;
    }
}
