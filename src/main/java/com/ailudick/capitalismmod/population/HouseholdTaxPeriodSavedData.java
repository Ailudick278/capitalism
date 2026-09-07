package com.ailudick.capitalismmod.population;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

/** Durable 30-day resident tax-base summaries; amounts are in the default currency. */
public final class HouseholdTaxPeriodSavedData extends SavedData {
    private static final String ID = "capitalismmod_household_tax_periods";
    private static final int MAX_ASSESSMENTS = 8192;
    private final List<Assessment> assessments = new ArrayList<>();

    public record Assessment(String id, String householdId, long periodStart, long periodEnd,
                             long grossWagesMinor, long wageTaxMinor, long consumptionMinor,
                             long consumptionTaxMinor) {}

    private HouseholdTaxPeriodSavedData() {}

    public static HouseholdTaxPeriodSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(HouseholdTaxPeriodSavedData::new, HouseholdTaxPeriodSavedData::load), ID);
    }

    public List<Assessment> assessments() { return List.copyOf(assessments); }
    public Assessment find(String id) {
        return assessments.stream().filter(assessment -> assessment.id().equals(id)).findFirst().orElse(null);
    }

    public boolean record(Assessment assessment) {
        if (assessment == null || assessment.id() == null || assessment.id().isBlank()
                || assessment.householdId() == null || assessment.householdId().isBlank()
                || assessment.periodStart() < 0L || assessment.periodEnd() < assessment.periodStart()
                || assessment.grossWagesMinor() < 0L || assessment.wageTaxMinor() < 0L
                || assessment.wageTaxMinor() > assessment.grossWagesMinor()
                || assessment.consumptionMinor() < 0L || assessment.consumptionTaxMinor() < 0L
                || assessment.consumptionTaxMinor() > assessment.consumptionMinor()
                || find(assessment.id()) != null) return false;
        assessments.add(assessment);
        while (assessments.size() > MAX_ASSESSMENTS) assessments.remove(0);
        setDirty();
        return true;
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Assessment assessment : assessments) {
            CompoundTag entry = new CompoundTag();
            entry.putString("id", assessment.id()); entry.putString("household", assessment.householdId());
            entry.putLong("start", assessment.periodStart()); entry.putLong("end", assessment.periodEnd());
            entry.putLong("grossWages", assessment.grossWagesMinor()); entry.putLong("wageTax", assessment.wageTaxMinor());
            entry.putLong("consumption", assessment.consumptionMinor());
            entry.putLong("consumptionTax", assessment.consumptionTaxMinor());
            list.add(entry);
        }
        tag.put("assessments", list);
        return tag;
    }

    public static HouseholdTaxPeriodSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        HouseholdTaxPeriodSavedData data = new HouseholdTaxPeriodSavedData();
        ListTag list = tag.getList("assessments", Tag.TAG_COMPOUND);
        for (int i = Math.max(0, list.size() - MAX_ASSESSMENTS); i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (!entry.getString("id").isBlank() && !entry.getString("household").isBlank()
                    && entry.getLong("start") >= 0L && entry.getLong("end") >= entry.getLong("start")
                    && entry.getLong("grossWages") >= 0L && entry.getLong("wageTax") >= 0L
                    && entry.getLong("wageTax") <= entry.getLong("grossWages")
                    && entry.getLong("consumption") >= 0L && entry.getLong("consumptionTax") >= 0L
                    && entry.getLong("consumptionTax") <= entry.getLong("consumption")) {
                data.assessments.add(new Assessment(entry.getString("id"), entry.getString("household"),
                        entry.getLong("start"), entry.getLong("end"), entry.getLong("grossWages"),
                        entry.getLong("wageTax"), entry.getLong("consumption"), entry.getLong("consumptionTax")));
            }
        }
        return data;
    }
}
