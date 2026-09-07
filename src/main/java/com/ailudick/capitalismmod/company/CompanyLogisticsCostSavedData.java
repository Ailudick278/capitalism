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
                                  int quantity, long estimatedCost, long appliedAt, boolean settled,
                                  String carrierCompanyId, long settledAt) {
        public CapitalizedCost(String shipmentId, String companyId, String itemId,
                               int quantity, long estimatedCost, long appliedAt) {
            this(shipmentId, companyId, itemId, quantity, estimatedCost, appliedAt, false, "", 0L);
        }

        public CapitalizedCost(String shipmentId, String companyId, String itemId,
                               int quantity, long estimatedCost, long appliedAt, boolean settled) {
            this(shipmentId, companyId, itemId, quantity, estimatedCost, appliedAt, settled, "", 0L);
        }

        private static final Codec<CapitalizedCost> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("shipmentId").forGetter(CapitalizedCost::shipmentId),
                Codec.STRING.fieldOf("companyId").forGetter(CapitalizedCost::companyId),
                Codec.STRING.fieldOf("itemId").forGetter(CapitalizedCost::itemId),
                Codec.INT.fieldOf("quantity").forGetter(CapitalizedCost::quantity),
                Codec.LONG.fieldOf("estimatedCost").forGetter(CapitalizedCost::estimatedCost),
                Codec.LONG.fieldOf("appliedAt").forGetter(CapitalizedCost::appliedAt),
                Codec.BOOL.optionalFieldOf("settled", false).forGetter(CapitalizedCost::settled),
                Codec.STRING.optionalFieldOf("carrierCompanyId", "").forGetter(CapitalizedCost::carrierCompanyId),
                Codec.LONG.optionalFieldOf("settledAt", 0L).forGetter(CapitalizedCost::settledAt)
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

    public List<CapitalizedCost> costs() {
        return List.copyOf(costs);
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

    /** Marks one accrued freight payable as settled by a named carrier company. */
    public boolean settle(String shipmentId, String carrierCompanyId, long settledAt) {
        if (shipmentId == null || shipmentId.isBlank() || carrierCompanyId == null || carrierCompanyId.isBlank()) {
            return false;
        }
        for (int i = 0; i < costs.size(); i++) {
            CapitalizedCost cost = costs.get(i);
            if (shipmentId.equals(cost.shipmentId())) {
                if (cost.settled()) return false;
                costs.set(i, new CapitalizedCost(cost.shipmentId(), cost.companyId(), cost.itemId(),
                        cost.quantity(), cost.estimatedCost(), cost.appliedAt(), true,
                        carrierCompanyId, Math.max(0L, settledAt)));
                setDirty();
                return true;
            }
        }
        return false;
    }

    /** Rebinds the inventory owner and carrier references during a merger. */
    public void transferCompany(String sourceId, String targetId) {
        if (sourceId == null || targetId == null || sourceId.isBlank()
                || targetId.isBlank() || sourceId.equals(targetId)) return;
        boolean changed = false;
        for (int i = 0; i < costs.size(); i++) {
            CapitalizedCost cost = costs.get(i);
            String owner = sourceId.equals(cost.companyId()) ? targetId : cost.companyId();
            String carrier = sourceId.equals(cost.carrierCompanyId()) ? targetId : cost.carrierCompanyId();
            if (!owner.equals(cost.companyId()) || !carrier.equals(cost.carrierCompanyId())) {
                costs.set(i, new CapitalizedCost(cost.shipmentId(), owner, cost.itemId(), cost.quantity(),
                        cost.estimatedCost(), cost.appliedAt(), cost.settled(), carrier, cost.settledAt()));
                changed = true;
            }
        }
        if (changed) setDirty();
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
