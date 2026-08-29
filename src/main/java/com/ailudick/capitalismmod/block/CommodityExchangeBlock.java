package com.ailudick.capitalismmod.block;

import com.ailudick.capitalismmod.market.CommodityMarket;
import com.ailudick.capitalismmod.menu.CommodityExchangeMenu;
import com.ailudick.capitalismmod.network.payload.SyncCommodityPayload;
import com.ailudick.capitalismmod.network.payload.SyncMarketOrdersPayload;
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

/**
 * Commodity exchange block. Opens a GUI where players place and trade buy/sell orders.
 */
public class CommodityExchangeBlock extends Block {
    public CommodityExchangeBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new CommodityExchangeMenu(id, inv),
                    Component.translatable("container.capitalismmod.commodity_exchange")));

            PacketDistributor.sendToPlayer(serverPlayer, new SyncCommodityPayload(
                    new HashMap<>(CommodityMarket.getPrices(serverPlayer.getServer())),
                    new HashMap<>(CommodityMarket.getHistory(serverPlayer.getServer()))));
            PacketDistributor.sendToPlayer(serverPlayer, new SyncMarketOrdersPayload(new ArrayList<>(CommodityMarket.getOrders(serverPlayer.getServer()))));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
