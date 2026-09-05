package com.ailudick.capitalismmod.block;

import com.ailudick.capitalismmod.mailbox.MailboxService;
import com.ailudick.capitalismmod.menu.MailboxMenu;
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

public class MailboxBlock extends Block {
    public MailboxBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (id, inventory, ignored) -> {
                        MailboxMenu menu = new MailboxMenu(id, inventory);
                        menu.setMessages(MailboxService.getMessages(serverPlayer));
                        return menu;
                    }, Component.translatable("container.capitalismmod.mailbox")));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
