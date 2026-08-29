package com.ailudick.capitalismmod.block;

import com.ailudick.capitalismmod.menu.BusinessLicenseMenu;
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

/**
 * Business bureau (工商局) block. Opens the company registration GUI.
 * Registration is stateless here; company data lives on the player. No block entity needed.
 */
public class BusinessBureau extends Block {
    public BusinessBureau(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new BusinessLicenseMenu(id, inv),
                    Component.translatable("container.capitalismmod.business_bureau")));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
