package com.ailudick.capitalismmod.business;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.UUID;

/** A sales contract between a sole proprietor and a system/NPC customer. */
public record BusinessOrder(String id, String businessId, UUID sellerUuid, String itemId, int quantity,
                             int remaining, long unitPrice, long createdTick, long deadline, String status) {
    private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);

    public static final Codec<BusinessOrder> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(BusinessOrder::id),
            Codec.STRING.fieldOf("businessId").forGetter(BusinessOrder::businessId),
            UUID_CODEC.fieldOf("sellerUuid").forGetter(BusinessOrder::sellerUuid),
            Codec.STRING.fieldOf("itemId").forGetter(BusinessOrder::itemId),
            Codec.INT.fieldOf("quantity").forGetter(BusinessOrder::quantity),
            Codec.INT.fieldOf("remaining").forGetter(BusinessOrder::remaining),
            Codec.LONG.fieldOf("unitPrice").forGetter(BusinessOrder::unitPrice),
            Codec.LONG.fieldOf("createdTick").forGetter(BusinessOrder::createdTick),
            Codec.LONG.fieldOf("deadline").forGetter(BusinessOrder::deadline),
            Codec.STRING.fieldOf("status").forGetter(BusinessOrder::status)
    ).apply(instance, BusinessOrder::new));

    public BusinessOrder withDelivery(int newRemaining, String newStatus) {
        return new BusinessOrder(id, businessId, sellerUuid, itemId, quantity, newRemaining, unitPrice,
                createdTick, deadline, newStatus);
    }

    public BusinessOrder withStatus(String newStatus) {
        return new BusinessOrder(id, businessId, sellerUuid, itemId, quantity, remaining, unitPrice,
                createdTick, deadline, newStatus);
    }
}
