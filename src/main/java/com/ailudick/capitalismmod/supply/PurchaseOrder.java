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
public record PurchaseOrder(String id, UUID buyerUuid, UUID supplierUuid, String companyName, String itemId, int remaining) {

    private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);

    public static final Codec<PurchaseOrder> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(PurchaseOrder::id),
            UUID_CODEC.fieldOf("buyerUuid").forGetter(PurchaseOrder::buyerUuid),
            UUID_CODEC.fieldOf("supplierUuid").forGetter(PurchaseOrder::supplierUuid),
            Codec.STRING.fieldOf("companyName").forGetter(PurchaseOrder::companyName),
            Codec.STRING.fieldOf("itemId").forGetter(PurchaseOrder::itemId),
            Codec.INT.fieldOf("remaining").forGetter(PurchaseOrder::remaining)
    ).apply(instance, PurchaseOrder::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, PurchaseOrder> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, PurchaseOrder::id,
            ByteBufCodecs.STRING_UTF8, o -> o.buyerUuid().toString(),
            ByteBufCodecs.STRING_UTF8, o -> o.supplierUuid().toString(),
            ByteBufCodecs.STRING_UTF8, PurchaseOrder::companyName,
            ByteBufCodecs.STRING_UTF8, PurchaseOrder::itemId,
            ByteBufCodecs.VAR_INT, PurchaseOrder::remaining,
            (id, buyer, supplier, companyName, itemId, remaining) ->
                    new PurchaseOrder(id, UUID.fromString(buyer), UUID.fromString(supplier), companyName, itemId, remaining));

    public PurchaseOrder withRemaining(int newRemaining) {
        return new PurchaseOrder(id, buyerUuid, supplierUuid, companyName, itemId, newRemaining);
    }
}
