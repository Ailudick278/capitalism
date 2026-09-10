package com.ailudick.capitalismmod.population;

import com.ailudick.capitalismmod.entity.PlaceholderNpc;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Materializes virtual NPC households as lightweight, persistent world entities. */
public final class NpcEntityService {
    private NpcEntityService() {}

    public static int materialize(MinecraftServer server, ServerLevel level, Vec3 center,
                                  String region, int limit) {
        if (server == null || level == null || center == null || region == null
                || region.isBlank() || limit <= 0) return 0;
        PopulationSavedData population = PopulationSavedData.get(server);
        AABB search = new AABB(center, center).inflate(96.0D);
        java.util.Set<String> present = new java.util.HashSet<>();
        for (PlaceholderNpc npc : level.getEntitiesOfClass(PlaceholderNpc.class, search)) {
            if (npc.isBound()) present.add(npc.householdId());
        }
        int created = 0;
        int index = 0;
        for (Household household : population.households()) {
            if (created >= limit) break;
            if (!household.id().startsWith("npc-") || !household.region().equals(region)
                    || present.contains(household.id())) continue;
            BlockPos spawn = findSpawn(level, center, index++);
            PlaceholderNpc npc = PlaceholderNpc.create(level);
            if (npc == null) continue;
            npc.moveTo(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D,
                    (float) (level.random.nextInt(360)), 0.0F);
            npc.bindHousehold(household.id());
            if (level.noCollision(npc) && level.addFreshEntity(npc)) {
                present.add(household.id());
                created++;
            }
        }
        return created;
    }

    private static BlockPos findSpawn(ServerLevel level, Vec3 center, int index) {
        int ring = index / 8 + 1;
        int slot = index % 8;
        double angle = slot * Math.PI / 4.0D;
        int x = (int) Math.floor(center.x + Math.cos(angle) * ring * 3.0D);
        int z = (int) Math.floor(center.z + Math.sin(angle) * ring * 3.0D);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        return new BlockPos(x, y, z);
    }
}
