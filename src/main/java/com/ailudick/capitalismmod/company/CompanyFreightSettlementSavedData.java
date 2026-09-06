package com.ailudick.capitalismmod.company;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

/** Persistent phase ledger for company-to-company freight settlement retries. */
public final class CompanyFreightSettlementSavedData extends SavedData {
    private static final String ID = "capitalismmod_company_freight_settlements";
    private final Map<String, Settlement> settlements = new HashMap<>();

    public record Settlement(String shipmentId, String buyerCompanyId, String carrierCompanyId,
                             long amount, boolean buyerDebited, boolean carrierCredited,
                             boolean payableClosed) {
        private static final Codec<Settlement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("shipmentId").forGetter(Settlement::shipmentId),
                Codec.STRING.fieldOf("buyerCompanyId").forGetter(Settlement::buyerCompanyId),
                Codec.STRING.fieldOf("carrierCompanyId").forGetter(Settlement::carrierCompanyId),
                Codec.LONG.fieldOf("amount").forGetter(Settlement::amount),
                Codec.BOOL.optionalFieldOf("buyerDebited", false).forGetter(Settlement::buyerDebited),
                Codec.BOOL.optionalFieldOf("carrierCredited", false).forGetter(Settlement::carrierCredited),
                Codec.BOOL.optionalFieldOf("payableClosed", false).forGetter(Settlement::payableClosed)
        ).apply(instance, Settlement::new));

        public Settlement {
            shipmentId = shipmentId == null ? "" : shipmentId;
            buyerCompanyId = buyerCompanyId == null ? "" : buyerCompanyId;
            carrierCompanyId = carrierCompanyId == null ? "" : carrierCompanyId;
            amount = Math.max(0L, amount);
        }

        public Settlement withBuyerDebited(boolean value) {
            return new Settlement(shipmentId, buyerCompanyId, carrierCompanyId, amount, value,
                    carrierCredited, payableClosed);
        }

        public Settlement withCarrierCredited(boolean value) {
            return new Settlement(shipmentId, buyerCompanyId, carrierCompanyId, amount,
                    buyerDebited, value, payableClosed);
        }

        public Settlement withPayableClosed(boolean value) {
            return new Settlement(shipmentId, buyerCompanyId, carrierCompanyId, amount,
                    buyerDebited, carrierCredited, value);
        }
    }

    private record State(Map<String, Settlement> settlements) {
        private static final Codec<State> CODEC = Codec.unboundedMap(Codec.STRING, Settlement.CODEC)
                .xmap(State::new, State::settlements);
    }

    private CompanyFreightSettlementSavedData() {}

    public static CompanyFreightSettlementSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(CompanyFreightSettlementSavedData::new,
                        CompanyFreightSettlementSavedData::load), ID);
    }

    public Settlement find(String shipmentId) {
        return shipmentId == null || shipmentId.isBlank() ? null : settlements.get(shipmentId);
    }

    public Settlement begin(String shipmentId, String buyerCompanyId, String carrierCompanyId, long amount) {
        Settlement existing = find(shipmentId);
        if (existing != null) return existing;
        if (shipmentId == null || shipmentId.isBlank() || buyerCompanyId == null || buyerCompanyId.isBlank()
                || carrierCompanyId == null || carrierCompanyId.isBlank() || amount <= 0L) return null;
        Settlement created = new Settlement(shipmentId, buyerCompanyId, carrierCompanyId, amount,
                false, false, false);
        settlements.put(shipmentId, created);
        setDirty();
        return created;
    }

    public Settlement update(Settlement settlement) {
        if (settlement == null || settlement.shipmentId().isBlank()) return null;
        settlements.put(settlement.shipmentId(), settlement);
        setDirty();
        return settlement;
    }

    public void remove(String shipmentId) {
        if (shipmentId != null && settlements.remove(shipmentId) != null) setDirty();
    }

    public void transferCompany(String sourceId, String targetId) {
        if (sourceId == null || targetId == null || sourceId.isBlank() || targetId.isBlank()
                || sourceId.equals(targetId)) return;
        boolean changed = false;
        for (Map.Entry<String, Settlement> entry : new HashMap<>(settlements).entrySet()) {
            Settlement settlement = entry.getValue();
            String buyer = sourceId.equals(settlement.buyerCompanyId()) ? targetId : settlement.buyerCompanyId();
            String carrier = sourceId.equals(settlement.carrierCompanyId()) ? targetId : settlement.carrierCompanyId();
            if (!buyer.equals(settlement.buyerCompanyId()) || !carrier.equals(settlement.carrierCompanyId())) {
                settlements.put(entry.getKey(), new Settlement(settlement.shipmentId(), buyer, carrier,
                        settlement.amount(), settlement.buyerDebited(), settlement.carrierCredited(),
                        settlement.payableClosed()));
                changed = true;
            }
        }
        if (changed) setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        State.CODEC.encodeStart(NbtOps.INSTANCE, new State(settlements)).result()
                .ifPresent(encoded -> tag.put("data", encoded));
        return tag;
    }

    public static CompanyFreightSettlementSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        CompanyFreightSettlementSavedData data = new CompanyFreightSettlementSavedData();
        if (tag.contains("data")) {
            State.CODEC.parse(NbtOps.INSTANCE, tag.get("data")).result()
                    .ifPresent(state -> data.settlements.putAll(state.settlements()));
        }
        return data;
    }
}
