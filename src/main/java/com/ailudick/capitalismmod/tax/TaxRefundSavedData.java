package com.ailudick.capitalismmod.tax;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.ArrayList;
import java.util.UUID;
import java.util.List;

public final class TaxRefundSavedData extends SavedData {
    private static final String ID = "capitalismmod_tax_refunds";
    private final List<Request> requests = new ArrayList<>();
    public record Request(String id, UUID taxpayerUuid, String currencyId, long amount, long requestedAt,
                          String status, long reviewedAt, String reviewer, String reason, String sourceSummary, String allocations,
                          List<TaxRefundAllocation> allocationDetails) {
        public Request(String id, UUID taxpayerUuid, String currencyId, long amount, long requestedAt,
                       String status, long reviewedAt, String reviewer, String reason) {
            this(id, taxpayerUuid, currencyId, amount, requestedAt, status, reviewedAt, reviewer, reason, "");
        }
        public Request(String id, UUID taxpayerUuid, String currencyId, long amount, long requestedAt,
                       String status, long reviewedAt, String reviewer, String reason, String sourceSummary) {
            this(id, taxpayerUuid, currencyId, amount, requestedAt, status, reviewedAt, reviewer, reason, sourceSummary, "");
        }
        public Request(String id, UUID taxpayerUuid, String currencyId, long amount, long requestedAt,
                       String status, long reviewedAt, String reviewer, String reason, String sourceSummary, String allocations) {
            this(id, taxpayerUuid, currencyId, amount, requestedAt, status, reviewedAt, reviewer, reason, sourceSummary, allocations, List.of());
        }
    }
    private TaxRefundSavedData() {}
    public static TaxRefundSavedData get(MinecraftServer server) { return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(TaxRefundSavedData::new, TaxRefundSavedData::load), ID); }
    public List<Request> all() { return List.copyOf(requests); }
    public List<Request> pendingFor(UUID uuid) { return requests.stream().filter(r -> r.taxpayerUuid().equals(uuid) && r.status().equals("PENDING")).toList(); }
    public long countSince(UUID uuid, long since) {
        return requests.stream().filter(r -> r.taxpayerUuid().equals(uuid) && r.requestedAt() >= since).count();
    }
    public Request get(String id) { return requests.stream().filter(r -> r.id().equals(id)).findFirst().orElse(null); }
    public boolean add(Request request) {
        if ((request.amount() <= 0L && !request.status().equals("REJECTED"))
                || (!request.status().equals("REJECTED") && !pendingFor(request.taxpayerUuid()).isEmpty())) return false;
        requests.add(request); setDirty(); return true;
    }
    public void replace(Request request) { for (int i = 0; i < requests.size(); i++) if (requests.get(i).id().equals(request.id())) { requests.set(i, request); setDirty(); return; } }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Request r : requests) { CompoundTag e = new CompoundTag(); e.putString("id", r.id()); e.putUUID("taxpayer", r.taxpayerUuid()); e.putString("currency", r.currencyId()); e.putLong("amount", r.amount()); e.putLong("requestedAt", r.requestedAt()); e.putString("status", r.status()); e.putLong("reviewedAt", r.reviewedAt()); e.putString("reviewer", r.reviewer()); e.putString("reason", r.reason()); e.putString("sourceSummary", r.sourceSummary()); e.putString("allocations", r.allocations()); ListTag details = new ListTag(); r.allocationDetails().forEach(a -> { CompoundTag d = new CompoundTag(); d.putString("source", a.sourceId()); d.putString("subjectType", a.subjectType()); d.putString("subjectId", a.subjectId()); d.putLong("periodStart", a.periodStart()); d.putLong("periodEnd", a.periodEnd()); d.putLong("originalCredit", a.originalCredit()); d.putLong("refundAmount", a.refundAmount()); details.add(d); }); e.put("allocationDetails", details); list.add(e); }
        tag.put("requests", list); return tag;
    }
    public static TaxRefundSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        TaxRefundSavedData data = new TaxRefundSavedData(); ListTag list = tag.getList("requests", 10);
        for (int i = 0; i < list.size(); i++) { CompoundTag e = list.getCompound(i); if (e.hasUUID("taxpayer")) { List<TaxRefundAllocation> details = new java.util.ArrayList<>(); ListTag detailList = e.getList("allocationDetails", 10); for (int j = 0; j < detailList.size(); j++) { CompoundTag d = detailList.getCompound(j); details.add(new TaxRefundAllocation(d.getString("source"), d.getString("subjectType"), d.getString("subjectId"), d.getLong("periodStart"), d.getLong("periodEnd"), d.getLong("originalCredit"), d.getLong("refundAmount"))); } data.requests.add(new Request(e.getString("id"), e.getUUID("taxpayer"), e.getString("currency"), e.getLong("amount"), e.getLong("requestedAt"), e.getString("status"), e.getLong("reviewedAt"), e.getString("reviewer"), e.getString("reason"), e.getString("sourceSummary"), e.getString("allocations"), details)); } }
        return data;
    }
}
