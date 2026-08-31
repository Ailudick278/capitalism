package com.ailudick.capitalismmod.supply;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

/**
 * World-persisted B2B supply market: supplier listings and pending backorders.
 */
public final class SupplyMarketSavedData extends SavedData {
    private static final String ID = "capitalismmod_supply_market";

    private final List<SupplyOffer> offers = new ArrayList<>();
    private final List<PurchaseOrder> orders = new ArrayList<>();

    private record State(List<SupplyOffer> offers, List<PurchaseOrder> orders) {
        static final Codec<State> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                SupplyOffer.CODEC.listOf().fieldOf("offers").forGetter(State::offers),
                PurchaseOrder.CODEC.listOf().fieldOf("orders").forGetter(State::orders)
        ).apply(instance, State::new));
    }

    private SupplyMarketSavedData() {
    }

    public static SupplyMarketSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(SupplyMarketSavedData::new, SupplyMarketSavedData::load), ID);
    }

    // ---- offers ----

    public List<SupplyOffer> offers() {
        return offers;
    }

    public void addOffer(SupplyOffer offer) {
        offers.add(offer);
        setDirty();
    }

    public void removeOffer(String offerId) {
        offers.removeIf(offer -> offer.id().equals(offerId));
        setDirty();
    }

    public SupplyOffer findOffer(String offerId) {
        for (SupplyOffer offer : offers) {
            if (offer.id().equals(offerId)) {
                return offer;
            }
        }
        return null;
    }

    public void replaceOffer(SupplyOffer offer) {
        for (int i = 0; i < offers.size(); i++) {
            if (offers.get(i).id().equals(offer.id())) {
                offers.set(i, offer);
                return;
            }
        }
    }

    // ---- orders ----

    public List<PurchaseOrder> orders() {
        return orders;
    }

    public void addOrder(PurchaseOrder order) {
        orders.add(order);
        setDirty();
    }

    public void removeOrder(String orderId) {
        orders.removeIf(order -> order.id().equals(orderId));
        setDirty();
    }

    public void replaceOrder(PurchaseOrder order) {
        for (int i = 0; i < orders.size(); i++) {
            if (orders.get(i).id().equals(order.id())) {
                orders.set(i, order);
                return;
            }
        }
    }

    // ---- persistence ----

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        State state = new State(new ArrayList<>(offers), new ArrayList<>(orders));
        State.CODEC.encodeStart(NbtOps.INSTANCE, state).result()
                .ifPresent(encoded -> tag.put("data", encoded));
        return tag;
    }

    public static SupplyMarketSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        SupplyMarketSavedData data = new SupplyMarketSavedData();
        if (tag.contains("data")) {
            State.CODEC.parse(NbtOps.INSTANCE, tag.get("data")).result().ifPresent(state -> {
                data.offers.addAll(state.offers());
                data.orders.addAll(state.orders());
            });
        }
        return data;
    }
}
