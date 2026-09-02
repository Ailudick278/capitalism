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

/** Persists cross-region cargo until it reaches the buyer's warehouse. */
public final class LogisticsSavedData extends SavedData {
    private static final String ID = "capitalismmod_logistics";
    private final List<Shipment> shipments = new ArrayList<>();

    public record Shipment(String id, UUID buyer, String itemId, int quantity, long deliveryTick,
                           String originRegion, String destinationRegion, TransportMode transport, boolean insured) {
        public Shipment(String id, UUID buyer, String itemId, int quantity, long deliveryTick) {
            this(id, buyer, itemId, quantity, deliveryTick, "unknown", "unknown", TransportMode.ROAD, false);
        }
    }

    private LogisticsSavedData() {
    }

    public static LogisticsSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(LogisticsSavedData::new, LogisticsSavedData::load), ID);
    }

    public List<Shipment> shipments() {
        return List.copyOf(shipments);
    }

    public void add(Shipment shipment) {
        if (shipment == null || shipment.buyer() == null || shipment.itemId() == null
                || shipment.quantity() <= 0 || shipment.deliveryTick() < 0) {
            return;
        }
        shipments.add(shipment);
        setDirty();
    }

    public void remove(String id) {
        shipments.removeIf(shipment -> shipment.id().equals(id));
        setDirty();
    }

    public boolean insure(String id, UUID buyer) {
        for (int i = 0; i < shipments.size(); i++) {
            Shipment shipment = shipments.get(i);
            if (shipment.id().equals(id) && shipment.buyer().equals(buyer) && !shipment.insured()) {
                shipments.set(i, new Shipment(shipment.id(), shipment.buyer(), shipment.itemId(), shipment.quantity(),
                        shipment.deliveryTick(), shipment.originRegion(), shipment.destinationRegion(),
                        shipment.transport(), true));
                setDirty();
                return true;
            }
        }
        return false;
    }

    public void replace(Shipment replacement) {
        for (int i = 0; i < shipments.size(); i++) {
            if (shipments.get(i).id().equals(replacement.id())) {
                shipments.set(i, replacement);
                setDirty();
                return;
            }
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Shipment shipment : shipments) {
            CompoundTag nbt = new CompoundTag();
            nbt.putString("id", shipment.id());
            nbt.putUUID("buyer", shipment.buyer());
            nbt.putString("item", shipment.itemId());
            nbt.putInt("quantity", shipment.quantity());
            nbt.putLong("delivery", shipment.deliveryTick());
            nbt.putString("originRegion", shipment.originRegion());
            nbt.putString("destinationRegion", shipment.destinationRegion());
            nbt.putString("transport", shipment.transport().id());
            nbt.putBoolean("insured", shipment.insured());
            list.add(nbt);
        }
        tag.put("shipments", list);
        return tag;
    }

    public static LogisticsSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        LogisticsSavedData data = new LogisticsSavedData();
        ListTag list = tag.getList("shipments", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag nbt = list.getCompound(i);
            if (nbt.hasUUID("buyer") && nbt.getInt("quantity") > 0 && nbt.getLong("delivery") >= 0) {
                data.shipments.add(new Shipment(nbt.getString("id"), nbt.getUUID("buyer"),
                        nbt.getString("item"), nbt.getInt("quantity"), nbt.getLong("delivery"),
                        nbt.getString("originRegion"), nbt.getString("destinationRegion"),
                        TransportMode.parse(nbt.getString("transport")), nbt.getBoolean("insured")));
            }
        }
        return data;
    }
}
