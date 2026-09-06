package com.ailudick.capitalismmod.economy.labor;

import com.ailudick.capitalismmod.company.Company;
import com.ailudick.capitalismmod.company.CompanyHelper;
import com.ailudick.capitalismmod.company.CompanySavedData;
import com.ailudick.capitalismmod.util.EconomyMath;
import com.ailudick.capitalismmod.economy.contract.EconomicContractBridge;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.EnumMap;
import java.util.UUID;

/** Transaction boundary for the player-facing labor market. */
public final class LaborMarketService {
    private LaborMarketService() {}

    public static String actor(ServerPlayer player) { return player == null ? "" : player.getUUID().toString(); }
    public static String employer(Company company) { return company == null ? "" : company.companyId(); }

    public static void ensureProfile(ServerPlayer player) {
        if (player == null) return;
        LaborMarketSavedData data = LaborMarketSavedData.get(player.getServer());
        if (data.profile(actor(player)) != null) return;
        EnumMap<LaborSkill, Integer> skills = new EnumMap<>(LaborSkill.class);
        skills.put(LaborSkill.FOUNDATION, 50);
        data.registerProfile(new LaborProfile(actor(player), skills, 100, 0L));
    }

    public static void ensureNpcProfile(MinecraftServer server, String id, int workingAge) {
        LaborMarketSavedData data = LaborMarketSavedData.get(server);
        if (data.profile(id) != null) return;
        EnumMap<LaborSkill, Integer> skills = new EnumMap<>(LaborSkill.class);
        skills.put(LaborSkill.FOUNDATION, 45);
        data.registerProfile(new LaborProfile(id, skills, workingAge > 0 ? 100 : 0, 100L));
    }

    public static boolean post(ServerPlayer player, String companyName, String role, int vacancies,
                               long dailyWageMinor, LaborSkill skill, int minimumSkill, int durationDays) {
        return post(player, companyName, role, vacancies, dailyWageMinor, skill, minimumSkill, durationDays, "spawn");
    }
    public static boolean post(ServerPlayer player, String companyName, String role, int vacancies,
                               long dailyWageMinor, LaborSkill skill, int minimumSkill, int durationDays, String region) {
        Company company = CompanyHelper.getCompany(player, companyName);
        long now = player.getServer().overworld().getGameTime();
        if (company == null || durationDays < 0 || durationDays > 100000 || dailyWageMinor <= 0L || region == null || region.isBlank()) return false;
        long closes = durationDays == 0 ? 0L : EconomyMath.add(now, Math.multiplyExact(durationDays, 24000L));
        if (closes < now) return false;
        try {
            return LaborMarketSavedData.get(player.getServer()).post(new JobOffer(
                    UUID.randomUUID().toString(), employer(company), role, vacancies, dailyWageMinor,
                    skill, minimumSkill, now, closes, region));
        } catch (RuntimeException ignored) { return false; }
    }

    public static boolean hire(ServerPlayer employer, String offerId, ServerPlayer worker) {
        if (employer == null || worker == null || employer.getServer() != worker.getServer()) return false;
        LaborMarketSavedData data = LaborMarketSavedData.get(employer.getServer());
        JobOffer offer = data.offer(offerId);
        Company company = CompanySavedData.get(employer.getServer()).get(offer == null ? "" : offer.employerId());
        if (offer == null || company == null || !company.ownerUuid().equals(employer.getUUID())) return false;
        ensureProfile(worker);
        LaborProfile profile = data.profile(actor(worker));
        if (profile == null || profile.participation() <= 0 || profile.skill(offer.requiredSkill()) < offer.minimumSkill()
                || offer.dailyWageMinor() < profile.effectiveReservationWageMinor() || data.activeForWorker(actor(worker)).size() > 0) return false;
        if (!data.reserveVacancy(offerId)) return false;
        try {
            EmploymentRecord employment = new EmploymentRecord(UUID.randomUUID().toString(), actor(worker), offer.employerId(),
                    offer.role(), offer.dailyWageMinor(), employer.getServer().overworld().getGameTime(), 0L, true);
            boolean hired = data.hire(employment);
            if (hired) EconomicContractBridge.employmentCreated(employer.getServer(), employment);
            return hired;
        } catch (RuntimeException e) { return false; }
    }

    public static boolean hireNpc(MinecraftServer server, String offerId, String npcId) {
        if (server == null || npcId == null || npcId.isBlank()) return false;
        LaborMarketSavedData data = LaborMarketSavedData.get(server); JobOffer offer = data.offer(offerId);
        LaborProfile profile = data.profile(npcId);
        if (offer == null || profile == null || profile.participation() <= 0 || profile.skill(offer.requiredSkill()) < offer.minimumSkill()
                || offer.dailyWageMinor() < profile.effectiveReservationWageMinor() || !data.activeForWorker(npcId).isEmpty()) return false;
        if (!data.reserveVacancy(offerId)) return false;
        EmploymentRecord employment = new EmploymentRecord(java.util.UUID.randomUUID().toString(), npcId, offer.employerId(), offer.role(),
                offer.dailyWageMinor(), server.overworld().getGameTime(), 0L, true);
        boolean hired = data.hire(employment);
        if (hired) EconomicContractBridge.employmentCreated(server, employment);
        return hired;
    }

    public static boolean end(ServerPlayer player, String employmentId) {
        LaborMarketSavedData data = LaborMarketSavedData.get(player.getServer());
        EmploymentRecord found = data.employments().stream().filter(e -> e.id().equals(employmentId) && e.active()).findFirst().orElse(null);
        if (found == null) return false;
        Company company = CompanySavedData.get(player.getServer()).get(found.employerId());
        if (!found.workerId().equals(actor(player)) && (company == null || !company.ownerUuid().equals(player.getUUID()))) return false;
        long endedAt = player.getServer().overworld().getGameTime();
        boolean ended = data.endEmployment(employmentId, endedAt);
        if (ended) EconomicContractBridge.employmentEnded(player.getServer(), employmentId, endedAt);
        return ended;
    }
}
