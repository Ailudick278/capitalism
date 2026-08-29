package com.ailudick.capitalismmod.block;

import com.ailudick.capitalismmod.auction.AuctionSavedData;
import com.ailudick.capitalismmod.menu.AuctionHouseMenu;
import com.ailudick.capitalismmod.network.payload.SyncAuctionsPayload;
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

/**
 * Auction house block. Opens a GUI where players list items for auction and bid.
 */
public class AuctionHouseBlock extends Block {
    public AuctionHouseBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new AuctionHouseMenu(id, inv),
                    Component.translatable("container.capitalismmod.auction_house")));

            PacketDistributor.sendToPlayer(serverPlayer, new SyncAuctionsPayload(
                    new ArrayList<>(AuctionSavedData.get(serverPlayer.getServer()).auctions())));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
