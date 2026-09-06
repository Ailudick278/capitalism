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

/** Durable share-escrow intent for a stock sell order. */
public final class StockSellIntentSavedData extends SavedData {
    private static final String ID = "capitalismmod_stock_sell_intents";
    private static final int MAX_INTENTS = 8192;
    private final List<Intent> intents = new ArrayList<>();

    public record Intent(String orderId, UUID sellerUuid, String stockId, int quantity,
                         long pricePerUnit, long createdAt, boolean sharesEscrowed) {
        public Intent {
            orderId = orderId == null ? "" : orderId;
            stockId = stockId == null ? "" : stockId;
            quantity = Math.max(0, quantity);
            pricePerUnit = Math.max(0L, pricePerUnit);
        }
        public Intent withSharesEscrowed(boolean value) {
            return new Intent(orderId, sellerUuid, stockId, quantity, pricePerUnit, createdAt, value);
        }
    }

    private StockSellIntentSavedData() {}

    public static StockSellIntentSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(StockSellIntentSavedData::new, StockSellIntentSavedData::load), ID);
    }
    public List<Intent> intents() { return List.copyOf(intents); }
    public Intent find(String orderId) {
        if (orderId == null || orderId.isBlank()) return null;
        return intents.stream().filter(intent -> orderId.equals(intent.orderId())).findFirst().orElse(null);
    }
    public void add(Intent intent) {
        if (intent == null || intent.orderId().isBlank() || intent.sellerUuid() == null
                || intent.stockId().isBlank() || intent.quantity() <= 0 || intent.pricePerUnit() <= 0
                || find(intent.orderId()) != null) return;
        intents.add(intent);
        while (intents.size() > MAX_INTENTS) intents.remove(0);
        setDirty();
    }
    public void markSharesEscrowed(String orderId) {
        Intent current = find(orderId);
        if (current == null || current.sharesEscrowed()) return;
        intents.set(intents.indexOf(current), current.withSharesEscrowed(true));
        setDirty();
    }
    public void remove(String orderId) {
        if (orderId != null && intents.removeIf(intent -> orderId.equals(intent.orderId()))) setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Intent intent : intents) {
            CompoundTag value = new CompoundTag();
            value.putString("orderId", intent.orderId());
            value.putUUID("seller", intent.sellerUuid());
            value.putString("stockId", intent.stockId());
            value.putInt("quantity", intent.quantity());
            value.putLong("pricePerUnit", intent.pricePerUnit());
            value.putLong("createdAt", intent.createdAt());
            value.putBoolean("sharesEscrowed", intent.sharesEscrowed());
            list.add(value);
        }
        tag.put("intents", list);
        return tag;
    }
    public static StockSellIntentSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        StockSellIntentSavedData data = new StockSellIntentSavedData();
        ListTag list = tag.getList("intents", Tag.TAG_COMPOUND);
        for (int i = Math.max(0, list.size() - MAX_INTENTS); i < list.size(); i++) {
            CompoundTag value = list.getCompound(i);
            if (!value.hasUUID("seller") || value.getString("orderId").isBlank()
                    || value.getString("stockId").isBlank() || value.getInt("quantity") <= 0
                    || value.getLong("pricePerUnit") <= 0L) continue;
            data.intents.add(new Intent(value.getString("orderId"), value.getUUID("seller"),
                    value.getString("stockId"), value.getInt("quantity"), value.getLong("pricePerUnit"),
                    value.getLong("createdAt"), value.getBoolean("sharesEscrowed")));
        }
        return data;
    }
}
