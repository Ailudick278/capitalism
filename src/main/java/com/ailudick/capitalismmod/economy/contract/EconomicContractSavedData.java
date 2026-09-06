package com.ailudick.capitalismmod.economy.contract;

import com.ailudick.capitalismmod.economy.expansion.EconomicActorRef;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

/** Persistent generic contract index. Detailed settlement stays in each domain service. */
public final class EconomicContractSavedData extends SavedData {
    private static final String ID = "capitalismmod_economic_contracts";
    private static final int MAX_CONTRACTS = 4096;
    private final List<EconomicContract> contracts = new ArrayList<>();

    private EconomicContractSavedData() {}

    public static EconomicContractSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(EconomicContractSavedData::new, EconomicContractSavedData::load), ID);
    }

    public List<EconomicContract> contracts() { return List.copyOf(contracts); }

    public EconomicContract find(String id) {
        if (id == null) return null;
        return contracts.stream().filter(contract -> id.equals(contract.id())).findFirst().orElse(null);
    }

    public boolean add(EconomicContract contract) {
        if (contract == null || find(contract.id()) != null) return false;
        contracts.add(contract);
        trim();
        setDirty();
        return true;
    }

    public boolean replace(EconomicContract contract) {
        if (contract == null) return false;
        for (int i = 0; i < contracts.size(); i++) {
            if (contracts.get(i).id().equals(contract.id())) {
                contracts.set(i, contract);
                setDirty();
                return true;
            }
        }
        return false;
    }

    public boolean transition(String id, ContractStatus next, long at) {
        EconomicContract current = find(id);
        if (current == null || !ContractEconomics.canTransition(current.status(), next)) return false;
        return replace(current.withStatus(next));
    }

    public boolean fulfill(String id, long quantity) {
        EconomicContract current = find(id);
        if (current == null || quantity <= 0L || current.status() != ContractStatus.ACTIVE) return false;
        return replace(current.fulfill(quantity));
    }

    public boolean breach(String id, long amountMinor) {
        EconomicContract current = find(id);
        if (current == null) return false;
        EconomicContract breached = current.breach(amountMinor);
        if (breached == current) return false;
        return replace(breached);
    }

    public boolean openDispute(MinecraftServer server, String id, String disputeId, long at, String reason) {
        EconomicContract current = find(id);
        if (current == null || (current.status() != ContractStatus.ACTIVE && current.status() != ContractStatus.OFFERED)) return false;
        if (!ContractDisputeSavedData.get(server).openOnce(disputeId, id, at, reason)) return false;
        return transition(id, ContractStatus.DISPUTED, at);
    }

    public int expire(long now) {
        int changed = 0;
        for (int i = 0; i < contracts.size(); i++) {
            EconomicContract contract = contracts.get(i);
            if ((contract.status() == ContractStatus.OFFERED || contract.status() == ContractStatus.ACTIVE)
                    && contract.endsAt() > 0L && now >= contract.endsAt()) {
                contracts.set(i, contract.withStatus(ContractStatus.EXPIRED));
                changed++;
            }
        }
        if (changed > 0) setDirty();
        return changed;
    }

    /** Applies the default overdue rule: unaccepted offers expire, active contracts breach. */
    public int settleOverdue(long now) {
        int changed = 0;
        for (EconomicContract contract : List.copyOf(contracts)) {
            // Trade orders own their deadline, refund and inventory semantics;
            // their domain service must emit the terminal contract event.
            if (contract.type() == ContractType.TRADE) continue;
            if (contract.endsAt() <= 0L || now <= contract.endsAt()) continue;
            if (contract.status() == ContractStatus.OFFERED && transition(contract.id(), ContractStatus.EXPIRED, now)) {
                changed++;
            } else if (contract.status() == ContractStatus.ACTIVE
                    && breach(contract.id(), contract.agreedAmountMinor())) {
                changed++;
            }
        }
        return changed;
    }

    private void trim() { while (contracts.size() > MAX_CONTRACTS) contracts.remove(0); }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (EconomicContract contract : contracts) {
            CompoundTag e = new CompoundTag();
            e.putString("id", contract.id()); e.putString("type", contract.type().name());
            putActor(e, "proposer", contract.proposer()); putActor(e, "counterparty", contract.counterparty());
            e.putLong("createdAt", contract.createdAt()); e.putLong("startsAt", contract.startsAt());
            e.putLong("endsAt", contract.endsAt()); e.putLong("amount", contract.agreedAmountMinor());
            e.putString("currency", contract.currencyId()); e.putString("status", contract.status().name());
            e.putLong("fulfilled", contract.fulfilledQuantity()); e.putLong("agreedQuantity", contract.agreedQuantity());
            e.putLong("breach", contract.breachAmountMinor());
            list.add(e);
        }
        tag.put("contracts", list); return tag;
    }

    private static void putActor(CompoundTag parent, String key, EconomicActorRef actor) {
        CompoundTag e = new CompoundTag(); e.putString("type", actor.type()); e.putString("id", actor.id()); parent.put(key, e);
    }

    private static EconomicActorRef actor(CompoundTag parent, String key) {
        if (!parent.contains(key, Tag.TAG_COMPOUND)) return null;
        CompoundTag e = parent.getCompound(key);
        try { return new EconomicActorRef(e.getString("type"), e.getString("id")); }
        catch (IllegalArgumentException ignored) { return null; }
    }

    public static EconomicContractSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        EconomicContractSavedData data = new EconomicContractSavedData();
        ListTag list = tag.getList("contracts", Tag.TAG_COMPOUND);
        for (int i = Math.max(0, list.size() - MAX_CONTRACTS); i < list.size(); i++) {
            CompoundTag e = list.getCompound(i);
            try {
                EconomicActorRef proposer = actor(e, "proposer"), counterparty = actor(e, "counterparty");
                if (proposer == null || counterparty == null || e.getString("id").isBlank()) continue;
                ContractType type = ContractType.valueOf(e.getString("type"));
                ContractStatus status = ContractStatus.valueOf(e.getString("status"));
                data.contracts.add(new EconomicContract(e.getString("id"), type, proposer, counterparty,
                        e.getLong("createdAt"), e.getLong("startsAt"), e.getLong("endsAt"),
                        Math.max(0L, e.getLong("amount")), e.getString("currency"), status,
                        Math.max(0L, e.getLong("fulfilled")), Math.max(0L, e.getLong("agreedQuantity")),
                        Math.max(0L, e.getLong("breach"))));
            } catch (IllegalArgumentException ignored) { }
        }
        data.trim(); return data;
    }
}
