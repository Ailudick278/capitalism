package com.ailudick.capitalismmod.economy.contract;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

/** Persistent dispute filings and administrative resolutions for generic contracts. */
public final class ContractDisputeSavedData extends SavedData {
    private static final String ID = "capitalismmod_contract_disputes";
    private static final int MAX_DISPUTES = 8192;
    private final List<Dispute> disputes = new ArrayList<>();

    public record Dispute(String id, String contractId, long openedAt, String reason,
                          String status, long resolvedAt, String resolution) {}

    private ContractDisputeSavedData() {}

    public static ContractDisputeSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(ContractDisputeSavedData::new, ContractDisputeSavedData::load), ID);
    }

    public List<Dispute> disputes() { return List.copyOf(disputes); }
    public Dispute find(String id) { return disputes.stream().filter(d -> d.id().equals(id)).findFirst().orElse(null); }
    public Dispute activeForContract(String contractId) {
        return disputes.stream().filter(d -> d.contractId().equals(contractId) && "OPEN".equals(d.status()))
                .findFirst().orElse(null);
    }

    public boolean openOnce(String id, String contractId, long openedAt, String reason) {
        if (id == null || id.isBlank() || contractId == null || contractId.isBlank()
                || reason == null || reason.isBlank() || activeForContract(contractId) != null) return false;
        Dispute existing = find(id);
        if (existing != null) {
            return existing.contractId().equals(contractId) && existing.reason().equals(reason.trim());
        }
        disputes.add(new Dispute(id, contractId, Math.max(0L, openedAt), reason.trim(), "OPEN", 0L, ""));
        while (disputes.size() > MAX_DISPUTES) disputes.remove(0);
        setDirty(); return true;
    }

    public boolean resolve(String id, String status, long resolvedAt, String resolution) {
        Dispute current = find(id);
        if (current == null || !"OPEN".equals(current.status()) || !isResolutionStatus(status)
                || resolution == null || resolution.isBlank()) return false;
        disputes.set(disputes.indexOf(current), new Dispute(current.id(), current.contractId(), current.openedAt(),
                current.reason(), status, Math.max(0L, resolvedAt), resolution.trim()));
        setDirty(); return true;
    }

    private static boolean isResolutionStatus(String status) {
        return "COMPLETED".equals(status) || "CANCELLED".equals(status) || "BREACHED".equals(status);
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Dispute d : disputes) {
            CompoundTag n = new CompoundTag(); n.putString("id", d.id()); n.putString("contract", d.contractId());
            n.putLong("opened", d.openedAt()); n.putString("reason", d.reason()); n.putString("status", d.status());
            n.putLong("resolved", d.resolvedAt()); n.putString("resolution", d.resolution()); list.add(n);
        }
        tag.put("disputes", list); return tag;
    }

    public static ContractDisputeSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        ContractDisputeSavedData data = new ContractDisputeSavedData();
        ListTag list = tag.getList("disputes", Tag.TAG_COMPOUND);
        for (int i = Math.max(0, list.size() - MAX_DISPUTES); i < list.size(); i++) {
            CompoundTag n = list.getCompound(i);
            if (!n.getString("id").isBlank() && !n.getString("contract").isBlank()
                    && !n.getString("reason").isBlank() && !n.getString("status").isBlank()) {
                data.disputes.add(new Dispute(n.getString("id"), n.getString("contract"), n.getLong("opened"),
                        n.getString("reason"), n.getString("status"), n.getLong("resolved"), n.getString("resolution")));
            }
        }
        return data;
    }
}
