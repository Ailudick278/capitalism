package com.ailudick.capitalismmod.auction;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.UUID;

/**
 * A single auction listing.
 *
 * @param currentBidder empty string means no bid yet
 * @param endTick       absolute tick when the auction closes
 */
public record Auction(String id, UUID seller, String itemId, int quantity, long startingPrice, long currentBid, String currentBidder, long endTick) {

    private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);

    public static final Codec<Auction> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(Auction::id),
            UUID_CODEC.fieldOf("seller").forGetter(Auction::seller),
            Codec.STRING.fieldOf("itemId").forGetter(Auction::itemId),
            Codec.INT.fieldOf("quantity").forGetter(Auction::quantity),
            Codec.LONG.fieldOf("startingPrice").forGetter(Auction::startingPrice),
            Codec.LONG.fieldOf("currentBid").forGetter(Auction::currentBid),
            Codec.STRING.fieldOf("currentBidder").forGetter(Auction::currentBidder),
            Codec.LONG.fieldOf("endTick").forGetter(Auction::endTick)
    ).apply(instance, Auction::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, Auction> STREAM_CODEC = StreamCodec.of(
            (buf, a) -> {
                ByteBufCodecs.STRING_UTF8.encode(buf, a.id());
                ByteBufCodecs.STRING_UTF8.encode(buf, a.seller().toString());
                ByteBufCodecs.STRING_UTF8.encode(buf, a.itemId());
                ByteBufCodecs.VAR_INT.encode(buf, a.quantity());
                ByteBufCodecs.VAR_LONG.encode(buf, a.startingPrice());
                ByteBufCodecs.VAR_LONG.encode(buf, a.currentBid());
                ByteBufCodecs.STRING_UTF8.encode(buf, a.currentBidder());
                ByteBufCodecs.VAR_LONG.encode(buf, a.endTick());
            },
            buf -> new Auction(
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    UUID.fromString(ByteBufCodecs.STRING_UTF8.decode(buf)),
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_LONG.decode(buf),
                    ByteBufCodecs.VAR_LONG.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    ByteBufCodecs.VAR_LONG.decode(buf)
            )
    );

    public Auction withBid(long bid, String bidder) {
        return new Auction(id, seller, itemId, quantity, startingPrice, bid, bidder, endTick);
    }
}
