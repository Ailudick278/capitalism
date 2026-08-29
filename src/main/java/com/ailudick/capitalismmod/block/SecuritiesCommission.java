package com.ailudick.capitalismmod.block;

import com.ailudick.capitalismmod.company.Company;
import com.ailudick.capitalismmod.company.CompanyHelper;
import com.ailudick.capitalismmod.menu.SecuritiesCommissionMenu;
import com.ailudick.capitalismmod.network.payload.SyncSecuritiesPayload;
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
import java.util.Map;

/**
 * Securities commission (证监会) block. Opens a GUI where the player lists (IPOs)
 * their companies on the stock exchange. Listing is stateless here; the listing
 * data lives in {@link com.ailudick.capitalismmod.economy.EconomySavedData}. No block entity needed.
 */
public class SecuritiesCommission extends Block {
    public SecuritiesCommission(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new SecuritiesCommissionMenu(id, inv),
                    Component.translatable("container.capitalismmod.securities_commission")));

            Map<String, Company> companies = CompanyHelper.getCompanies(serverPlayer);
            List<String> listed = new ArrayList<>();
            for (String name : companies.keySet()) {
                if (CompanyHelper.isListed(serverPlayer, name)) {
                    listed.add(name);
                }
            }
            PacketDistributor.sendToPlayer(serverPlayer, new SyncSecuritiesPayload(new HashMap<>(companies), listed));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
