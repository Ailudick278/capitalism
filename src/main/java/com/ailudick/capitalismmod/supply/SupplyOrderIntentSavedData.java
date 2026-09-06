package com.ailudick.capitalismmod.supply;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Durable prepayment intent used to recover an order interrupted before persistence. */
public final class SupplyOrderIntentSavedData extends SavedData {
    private static final String ID = "capitalismmod_supply_order_intents";
    private static final int MAX_INTENTS = 8192;
    private final List<Intent> intents = new ArrayList<>();

    public record Intent(String orderId, UUID buyerUuid, UUID supplierUuid, String companyName,
                         String itemId, int quantity, String originRegion, String destinationRegion,
                         long unitPrice, long createdAt, String buyerCompanyId, int qualityScore,
                         boolean paid) {
        public Intent {
            quantity = Math.max(0, quantity);
            unitPrice = Math.max(0L, unitPrice);
            buyerCompanyId = buyerCompanyId == null ? "" : buyerCompanyId;
            qualityScore = Math.max(0, Math.min(100, qualityScore));
        }

        public Intent withPaid(boolean value) {
            return new Intent(orderId, buyerUuid, supplierUuid, companyName, itemId, quantity,
                    originRegion, destinationRegion, unitPrice, createdAt, buyerCompanyId,
                    qualityScore, value);
        }
    }

    private SupplyOrderIntentSavedData() {}

    public static SupplyOrderIntentSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(SupplyOrderIntentSavedData::new, SupplyOrderIntentSavedData::load), ID);
    }

    public List<Intent> intents() { return List.copyOf(intents); }

    public Intent find(String orderId) {
        if (orderId == null || orderId.isBlank()) return null;
        return intents.stream().filter(intent -> orderId.equals(intent.orderId())).findFirst().orElse(null);
    }

    public void add(Intent intent) {
        if (intent == null || intent.orderId() == null || intent.orderId().isBlank()
                || intent.buyerUuid() == null || intent.supplierUuid() == null
                || intent.itemId() == null || intent.itemId().isBlank() || intent.quantity() <= 0
                || intent.unitPrice() <= 0L || find(intent.orderId()) != null) return;
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
        if (orderId == null || orderId.isBlank()) return;
        if (intents.removeIf(intent -> orderId.equals(intent.orderId()))) setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Intent intent : intents) {
            CompoundTag value = new CompoundTag();
            value.putString("orderId", intent.orderId());
            value.putUUID("buyer", intent.buyerUuid());
            value.putUUID("supplier", intent.supplierUuid());
            value.putString("companyName", intent.companyName());
            value.putString("itemId", intent.itemId());
            value.putInt("quantity", intent.quantity());
            value.putString("origin", intent.originRegion());
            value.putString("destination", intent.destinationRegion());
            value.putLong("unitPrice", intent.unitPrice());
            value.putLong("createdAt", intent.createdAt());
            value.putString("buyerCompanyId", intent.buyerCompanyId());
            value.putInt("qualityScore", intent.qualityScore());
            value.putBoolean("paid", intent.paid());
            list.add(value);
        }
        tag.put("intents", list);
        return tag;
    }

    public static SupplyOrderIntentSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        SupplyOrderIntentSavedData data = new SupplyOrderIntentSavedData();
        ListTag list = tag.getList("intents", Tag.TAG_COMPOUND);
        for (int i = Math.max(0, list.size() - MAX_INTENTS); i < list.size(); i++) {
            CompoundTag value = list.getCompound(i);
            if (!value.hasUUID("buyer") || !value.hasUUID("supplier")
                    || value.getString("orderId").isBlank() || value.getString("itemId").isBlank()
                    || value.getInt("quantity") <= 0 || value.getLong("unitPrice") <= 0L) continue;
            data.intents.add(new Intent(value.getString("orderId"), value.getUUID("buyer"),
                    value.getUUID("supplier"), value.getString("companyName"), value.getString("itemId"),
                    value.getInt("quantity"), value.getString("origin"), value.getString("destination"),
                    value.getLong("unitPrice"), value.getLong("createdAt"), value.getString("buyerCompanyId"),
                    value.getInt("qualityScore"), value.getBoolean("paid")));
        }
        return data;
    }
}
