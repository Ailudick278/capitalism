package com.ailudick.capitalismmod.network.payload;

import com.ailudick.capitalismmod.CapitalismMod;
import com.ailudick.capitalismmod.bond.BondHolding;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Server -> Client: sync the player's own bond holdings.
 */
public record SyncBondsPayload(List<BondHolding> holdings) implements CustomPacketPayload {
    public static final Type<SyncBondsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CapitalismMod.MODID, "sync_bonds"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncBondsPayload> STREAM_CODEC = StreamCodec.composite(
            BondHolding.STREAM_CODEC.apply(ByteBufCodecs.list()), SyncBondsPayload::holdings,
            SyncBondsPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
