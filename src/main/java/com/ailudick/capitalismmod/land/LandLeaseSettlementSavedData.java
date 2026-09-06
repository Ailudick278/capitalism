package com.ailudick.capitalismmod.land;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Durable journal for the multi-step settlement that ends a land lease. */
public final class LandLeaseSettlementSavedData extends SavedData {
    private static final String ID = "capitalismmod_land_lease_settlements";
    private static final int MAX_RECORDS = 4096;
    private final List<Settlement> settlements = new ArrayList<>();

    public record Settlement(String id, String landId, UUID tenantUuid, UUID ownerUuid,
                             long ownerAmount, long tenantRefund, long debtAmount,
                             long createdAt, boolean completed) {}

    private LandLeaseSettlementSavedData() {}

    public static LandLeaseSettlementSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(LandLeaseSettlementSavedData::new, LandLeaseSettlementSavedData::load), ID);
    }

    public Settlement find(String id) {
        return settlements.stream().filter(s -> s.id().equals(id)).findFirst().orElse(null);
    }

    public List<Settlement> pending() {
        return settlements.stream().filter(s -> !s.completed()).toList();
    }

    public boolean put(Settlement settlement) {
        if (settlement == null || settlement.id() == null || settlement.id().isBlank()
                || settlement.landId() == null || settlement.landId().isBlank()
                || settlement.tenantUuid() == null || settlement.ownerUuid() == null) return false;
        for (int i = 0; i < settlements.size(); i++) {
            if (!settlements.get(i).id().equals(settlement.id())) continue;
            settlements.set(i, settlement);
            setDirty();
            return true;
        }
        if (settlements.size() >= MAX_RECORDS) return false;
        settlements.add(settlement);
        setDirty();
        return true;
    }

    public void complete(String id) {
        for (int i = 0; i < settlements.size(); i++) {
            Settlement current = settlements.get(i);
            if (!current.id().equals(id) || current.completed()) continue;
            settlements.set(i, new Settlement(current.id(), current.landId(), current.tenantUuid(),
                    current.ownerUuid(), current.ownerAmount(), current.tenantRefund(), current.debtAmount(),
                    current.createdAt(), true));
            setDirty();
            return;
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Settlement settlement : settlements) {
            CompoundTag entry = new CompoundTag();
            entry.putString("id", settlement.id());
            entry.putString("land", settlement.landId());
            entry.putUUID("tenant", settlement.tenantUuid());
            entry.putUUID("owner", settlement.ownerUuid());
            entry.putLong("ownerAmount", settlement.ownerAmount());
            entry.putLong("tenantRefund", settlement.tenantRefund());
            entry.putLong("debtAmount", settlement.debtAmount());
            entry.putLong("createdAt", settlement.createdAt());
            entry.putBoolean("completed", settlement.completed());
            list.add(entry);
        }
        tag.put("settlements", list);
        return tag;
    }

    public static LandLeaseSettlementSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        LandLeaseSettlementSavedData data = new LandLeaseSettlementSavedData();
        ListTag list = tag.getList("settlements", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(list.size(), MAX_RECORDS); i++) {
            CompoundTag entry = list.getCompound(i);
            if (!entry.hasUUID("tenant") || !entry.hasUUID("owner")
                    || entry.getString("id").isBlank() || entry.getString("land").isBlank()) continue;
            data.settlements.add(new Settlement(entry.getString("id"), entry.getString("land"),
                    entry.getUUID("tenant"), entry.getUUID("owner"), Math.max(0L, entry.getLong("ownerAmount")),
                    Math.max(0L, entry.getLong("tenantRefund")), Math.max(0L, entry.getLong("debtAmount")),
                    entry.getLong("createdAt"), entry.getBoolean("completed")));
        }
        return data;
    }
}
