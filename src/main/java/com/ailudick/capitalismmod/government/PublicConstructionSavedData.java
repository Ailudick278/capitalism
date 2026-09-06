package com.ailudick.capitalismmod.government;

import com.ailudick.capitalismmod.market.LogisticsInfrastructureSavedData;
import com.ailudick.capitalismmod.company.Company;
import com.ailudick.capitalismmod.company.CompanyHelper;
import com.ailudick.capitalismmod.company.CompanyLifecycleService;
import com.ailudick.capitalismmod.company.CompanySavedData;
import com.ailudick.capitalismmod.currency.Currencies;
import com.ailudick.capitalismmod.market.InventoryOwner;
import com.ailudick.capitalismmod.market.WarehouseSavedData;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

/** Persistent, budget-backed public facility construction queue. */
public final class PublicConstructionSavedData extends SavedData {
    private static final String ID = "capitalismmod_public_construction";
    private static final int MAX_PROJECTS = 4096;
    private final List<Project> projects = new ArrayList<>();

    public record Project(String id, String region, String facility, int units, int completedUnits,
                          String contractorCompanyId,
                          long startDay, long lastProgressDay) {}

    private PublicConstructionSavedData() {}

    public static PublicConstructionSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(PublicConstructionSavedData::new, PublicConstructionSavedData::load), ID);
    }

    public List<Project> projects() { return List.copyOf(projects); }

    public Project start(String id, String region, String facility, int units, long day) {
        return start(id, region, facility, units, "", day);
    }

    public Project start(String id, String region, String facility, int units, String contractorCompanyId, long day) {
        if (id == null || id.isBlank() || region == null || region.isBlank()
                || !PublicConstructionEconomics.validFacility(facility) || units <= 0
                || units > 1_000_000 || projects.size() >= MAX_PROJECTS
                || contractorCompanyId == null) return null;
        Project project = new Project(id, region, facility, units, 0, contractorCompanyId, day, day);
        projects.add(project);
        setDirty();
        return project;
    }

    /** Advances each active project by at most one funded unit for the settlement day. */
    public int settleDaily(MinecraftServer server, long day) {
        GovernmentPolicySavedData policy = GovernmentPolicySavedData.get(server);
        LogisticsInfrastructureSavedData infrastructure = LogisticsInfrastructureSavedData.get(server);
        int delivered = 0;
        for (int i = 0; i < projects.size(); i++) {
            Project project = projects.get(i);
            if (project.completedUnits() >= project.units() || project.lastProgressDay() >= day) continue;
            if (infrastructure.count(project.region(), project.facility()) >= 1_000_000) continue;
            long cost = PublicConstructionEconomics.unitCost(project.facility());
            String receipt = "public-construction:" + project.id() + ":" + day;
            Company contractor = null;
            if (!project.contractorCompanyId().isBlank()) {
                contractor = CompanySavedData.get(server).get(project.contractorCompanyId());
                if (contractor == null || !"construction".equals(contractor.type())
                        || !CompanyLifecycleService.canOperate(server, contractor.companyId())) continue;
                if (policy.treasuryMinor() < cost) continue;
                InventoryOwner owner = InventoryOwner.company(contractor.companyId());
                if (!WarehouseSavedData.get(server).canConsumeBatch(owner,
                        PublicConstructionEconomics.materials(project.facility()))) continue;
                if (!WarehouseSavedData.get(server).consumeBatchOnce(owner,
                        PublicConstructionEconomics.materials(project.facility()), receipt + ":materials")) continue;
                if (!CompanyHelper.creditTreasuryNonOperatingOnce(server, contractor.companyId(), Currencies.USD.id(),
                        cost, "public-construction-revenue", "Public construction contract payment", receipt)) continue;
            }
            if (!policy.hasSpending(receipt) && !policy.spend("public-construction:" + project.region(), day,
                    cost, receipt)) continue;
            if (!infrastructure.changePublicFacility(project.region(), project.facility(), 1)) continue;
            projects.set(i, new Project(project.id(), project.region(), project.facility(), project.units(),
                    project.completedUnits() + 1, project.contractorCompanyId(), project.startDay(), day));
            delivered++;
        }
        if (delivered > 0) setDirty();
        return delivered;
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Project project : projects) {
            CompoundTag entry = new CompoundTag();
            entry.putString("id", project.id()); entry.putString("region", project.region());
            entry.putString("facility", project.facility()); entry.putInt("units", project.units());
            entry.putInt("completed", project.completedUnits()); entry.putLong("startDay", project.startDay());
            entry.putString("contractor", project.contractorCompanyId());
            entry.putLong("lastDay", project.lastProgressDay()); list.add(entry);
        }
        tag.put("projects", list);
        return tag;
    }

    public static PublicConstructionSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        PublicConstructionSavedData data = new PublicConstructionSavedData();
        ListTag list = tag.getList("projects", Tag.TAG_COMPOUND);
        for (int i = Math.max(0, list.size() - MAX_PROJECTS); i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            String id = entry.getString("id"), region = entry.getString("region"), facility = entry.getString("facility");
            String contractor = entry.contains("contractor") ? entry.getString("contractor") : "";
            int units = entry.getInt("units"), completed = entry.getInt("completed");
            if (!id.isBlank() && !region.isBlank() && PublicConstructionEconomics.validFacility(facility)
                    && units > 0 && units <= 1_000_000 && completed >= 0 && completed <= units) {
                data.projects.add(new Project(id, region, facility, units, completed,
                        contractor, entry.getLong("startDay"), entry.getLong("lastDay")));
            }
        }
        return data;
    }
}
