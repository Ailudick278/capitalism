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

/** Persists lease security deposits independently from the land claim record. */
public final class LandLeaseDepositSavedData extends SavedData {
    private static final String ID = "capitalismmod_land_lease_deposits";
    private static final int MAX_DEPOSITS = 4096;
    private final List<Deposit> deposits = new ArrayList<>();

    public record Deposit(String landId, UUID tenantUuid, UUID ownerUuid, long amount) {}

    private LandLeaseDepositSavedData() {}

    public static LandLeaseDepositSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(LandLeaseDepositSavedData::new, LandLeaseDepositSavedData::load), ID);
    }

    public Deposit find(String landId) {
        return deposits.stream().filter(deposit -> deposit.landId().equals(landId)).findFirst().orElse(null);
    }

    public void put(Deposit deposit) {
        if (deposit == null || deposit.landId() == null || deposit.landId().isBlank()
                || deposit.tenantUuid() == null || deposit.ownerUuid() == null || deposit.amount() < 0L) {
            return;
        }
        deposits.removeIf(current -> current.landId().equals(deposit.landId()));
        if (deposit.amount() > 0L) {
            if (deposits.size() >= MAX_DEPOSITS) return;
            deposits.add(deposit);
        }
        setDirty();
    }

    public Deposit take(String landId) {
        for (int i = 0; i < deposits.size(); i++) {
            Deposit deposit = deposits.get(i);
            if (deposit.landId().equals(landId)) {
                deposits.remove(i);
                setDirty();
                return deposit;
            }
        }
        return null;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Deposit deposit : deposits) {
            CompoundTag entry = new CompoundTag();
            entry.putString("land", deposit.landId());
            entry.putUUID("tenant", deposit.tenantUuid());
            entry.putUUID("owner", deposit.ownerUuid());
            entry.putLong("amount", deposit.amount());
            list.add(entry);
        }
        tag.put("deposits", list);
        return tag;
    }

    public static LandLeaseDepositSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        LandLeaseDepositSavedData data = new LandLeaseDepositSavedData();
        ListTag list = tag.getList("deposits", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(list.size(), MAX_DEPOSITS); i++) {
            CompoundTag entry = list.getCompound(i);
            String land = entry.getString("land");
            long amount = entry.getLong("amount");
            if (!land.isBlank() && entry.hasUUID("tenant") && entry.hasUUID("owner") && amount > 0L) {
                data.deposits.add(new Deposit(land, entry.getUUID("tenant"), entry.getUUID("owner"), amount));
            }
        }
        return data;
    }
}
