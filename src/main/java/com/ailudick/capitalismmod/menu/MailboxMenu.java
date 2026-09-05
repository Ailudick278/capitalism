package com.ailudick.capitalismmod.menu;

import com.ailudick.capitalismmod.init.ModMenuTypes;
import com.ailudick.capitalismmod.mailbox.MailboxMessage;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class MailboxMenu extends AbstractContainerMenu {
    private List<MailboxMessage> messages = List.of();

    public MailboxMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.MAILBOX_MENU.get(), containerId);
    }

    public List<MailboxMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<MailboxMessage> messages) {
        this.messages = List.copyOf(messages);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
