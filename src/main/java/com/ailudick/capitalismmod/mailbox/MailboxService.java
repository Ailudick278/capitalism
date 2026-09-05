package com.ailudick.capitalismmod.mailbox;

import com.ailudick.capitalismmod.init.ModAttachments;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/** Server-side entry point for writing player messages. */
public final class MailboxService {
    private MailboxService() {}

    public static List<MailboxMessage> getMessages(ServerPlayer player) {
        return List.copyOf(player.getData(ModAttachments.MAILBOX_MESSAGES));
    }

    public static void sendMessage(ServerPlayer player, String sender, String subject, String body) {
        List<MailboxMessage> messages = new ArrayList<>(player.getData(ModAttachments.MAILBOX_MESSAGES));
        messages.add(new MailboxMessage(sender, subject, body, player.level().getGameTime()));
        player.setData(ModAttachments.MAILBOX_MESSAGES, messages);
    }
}
