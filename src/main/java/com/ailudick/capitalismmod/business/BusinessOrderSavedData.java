package com.ailudick.capitalismmod.business;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

/** Server-level persistent order registry. */
public final class BusinessOrderSavedData extends SavedData {
    private static final String ID = "capitalismmod_business_orders";
    private final Map<String, BusinessOrder> orders = new HashMap<>();

    private record State(Map<String, BusinessOrder> orders) {
        private static final Codec<State> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.unboundedMap(Codec.STRING, BusinessOrder.CODEC).fieldOf("orders").forGetter(State::orders)
        ).apply(instance, State::new));
    }

    private BusinessOrderSavedData() {
    }

    public static BusinessOrderSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(BusinessOrderSavedData::new, BusinessOrderSavedData::load), ID);
    }

    public Map<String, BusinessOrder> orders() {
        return Map.copyOf(orders);
    }

    public BusinessOrder get(String id) {
        return orders.get(id);
    }

    public void put(BusinessOrder order) {
        orders.put(order.id(), order);
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        State.CODEC.encodeStart(NbtOps.INSTANCE, new State(orders)).result()
                .ifPresent(encoded -> tag.put("data", encoded));
        return tag;
    }

    public static BusinessOrderSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        BusinessOrderSavedData data = new BusinessOrderSavedData();
        if (tag.contains("data")) {
            State.CODEC.parse(NbtOps.INSTANCE, tag.get("data")).result()
                    .ifPresent(state -> data.orders.putAll(state.orders()));
        }
        return data;
    }
}
