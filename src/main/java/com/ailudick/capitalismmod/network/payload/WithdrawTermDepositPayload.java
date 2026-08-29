package com.ailudick.capitalismmod.network.payload;

import com.ailudick.capitalismmod.CapitalismMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client -> Server: withdraw a term deposit early (by index in the account's termDeposits list).
 */
public record WithdrawTermDepositPayload(String accountId, int index) implements CustomPacketPayload {
    public static final Type<WithdrawTermDepositPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CapitalismMod.MODID, "withdraw_term_deposit"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WithdrawTermDepositPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, WithdrawTermDepositPayload::accountId,
            ByteBufCodecs.VAR_INT, WithdrawTermDepositPayload::index,
            WithdrawTermDepositPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
