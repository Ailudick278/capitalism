package com.ailudick.capitalismmod.market;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Durable prepayment intent for a commodity buy order. */
public final class CommodityBuyIntentSavedData extends SavedData {
    private static final String ID = "capitalismmod_commodity_buy_intents";
    private static final int MAX_INTENTS = 8192;
    private final List<Intent> intents = new ArrayList<>();

    public record Intent(String orderId, UUID buyerUuid, String itemId, int quantity,
                         long pricePerUnit, long createdAt, boolean paid) {
        public Intent {
            orderId = orderId == null ? "" : orderId;
            itemId = itemId == null ? "" : itemId;
            quantity = Math.max(0, quantity);
            pricePerUnit = Math.max(0L, pricePerUnit);
        }

        public Intent withPaid(boolean value) {
            return new Intent(orderId, buyerUuid, itemId, quantity, pricePerUnit, createdAt, value);
        }
    }

    private CommodityBuyIntentSavedData() {}

    public static CommodityBuyIntentSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(CommodityBuyIntentSavedData::new, CommodityBuyIntentSavedData::load), ID);
    }

    public List<Intent> intents() { return List.copyOf(intents); }

    public Intent find(String orderId) {
        if (orderId == null || orderId.isBlank()) return null;
        return intents.stream().filter(intent -> orderId.equals(intent.orderId())).findFirst().orElse(null);
    }

    public void add(Intent intent) {
        if (intent == null || intent.orderId().isBlank() || intent.buyerUuid() == null
                || intent.itemId().isBlank() || intent.quantity() <= 0 || intent.pricePerUnit() <= 0
                || find(intent.orderId()) != null) return;
        intents.add(intent);
        while (intents.size() > MAX_INTENTS) intents.remove(0);
        setDirty();
    }

    public void markPaid(String orderId) {
        Intent current = find(orderId);
        if (current == null || current.paid()) return;
        intents.set(intents.indexOf(current), current.withPaid(true));
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
            value.putUUID("buyer", intent.buyerUuid());
            value.putString("itemId", intent.itemId());
            value.putInt("quantity", intent.quantity());
            value.putLong("pricePerUnit", intent.pricePerUnit());
            value.putLong("createdAt", intent.createdAt());
            value.putBoolean("paid", intent.paid());
            list.add(value);
        }
        tag.put("intents", list);
        return tag;
    }

    public static CommodityBuyIntentSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        CommodityBuyIntentSavedData data = new CommodityBuyIntentSavedData();
        ListTag list = tag.getList("intents", Tag.TAG_COMPOUND);
        for (int i = Math.max(0, list.size() - MAX_INTENTS); i < list.size(); i++) {
            CompoundTag value = list.getCompound(i);
            if (!value.hasUUID("buyer") || value.getString("orderId").isBlank()
                    || value.getString("itemId").isBlank() || value.getInt("quantity") <= 0
                    || value.getLong("pricePerUnit") <= 0L) continue;
            data.intents.add(new Intent(value.getString("orderId"), value.getUUID("buyer"),
                    value.getString("itemId"), value.getInt("quantity"), value.getLong("pricePerUnit"),
                    value.getLong("createdAt"), value.getBoolean("paid")));
        }
        return data;
    }
}
