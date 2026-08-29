package com.ailudick.capitalismmod.network.payload;

import com.ailudick.capitalismmod.CapitalismMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client -> Server: deposit or withdraw between wallet and bank account.
 *
 * @param deposit {@code true} = wallet -> account, {@code false} = account -> wallet
 */
public record BankTransactionPayload(String accountId, String currencyId, long amount, boolean deposit) implements CustomPacketPayload {
    public static final Type<BankTransactionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CapitalismMod.MODID, "bank_transaction"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BankTransactionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, BankTransactionPayload::accountId,
            ByteBufCodecs.STRING_UTF8, BankTransactionPayload::currencyId,
            ByteBufCodecs.VAR_LONG, BankTransactionPayload::amount,
            ByteBufCodecs.BOOL, BankTransactionPayload::deposit,
            BankTransactionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
