package com.ailudick.capitalismmod.population;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

/** World-persisted daily household risk observations. */
public final class HouseholdFinancialRiskSavedData extends SavedData {
    private static final String ID = "capitalismmod_household_financial_risk";
    private static final int MAX_RECORDS = 32768;
    private final List<Assessment> assessments = new ArrayList<>();

    public record Assessment(String id, String householdId, long day, long cashMinor,
                             long dailyNeedMinor, long rentArrearsMinor, long wageArrearsMinor,
                             long bankDebtMinor,
                             int unemploymentDays, int score, boolean bankOverdue) {}

    private HouseholdFinancialRiskSavedData() {}
    public static HouseholdFinancialRiskSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(HouseholdFinancialRiskSavedData::new, HouseholdFinancialRiskSavedData::load), ID);
    }
    public List<Assessment> assessments() { return List.copyOf(assessments); }
    public Assessment find(String id) { return assessments.stream().filter(a -> a.id().equals(id)).findFirst().orElse(null); }
    public Assessment latest(String householdId) {
        return assessments.stream().filter(a -> a.householdId().equals(householdId))
                .max(java.util.Comparator.comparingLong(Assessment::day)).orElse(null);
    }
    public boolean record(Assessment assessment) {
        if (assessment == null || assessment.id() == null || assessment.id().isBlank()
                || assessment.householdId() == null || assessment.householdId().isBlank()
                || assessment.day() < 0L || assessment.cashMinor() < 0L
                || assessment.dailyNeedMinor() < 0L || assessment.rentArrearsMinor() < 0L
                || assessment.wageArrearsMinor() < 0L || assessment.bankDebtMinor() < 0L
                || assessment.unemploymentDays() < 0
                || assessment.score() < 0 || assessment.score() > 100
                || find(assessment.id()) != null) return false;
        assessments.add(assessment);
        while (assessments.size() > MAX_RECORDS) assessments.remove(0);
        setDirty(); return true;
    }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Assessment a : assessments) {
            CompoundTag e = new CompoundTag(); e.putString("id", a.id()); e.putString("household", a.householdId());
            e.putLong("day", a.day()); e.putLong("cash", a.cashMinor()); e.putLong("need", a.dailyNeedMinor());
            e.putLong("rentArrears", a.rentArrearsMinor()); e.putLong("wageArrears", a.wageArrearsMinor());
            e.putLong("bankDebt", a.bankDebtMinor());
            e.putInt("unemployment", a.unemploymentDays()); e.putInt("score", a.score()); list.add(e);
            e.putBoolean("bankOverdue", a.bankOverdue());
        }
        tag.put("assessments", list); return tag;
    }
    public static HouseholdFinancialRiskSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        HouseholdFinancialRiskSavedData data = new HouseholdFinancialRiskSavedData();
        ListTag list = tag.getList("assessments", Tag.TAG_COMPOUND);
        for (int i = Math.max(0, list.size() - MAX_RECORDS); i < list.size(); i++) {
            CompoundTag e = list.getCompound(i);
            if (!e.getString("id").isBlank() && !e.getString("household").isBlank()
                    && e.getLong("day") >= 0L && e.getLong("cash") >= 0L && e.getLong("need") >= 0L
                    && e.getLong("rentArrears") >= 0L && e.getLong("wageArrears") >= 0L
                    && e.getLong("bankDebt") >= 0L
                    && e.getInt("unemployment") >= 0 && e.getInt("score") >= 0 && e.getInt("score") <= 100) {
                data.assessments.add(new Assessment(e.getString("id"), e.getString("household"), e.getLong("day"),
                        e.getLong("cash"), e.getLong("need"), e.getLong("rentArrears"),
                        e.getLong("wageArrears"), e.getLong("bankDebt"), e.getInt("unemployment"),
                        e.getInt("score"), e.getBoolean("bankOverdue")));
            }
        }
        return data;
    }
}
