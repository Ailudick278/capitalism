package com.ailudick.capitalismmod.supply;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.UUID;

/**
 * A supplier's listing: a company offering to sell one of its produced commodities at a fixed price.
 *
 * @param ownerUuid   the company owner's UUID
 * @param companyName the company name (unique per owner)
 */
public record SupplyOffer(String id, UUID ownerUuid, String companyName, String itemId, long price, String region,
                          int qualityScore) {

    public SupplyOffer(String id, UUID ownerUuid, String companyName, String itemId, long price) {
        this(id, ownerUuid, companyName, itemId, price, "unknown", 0);
    }

    public SupplyOffer(String id, UUID ownerUuid, String companyName, String itemId, long price, String region) {
        this(id, ownerUuid, companyName, itemId, price, region, 0);
    }

    public SupplyOffer {
        qualityScore = Math.max(0, Math.min(100, qualityScore));
    }

    private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);

    public static final Codec<SupplyOffer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(SupplyOffer::id),
            UUID_CODEC.fieldOf("ownerUuid").forGetter(SupplyOffer::ownerUuid),
            Codec.STRING.fieldOf("companyName").forGetter(SupplyOffer::companyName),
            Codec.STRING.fieldOf("itemId").forGetter(SupplyOffer::itemId),
            Codec.LONG.fieldOf("price").forGetter(SupplyOffer::price),
            Codec.STRING.optionalFieldOf("region", "unknown").forGetter(SupplyOffer::region),
            Codec.INT.optionalFieldOf("qualityScore", 0).forGetter(SupplyOffer::qualityScore)
    ).apply(instance, SupplyOffer::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SupplyOffer> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> {
                buf.writeUtf(value.id());
                buf.writeUtf(value.ownerUuid().toString());
                buf.writeUtf(value.companyName());
                buf.writeUtf(value.itemId());
                buf.writeVarLong(value.price());
                buf.writeUtf(value.region());
                buf.writeVarInt(value.qualityScore());
            },
            buf -> new SupplyOffer(buf.readUtf(), UUID.fromString(buf.readUtf()), buf.readUtf(), buf.readUtf(),
                    buf.readVarLong(), buf.readUtf(), buf.readVarInt()));
}
