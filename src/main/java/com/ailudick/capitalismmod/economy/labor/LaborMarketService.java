package com.ailudick.capitalismmod.economy.labor;

import com.ailudick.capitalismmod.company.Company;
import com.ailudick.capitalismmod.company.CompanyHelper;
import com.ailudick.capitalismmod.company.CompanySavedData;
import com.ailudick.capitalismmod.util.EconomyMath;
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

    public static boolean post(ServerPlayer player, String companyName, String role, int vacancies,
                               long dailyWageMinor, LaborSkill skill, int minimumSkill, int durationDays) {
        Company company = CompanyHelper.getCompany(player, companyName);
        long now = player.getServer().overworld().getGameTime();
        if (company == null || durationDays < 0 || durationDays > 100000 || dailyWageMinor <= 0L) return false;
        long closes = durationDays == 0 ? 0L : EconomyMath.add(now, Math.multiplyExact(durationDays, 24000L));
        if (closes < now) return false;
        try {
            return LaborMarketSavedData.get(player.getServer()).post(new JobOffer(
                    UUID.randomUUID().toString(), employer(company), role, vacancies, dailyWageMinor,
                    skill, minimumSkill, now, closes));
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
                || offer.dailyWageMinor() < profile.reservationWageMinor() || data.activeForWorker(actor(worker)).size() > 0) return false;
        if (!data.reserveVacancy(offerId)) return false;
        try {
            return data.hire(new EmploymentRecord(UUID.randomUUID().toString(), actor(worker), offer.employerId(),
                    offer.role(), offer.dailyWageMinor(), employer.getServer().overworld().getGameTime(), 0L, true));
        } catch (RuntimeException e) { return false; }
    }

    public static boolean end(ServerPlayer player, String employmentId) {
        LaborMarketSavedData data = LaborMarketSavedData.get(player.getServer());
        EmploymentRecord found = data.employments().stream().filter(e -> e.id().equals(employmentId) && e.active()).findFirst().orElse(null);
        if (found == null) return false;
        Company company = CompanySavedData.get(player.getServer()).get(found.employerId());
        if (!found.workerId().equals(actor(player)) && (company == null || !company.ownerUuid().equals(player.getUUID()))) return false;
        return data.endEmployment(employmentId, player.getServer().overworld().getGameTime());
    }
}
