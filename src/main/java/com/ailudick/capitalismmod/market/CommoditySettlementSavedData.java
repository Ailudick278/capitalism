package com.ailudick.capitalismmod.market;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;

/** Durable idempotency ledger for commodity-order escrow releases. */
public final class CommoditySettlementSavedData extends SavedData {
    private static final String ID = "capitalismmod_commodity_settlements";
    private static final int MAX_RECORDS = 8192;
    private final Set<String> released = new HashSet<>();

    private CommoditySettlementSavedData() {
    }

    public static CommoditySettlementSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(CommoditySettlementSavedData::new, CommoditySettlementSavedData::load), ID);
    }

    public boolean has(String orderId) {
        return orderId != null && !orderId.isBlank() && released.contains(orderId);
    }

    public Set<String> releasedOrderIds() {
        return Set.copyOf(released);
    }

    public void record(String orderId) {
        if (orderId == null || orderId.isBlank() || !released.add(orderId)) return;
        while (released.size() > MAX_RECORDS) released.remove(released.iterator().next());
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        released.forEach(id -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("id", id);
            list.add(entry);
        });
        tag.put("released", list);
        return tag;
    }

    public static CommoditySettlementSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        CommoditySettlementSavedData data = new CommoditySettlementSavedData();
        ListTag list = tag.getList("released", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            String id = list.getCompound(i).getString("id");
            if (!id.isBlank()) data.released.add(id);
        }
        while (data.released.size() > MAX_RECORDS) data.released.remove(data.released.iterator().next());
        return data;
    }
}
