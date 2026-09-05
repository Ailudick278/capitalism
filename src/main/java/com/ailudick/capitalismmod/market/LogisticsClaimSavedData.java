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

/** Audit ledger for automatically settled cargo insurance claims. */
public final class LogisticsClaimSavedData extends SavedData {
    private static final String ID = "capitalismmod_logistics_claims";
    private static final int MAX_RECORDS = 4096;
    private final List<Claim> claims = new ArrayList<>();

    public record Claim(String claimId, String shipmentId, UUID buyer, long insuredValue,
                        long actualLoss, long payout, long decidedAt, String status) {
    }

    private LogisticsClaimSavedData() {
    }

    public static LogisticsClaimSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(LogisticsClaimSavedData::new, LogisticsClaimSavedData::load), ID);
    }

    public List<Claim> claims() {
        return List.copyOf(claims);
    }

    public void settle(Claim claim) {
        if (claim == null || claim.shipmentId() == null || claim.shipmentId().isBlank()
                || claim.buyer() == null || claims.stream().anyMatch(existing -> existing.shipmentId().equals(claim.shipmentId()))) {
            return;
        }
        claims.add(claim);
        while (claims.size() > MAX_RECORDS) {
            claims.remove(0);
        }
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Claim claim : claims) {
            CompoundTag entry = new CompoundTag();
            entry.putString("claimId", claim.claimId());
            entry.putString("shipmentId", claim.shipmentId());
            entry.putUUID("buyer", claim.buyer());
            entry.putLong("insuredValue", claim.insuredValue());
            entry.putLong("actualLoss", claim.actualLoss());
            entry.putLong("payout", claim.payout());
            entry.putLong("decidedAt", claim.decidedAt());
            entry.putString("status", claim.status());
            list.add(entry);
        }
        tag.put("claims", list);
        return tag;
    }

    public static LogisticsClaimSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        LogisticsClaimSavedData data = new LogisticsClaimSavedData();
        ListTag list = tag.getList("claims", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (entry.hasUUID("buyer") && !entry.getString("shipmentId").isBlank()) {
                data.claims.add(new Claim(entry.getString("claimId"), entry.getString("shipmentId"),
                        entry.getUUID("buyer"), Math.max(0L, entry.getLong("insuredValue")),
                        Math.max(0L, entry.getLong("actualLoss")), Math.max(0L, entry.getLong("payout")),
                        Math.max(0L, entry.getLong("decidedAt")), entry.getString("status")));
            }
        }
        while (data.claims.size() > MAX_RECORDS) {
            data.claims.remove(0);
        }
        return data;
    }
}
