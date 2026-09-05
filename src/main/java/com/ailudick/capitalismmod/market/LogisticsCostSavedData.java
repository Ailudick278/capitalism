package com.ailudick.capitalismmod.market;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Persistent planning ledger for the fuel cost of cross-region shipments. */
public final class LogisticsCostSavedData extends SavedData {
    private static final String ID = "capitalismmod_logistics_costs";
    private static final int MAX_RECORDS = 4096;
    private final List<FuelPlan> plans = new ArrayList<>();

    public record FuelPlan(String shipmentId, UUID buyer, String itemId, int quantity,
                           String originRegion, String destinationRegion, TransportMode transport,
                           String fuelItemId, int fuelUnits, long fuelUnitPrice,
                           long estimatedCost, long createdAt) {
        private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);
        private static final Codec<FuelPlan> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("shipmentId").forGetter(FuelPlan::shipmentId),
                UUID_CODEC.fieldOf("buyer").forGetter(FuelPlan::buyer),
                Codec.STRING.fieldOf("itemId").forGetter(FuelPlan::itemId),
                Codec.INT.fieldOf("quantity").forGetter(FuelPlan::quantity),
                Codec.STRING.fieldOf("originRegion").forGetter(FuelPlan::originRegion),
                Codec.STRING.fieldOf("destinationRegion").forGetter(FuelPlan::destinationRegion),
                Codec.STRING.fieldOf("transport").forGetter(plan -> plan.transport().id()),
                Codec.STRING.fieldOf("fuelItemId").forGetter(FuelPlan::fuelItemId),
                Codec.INT.fieldOf("fuelUnits").forGetter(FuelPlan::fuelUnits),
                Codec.LONG.fieldOf("fuelUnitPrice").forGetter(FuelPlan::fuelUnitPrice),
                Codec.LONG.fieldOf("estimatedCost").forGetter(FuelPlan::estimatedCost),
                Codec.LONG.fieldOf("createdAt").forGetter(FuelPlan::createdAt)
        ).apply(instance, (shipmentId, buyer, itemId, quantity, origin, destination, transport,
                           fuelItem, units, unitPrice, cost, createdAt) -> new FuelPlan(
                shipmentId, buyer, itemId, quantity, origin, destination,
                TransportMode.parse(transport), fuelItem, units, unitPrice, cost, createdAt)));
    }

    private record State(List<FuelPlan> plans) {
        private static final Codec<State> CODEC = FuelPlan.CODEC.listOf().xmap(State::new, State::plans);
    }

    private LogisticsCostSavedData() {}

    public static LogisticsCostSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(LogisticsCostSavedData::new, LogisticsCostSavedData::load), ID);
    }

    public void record(FuelPlan plan) {
        if (plan == null || plan.shipmentId() == null || plan.shipmentId().isBlank()
                || plan.buyer() == null || plan.quantity() <= 0 || plan.fuelUnits() <= 0) return;
        plans.add(plan);
        while (plans.size() > MAX_RECORDS) plans.remove(0);
        setDirty();
    }

    public List<FuelPlan> forBuyer(UUID buyer) {
        if (buyer == null) return List.of();
        return plans.stream().filter(plan -> buyer.equals(plan.buyer())).toList();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        State.CODEC.encodeStart(NbtOps.INSTANCE, new State(plans)).result()
                .ifPresent(encoded -> tag.put("data", encoded));
        return tag;
    }

    public static LogisticsCostSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        LogisticsCostSavedData data = new LogisticsCostSavedData();
        if (tag.contains("data")) {
            State.CODEC.parse(NbtOps.INSTANCE, tag.get("data")).result()
                    .ifPresent(state -> data.plans.addAll(state.plans()));
        }
        while (data.plans.size() > MAX_RECORDS) data.plans.remove(0);
        return data;
    }
}
