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

/** Append-only lifecycle audit for paid supply orders and their deliveries. */
public final class SupplyOrderAuditSavedData extends SavedData {
    private static final String ID = "capitalismmod_supply_order_audit";
    private static final int MAX_EVENTS = 8192;
    private final List<Event> events = new ArrayList<>();

    public record Event(String orderId, String type, UUID buyerUuid, UUID supplierUuid, String itemId,
                        int quantity, long amount, long occurredAt) {
    }

    private SupplyOrderAuditSavedData() {
    }

    public static SupplyOrderAuditSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(SupplyOrderAuditSavedData::new, SupplyOrderAuditSavedData::load), ID);
    }

    public List<Event> events() {
        return List.copyOf(events);
    }

    public List<Event> forOrder(String orderId) {
        return events.stream().filter(event -> event.orderId().equals(orderId)).toList();
    }

    public void append(Event event) {
        if (event == null || event.orderId() == null || event.orderId().isBlank()
                || event.type() == null || event.type().isBlank() || event.buyerUuid() == null
                || event.supplierUuid() == null || event.quantity() < 0 || event.amount() < 0) {
            return;
        }
        events.add(event);
        while (events.size() > MAX_EVENTS) {
            events.remove(0);
        }
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Event event : events) {
            CompoundTag entry = new CompoundTag();
            entry.putString("orderId", event.orderId());
            entry.putString("type", event.type());
            entry.putUUID("buyer", event.buyerUuid());
            entry.putUUID("supplier", event.supplierUuid());
            entry.putString("item", event.itemId());
            entry.putInt("quantity", event.quantity());
            entry.putLong("amount", event.amount());
            entry.putLong("occurredAt", event.occurredAt());
            list.add(entry);
        }
        tag.put("events", list);
        return tag;
    }

    public static SupplyOrderAuditSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        SupplyOrderAuditSavedData data = new SupplyOrderAuditSavedData();
        ListTag list = tag.getList("events", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (entry.hasUUID("buyer") && entry.hasUUID("supplier")
                    && !entry.getString("orderId").isBlank() && !entry.getString("type").isBlank()) {
                data.events.add(new Event(entry.getString("orderId"), entry.getString("type"),
                        entry.getUUID("buyer"), entry.getUUID("supplier"), entry.getString("item"),
                        Math.max(0, entry.getInt("quantity")), Math.max(0L, entry.getLong("amount")),
                        Math.max(0L, entry.getLong("occurredAt"))));
            }
        }
        while (data.events.size() > MAX_EVENTS) {
            data.events.remove(0);
        }
        return data;
    }
}
