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

/** Persistent invoices for scheduled land-rent installments. */
public final class LandRentBillSavedData extends SavedData {
    private static final String ID = "capitalismmod_land_rent_bills";
    private static final int MAX_BILLS = 8192;
    private final List<Bill> bills = new ArrayList<>();

    public record Bill(String id, String landId, UUID tenantUuid, UUID ownerUuid,
                       long amount, long dueAt, String status, long createdAt) {}

    private LandRentBillSavedData() {}

    public static LandRentBillSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(LandRentBillSavedData::new, LandRentBillSavedData::load), ID);
    }

    public Bill find(String id) {
        return bills.stream().filter(bill -> bill.id().equals(id)).findFirst().orElse(null);
    }

    public List<Bill> bills() {
        return List.copyOf(bills);
    }

    public static boolean isValidStatus(String status) {
        return LandRentBillStatus.isValid(status);
    }

    public boolean create(Bill bill) {
        if (bill == null || bill.id() == null || bill.id().isBlank() || bill.landId() == null
                || bill.landId().isBlank() || bill.tenantUuid() == null || bill.ownerUuid() == null
                || bill.amount() <= 0L || bill.dueAt() < 0L || !isValidStatus(bill.status())) return false;
        if (find(bill.id()) != null || bills.size() >= MAX_BILLS) return false;
        bills.add(bill);
        setDirty();
        return true;
    }

    public boolean markStatus(String id, String status) {
        for (int i = 0; i < bills.size(); i++) {
            Bill current = bills.get(i);
            if (!current.id().equals(id) || !isValidStatus(status)) continue;
            bills.set(i, new Bill(current.id(), current.landId(), current.tenantUuid(), current.ownerUuid(),
                    current.amount(), current.dueAt(), status, current.createdAt()));
            setDirty();
            return true;
        }
        return false;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Bill bill : bills) {
            CompoundTag entry = new CompoundTag();
            entry.putString("id", bill.id());
            entry.putString("land", bill.landId());
            entry.putUUID("tenant", bill.tenantUuid());
            entry.putUUID("owner", bill.ownerUuid());
            entry.putLong("amount", bill.amount());
            entry.putLong("dueAt", bill.dueAt());
            entry.putString("status", bill.status());
            entry.putLong("createdAt", bill.createdAt());
            list.add(entry);
        }
        tag.put("bills", list);
        return tag;
    }

    public static LandRentBillSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        LandRentBillSavedData data = new LandRentBillSavedData();
        ListTag list = tag.getList("bills", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(list.size(), MAX_BILLS); i++) {
            CompoundTag entry = list.getCompound(i);
            String id = entry.getString("id");
            String land = entry.getString("land");
            String status = entry.getString("status");
            long amount = entry.getLong("amount");
            if (id.isBlank() || land.isBlank() || !isValidStatus(status) || amount <= 0L
                    || !entry.hasUUID("tenant") || !entry.hasUUID("owner")) continue;
            data.bills.add(new Bill(id, land, entry.getUUID("tenant"), entry.getUUID("owner"), amount,
                    Math.max(0L, entry.getLong("dueAt")), status, Math.max(0L, entry.getLong("createdAt"))));
        }
        return data;
    }
}
