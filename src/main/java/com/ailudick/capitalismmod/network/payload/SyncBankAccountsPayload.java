package com.ailudick.capitalismmod.network.payload;

import com.ailudick.capitalismmod.CapitalismMod;
import com.ailudick.capitalismmod.bank.BankAccount;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Server -> Client: sync the player's bank accounts (account id -> account).
 */
public record SyncBankAccountsPayload(Map<String, BankAccount> accounts) implements CustomPacketPayload {
    public static final Type<SyncBankAccountsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CapitalismMod.MODID, "sync_bank_accounts"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncBankAccountsPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.fromCodec(BankAccount.CODEC)), SyncBankAccountsPayload::accounts,
            SyncBankAccountsPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
