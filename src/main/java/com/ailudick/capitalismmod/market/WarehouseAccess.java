package com.ailudick.capitalismmod.market;

import com.ailudick.capitalismmod.company.Company;
import com.ailudick.capitalismmod.company.CompanyHelper;
import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedHashMap;
import java.util.Map;

/** Server-side resolver for warehouse ownership and access checks. */
public final class WarehouseAccess {
    private WarehouseAccess() {}

    public static InventoryOwner personal(ServerPlayer player) {
        return InventoryOwner.player(player.getUUID());
    }

    public static Map<String, String> accessibleOwners(ServerPlayer player) {
        Map<String, String> result = new LinkedHashMap<>();
        result.put(personal(player).storageKey(), "个人仓库");
        for (Company company : CompanyHelper.getCompanies(player).values()) {
            if (company.ownerUuid().equals(player.getUUID())) {
                result.put(InventoryOwner.company(company.companyId()).storageKey(), "公司仓库：" + company.name());
            }
        }
        return result;
    }

    /** Resolves a client-supplied key only after checking current server-side access. */
    public static InventoryOwner resolve(ServerPlayer player, String key) {
        if (key == null || !accessibleOwners(player).containsKey(key)) return null;
        return InventoryOwner.parse(key);
    }
}
