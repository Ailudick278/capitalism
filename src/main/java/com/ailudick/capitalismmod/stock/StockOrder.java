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
public record StockOrder(String id, String ownerId, String stockId, int quantity, long pricePerUnit,
                         boolean sell, long createdAt) {

    public static final Codec<StockOrder> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(StockOrder::id),
            Codec.STRING.fieldOf("ownerId").forGetter(StockOrder::ownerId),
            Codec.STRING.fieldOf("stockId").forGetter(StockOrder::stockId),
            Codec.INT.fieldOf("quantity").forGetter(StockOrder::quantity),
            Codec.LONG.fieldOf("pricePerUnit").forGetter(StockOrder::pricePerUnit),
            Codec.BOOL.fieldOf("sell").forGetter(StockOrder::sell),
            Codec.LONG.optionalFieldOf("createdAt", 0L).forGetter(StockOrder::createdAt)
    ).apply(instance, StockOrder::new));

    /** Manual codec because NeoForge's composite helper has a six-field limit. */
    public static final StreamCodec<RegistryFriendlyByteBuf, StockOrder> STREAM_CODEC = StreamCodec.of(
            (buf, order) -> {
                ByteBufCodecs.STRING_UTF8.encode(buf, order.id());
                ByteBufCodecs.STRING_UTF8.encode(buf, order.ownerId());
                ByteBufCodecs.STRING_UTF8.encode(buf, order.stockId());
                ByteBufCodecs.VAR_INT.encode(buf, order.quantity());
                ByteBufCodecs.VAR_LONG.encode(buf, order.pricePerUnit());
                ByteBufCodecs.BOOL.encode(buf, order.sell());
                ByteBufCodecs.VAR_LONG.encode(buf, order.createdAt());
            },
            buf -> new StockOrder(
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_LONG.decode(buf),
                    ByteBufCodecs.BOOL.decode(buf),
                    ByteBufCodecs.VAR_LONG.decode(buf)
            )
    );

    /** Compatibility constructor for orders saved before time priority was added. */
    public StockOrder(String id, String ownerId, String stockId, int quantity, long pricePerUnit, boolean sell) {
        this(id, ownerId, stockId, quantity, pricePerUnit, sell, 0L);
    }

    public StockOrder withQuantity(int newQuantity) {
        return new StockOrder(id, ownerId, stockId, newQuantity, pricePerUnit, sell, createdAt);
    }
}
