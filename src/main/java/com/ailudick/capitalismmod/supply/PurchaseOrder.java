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
                            String originRegion, String destinationRegion, long unitPrice, long createdAt,
                            String buyerCompanyId, int originalQuantity, long inputCreditMinor) {
    public PurchaseOrder(String id, UUID buyerUuid, UUID supplierUuid, String companyName, String itemId, int remaining,
                         String originRegion, String destinationRegion, long unitPrice, long createdAt) {
        this(id, buyerUuid, supplierUuid, companyName, itemId, remaining, originRegion, destinationRegion,
                unitPrice, createdAt, "", remaining, 0L);
    }

    /** Compatibility constructor for saves and callers that already track the buyer company. */
    public PurchaseOrder(String id, UUID buyerUuid, UUID supplierUuid, String companyName, String itemId, int remaining,
                         String originRegion, String destinationRegion, long unitPrice, long createdAt,
                         String buyerCompanyId) {
        this(id, buyerUuid, supplierUuid, companyName, itemId, remaining, originRegion, destinationRegion,
                unitPrice, createdAt, buyerCompanyId, remaining, 0L);
    }

    public PurchaseOrder(String id, UUID buyerUuid, UUID supplierUuid, String companyName, String itemId, int remaining) {
        this(id, buyerUuid, supplierUuid, companyName, itemId, remaining, "unknown", "unknown", 0L, 0L,
                "", remaining, 0L);
    }

    public PurchaseOrder(String id, UUID buyerUuid, UUID supplierUuid, String companyName, String itemId, int remaining,
                         String originRegion, String destinationRegion) {
        this(id, buyerUuid, supplierUuid, companyName, itemId, remaining, originRegion, destinationRegion, 0L, 0L,
                "", remaining, 0L);
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
            Codec.STRING.optionalFieldOf("destinationRegion", "unknown").forGetter(PurchaseOrder::destinationRegion),
            Codec.LONG.optionalFieldOf("unitPrice", 0L).forGetter(PurchaseOrder::unitPrice),
            Codec.LONG.optionalFieldOf("createdAt", 0L).forGetter(PurchaseOrder::createdAt),
            Codec.STRING.optionalFieldOf("buyerCompanyId", "").forGetter(PurchaseOrder::buyerCompanyId),
            Codec.INT.optionalFieldOf("originalQuantity", 0).forGetter(PurchaseOrder::originalQuantity),
            Codec.LONG.optionalFieldOf("inputCreditMinor", 0L).forGetter(PurchaseOrder::inputCreditMinor)
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
                ByteBufCodecs.VAR_LONG.encode(buffer, order.unitPrice());
                ByteBufCodecs.VAR_LONG.encode(buffer, order.createdAt());
                ByteBufCodecs.STRING_UTF8.encode(buffer, order.buyerCompanyId());
                ByteBufCodecs.VAR_INT.encode(buffer, order.originalQuantity());
                ByteBufCodecs.VAR_LONG.encode(buffer, order.inputCreditMinor());
            },
            buffer -> new PurchaseOrder(
                    ByteBufCodecs.STRING_UTF8.decode(buffer),
                    UUID.fromString(ByteBufCodecs.STRING_UTF8.decode(buffer)),
                    UUID.fromString(ByteBufCodecs.STRING_UTF8.decode(buffer)),
                    ByteBufCodecs.STRING_UTF8.decode(buffer),
                    ByteBufCodecs.STRING_UTF8.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.STRING_UTF8.decode(buffer),
                    ByteBufCodecs.STRING_UTF8.decode(buffer),
                    ByteBufCodecs.VAR_LONG.decode(buffer),
                ByteBufCodecs.VAR_LONG.decode(buffer),
                    ByteBufCodecs.STRING_UTF8.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.VAR_LONG.decode(buffer)));

    public PurchaseOrder withRemaining(int newRemaining) {
        return new PurchaseOrder(id, buyerUuid, supplierUuid, companyName, itemId, newRemaining,
                originRegion, destinationRegion, unitPrice, createdAt, buyerCompanyId,
                originalQuantity, inputCreditMinor);
    }

    public PurchaseOrder withOriginalQuantity(int quantity) {
        return new PurchaseOrder(id, buyerUuid, supplierUuid, companyName, itemId, remaining,
                originRegion, destinationRegion, unitPrice, createdAt, buyerCompanyId,
                Math.max(0, quantity), inputCreditMinor);
    }

    public PurchaseOrder withInputCreditMinor(long amount) {
        return new PurchaseOrder(id, buyerUuid, supplierUuid, companyName, itemId, remaining,
                originRegion, destinationRegion, unitPrice, createdAt, buyerCompanyId,
                originalQuantity, Math.max(0L, amount));
    }
}
