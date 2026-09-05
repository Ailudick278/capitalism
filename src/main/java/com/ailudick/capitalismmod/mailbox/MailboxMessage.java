package com.ailudick.capitalismmod.mailbox;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** A persisted message prepared for future bank, market, and system notices. */
public record MailboxMessage(String sender, String subject, String body, long timestamp) {
    public static final Codec<MailboxMessage> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("sender").forGetter(MailboxMessage::sender),
            Codec.STRING.fieldOf("subject").forGetter(MailboxMessage::subject),
            Codec.STRING.fieldOf("body").forGetter(MailboxMessage::body),
            Codec.LONG.fieldOf("timestamp").forGetter(MailboxMessage::timestamp)
    ).apply(instance, MailboxMessage::new));
}
