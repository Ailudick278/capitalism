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
public record SupplyOffer(String id, UUID ownerUuid, String companyName, String itemId, long price) {

    private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);

    public static final Codec<SupplyOffer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(SupplyOffer::id),
            UUID_CODEC.fieldOf("ownerUuid").forGetter(SupplyOffer::ownerUuid),
            Codec.STRING.fieldOf("companyName").forGetter(SupplyOffer::companyName),
            Codec.STRING.fieldOf("itemId").forGetter(SupplyOffer::itemId),
            Codec.LONG.fieldOf("price").forGetter(SupplyOffer::price)
    ).apply(instance, SupplyOffer::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SupplyOffer> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SupplyOffer::id,
            ByteBufCodecs.STRING_UTF8, o -> o.ownerUuid().toString(),
            ByteBufCodecs.STRING_UTF8, SupplyOffer::companyName,
            ByteBufCodecs.STRING_UTF8, SupplyOffer::itemId,
            ByteBufCodecs.VAR_LONG, SupplyOffer::price,
            (id, owner, companyName, itemId, price) -> new SupplyOffer(id, UUID.fromString(owner), companyName, itemId, price));
}
