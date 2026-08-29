package com.ailudick.capitalismmod.block;

import com.ailudick.capitalismmod.menu.StockExchangeMenu;
import com.ailudick.capitalismmod.network.payload.SyncStockOrdersPayload;
import com.ailudick.capitalismmod.network.payload.SyncStocksPayload;
import com.ailudick.capitalismmod.stock.StockMarket;
import com.ailudick.capitalismmod.wallet.EconomyHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
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
 * Stock exchange block. Opens a GUI where players buy and sell stocks.
 */
public class StockExchangeBlock extends Block {
    public StockExchangeBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new StockExchangeMenu(id, inv),
                    Component.translatable("container.capitalismmod.stock_exchange")));

            PacketDistributor.sendToPlayer(serverPlayer, new SyncStocksPayload(
                    new HashMap<>(StockMarket.getPrices(serverPlayer.getServer())),
                    new HashMap<>(StockMarket.getPortfolio(serverPlayer.getServer(), serverPlayer)),
                    new HashMap<>(StockMarket.getHistory(serverPlayer.getServer())),
                    new HashMap<>(StockMarket.getCompanyStocks(serverPlayer.getServer()))));
            PacketDistributor.sendToPlayer(serverPlayer, new SyncStockOrdersPayload(new ArrayList<>(StockMarket.getOrders(serverPlayer.getServer()))));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
