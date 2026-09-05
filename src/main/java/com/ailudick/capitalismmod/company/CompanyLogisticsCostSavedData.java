package com.ailudick.capitalismmod.company;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

/** Audit trail for estimated inbound freight capitalized into company inventory. */
public final class CompanyLogisticsCostSavedData extends SavedData {
    private static final String ID = "capitalismmod_company_logistics_costs";
    private static final int MAX_RECORDS = 4096;
    private final List<CapitalizedCost> costs = new ArrayList<>();

    public record CapitalizedCost(String shipmentId, String companyId, String itemId,
                                  int quantity, long estimatedCost, long appliedAt, boolean settled) {
        public CapitalizedCost(String shipmentId, String companyId, String itemId,
                               int quantity, long estimatedCost, long appliedAt) {
            this(shipmentId, companyId, itemId, quantity, estimatedCost, appliedAt, false);
        }

        private static final Codec<CapitalizedCost> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("shipmentId").forGetter(CapitalizedCost::shipmentId),
                Codec.STRING.fieldOf("companyId").forGetter(CapitalizedCost::companyId),
                Codec.STRING.fieldOf("itemId").forGetter(CapitalizedCost::itemId),
                Codec.INT.fieldOf("quantity").forGetter(CapitalizedCost::quantity),
                Codec.LONG.fieldOf("estimatedCost").forGetter(CapitalizedCost::estimatedCost),
                Codec.LONG.fieldOf("appliedAt").forGetter(CapitalizedCost::appliedAt),
                Codec.BOOL.optionalFieldOf("settled", false).forGetter(CapitalizedCost::settled)
        ).apply(instance, CapitalizedCost::new));
    }

    private record State(List<CapitalizedCost> costs) {
        private static final Codec<State> CODEC = CapitalizedCost.CODEC.listOf().xmap(State::new, State::costs);
    }

    private CompanyLogisticsCostSavedData() {
    }

    public static CompanyLogisticsCostSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(CompanyLogisticsCostSavedData::new, CompanyLogisticsCostSavedData::load), ID);
    }

    public boolean hasShipment(String shipmentId) {
        return shipmentId != null && costs.stream().anyMatch(cost -> shipmentId.equals(cost.shipmentId()));
    }

    public void record(CapitalizedCost cost) {
        if (cost == null || cost.shipmentId() == null || cost.shipmentId().isBlank()
                || cost.companyId() == null || cost.companyId().isBlank()
                || cost.quantity() <= 0 || cost.estimatedCost() < 0L || hasShipment(cost.shipmentId())) return;
        costs.add(cost);
        while (costs.size() > MAX_RECORDS) costs.remove(0);
        setDirty();
    }

    public List<CapitalizedCost> forCompany(String companyId) {
        if (companyId == null || companyId.isBlank()) return List.of();
        return costs.stream().filter(cost -> companyId.equals(cost.companyId())).toList();
    }

    public long outstandingCost(String companyId) {
        long total = 0L;
        for (CapitalizedCost cost : forCompany(companyId)) {
            if (!cost.settled()) {
                try {
                    total = Math.addExact(total, cost.estimatedCost());
                } catch (ArithmeticException e) {
                    return Long.MAX_VALUE;
                }
            }
        }
        return total;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        State.CODEC.encodeStart(NbtOps.INSTANCE, new State(List.copyOf(costs))).result()
                .ifPresent(encoded -> tag.put("data", encoded));
        return tag;
    }

    public static CompanyLogisticsCostSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        CompanyLogisticsCostSavedData data = new CompanyLogisticsCostSavedData();
        if (tag.contains("data")) {
            State.CODEC.parse(NbtOps.INSTANCE, tag.get("data")).result().ifPresent(state -> {
                data.costs.addAll(state.costs());
                while (data.costs.size() > MAX_RECORDS) data.costs.remove(0);
            });
        }
        return data;
    }
}
