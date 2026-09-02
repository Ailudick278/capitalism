package com.ailudick.capitalismmod.supply;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.UUID;

/**
 * A backorder: the buyer paid up front, but the supplier did not have enough stock,
 * so the remaining quantity is delivered as the supplier produces more.
 *
 * @param remaining quantity still to be delivered
 */
public record PurchaseOrder(String id, UUID buyerUuid, UUID supplierUuid, String companyName, String itemId, int remaining,
                            String originRegion, String destinationRegion) {

    public PurchaseOrder(String id, UUID buyerUuid, UUID supplierUuid, String companyName, String itemId, int remaining) {
        this(id, buyerUuid, supplierUuid, companyName, itemId, remaining, "unknown", "unknown");
    }

    private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);

    public static final Codec<PurchaseOrder> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(PurchaseOrder::id),
            UUID_CODEC.fieldOf("buyerUuid").forGetter(PurchaseOrder::buyerUuid),
            UUID_CODEC.fieldOf("supplierUuid").forGetter(PurchaseOrder::supplierUuid),
            Codec.STRING.fieldOf("companyName").forGetter(PurchaseOrder::companyName),
            Codec.STRING.fieldOf("itemId").forGetter(PurchaseOrder::itemId),
            Codec.INT.fieldOf("remaining").forGetter(PurchaseOrder::remaining),
            Codec.STRING.optionalFieldOf("originRegion", "unknown").forGetter(PurchaseOrder::originRegion),
            Codec.STRING.optionalFieldOf("destinationRegion", "unknown").forGetter(PurchaseOrder::destinationRegion)
    ).apply(instance, PurchaseOrder::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, PurchaseOrder> STREAM_CODEC = StreamCodec.of(
            (buffer, order) -> {
                ByteBufCodecs.STRING_UTF8.encode(buffer, order.id());
                ByteBufCodecs.STRING_UTF8.encode(buffer, order.buyerUuid().toString());
                ByteBufCodecs.STRING_UTF8.encode(buffer, order.supplierUuid().toString());
                ByteBufCodecs.STRING_UTF8.encode(buffer, order.companyName());
                ByteBufCodecs.STRING_UTF8.encode(buffer, order.itemId());
                ByteBufCodecs.VAR_INT.encode(buffer, order.remaining());
                ByteBufCodecs.STRING_UTF8.encode(buffer, order.originRegion());
                ByteBufCodecs.STRING_UTF8.encode(buffer, order.destinationRegion());
            },
            buffer -> new PurchaseOrder(
                    ByteBufCodecs.STRING_UTF8.decode(buffer),
                    UUID.fromString(ByteBufCodecs.STRING_UTF8.decode(buffer)),
                    UUID.fromString(ByteBufCodecs.STRING_UTF8.decode(buffer)),
                    ByteBufCodecs.STRING_UTF8.decode(buffer),
                    ByteBufCodecs.STRING_UTF8.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.STRING_UTF8.decode(buffer),
                    ByteBufCodecs.STRING_UTF8.decode(buffer)));

    public PurchaseOrder withRemaining(int newRemaining) {
        return new PurchaseOrder(id, buyerUuid, supplierUuid, companyName, itemId, newRemaining, originRegion, destinationRegion);
    }
}
