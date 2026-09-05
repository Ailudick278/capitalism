package com.ailudick.capitalismmod.block;

import com.ailudick.capitalismmod.market.WarehouseSavedData;
import com.ailudick.capitalismmod.market.WarehouseAccess;
import com.ailudick.capitalismmod.menu.WarehouseMenu;
import com.ailudick.capitalismmod.network.payload.SyncWarehousePayload;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;

/**
 * Warehouse block. Opens a GUI where players deposit and withdraw commodities from
 * the exchange's delivery warehouse.
 */
public class WarehouseBlock extends Block {
    public WarehouseBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new WarehouseMenu(id, inv),
                    Component.translatable("container.capitalismmod.warehouse")));

            PacketDistributor.sendToPlayer(serverPlayer, new SyncWarehousePayload(
                    new HashMap<>(WarehouseSavedData.get(serverPlayer.getServer()).storage(WarehouseAccess.personal(serverPlayer))),
                    WarehouseAccess.personal(serverPlayer).storageKey(), WarehouseAccess.accessibleOwners(serverPlayer)));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
