package com.ailudick.capitalismmod.government;

import com.ailudick.capitalismmod.market.LogisticsInfrastructureSavedData;
import com.ailudick.capitalismmod.company.Company;
import com.ailudick.capitalismmod.company.CompanyHelper;
import com.ailudick.capitalismmod.company.CompanyLifecycleService;
import com.ailudick.capitalismmod.company.CompanySavedData;
import com.ailudick.capitalismmod.currency.Currencies;
import com.ailudick.capitalismmod.market.InventoryOwner;
import com.ailudick.capitalismmod.market.WarehouseSavedData;
import com.ailudick.capitalismmod.company.CompanyLaborSavedData;
import com.ailudick.capitalismmod.economy.labor.LaborMarketSavedData;
import com.ailudick.capitalismmod.economy.FinancialSettlementJournalSavedData;
import com.ailudick.capitalismmod.economy.contract.ContractStatus;
import com.ailudick.capitalismmod.economy.contract.ContractType;
import com.ailudick.capitalismmod.economy.expansion.EconomicActorRef;
import com.ailudick.capitalismmod.economy.contract.EconomicContract;
import com.ailudick.capitalismmod.economy.contract.EconomicContractSavedData;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Persistent, budget-backed public facility construction queue. */
public final class PublicConstructionSavedData extends SavedData {
    private static final String ID = "capitalismmod_public_construction";
    private static final int MAX_PROJECTS = 4096;
    private final List<Project> projects = new ArrayList<>();
    private final List<Bid> bids = new ArrayList<>();

    public record Project(String id, String region, String facility, int units, int completedUnits,
                          String contractorCompanyId,
                          long contractPriceMinor,
                          long startDay, long lastProgressDay) {}
    public record Bid(String projectId, String companyId, long unitPriceMinor, long submittedDay) {}

    private PublicConstructionSavedData() {}

    public static PublicConstructionSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(PublicConstructionSavedData::new, PublicConstructionSavedData::load), ID);
    }

    public List<Project> projects() { return List.copyOf(projects); }
    public List<Bid> bids() { return List.copyOf(bids); }

    public boolean hasActiveProject(String region, String facility) {
        return projects.stream().anyMatch(p -> p.region().equals(region) && p.facility().equals(facility)
                && p.completedUnits() < p.units());
    }

    public Project start(String id, String region, String facility, int units, long day) {
        return start(id, region, facility, units, "", day);
    }

    public Project start(String id, String region, String facility, int units, String contractorCompanyId, long day) {
        if (id == null || id.isBlank() || region == null || region.isBlank()
                || !PublicConstructionEconomics.validFacility(facility) || units <= 0
                || units > 1_000_000 || projects.size() >= MAX_PROJECTS
                || contractorCompanyId == null
                || projects.stream().anyMatch(existing -> id.equals(existing.id()))) return null;
        Project project = new Project(id, region, facility, units, 0, contractorCompanyId,
                contractorCompanyId.isBlank() ? 0L : PublicConstructionEconomics.unitCost(facility), day, day);
        projects.add(project);
        setDirty();
        return project;
    }

    public boolean submitBid(String projectId, String companyId, long unitPriceMinor, long day) {
        Project project = projects.stream().filter(p -> p.id().equals(projectId)).findFirst().orElse(null);
        if (project == null || project.completedUnits() > 0 || !project.contractorCompanyId().isBlank()
                || companyId == null || companyId.isBlank() || unitPriceMinor <= 0L
                || unitPriceMinor > PublicConstructionEconomics.unitCost(project.facility()) * 2L
                || bids.stream().anyMatch(b -> b.projectId().equals(projectId) && b.companyId().equals(companyId))) return false;
        bids.add(new Bid(projectId, companyId, unitPriceMinor, day));
        setDirty();
        return true;
    }

    private void awardLowestBid(MinecraftServer server, Project project) {
        if (!project.contractorCompanyId().isBlank()) return;
        Bid winner = bids.stream().filter(b -> b.projectId().equals(project.id()))
                .filter(b -> {
                    Company company = CompanySavedData.get(server).get(b.companyId());
                    return company != null && "construction".equals(company.type())
                            && CompanyLifecycleService.canOperate(server, company.companyId());
                }).min(Comparator.comparingLong(Bid::unitPriceMinor).thenComparing(Bid::submittedDay)).orElse(null);
        if (winner == null) return;
        if (!createContract(server, project, winner.companyId(), winner.unitPriceMinor())) return;
        int index = projects.indexOf(project);
        projects.set(index, new Project(project.id(), project.region(), project.facility(), project.units(),
                project.completedUnits(), winner.companyId(), winner.unitPriceMinor(), project.startDay(), project.lastProgressDay()));
        setDirty();
    }

    private boolean createContract(MinecraftServer server, Project project, String companyId, long unitPrice) {
        String contractId = "public-construction:" + project.id();
        EconomicContractSavedData contracts = EconomicContractSavedData.get(server);
        if (contracts.find(contractId) != null) return true;
        long total;
        try { total = Math.multiplyExact(unitPrice, project.units()); }
        catch (ArithmeticException e) { return false; }
        long endsAt;
        try { endsAt = Math.addExact(server.overworld().getGameTime(), Math.multiplyExact(Math.max(1, project.units()), 24000L)); }
        catch (ArithmeticException e) { endsAt = Long.MAX_VALUE; }
        return contracts.add(new EconomicContract(contractId, ContractType.PUBLIC_CONSTRUCTION,
                EconomicActorRef.of("government", "treasury"), EconomicActorRef.of("company", companyId),
                server.overworld().getGameTime(), server.overworld().getGameTime(), endsAt, total,
                Currencies.USD.id(), ContractStatus.ACTIVE, 0L, project.units(), 0L));
    }

    /** Advances each active project by at most one funded unit for the settlement day. */
    public int settleDaily(MinecraftServer server, long day) {
        GovernmentPolicySavedData policy = GovernmentPolicySavedData.get(server);
        LogisticsInfrastructureSavedData infrastructure = LogisticsInfrastructureSavedData.get(server);
        int delivered = 0;
        for (int i = 0; i < projects.size(); i++) {
            Project project = projects.get(i);
            if (project.completedUnits() >= project.units() || project.lastProgressDay() >= day) continue;
            awardLowestBid(server, project);
            project = projects.get(i);
            if (!project.contractorCompanyId().isBlank()
                    && !createContract(server, project, project.contractorCompanyId(),
                    project.contractPriceMinor() > 0L ? project.contractPriceMinor()
                            : PublicConstructionEconomics.unitCost(project.facility()))) continue;
            if (!project.contractorCompanyId().isBlank()) {
                var contract = EconomicContractSavedData.get(server).find("public-construction:" + project.id());
                if (contract == null || contract.status() != ContractStatus.ACTIVE) continue;
            }
            if (infrastructure.count(project.region(), project.facility()) >= 1_000_000) continue;
            long cost = project.contractPriceMinor() > 0L ? project.contractPriceMinor()
                    : PublicConstructionEconomics.unitCost(project.facility());
            String receipt = "public-construction:" + project.id() + ":" + day;
            FinancialSettlementJournalSavedData journal = FinancialSettlementJournalSavedData.get(server);
            Company contractor = null;
            if (!project.contractorCompanyId().isBlank()) {
                contractor = CompanySavedData.get(server).get(project.contractorCompanyId());
                if (contractor == null || !"construction".equals(contractor.type())
                        || !CompanyLifecycleService.canOperate(server, contractor.companyId())) continue;
                long legacyWorkers = CompanyLaborSavedData.get(server).contracts(contractor.companyId()).stream()
                        .filter(CompanyLaborSavedData.WorkerContract::active)
                        .filter(worker -> PublicConstructionEconomics.isConstructionRole(worker.role()))
                        .mapToLong(CompanyLaborSavedData.WorkerContract::count).sum();
                long marketWorkers = LaborMarketSavedData.get(server).activeForEmployer(contractor.companyId()).stream()
                        .filter(worker -> PublicConstructionEconomics.isConstructionRole(worker.role())).count();
                if (legacyWorkers + marketWorkers <= 0L) continue;
                if (!policy.hasSpending(receipt) && policy.treasuryMinor() < cost) continue;
                InventoryOwner owner = InventoryOwner.company(contractor.companyId());
                String materialsSource = receipt + ":materials";
                WarehouseSavedData warehouse = WarehouseSavedData.get(server);
                if (!journal.isCompleted(receipt, "materials")
                        && !warehouse.hasConsumedSource(materialsSource)
                        && !warehouse.canConsumeBatch(owner, PublicConstructionEconomics.materials(project.facility()))) continue;
                journal.markStarted(receipt, "public-construction", "materials", 0L,
                        server.overworld().getGameTime());
                if (!warehouse.consumeBatchOnce(owner,
                        PublicConstructionEconomics.materials(project.facility()), materialsSource)) continue;
                journal.markCompleted(receipt, "public-construction", "materials", 0L,
                        server.overworld().getGameTime());
                journal.markStarted(receipt, "public-construction", "contractor-payment", cost,
                        server.overworld().getGameTime());
                if (!CompanyHelper.creditTreasuryNonOperatingOnce(server, contractor.companyId(), Currencies.USD.id(),
                        cost, "public-construction-revenue", "Public construction contract payment", receipt)) continue;
                journal.markCompleted(receipt, "public-construction", "contractor-payment", cost,
                        server.overworld().getGameTime());
            }
            journal.markStarted(receipt, "public-construction", "government-spending", cost,
                    server.overworld().getGameTime());
            if (!policy.hasSpending(receipt) && !policy.spend("public-construction:" + project.region(), day,
                    cost, receipt)) continue;
            journal.markCompleted(receipt, "public-construction", "government-spending", cost,
                    server.overworld().getGameTime());
            if (!journal.isCompleted(receipt, "facility")) {
                journal.markStarted(receipt, "public-construction", "facility", 1L,
                        server.overworld().getGameTime());
                if (!infrastructure.changePublicFacility(project.region(), project.facility(), 1)) continue;
                journal.markCompleted(receipt, "public-construction", "facility", 1L,
                        server.overworld().getGameTime());
            }
            projects.set(i, new Project(project.id(), project.region(), project.facility(), project.units(),
                    project.completedUnits() + 1, project.contractorCompanyId(), project.contractPriceMinor(),
                    project.startDay(), day));
            String contractId = "public-construction:" + project.id();
            if (!project.contractorCompanyId().isBlank()) {
                EconomicContractSavedData contracts = EconomicContractSavedData.get(server);
                contracts.fulfill(contractId, 1L);
                if (project.completedUnits() + 1 >= project.units()) {
                    contracts.transition(contractId, ContractStatus.COMPLETED, server.overworld().getGameTime());
                }
                recordInspection(server, project, project.completedUnits() + 1, day);
            }
            delivered++;
        }
        if (delivered > 0) setDirty();
        return delivered;
    }

    /** Charges the contractor for pending quality failures without new government funding. */
    public int settleRework(MinecraftServer server, long day) {
        var inspections = PublicConstructionInspectionSavedData.get(server);
        var warehouse = WarehouseSavedData.get(server);
        int completed = 0;
        for (var inspection : inspections.pendingReworkBefore(day)) {
            Project project = projects.stream().filter(p -> p.id().equals(inspection.projectId())).findFirst().orElse(null);
            if (project == null || project.contractorCompanyId().isBlank()) continue;
            Company contractor = CompanySavedData.get(server).get(project.contractorCompanyId());
            if (contractor == null || !CompanyLifecycleService.canOperate(server, contractor.companyId())) continue;
            long cost = PublicConstructionEconomics.reworkCost(
                    PublicConstructionEconomics.unitCost(project.facility()));
            if (cost <= 0L || contractor.treasuryOf(Currencies.USD.id()) < cost) continue;
            String source = "public-rework:" + project.id() + ":" + inspection.unit();
            InventoryOwner owner = InventoryOwner.company(contractor.companyId());
            if (!warehouse.consumeBatchOnce(owner, PublicConstructionEconomics.materials(project.facility()), source + ":materials")) continue;
            if (!CompanyHelper.debitTreasuryNonOperatingOnce(server, contractor.companyId(), Currencies.USD.id(),
                    cost, "public-construction-rework", "Construction quality rework", source)) continue;
            if (inspections.markReworkCompleted(project.id(), inspection.unit(), day)) completed++;
        }
        return completed;
    }

    private void recordInspection(MinecraftServer server, Project project, int unit, long day) {
        var labor = LaborMarketSavedData.get(server);
        var legacy = CompanyLaborSavedData.get(server).contracts(project.contractorCompanyId()).stream()
                .filter(CompanyLaborSavedData.WorkerContract::active)
                .filter(worker -> PublicConstructionEconomics.isConstructionRole(worker.role())).toList();
        var market = labor.activeForEmployer(project.contractorCompanyId()).stream()
                .filter(worker -> PublicConstructionEconomics.isConstructionRole(worker.role())).toList();
        long workerDays = legacy.stream().mapToLong(CompanyLaborSavedData.WorkerContract::count).sum() + market.size();
        long weightedSkill = legacy.stream().mapToLong(worker ->
                (long) worker.count() * Math.max(0, Math.min(100, worker.skill()))).sum();
        weightedSkill += market.stream().mapToLong(worker -> {
            var profile = labor.profile(worker.workerId());
            return profile == null ? 0L : profile.averageSkill();
        }).sum();
        int skill = workerDays <= 0L ? 0 : (int) Math.min(100L, weightedSkill / workerDays);
        int quality = PublicConstructionEconomics.qualityScore(skill, workerDays);
        PublicConstructionInspectionSavedData.get(server).record(new PublicConstructionInspectionSavedData.Inspection(
                project.id(), unit, workerDays, skill, quality, quality >= 50 ? "ACCEPTED" : "REWORK_REQUIRED", day));
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Project project : projects) {
            CompoundTag entry = new CompoundTag();
            entry.putString("id", project.id()); entry.putString("region", project.region());
            entry.putString("facility", project.facility()); entry.putInt("units", project.units());
            entry.putInt("completed", project.completedUnits()); entry.putLong("startDay", project.startDay());
            entry.putString("contractor", project.contractorCompanyId());
            entry.putLong("contractPrice", project.contractPriceMinor());
            entry.putLong("lastDay", project.lastProgressDay()); list.add(entry);
        }
        tag.put("projects", list);
        ListTag bidList = new ListTag();
        for (Bid bid : bids) {
            CompoundTag entry = new CompoundTag(); entry.putString("project", bid.projectId());
            entry.putString("company", bid.companyId()); entry.putLong("price", bid.unitPriceMinor());
            entry.putLong("day", bid.submittedDay()); bidList.add(entry);
        }
        tag.put("bids", bidList);
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
                        contractor, Math.max(0L, entry.getLong("contractPrice")),
                        entry.getLong("startDay"), entry.getLong("lastDay")));
            }
        }
        ListTag bidEntries = tag.getList("bids", Tag.TAG_COMPOUND);
        for (int i = Math.max(0, bidEntries.size() - 8192); i < bidEntries.size(); i++) {
            CompoundTag entry = bidEntries.getCompound(i);
            String project = entry.getString("project"), company = entry.getString("company");
            long price = entry.getLong("price");
            if (!project.isBlank() && !company.isBlank() && price > 0L) {
                data.bids.add(new Bid(project, company, price, entry.getLong("day")));
            }
        }
        return data;
    }
}
