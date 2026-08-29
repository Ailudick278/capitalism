package com.ailudick.capitalismmod.futures;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.UUID;

/**
 * A single futures position on a commodity contract.
 *
 * @param longSide   {@code true} = long (bet price rises), {@code false} = short (bet price falls)
 * @param entryPrice price at which the position was opened (or last marked), in USD
 * @param margin     margin frozen for this position, in USD
 */
public record Position(String id, UUID playerId, String itemId, int quantity, long entryPrice, long margin, boolean longSide) {

    private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);

    public static final Codec<Position> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(Position::id),
            UUID_CODEC.fieldOf("playerId").forGetter(Position::playerId),
            Codec.STRING.fieldOf("itemId").forGetter(Position::itemId),
            Codec.INT.fieldOf("quantity").forGetter(Position::quantity),
            Codec.LONG.fieldOf("entryPrice").forGetter(Position::entryPrice),
            Codec.LONG.fieldOf("margin").forGetter(Position::margin),
            Codec.BOOL.fieldOf("longSide").forGetter(Position::longSide)
    ).apply(instance, Position::new));

    // Manual StreamCodec: StreamCodec.composite has no 7-field overload.
    public static final StreamCodec<RegistryFriendlyByteBuf, Position> STREAM_CODEC = StreamCodec.of(
            (buf, pos) -> {
                ByteBufCodecs.STRING_UTF8.encode(buf, pos.id());
                ByteBufCodecs.STRING_UTF8.encode(buf, pos.playerId().toString());
                ByteBufCodecs.STRING_UTF8.encode(buf, pos.itemId());
                ByteBufCodecs.VAR_INT.encode(buf, pos.quantity());
                ByteBufCodecs.VAR_LONG.encode(buf, pos.entryPrice());
                ByteBufCodecs.VAR_LONG.encode(buf, pos.margin());
                ByteBufCodecs.BOOL.encode(buf, pos.longSide());
            },
            buf -> new Position(
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    UUID.fromString(ByteBufCodecs.STRING_UTF8.decode(buf)),
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_LONG.decode(buf),
                    ByteBufCodecs.VAR_LONG.decode(buf),
                    ByteBufCodecs.BOOL.decode(buf)
            )
    );

    public Position withEntryPrice(long newEntryPrice) {
        return new Position(id, playerId, itemId, quantity, newEntryPrice, margin, longSide);
    }
}
