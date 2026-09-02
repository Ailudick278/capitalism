package com.ailudick.capitalismmod.network.payload;

import com.ailudick.capitalismmod.CapitalismMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OperationResultPayload(boolean success, String message) implements CustomPacketPayload {
    public static final Type<OperationResultPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CapitalismMod.MODID, "operation_result"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OperationResultPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, OperationResultPayload::success,
            ByteBufCodecs.STRING_UTF8, OperationResultPayload::message,
            OperationResultPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
