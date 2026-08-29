package com.ailudick.capitalismmod.block;

import com.ailudick.capitalismmod.futures.FuturesMarket;
import com.ailudick.capitalismmod.futures.Position;
import com.ailudick.capitalismmod.menu.FuturesExchangeMenu;
import com.ailudick.capitalismmod.network.payload.SyncFuturesPayload;
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
import java.util.HashMap;
import java.util.List;

/**
 * Futures exchange block. Opens a GUI where players trade margin-based commodity
 * futures (open/close long or short positions, deposit/withdraw margin).
 */
public class FuturesExchangeBlock extends Block {
    public FuturesExchangeBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new FuturesExchangeMenu(id, inv),
                    Component.translatable("container.capitalismmod.futures_exchange")));

            List<Position> mine = new ArrayList<>();
            for (Position position : FuturesMarket.getPositions(serverPlayer.getServer())) {
                if (position.playerId().equals(serverPlayer.getUUID())) {
                    mine.add(position);
                }
            }
            PacketDistributor.sendToPlayer(serverPlayer, new SyncFuturesPayload(
                    new HashMap<>(FuturesMarket.getPrices(serverPlayer.getServer())),
                    mine,
                    FuturesMarket.marginBalance(serverPlayer.getServer(), serverPlayer.getUUID()),
                    new HashMap<>(FuturesMarket.getDaysToExpiry(serverPlayer.getServer()))));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
