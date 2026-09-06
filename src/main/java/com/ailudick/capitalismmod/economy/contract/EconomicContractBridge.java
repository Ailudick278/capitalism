package com.ailudick.capitalismmod.economy.contract;

import com.ailudick.capitalismmod.company.CompanyFreightContractSavedData;
import com.ailudick.capitalismmod.economy.expansion.EconomicActorRef;
import net.minecraft.server.MinecraftServer;

/** Mirrors the existing freight domain lifecycle into the generic contract index. */
public final class EconomicContractBridge {
    private EconomicContractBridge() {}
    public static void offered(MinecraftServer server, CompanyFreightContractSavedData.Contract c) {
        if (server == null || c == null) return;
        EconomicContractSavedData data = EconomicContractSavedData.get(server);
        if (data.find(c.id()) == null) data.add(new EconomicContract(c.id(), ContractType.FREIGHT,
                new EconomicActorRef("company", c.buyerCompanyId()), new EconomicActorRef("company", c.carrierCompanyId()),
                c.createdAt(), c.createdAt(), c.expiresAt(), c.quotedCost(), "usd", ContractStatus.OFFERED, 0L, 0L));
    }
    public static void status(MinecraftServer server, String id, ContractStatus status, long at) {
        if (server == null || id == null || status == null) return;
        EconomicContractSavedData data = EconomicContractSavedData.get(server); EconomicContract c = data.find(id);
        if (c != null) data.replace(c.withStatus(status));
    }

    public static void syncFreight(MinecraftServer server) {
        if (server == null) return;
        EconomicContractSavedData generic = EconomicContractSavedData.get(server);
        for (CompanyFreightContractSavedData.Contract freight : CompanyFreightContractSavedData.get(server).contracts()) {
            EconomicContract current = generic.find(freight.id());
            if (current == null) { offered(server, freight); current = generic.find(freight.id()); }
            if (current == null) continue;
            ContractStatus next = switch (freight.status()) {
                case "accepted" -> ContractStatus.ACTIVE;
                case "settled" -> ContractStatus.COMPLETED;
                case "cancelled" -> ContractStatus.CANCELLED;
                case "expired" -> ContractStatus.EXPIRED;
                case "loss" -> ContractStatus.BREACHED;
                default -> ContractStatus.OFFERED;
            };
            if (current.status() != next) generic.replace(current.withStatus(next));
        }
    }
}
