package com.ailudick.capitalismmod.stock;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * A single limit order on the stock exchange.
 *
 * @param sell         {@code true} = sell order (shares escrowed), {@code false} = buy order (money escrowed)
 * @param pricePerUnit limit price per share, in USD
 */
public record StockOrder(String id, String ownerId, String stockId, int quantity, long pricePerUnit, boolean sell) {

    public static final Codec<StockOrder> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(StockOrder::id),
            Codec.STRING.fieldOf("ownerId").forGetter(StockOrder::ownerId),
            Codec.STRING.fieldOf("stockId").forGetter(StockOrder::stockId),
            Codec.INT.fieldOf("quantity").forGetter(StockOrder::quantity),
            Codec.LONG.fieldOf("pricePerUnit").forGetter(StockOrder::pricePerUnit),
            Codec.BOOL.fieldOf("sell").forGetter(StockOrder::sell)
    ).apply(instance, StockOrder::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, StockOrder> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, StockOrder::id,
            ByteBufCodecs.STRING_UTF8, StockOrder::ownerId,
            ByteBufCodecs.STRING_UTF8, StockOrder::stockId,
            ByteBufCodecs.VAR_INT, StockOrder::quantity,
            ByteBufCodecs.VAR_LONG, StockOrder::pricePerUnit,
            ByteBufCodecs.BOOL, StockOrder::sell,
            StockOrder::new);

    public StockOrder withQuantity(int newQuantity) {
        return new StockOrder(id, ownerId, stockId, newQuantity, pricePerUnit, sell);
    }
}
