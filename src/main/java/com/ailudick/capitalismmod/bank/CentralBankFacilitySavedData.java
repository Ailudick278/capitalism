package com.ailudick.capitalismmod.bank;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

/** Persistent central-bank facility ledger; cash settlement is handled separately. */
public final class CentralBankFacilitySavedData extends SavedData {
    private static final String ID = "capitalismmod_central_bank_facilities";
    private static final int MAX_FACILITIES = 256;
    private final List<Facility> facilities = new ArrayList<>();

    public record Facility(String id, long issuedDay, long principalMinor, long remainingPrincipal,
                           int annualRateBasisPoints, int termDays, int daysRemaining,
                           long lastSettlementDay) {}

    private CentralBankFacilitySavedData() {}

    public static CentralBankFacilitySavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(CentralBankFacilitySavedData::new, CentralBankFacilitySavedData::load), ID);
    }

    public List<Facility> facilities() { return List.copyOf(facilities); }
    public Facility find(String id) {
        return id == null ? null : facilities.stream().filter(f -> id.equals(f.id())).findFirst().orElse(null);
    }

    public boolean add(Facility facility) {
        if (facility == null || facility.id() == null || facility.id().isBlank()
                || !CentralBankFacilityEconomics.validTerms(facility.principalMinor(),
                facility.annualRateBasisPoints(), facility.termDays())
                || facility.remainingPrincipal() < 0L || facility.remainingPrincipal() > facility.principalMinor()
                || facility.daysRemaining() < 0 || facility.daysRemaining() > facility.termDays()
                || find(facility.id()) != null || facilities.size() >= MAX_FACILITIES) return false;
        facilities.add(facility);
        setDirty();
        return true;
    }

    public boolean replace(Facility replacement) {
        if (replacement == null || replacement.id() == null) return false;
        for (int i = 0; i < facilities.size(); i++) {
            if (replacement.id().equals(facilities.get(i).id())) {
                facilities.set(i, replacement);
                setDirty();
                return true;
            }
        }
        return false;
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Facility f : facilities) {
            CompoundTag e = new CompoundTag();
            e.putString("id", f.id()); e.putLong("issuedDay", f.issuedDay());
            e.putLong("principal", f.principalMinor()); e.putLong("remaining", f.remainingPrincipal());
            e.putInt("rateBps", f.annualRateBasisPoints()); e.putInt("termDays", f.termDays());
            e.putInt("daysRemaining", f.daysRemaining()); e.putLong("lastSettlementDay", f.lastSettlementDay());
            list.add(e);
        }
        tag.put("facilities", list);
        return tag;
    }

    public static CentralBankFacilitySavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        CentralBankFacilitySavedData data = new CentralBankFacilitySavedData();
        ListTag list = tag.getList("facilities", Tag.TAG_COMPOUND);
        for (int i = Math.max(0, list.size() - MAX_FACILITIES); i < list.size(); i++) {
            CompoundTag e = list.getCompound(i);
            Facility f = new Facility(e.getString("id"), e.getLong("issuedDay"), e.getLong("principal"),
                    e.getLong("remaining"), e.getInt("rateBps"), e.getInt("termDays"),
                    e.getInt("daysRemaining"), e.getLong("lastSettlementDay"));
            if (f.issuedDay() >= 0L && f.lastSettlementDay() >= -1L
                    && data.add(f)) { /* validated and bounded */ }
        }
        return data;
    }
}
