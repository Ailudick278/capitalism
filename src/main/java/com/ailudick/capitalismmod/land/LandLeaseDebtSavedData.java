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

/** Persists rent still owed after a lease has ended. Amounts are in currency minor units. */
public final class LandLeaseDebtSavedData extends SavedData {
    private static final String ID = "capitalismmod_land_lease_debts";
    private final List<Debt> debts = new ArrayList<>();

    public record Debt(String id, String landId, UUID tenantUuid, UUID ownerUuid, long amount, long createdAt) {}

    private LandLeaseDebtSavedData() {}

    public static LandLeaseDebtSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(LandLeaseDebtSavedData::new, LandLeaseDebtSavedData::load), ID);
    }

    public List<Debt> debts() {
        return List.copyOf(debts);
    }

    public List<Debt> forTenant(UUID tenantUuid) {
        return debts.stream().filter(debt -> debt.tenantUuid().equals(tenantUuid)).toList();
    }

    public void add(Debt debt) {
        if (debt == null || debt.tenantUuid() == null || debt.ownerUuid() == null
                || debt.amount() <= 0L || debt.landId() == null || debt.landId().isBlank()) {
            return;
        }
        for (int i = 0; i < debts.size(); i++) {
            Debt current = debts.get(i);
            if (current.landId().equals(debt.landId())
                    && current.tenantUuid().equals(debt.tenantUuid())
                    && current.ownerUuid().equals(debt.ownerUuid())) {
                long combined = current.amount() > Long.MAX_VALUE - debt.amount()
                        ? Long.MAX_VALUE : current.amount() + debt.amount();
                debts.set(i, new Debt(current.id(), current.landId(), current.tenantUuid(),
                        current.ownerUuid(), combined, Math.min(current.createdAt(), debt.createdAt())));
                setDirty();
                return;
            }
        }
        debts.add(debt);
        setDirty();
    }

    public void addOnce(Debt debt) {
        if (debt == null || debt.id() == null || debt.id().isBlank() || debt.tenantUuid() == null
                || debt.ownerUuid() == null || debt.amount() <= 0L || debt.landId() == null || debt.landId().isBlank()
                || debts.stream().anyMatch(current -> current.id().equals(debt.id()))) return;
        // Keep this receipt as its own row instead of merging by parties. If the
        // server stops after the append but before the settlement journal closes,
        // a retry can find the exact id and cannot add the same debt again.
        debts.add(debt);
        setDirty();
    }

    public void remove(String id) {
        if (debts.removeIf(debt -> debt.id().equals(id))) {
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Debt debt : debts) {
            CompoundTag entry = new CompoundTag();
            entry.putString("id", debt.id());
            entry.putString("land", debt.landId());
            entry.putUUID("tenant", debt.tenantUuid());
            entry.putUUID("owner", debt.ownerUuid());
            entry.putLong("amount", debt.amount());
            entry.putLong("createdAt", debt.createdAt());
            list.add(entry);
        }
        tag.put("debts", list);
        return tag;
    }

    public static LandLeaseDebtSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        LandLeaseDebtSavedData data = new LandLeaseDebtSavedData();
        ListTag list = tag.getList("debts", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (entry.hasUUID("tenant") && entry.hasUUID("owner")
                    && entry.getLong("amount") > 0L && !entry.getString("land").isBlank()) {
                data.debts.add(new Debt(entry.getString("id"), entry.getString("land"),
                        entry.getUUID("tenant"), entry.getUUID("owner"), entry.getLong("amount"),
                        entry.getLong("createdAt")));
            }
        }
        return data;
    }
}
