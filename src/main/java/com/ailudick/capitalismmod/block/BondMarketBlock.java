package com.ailudick.capitalismmod.block;

import com.ailudick.capitalismmod.bond.BondHolding;
import com.ailudick.capitalismmod.bond.BondSavedData;
import com.ailudick.capitalismmod.menu.BondMarketMenu;
import com.ailudick.capitalismmod.network.payload.SyncBondsPayload;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Bond market block. Opens a GUI where players buy and redeem government bonds.
 */
public class BondMarketBlock extends Block {
    public BondMarketBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new BondMarketMenu(id, inv),
                    Component.translatable("container.capitalismmod.bond_market")));

            List<BondHolding> mine = new ArrayList<>();
            for (BondHolding holding : BondSavedData.get(serverPlayer.getServer()).holdings()) {
                if (holding.holder().equals(serverPlayer.getUUID())) {
                    mine.add(holding);
                }
            }
            PacketDistributor.sendToPlayer(serverPlayer, new SyncBondsPayload(mine));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
