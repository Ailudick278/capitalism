package com.ailudick.capitalismmod.block;

import com.ailudick.capitalismmod.company.CompanyHelper;
import com.ailudick.capitalismmod.menu.TaxBureauMenu;
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
 * Tax bureau (税务局) block. Opens a GUI where the player pays their companies'
 * corporate income tax and reimburses (报销) invoices.
 */
public class TaxBureau extends Block {
    public TaxBureau(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new TaxBureauMenu(id, inv),
                    Component.translatable("container.capitalismmod.tax_bureau")));
            PacketDistributor.sendToPlayer(serverPlayer, new SyncConglomeratePayload(
                    CompanyHelper.getConglomerate(serverPlayer).name(),
                    new HashMap<>(CompanyHelper.getCompanies(serverPlayer))));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
