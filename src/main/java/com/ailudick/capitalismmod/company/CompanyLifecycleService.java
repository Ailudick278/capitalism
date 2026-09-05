package com.ailudick.capitalismmod.company;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Applies the small legal-status state machine currently supported by companies. */
public final class CompanyLifecycleService {
    private CompanyLifecycleService() {
    }

    public static boolean canOperate(MinecraftServer server, String companyId) {
        return server != null && "ACTIVE".equals(CompanyStatusSavedData.get(server).statusOf(companyId));
    }

    public static String status(MinecraftServer server, String companyId) {
        return CompanyStatusSavedData.get(server).statusOf(companyId);
    }

    public static boolean suspend(ServerPlayer player, Company company, String reason) {
        if (player == null || company == null || !company.ownerUuid().equals(player.getUUID())) return false;
        CompanyStatusSavedData data = CompanyStatusSavedData.get(player.getServer());
        if (!"ACTIVE".equals(data.statusOf(company.companyId()))) return false;
        data.set(new CompanyStatusSavedData.Status(company.companyId(), "SUSPENDED",
                player.getServer().overworld().getGameTime(), reason == null ? "" : reason));
        return true;
    }

    public static boolean resume(ServerPlayer player, Company company) {
        if (player == null || company == null || !company.ownerUuid().equals(player.getUUID())) return false;
        CompanyStatusSavedData data = CompanyStatusSavedData.get(player.getServer());
        if (!"SUSPENDED".equals(data.statusOf(company.companyId()))) return false;
        data.set(new CompanyStatusSavedData.Status(company.companyId(), "ACTIVE",
                player.getServer().overworld().getGameTime(), "resumed by owner"));
        return true;
    }
}
