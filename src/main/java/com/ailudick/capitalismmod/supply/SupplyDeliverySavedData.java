package com.ailudick.capitalismmod.supply;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

/** Durable progress journal for batches dispatched from a backorder. */
public final class SupplyDeliverySavedData extends SavedData {
    private static final String ID = "capitalismmod_supply_deliveries";
    private static final int MAX_RECORDS = 8192;
    private final List<Delivery> deliveries = new ArrayList<>();

    public record Delivery(String key, String orderId, int quantity, int remaining) {
        public Delivery {
            key = key == null ? "" : key;
            orderId = orderId == null ? "" : orderId;
            quantity = Math.max(0, quantity);
            remaining = Math.max(0, remaining);
        }
    }

    private SupplyDeliverySavedData() {
    }

    public static SupplyDeliverySavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(SupplyDeliverySavedData::new, SupplyDeliverySavedData::load), ID);
    }

    public Delivery find(String key) {
        if (key == null || key.isBlank()) return null;
        return deliveries.stream().filter(delivery -> key.equals(delivery.key())).findFirst().orElse(null);
    }

    public void record(Delivery delivery) {
        if (delivery == null || delivery.key().isBlank() || delivery.orderId().isBlank()
                || delivery.quantity() <= 0 || find(delivery.key()) != null) return;
        deliveries.add(delivery);
        while (deliveries.size() > MAX_RECORDS) deliveries.remove(0);
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Delivery delivery : deliveries) {
            CompoundTag entry = new CompoundTag();
            entry.putString("key", delivery.key());
            entry.putString("orderId", delivery.orderId());
            entry.putInt("quantity", delivery.quantity());
            entry.putInt("remaining", delivery.remaining());
            list.add(entry);
        }
        tag.put("deliveries", list);
        return tag;
    }

    public static SupplyDeliverySavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        SupplyDeliverySavedData data = new SupplyDeliverySavedData();
        ListTag list = tag.getList("deliveries", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (!entry.getString("key").isBlank() && !entry.getString("orderId").isBlank()
                    && entry.getInt("quantity") > 0) {
                data.deliveries.add(new Delivery(entry.getString("key"), entry.getString("orderId"),
                        entry.getInt("quantity"), entry.getInt("remaining")));
            }
        }
        while (data.deliveries.size() > MAX_RECORDS) data.deliveries.remove(0);
        return data;
    }
}
