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
import java.util.UUID;

/** Persistent freight offers and carrier acceptances for company shipments. */
public final class CompanyFreightContractSavedData extends SavedData {
    private static final String ID = "capitalismmod_company_freight_contracts";
    private static final int MAX_CONTRACTS = 4096;
    private final List<Contract> contracts = new ArrayList<>();

    public record Contract(String id, String shipmentId, String buyerCompanyId,
                           String carrierCompanyId, long quotedCost, long createdAt,
                           long acceptedAt, String status) {
        private static final Codec<Contract> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("id").forGetter(Contract::id),
                Codec.STRING.fieldOf("shipmentId").forGetter(Contract::shipmentId),
                Codec.STRING.fieldOf("buyerCompanyId").forGetter(Contract::buyerCompanyId),
                Codec.STRING.fieldOf("carrierCompanyId").forGetter(Contract::carrierCompanyId),
                Codec.LONG.fieldOf("quotedCost").forGetter(Contract::quotedCost),
                Codec.LONG.fieldOf("createdAt").forGetter(Contract::createdAt),
                Codec.LONG.optionalFieldOf("acceptedAt", 0L).forGetter(Contract::acceptedAt),
                Codec.STRING.optionalFieldOf("status", "offered").forGetter(Contract::status)
        ).apply(instance, Contract::new));

        public Contract {
            id = id == null ? "" : id;
            shipmentId = shipmentId == null ? "" : shipmentId;
            buyerCompanyId = buyerCompanyId == null ? "" : buyerCompanyId;
            carrierCompanyId = carrierCompanyId == null ? "" : carrierCompanyId;
            quotedCost = Math.max(0L, quotedCost);
            createdAt = Math.max(0L, createdAt);
            acceptedAt = Math.max(0L, acceptedAt);
            status = status == null || status.isBlank() ? "offered" : status;
        }
    }

    private record State(List<Contract> contracts) {
        private static final Codec<State> CODEC = Contract.CODEC.listOf().xmap(State::new, State::contracts);
    }

    private CompanyFreightContractSavedData() {
    }

    public static CompanyFreightContractSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(CompanyFreightContractSavedData::new, CompanyFreightContractSavedData::load), ID);
    }

    public Contract find(String id) {
        if (id == null || id.isBlank()) return null;
        for (int i = contracts.size() - 1; i >= 0; i--) {
            if (id.equals(contracts.get(i).id())) return contracts.get(i);
        }
        return null;
    }

    public Contract activeForShipment(String shipmentId) {
        if (shipmentId == null || shipmentId.isBlank()) return null;
        for (int i = contracts.size() - 1; i >= 0; i--) {
            Contract contract = contracts.get(i);
            if (shipmentId.equals(contract.shipmentId()) && !"cancelled".equals(contract.status())
                    && !"settled".equals(contract.status())) return contract;
        }
        return null;
    }

    public List<Contract> forCompany(String companyId) {
        if (companyId == null || companyId.isBlank()) return List.of();
        return contracts.stream().filter(contract -> companyId.equals(contract.buyerCompanyId())
                || companyId.equals(contract.carrierCompanyId())).toList();
    }

    public Contract offer(String shipmentId, String buyerCompanyId, String carrierCompanyId,
                          long quotedCost, long createdAt) {
        if (shipmentId == null || shipmentId.isBlank() || buyerCompanyId == null || buyerCompanyId.isBlank()
                || carrierCompanyId == null || carrierCompanyId.isBlank() || quotedCost <= 0L
                || activeForShipment(shipmentId) != null) return null;
        Contract contract = new Contract(UUID.randomUUID().toString().substring(0, 12), shipmentId,
                buyerCompanyId, carrierCompanyId, quotedCost, createdAt, 0L, "offered");
        contracts.add(contract);
        while (contracts.size() > MAX_CONTRACTS) contracts.remove(0);
        setDirty();
        return contract;
    }

    public boolean accept(String id, long acceptedAt) {
        for (int i = 0; i < contracts.size(); i++) {
            Contract contract = contracts.get(i);
            if (id != null && id.equals(contract.id()) && "offered".equals(contract.status())) {
                contracts.set(i, new Contract(contract.id(), contract.shipmentId(), contract.buyerCompanyId(),
                        contract.carrierCompanyId(), contract.quotedCost(), contract.createdAt(),
                        Math.max(0L, acceptedAt), "accepted"));
                setDirty();
                return true;
            }
        }
        return false;
    }

    public boolean settle(String id, long settledAt) {
        for (int i = 0; i < contracts.size(); i++) {
            Contract contract = contracts.get(i);
            if (id != null && id.equals(contract.id()) && "accepted".equals(contract.status())) {
                contracts.set(i, new Contract(contract.id(), contract.shipmentId(), contract.buyerCompanyId(),
                        contract.carrierCompanyId(), contract.quotedCost(), contract.createdAt(),
                        contract.acceptedAt(), "settled"));
                setDirty();
                return true;
            }
        }
        return false;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        State.CODEC.encodeStart(NbtOps.INSTANCE, new State(List.copyOf(contracts))).result()
                .ifPresent(encoded -> tag.put("data", encoded));
        return tag;
    }

    public static CompanyFreightContractSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        CompanyFreightContractSavedData data = new CompanyFreightContractSavedData();
        if (tag.contains("data")) {
            State.CODEC.parse(NbtOps.INSTANCE, tag.get("data")).result().ifPresent(state -> {
                data.contracts.addAll(state.contracts());
                while (data.contracts.size() > MAX_CONTRACTS) data.contracts.remove(0);
            });
        }
        return data;
    }
}
