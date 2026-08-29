package com.ailudick.capitalismmod.network.payload;

import com.ailudick.capitalismmod.CapitalismMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client -> Server: open a new bank account using a blank card.
 *
 * @param credit whether to open a credit account (requires a credit card)
 */
public record OpenAccountPayload(boolean credit) implements CustomPacketPayload {
    public static final Type<OpenAccountPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CapitalismMod.MODID, "open_account"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenAccountPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, OpenAccountPayload::credit,
            OpenAccountPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
