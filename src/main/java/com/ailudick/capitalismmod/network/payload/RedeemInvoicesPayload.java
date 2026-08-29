package com.ailudick.capitalismmod.network.payload;

import com.ailudick.capitalismmod.CapitalismMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client -> Server: reimburse (报销) all invoices in the player's inventory.
 */
public record RedeemInvoicesPayload() implements CustomPacketPayload {
    public static final Type<RedeemInvoicesPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CapitalismMod.MODID, "redeem_invoices"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RedeemInvoicesPayload> STREAM_CODEC =
            StreamCodec.unit(new RedeemInvoicesPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
