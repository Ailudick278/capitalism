package com.ailudick.capitalismmod.stock;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Durable intent for a single stock fill, used to recover share/payment ordering. */
public final class StockTradeIntentSavedData extends SavedData {
    private static final String ID = "capitalismmod_stock_trade_intents";
    private static final int MAX_INTENTS = 32768;
    private final List<Intent> intents = new ArrayList<>();

    public record Intent(String id, String stockId, String buyOrderId, String sellOrderId,
                         UUID buyer, UUID seller, int fill, long gross, int buyQuantityBefore,
                         int sellQuantityBefore, boolean buyerInitiated, long createdAt) {
        public Intent {
            id = id == null ? "" : id.trim(); stockId = stockId == null ? "" : stockId.trim();
            buyOrderId = buyOrderId == null ? "" : buyOrderId.trim();
            sellOrderId = sellOrderId == null ? "" : sellOrderId.trim();
        }
    }

    private StockTradeIntentSavedData() {}

    public static StockTradeIntentSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(StockTradeIntentSavedData::new, StockTradeIntentSavedData::load), ID);
    }

    public List<Intent> intents() { return List.copyOf(intents); }

    public void add(Intent intent) {
        if (intent == null || intent.id().isBlank() || intent.stockId().isBlank()
                || intent.buyOrderId().isBlank() || intent.sellOrderId().isBlank()
                || intent.buyer() == null || intent.seller() == null || intent.fill() <= 0
                || intent.gross() < 0L || intent.buyQuantityBefore() < intent.fill()
                || intent.sellQuantityBefore() < intent.fill()
                || intents.stream().anyMatch(existing -> existing.id().equals(intent.id()))) return;
        intents.add(intent);
        while (intents.size() > MAX_INTENTS) intents.remove(0);
        setDirty();
    }

    public void remove(String id) {
        if (id != null && intents.removeIf(intent -> id.equals(intent.id()))) setDirty();
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Intent intent : intents) {
            CompoundTag value = new CompoundTag();
            value.putString("id", intent.id()); value.putString("stock", intent.stockId());
            value.putString("buyOrder", intent.buyOrderId()); value.putString("sellOrder", intent.sellOrderId());
            value.putUUID("buyer", intent.buyer()); value.putUUID("seller", intent.seller());
            value.putInt("fill", intent.fill()); value.putLong("gross", intent.gross());
            value.putInt("buyBefore", intent.buyQuantityBefore()); value.putInt("sellBefore", intent.sellQuantityBefore());
            value.putBoolean("buyerInitiated", intent.buyerInitiated()); value.putLong("createdAt", intent.createdAt());
            list.add(value);
        }
        tag.put("intents", list); return tag;
    }

    public static StockTradeIntentSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        StockTradeIntentSavedData data = new StockTradeIntentSavedData();
        ListTag list = tag.getList("intents", Tag.TAG_COMPOUND);
        for (int i = Math.max(0, list.size() - MAX_INTENTS); i < list.size(); i++) {
            CompoundTag value = list.getCompound(i);
            try {
                if (!value.hasUUID("buyer") || !value.hasUUID("seller")) continue;
                data.add(new Intent(value.getString("id"), value.getString("stock"), value.getString("buyOrder"),
                        value.getString("sellOrder"), value.getUUID("buyer"), value.getUUID("seller"),
                        value.getInt("fill"), Math.max(0L, value.getLong("gross")), value.getInt("buyBefore"),
                        value.getInt("sellBefore"), value.getBoolean("buyerInitiated"), value.getLong("createdAt")));
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed optional intents so one bad fill cannot block world loading.
            }
        }
        return data;
    }
}
