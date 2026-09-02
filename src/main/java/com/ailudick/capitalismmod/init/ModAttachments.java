package com.ailudick.capitalismmod.init;

import com.ailudick.capitalismmod.CapitalismMod;
import com.ailudick.capitalismmod.bank.BankAccount;
import com.ailudick.capitalismmod.company.Conglomerate;
import com.mojang.serialization.Codec;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.HashMap;
import java.util.Map;

public class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, CapitalismMod.MODID);

    // Player bank accounts: account id -> account.
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Map<String, BankAccount>>> BANK_ACCOUNTS =
            ATTACHMENT_TYPES.register("bank_accounts",
                    () -> AttachmentType.<Map<String, BankAccount>>builder(() -> new HashMap<String, BankAccount>())
                            .serialize(Codec.unboundedMap(Codec.STRING, BankAccount.CODEC))
                            .build());

    // Player's conglomerate (group): auto-created on first join, holds all their companies.
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Conglomerate>> CONGLOMERATE =
            ATTACHMENT_TYPES.register("conglomerate",
                    () -> AttachmentType.<Conglomerate>builder(() -> Conglomerate.create(""))
                            .serialize(Conglomerate.CODEC)
                            .build());

    // Last Minecraft day for which bank interest was applied to this player.
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Long>> LAST_BANK_SETTLEMENT_DAY =
            ATTACHMENT_TYPES.register("last_bank_settlement_day",
                    () -> AttachmentType.<Long>builder(() -> -1L)
                            .serialize(com.mojang.serialization.Codec.LONG)
                            .build());

    // World tick at which the player last replaced a bank card.
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Long>> LAST_CARD_REPLACEMENT_TICK =
            ATTACHMENT_TYPES.register("last_card_replacement_tick",
                    () -> AttachmentType.<Long>builder(() -> Long.MIN_VALUE)
                            .serialize(com.mojang.serialization.Codec.LONG)
                            .build());
}
