package com.ailudick.capitalismmod.network.payload;
import com.ailudick.capitalismmod.CapitalismMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
public record RequestTaxRefundPayload(String currencyId, long amount) implements CustomPacketPayload {
    public static final Type<RequestTaxRefundPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CapitalismMod.MODID, "request_tax_refund"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestTaxRefundPayload> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> { buf.writeUtf(value.currencyId()); buf.writeVarLong(value.amount()); },
            buf -> new RequestTaxRefundPayload(buf.readUtf(16), buf.readVarLong()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
