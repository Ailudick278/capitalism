package com.ailudick.capitalismmod.block;

import com.ailudick.capitalismmod.menu.ProcurementMenu;
import com.ailudick.capitalismmod.network.payload.SyncSupplyMarketPayload;
import com.ailudick.capitalismmod.supply.PurchaseOrder;
import com.ailudick.capitalismmod.supply.SupplyMarketSavedData;
import com.ailudick.capitalismmod.company.CompanyHelper;
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
 * Procurement block. Opens a GUI where players browse supplier offers and place purchase orders.
 */
public class ProcurementBlock extends Block {
    public ProcurementBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new ProcurementMenu(id, inv),
                    Component.translatable("container.capitalismmod.procurement")));

            List<PurchaseOrder> mine = new ArrayList<>();
            for (PurchaseOrder order : SupplyMarketSavedData.get(serverPlayer.getServer()).orders()) {
                if (order.buyerUuid().equals(serverPlayer.getUUID())) {
                    mine.add(order);
                }
            }
            PacketDistributor.sendToPlayer(serverPlayer, new SyncSupplyMarketPayload(
                    new ArrayList<>(SupplyMarketSavedData.get(serverPlayer.getServer()).offers()), mine,
                    new ArrayList<>(CompanyHelper.getCompanies(serverPlayer).keySet())));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
