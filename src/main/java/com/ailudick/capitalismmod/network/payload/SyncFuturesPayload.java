package com.ailudick.capitalismmod.network.payload;

import com.ailudick.capitalismmod.CapitalismMod;
import com.ailudick.capitalismmod.futures.Position;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Server -> Client: sync futures prices, the player's own positions, margin balance,
 * and days-to-expiry per commodity.
 */
public record SyncFuturesPayload(Map<String, Long> prices, List<Position> positions, long marginBalance, Map<String, Long> daysToExpiry) implements CustomPacketPayload {
    public static final Type<SyncFuturesPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CapitalismMod.MODID, "sync_futures"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncFuturesPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_LONG), SyncFuturesPayload::prices,
            Position.STREAM_CODEC.apply(ByteBufCodecs.list()), SyncFuturesPayload::positions,
            ByteBufCodecs.VAR_LONG, SyncFuturesPayload::marginBalance,
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_LONG), SyncFuturesPayload::daysToExpiry,
            SyncFuturesPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
