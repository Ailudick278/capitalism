package com.ailudick.capitalismmod.block;

import com.ailudick.capitalismmod.blockentity.ShopBlockEntity;
import com.ailudick.capitalismmod.init.ModBlockEntities;
import com.ailudick.capitalismmod.menu.ShopMenu;
import com.ailudick.capitalismmod.network.payload.SyncShopDataPayload;
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
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

public class ShopBlock extends Block implements EntityBlock {
    public ShopBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            MenuProvider provider = getMenuProvider(state, level, pos);
            if (provider != null) {
                serverPlayer.openMenu(provider);

                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity instanceof ShopBlockEntity shop) {
                    PacketDistributor.sendToPlayer(serverPlayer, new SyncShopDataPayload(shop.getOffers()));
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof ShopBlockEntity shop) {
            return new SimpleMenuProvider(
                    (id, inv, player) -> new ShopMenu(id, inv, shop),
                    Component.translatable("container.capitalismmod.shop"));
        }
        return null;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.SHOP_BE.get().create(pos, state);
    }
}
