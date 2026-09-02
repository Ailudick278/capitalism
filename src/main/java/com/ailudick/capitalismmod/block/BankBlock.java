package com.ailudick.capitalismmod.block;

import com.ailudick.capitalismmod.bank.BankAccountHelper;
import com.ailudick.capitalismmod.menu.BankMenu;
import com.ailudick.capitalismmod.network.payload.SyncBankAccountsPayload;
import com.ailudick.capitalismmod.network.payload.SyncPersonalAssetsPayload;
import com.ailudick.capitalismmod.economy.PersonalAssets;
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

/**
 * Bank block. Opens a GUI where the player manages their bank accounts
 * (open account, deposit/withdraw, replace card). No block entity needed.
 */
public class BankBlock extends Block {
    public BankBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new BankMenu(id, inv),
                    Component.translatable("container.capitalismmod.bank")));

            PacketDistributor.sendToPlayer(serverPlayer, new SyncBankAccountsPayload(BankAccountHelper.getAccounts(serverPlayer)));
            PacketDistributor.sendToPlayer(serverPlayer, new SyncPersonalAssetsPayload(
                    PersonalAssets.estimate(serverPlayer.getServer(), serverPlayer)));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
