package com.ailudick.capitalismmod.supply;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;

/** Durable idempotency keys for money settlements attached to supply orders. */
public final class SupplySettlementSavedData extends SavedData {
    private static final String ID = "capitalismmod_supply_settlements";
    private static final int MAX_KEYS = 8192;
    private final Set<String> supplierPayments = new HashSet<>();
    private final Set<String> orderRefunds = new HashSet<>();

    private SupplySettlementSavedData() {
    }

    public static SupplySettlementSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(SupplySettlementSavedData::new, SupplySettlementSavedData::load), ID);
    }

    public boolean hasSupplierPayment(String sourceId) {
        return sourceId != null && !sourceId.isBlank() && supplierPayments.contains(sourceId);
    }

    public void recordSupplierPayment(String sourceId) {
        if (sourceId == null || sourceId.isBlank() || !supplierPayments.add(sourceId)) return;
        trim(supplierPayments);
        setDirty();
    }

    public boolean hasOrderRefund(String orderId) {
        return orderId != null && !orderId.isBlank() && orderRefunds.contains(orderId);
    }

    public void recordOrderRefund(String orderId) {
        if (orderId == null || orderId.isBlank() || !orderRefunds.add(orderId)) return;
        trim(orderRefunds);
        setDirty();
    }

    private static void trim(Set<String> keys) {
        while (keys.size() > MAX_KEYS) {
            keys.remove(keys.iterator().next());
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag payments = new ListTag();
        supplierPayments.forEach(key -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("id", key);
            payments.add(entry);
        });
        ListTag refunds = new ListTag();
        orderRefunds.forEach(key -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("id", key);
            refunds.add(entry);
        });
        tag.put("supplierPayments", payments);
        tag.put("orderRefunds", refunds);
        return tag;
    }

    public static SupplySettlementSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        SupplySettlementSavedData data = new SupplySettlementSavedData();
        ListTag payments = tag.getList("supplierPayments", Tag.TAG_COMPOUND);
        for (int i = 0; i < payments.size(); i++) {
            String id = payments.getCompound(i).getString("id");
            if (!id.isBlank()) data.supplierPayments.add(id);
        }
        ListTag refunds = tag.getList("orderRefunds", Tag.TAG_COMPOUND);
        for (int i = 0; i < refunds.size(); i++) {
            String id = refunds.getCompound(i).getString("id");
            if (!id.isBlank()) data.orderRefunds.add(id);
        }
        trim(data.supplierPayments);
        trim(data.orderRefunds);
        return data;
    }
}
