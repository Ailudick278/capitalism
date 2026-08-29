package com.ailudick.capitalismmod.market;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * A single order on the commodity exchange. Prices are always in USD.
 *
 * @param sell        {@code true} = sell order (commodity is escrowed), {@code false} = buy order (money is escrowed)
 * @param pricePerUnit price per single item, in USD
 */
public record MarketOrder(String id, String ownerId, ItemStack commodity, int quantity, long pricePerUnit, boolean sell) {

    /** Persistence codec: {@code commodity} is stored as its item registry id (commodities carry no NBT components). */
    private static final Codec<ItemStack> ITEM_CODEC = Codec.STRING.xmap(
            itemId -> {
                Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
                return new ItemStack(item == null ? Items.AIR : item);
            },
            stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());

    public static final Codec<MarketOrder> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(MarketOrder::id),
            Codec.STRING.fieldOf("ownerId").forGetter(MarketOrder::ownerId),
            ITEM_CODEC.fieldOf("commodity").forGetter(MarketOrder::commodity),
            Codec.INT.fieldOf("quantity").forGetter(MarketOrder::quantity),
            Codec.LONG.fieldOf("pricePerUnit").forGetter(MarketOrder::pricePerUnit),
            Codec.BOOL.fieldOf("sell").forGetter(MarketOrder::sell)
    ).apply(instance, MarketOrder::new));

    // Manual StreamCodec: StreamCodec.composite has no 6-field overload.
    public static final StreamCodec<RegistryFriendlyByteBuf, MarketOrder> STREAM_CODEC = StreamCodec.of(
            (buf, order) -> {
                ByteBufCodecs.STRING_UTF8.encode(buf, order.id());
                ByteBufCodecs.STRING_UTF8.encode(buf, order.ownerId());
                ItemStack.STREAM_CODEC.encode(buf, order.commodity());
                ByteBufCodecs.VAR_INT.encode(buf, order.quantity());
                ByteBufCodecs.VAR_LONG.encode(buf, order.pricePerUnit());
                ByteBufCodecs.BOOL.encode(buf, order.sell());
            },
            buf -> new MarketOrder(
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    ItemStack.STREAM_CODEC.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_LONG.decode(buf),
                    ByteBufCodecs.BOOL.decode(buf)
            )
    );

    public MarketOrder withQuantity(int newQuantity) {
        return new MarketOrder(id, ownerId, commodity, newQuantity, pricePerUnit, sell);
    }
}
