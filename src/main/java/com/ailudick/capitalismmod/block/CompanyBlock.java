package com.ailudick.capitalismmod.block;

import com.ailudick.capitalismmod.company.CompanyHelper;
import com.ailudick.capitalismmod.menu.CompanyMenu;
import com.ailudick.capitalismmod.network.payload.SyncConglomeratePayload;
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
 * Company (公司) block. Opens a GUI where the player views and manages their
 * companies (upgrade and withdraw). No block entity needed.
 */
public class CompanyBlock extends Block {
    public CompanyBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new CompanyMenu(id, inv),
                    Component.translatable("container.capitalismmod.company")));
            PacketDistributor.sendToPlayer(serverPlayer, new SyncConglomeratePayload(
                    CompanyHelper.getConglomerate(serverPlayer).name(),
                    new HashMap<>(CompanyHelper.getCompanies(serverPlayer))));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
