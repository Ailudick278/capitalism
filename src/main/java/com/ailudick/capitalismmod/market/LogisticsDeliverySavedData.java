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

/** Durable idempotency ledger for cargo that has been credited to a warehouse. */
public final class LogisticsDeliverySavedData extends SavedData {
    private static final String ID = "capitalismmod_logistics_deliveries";
    private static final int MAX_RECORDS = 8192;
    private final List<Delivery> deliveries = new ArrayList<>();

    public record Delivery(String shipmentId, UUID buyer, String itemId, int quantity, long deliveredAt) {
    }

    private LogisticsDeliverySavedData() {
    }

    public static LogisticsDeliverySavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(LogisticsDeliverySavedData::new, LogisticsDeliverySavedData::load), ID);
    }

    public boolean hasShipment(String shipmentId) {
        return shipmentId != null && !shipmentId.isBlank()
                && deliveries.stream().anyMatch(delivery -> shipmentId.equals(delivery.shipmentId()));
    }

    public void record(Delivery delivery) {
        if (delivery == null || delivery.shipmentId() == null || delivery.shipmentId().isBlank()
                || delivery.buyer() == null || delivery.itemId() == null || delivery.itemId().isBlank()
                || delivery.quantity() <= 0 || hasShipment(delivery.shipmentId())) {
            return;
        }
        deliveries.add(delivery);
        while (deliveries.size() > MAX_RECORDS) {
            deliveries.remove(0);
        }
        setDirty();
    }

    public List<Delivery> deliveries() {
        return List.copyOf(deliveries);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Delivery delivery : deliveries) {
            CompoundTag entry = new CompoundTag();
            entry.putString("shipmentId", delivery.shipmentId());
            entry.putUUID("buyer", delivery.buyer());
            entry.putString("item", delivery.itemId());
            entry.putInt("quantity", delivery.quantity());
            entry.putLong("deliveredAt", delivery.deliveredAt());
            list.add(entry);
        }
        tag.put("deliveries", list);
        return tag;
    }

    public static LogisticsDeliverySavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        LogisticsDeliverySavedData data = new LogisticsDeliverySavedData();
        ListTag list = tag.getList("deliveries", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (entry.hasUUID("buyer") && !entry.getString("shipmentId").isBlank()
                    && !entry.getString("item").isBlank() && entry.getInt("quantity") > 0) {
                data.deliveries.add(new Delivery(entry.getString("shipmentId"), entry.getUUID("buyer"),
                        entry.getString("item"), entry.getInt("quantity"),
                        Math.max(0L, entry.getLong("deliveredAt"))));
            }
        }
        while (data.deliveries.size() > MAX_RECORDS) {
            data.deliveries.remove(0);
        }
        return data;
    }
}
