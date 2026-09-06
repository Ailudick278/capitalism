package com.ailudick.capitalismmod.market;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Durable inventory-escrow intent for a commodity sell order. */
public final class CommoditySellIntentSavedData extends SavedData {
    private static final String ID = "capitalismmod_commodity_sell_intents";
    private static final int MAX_INTENTS = 8192;
    private final List<Intent> intents = new ArrayList<>();

    public record Intent(String orderId, UUID sellerUuid, String itemId, int quantity,
                         long pricePerUnit, long createdAt, long warehouseBefore, boolean escrowed) {
        public Intent {
            orderId = orderId == null ? "" : orderId;
            itemId = itemId == null ? "" : itemId;
            quantity = Math.max(0, quantity);
            pricePerUnit = Math.max(0L, pricePerUnit);
            warehouseBefore = Math.max(0L, warehouseBefore);
        }

        public Intent withEscrowed(boolean value) {
            return new Intent(orderId, sellerUuid, itemId, quantity, pricePerUnit, createdAt, warehouseBefore, value);
        }
    }

    private CommoditySellIntentSavedData() {}

    public static CommoditySellIntentSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(CommoditySellIntentSavedData::new, CommoditySellIntentSavedData::load), ID);
    }

    public List<Intent> intents() { return List.copyOf(intents); }

    public void add(Intent intent) {
        if (intent == null || intent.orderId().isBlank() || intent.sellerUuid() == null
                || intent.itemId().isBlank() || intent.quantity() <= 0 || intent.pricePerUnit() <= 0
                || find(intent.orderId()) != null) return;
        intents.add(intent);
        while (intents.size() > MAX_INTENTS) intents.remove(0);
        setDirty();
    }

    public Intent find(String orderId) {
        if (orderId == null || orderId.isBlank()) return null;
        return intents.stream().filter(intent -> orderId.equals(intent.orderId())).findFirst().orElse(null);
    }

    public void markEscrowed(String orderId) {
        Intent current = find(orderId);
        if (current == null || current.escrowed()) return;
        intents.set(intents.indexOf(current), current.withEscrowed(true));
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
            value.putString("itemId", intent.itemId());
            value.putInt("quantity", intent.quantity());
            value.putLong("pricePerUnit", intent.pricePerUnit());
            value.putLong("createdAt", intent.createdAt());
            value.putLong("warehouseBefore", intent.warehouseBefore());
            value.putBoolean("escrowed", intent.escrowed());
            list.add(value);
        }
        tag.put("intents", list);
        return tag;
    }

    public static CommoditySellIntentSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        CommoditySellIntentSavedData data = new CommoditySellIntentSavedData();
        ListTag list = tag.getList("intents", Tag.TAG_COMPOUND);
        for (int i = Math.max(0, list.size() - MAX_INTENTS); i < list.size(); i++) {
            CompoundTag value = list.getCompound(i);
            if (!value.hasUUID("seller") || value.getString("orderId").isBlank()
                    || value.getString("itemId").isBlank() || value.getInt("quantity") <= 0
                    || value.getLong("pricePerUnit") <= 0L) continue;
            data.intents.add(new Intent(value.getString("orderId"), value.getUUID("seller"),
                    value.getString("itemId"), value.getInt("quantity"), value.getLong("pricePerUnit"),
                    value.getLong("createdAt"), value.getLong("warehouseBefore"), value.getBoolean("escrowed")));
        }
        return data;
    }
}
