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

/** Persistent audit ledger for cargo that was lost after repeated disruptions. */
public final class LogisticsLossSavedData extends SavedData {
    private static final String ID = "capitalismmod_logistics_losses";
    private static final int MAX_RECORDS = 4096;
    private final List<Loss> losses = new ArrayList<>();

    public record Loss(String shipmentId, UUID buyer, String itemId, int quantity,
                       String originRegion, String destinationRegion, TransportMode transport,
                       int disruptionCount, long lostAt, boolean acknowledged, String supplyOrderId,
                       String buyerCompanyId, long unitPrice) {
        public Loss {
            disruptionCount = Math.max(0, disruptionCount);
            lostAt = Math.max(0L, lostAt);
            supplyOrderId = supplyOrderId == null ? "" : supplyOrderId;
            buyerCompanyId = buyerCompanyId == null ? "" : buyerCompanyId;
            unitPrice = Math.max(0L, unitPrice);
        }
    }

    private LogisticsLossSavedData() {
    }

    public static LogisticsLossSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(LogisticsLossSavedData::new, LogisticsLossSavedData::load), ID);
    }

    public List<Loss> losses() {
        return List.copyOf(losses);
    }

    public List<Loss> unreadFor(UUID buyer) {
        return losses.stream().filter(loss -> loss.buyer().equals(buyer) && !loss.acknowledged()).toList();
    }

    public void add(Loss loss) {
        if (loss == null || loss.shipmentId() == null || loss.shipmentId().isBlank()
                || loss.buyer() == null || loss.quantity() <= 0 || loss.itemId() == null
                || loss.itemId().isBlank() || losses.stream().anyMatch(existing -> existing.shipmentId().equals(loss.shipmentId()))) {
            return;
        }
        losses.add(loss);
        while (losses.size() > MAX_RECORDS) {
            losses.remove(0);
        }
        setDirty();
    }

    public void acknowledge(UUID buyer) {
        boolean changed = false;
        for (int i = 0; i < losses.size(); i++) {
            Loss loss = losses.get(i);
            if (loss.buyer().equals(buyer) && !loss.acknowledged()) {
                losses.set(i, new Loss(loss.shipmentId(), loss.buyer(), loss.itemId(), loss.quantity(),
                        loss.originRegion(), loss.destinationRegion(), loss.transport(), loss.disruptionCount(),
                        loss.lostAt(), true, loss.supplyOrderId(), loss.buyerCompanyId(), loss.unitPrice()));
                changed = true;
            }
        }
        if (changed) {
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Loss loss : losses) {
            CompoundTag entry = new CompoundTag();
            entry.putString("shipmentId", loss.shipmentId());
            entry.putUUID("buyer", loss.buyer());
            entry.putString("item", loss.itemId());
            entry.putInt("quantity", loss.quantity());
            entry.putString("originRegion", loss.originRegion());
            entry.putString("destinationRegion", loss.destinationRegion());
            entry.putString("transport", loss.transport().id());
            entry.putInt("disruptions", loss.disruptionCount());
            entry.putLong("lostAt", loss.lostAt());
            entry.putBoolean("acknowledged", loss.acknowledged());
            entry.putString("supplyOrderId", loss.supplyOrderId());
            entry.putString("buyerCompanyId", loss.buyerCompanyId());
            entry.putLong("unitPrice", loss.unitPrice());
            list.add(entry);
        }
        tag.put("losses", list);
        return tag;
    }

    public static LogisticsLossSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        LogisticsLossSavedData data = new LogisticsLossSavedData();
        ListTag list = tag.getList("losses", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (entry.hasUUID("buyer") && entry.getInt("quantity") > 0
                    && !entry.getString("shipmentId").isBlank() && !entry.getString("item").isBlank()) {
                data.losses.add(new Loss(entry.getString("shipmentId"), entry.getUUID("buyer"),
                        entry.getString("item"), entry.getInt("quantity"), entry.getString("originRegion"),
                        entry.getString("destinationRegion"), TransportMode.parse(entry.getString("transport")),
                        entry.getInt("disruptions"), entry.getLong("lostAt"), entry.getBoolean("acknowledged"),
                        entry.getString("supplyOrderId"), entry.getString("buyerCompanyId"),
                        Math.max(0L, entry.getLong("unitPrice"))));
            }
        }
        while (data.losses.size() > MAX_RECORDS) {
            data.losses.remove(0);
        }
        return data;
    }
}
