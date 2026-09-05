package com.ailudick.capitalismmod.land;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

/** Stores land-tax accruals that have not yet reached the end of their tax period. */
public final class LandTaxPeriodSavedData extends SavedData {
    private static final String ID = "capitalismmod_land_tax_periods";
    private final Map<String, Accrual> accruals = new HashMap<>();

    public record Accrual(long amount, long periodStart, long periodEnd) {}

    private LandTaxPeriodSavedData() {}

    public static LandTaxPeriodSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(LandTaxPeriodSavedData::new, LandTaxPeriodSavedData::load), ID);
    }

    public Accrual get(String claimId) {
        return accruals.getOrDefault(claimId, new Accrual(0L, 0L, 0L));
    }

    public void add(String claimId, long amount, long periodStart, long periodEnd) {
        if (amount <= 0L) return;
        Accrual current = get(claimId);
        long next = current.amount() > Long.MAX_VALUE - amount
                ? Long.MAX_VALUE : current.amount() + amount;
        accruals.put(claimId, new Accrual(next, periodStart, periodEnd));
        setDirty();
    }

    public void clear(String claimId) {
        if (accruals.remove(claimId) != null) setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        accruals.forEach((claimId, accrual) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("claimId", claimId);
            entry.putLong("amount", accrual.amount());
            entry.putLong("periodStart", accrual.periodStart());
            entry.putLong("periodEnd", accrual.periodEnd());
            list.add(entry);
        });
        tag.put("accruals", list);
        return tag;
    }

    public static LandTaxPeriodSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        LandTaxPeriodSavedData data = new LandTaxPeriodSavedData();
        ListTag list = tag.getList("accruals", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            String claimId = entry.getString("claimId");
            if (!claimId.isBlank() && entry.getLong("amount") > 0L) {
                data.accruals.put(claimId, new Accrual(entry.getLong("amount"),
                        entry.getLong("periodStart"), entry.getLong("periodEnd")));
            }
        }
        return data;
    }
}
