package com.ailudick.capitalismmod.progression;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Civilization-wide technology checks. One completed advancement unlocks the era for the settlement. */
public final class TechnologyProgression {
    private TechnologyProgression() {}

    public static boolean isUnlocked(ServerLevel level, TechnologyEra era) {
        if (era == null || era == TechnologyEra.PRE_INDUSTRIAL) return true;
        String advancementId = switch (era) {
            case STEAM_INDUSTRY -> "steam_industry";
            case ELECTRIFICATION -> "electrification";
            case PETROCHEMICAL -> "petrochemical";
            case INFORMATION -> "information";
            default -> "root";
        };
        AdvancementHolder advancement = level.getServer().getAdvancements()
                .get(ResourceLocation.fromNamespaceAndPath("capitalismmod", "technology/" + advancementId));
        if (advancement == null) return false;
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (player.getAdvancements().getOrStartProgress(advancement).isDone()) return true;
        }
        return false;
    }
}
