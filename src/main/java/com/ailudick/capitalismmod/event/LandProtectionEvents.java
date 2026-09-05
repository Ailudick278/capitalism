package com.ailudick.capitalismmod.event;

import com.ailudick.capitalismmod.CapitalismMod;
import com.ailudick.capitalismmod.land.LandHelper;
import com.ailudick.capitalismmod.land.LandSavedData;
import com.ailudick.capitalismmod.land.LandOperationLogSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDestroyBlockEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.common.util.TriState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.tags.BlockTags;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.PistonEvent;

@EventBusSubscriber(modid = CapitalismMod.MODID)
public final class LandProtectionEvents {
    private LandProtectionEvents() {}

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player && !LandHelper.canBuild(player, event.getPos())) {
            event.setCanceled(true);
            deny(player, event.getPos(), "拒绝破坏方块");
        }
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !LandHelper.canBuild(player, event.getBlockSnapshot().getPos())) {
            event.setCanceled(true);
            deny(player, event.getBlockSnapshot().getPos(), "拒绝放置方块");
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            var state = event.getLevel().getBlockState(event.getPos());
            boolean denied = state.hasBlockEntity()
                    ? !LandHelper.canContainer(player, event.getPos())
                    : isRedstoneBlock(state) ? !LandHelper.canRedstone(player, event.getPos())
                    : !LandHelper.canInteract(player, event.getPos());
            if (denied) {
                event.setCanceled(true);
                deny(player, event.getPos(), state.hasBlockEntity() ? "拒绝容器访问"
                        : isRedstoneBlock(state) ? "拒绝红石操作" : "拒绝方块交互");
            }
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer player && isFluidBucket(event.getItemStack())
                && !LandHelper.canInteract(player, player.blockPosition())) {
            event.setCanceled(true);
            deny(player, player.blockPosition(), "拒绝桶装流体");
        }
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        if (!(event.getEntity().level() instanceof ServerLevel)
                || !(event.getSource().getEntity() instanceof ServerPlayer attacker)) return;
        if (!LandHelper.canInteract(attacker, event.getEntity().blockPosition())) {
            event.setNewDamage(0.0F);
            deny(attacker, event.getEntity().blockPosition(), "拒绝攻击土地内实体");
        }
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (event.getProjectile().level() instanceof ServerLevel level
                && event.getRayTraceResult().getLocation() != null) {
            BlockPos pos = BlockPos.containing(event.getRayTraceResult().getLocation());
            if (isClaimed(level, pos)) event.setCanceled(true);
            if (isClaimed(level, pos) && event.getProjectile().getOwner() instanceof ServerPlayer player) {
                deny(player, pos, "拒绝投掷物命中");
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDestroyBlock(LivingDestroyBlockEvent event) {
        if (event.getEntity().level() instanceof ServerLevel level && isClaimed(level, event.getPos())) {
            event.setCanceled(true);
            if (event.getEntity() instanceof ServerPlayer player) deny(player, event.getPos(), "拒绝生物破坏方块");
        }
    }

    @SubscribeEvent
    public static void onFarmlandTrample(BlockEvent.FarmlandTrampleEvent event) {
        if (event.getLevel() instanceof ServerLevel level && isClaimed(level, event.getPos())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player && !LandHelper.canInteract(player, event.getEntity().blockPosition())) {
            event.setCanceled(true);
            deny(player, event.getEntity().blockPosition(), "拒绝丢弃物品");
        }
    }

    @SubscribeEvent
    public static void onItemPickup(ItemEntityPickupEvent.Pre event) {
        if (event.getPlayer() instanceof ServerPlayer player
                && !LandHelper.canInteract(player, event.getItemEntity().blockPosition())) {
            event.setCanPickup(TriState.FALSE);
            deny(player, event.getItemEntity().blockPosition(), "拒绝拾取物品");
        }
    }

    private static boolean isFluidBucket(net.minecraft.world.item.ItemStack stack) {
        return stack.is(Items.WATER_BUCKET) || stack.is(Items.LAVA_BUCKET)
                || stack.is(Items.POWDER_SNOW_BUCKET);
    }

    private static boolean isRedstoneBlock(net.minecraft.world.level.block.state.BlockState state) {
        return state.is(Blocks.LEVER) || state.is(BlockTags.BUTTONS) || state.is(BlockTags.PRESSURE_PLATES)
                || state.is(BlockTags.TRAPDOORS);
    }

    @SubscribeEvent
    public static void onFirePhysics(BlockEvent.NeighborNotifyEvent event) {
        if (event.getLevel() instanceof ServerLevel level
                && event.getState().is(Blocks.FIRE) && isClaimed(level, event.getPos())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPiston(PistonEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        if (isClaimed(level, event.getPos()) || isClaimed(level, event.getFaceOffsetPos())) {
            event.setCanceled(true);
            return;
        }

        var resolver = event.getStructureHelper();
        if (resolver != null && (resolver.getToPush().stream().anyMatch(pos -> isClaimed(level, pos))
                || resolver.getToDestroy().stream().anyMatch(pos -> isClaimed(level, pos)))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onFluidPlace(BlockEvent.FluidPlaceBlockEvent event) {
        if (event.getLevel() instanceof ServerLevel level && isClaimed(level, event.getPos())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        event.getAffectedBlocks().removeIf(pos -> {
            return event.getLevel() instanceof ServerLevel level && isClaimed(level, pos);
        });
    }

    private static boolean isClaimed(ServerLevel level, BlockPos pos) {
        String key = level.dimension().location() + ":"
                + Math.floorDiv(pos.getX(), 16) + ":" + Math.floorDiv(pos.getZ(), 16);
        return LandSavedData.get(level.getServer()).get(key) != null;
    }

    private static void deny(ServerPlayer player, BlockPos pos, String action) {
        String dimension = player.level().dimension().location().toString();
        LandOperationLogSavedData.get(player.getServer()).record(player.level().getGameTime(), player.getUUID(),
                action, dimension, Math.floorDiv(pos.getX(), 16), Math.floorDiv(pos.getZ(), 16));
        player.displayClientMessage(net.minecraft.network.chat.Component.literal(action), true);
    }
}
