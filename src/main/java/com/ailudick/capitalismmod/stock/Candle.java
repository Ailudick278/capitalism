package com.ailudick.capitalismmod.stock;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * A single candlestick (OHLC) for a stock over one settlement period.
 */
public record Candle(long open, long high, long low, long close) {

    public static final Codec<Candle> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.fieldOf("open").forGetter(Candle::open),
            Codec.LONG.fieldOf("high").forGetter(Candle::high),
            Codec.LONG.fieldOf("low").forGetter(Candle::low),
            Codec.LONG.fieldOf("close").forGetter(Candle::close)
    ).apply(instance, Candle::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, Candle> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, Candle::open,
            ByteBufCodecs.VAR_LONG, Candle::high,
            ByteBufCodecs.VAR_LONG, Candle::low,
            ByteBufCodecs.VAR_LONG, Candle::close,
            Candle::new);

    /** Percent change of this candle, (close - open) / open * 100. */
    public double percentChange() {
        if (open == 0) {
            return 0.0;
        }
        return (double) (close - open) / open * 100.0;
    }
}
