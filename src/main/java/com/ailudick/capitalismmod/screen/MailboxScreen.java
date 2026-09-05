package com.ailudick.capitalismmod.screen;

import com.ailudick.capitalismmod.client.GuiStyles;
import com.ailudick.capitalismmod.mailbox.MailboxMessage;
import com.ailudick.capitalismmod.menu.MailboxMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class MailboxScreen extends AbstractContainerScreen<MailboxMenu> {
    public MailboxScreen(MailboxMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 320;
        imageHeight = 210;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        GuiStyles.drawBackground(graphics, leftPos, topPos, imageWidth, imageHeight);
        graphics.fill(leftPos + 6, topPos + 6, leftPos + imageWidth - 6, topPos + 32, 0xCC14263A);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 12, 14, GuiStyles.ACCENT, false);
        if (menu.getMessages().isEmpty()) {
            graphics.drawString(font, Component.translatable("gui.capitalismmod.mailbox.empty"), 16, 54, GuiStyles.TEXT_DIM, false);
            return;
        }
        int y = 48;
        for (MailboxMessage message : menu.getMessages()) {
            graphics.drawString(font, Component.literal(message.subject()), 16, y, GuiStyles.TEXT, false);
            graphics.drawString(font, Component.translatable("gui.capitalismmod.mailbox.from", message.sender()), 16, y + 12, GuiStyles.TEXT_DIM, false);
            y += 30;
            if (y > imageHeight - 20) break;
        }
    }
}
