package com.ailudick.capitalismmod.tax;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Reviewable tax-correction applications. */
public final class TaxCorrectionRequestSavedData extends SavedData {
    private static final String ID = "capitalismmod_tax_correction_requests";
    public record Request(String id, String businessId, long periodEnd, long revenue, long expenses,
                          String reason, UUID applicant, long createdAt, String status,
                          String reviewer, long reviewedAt, String reviewReason) {}
    private final List<Request> requests = new ArrayList<>();
    private TaxCorrectionRequestSavedData() {}
    public static TaxCorrectionRequestSavedData get(MinecraftServer server) { return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(TaxCorrectionRequestSavedData::new, TaxCorrectionRequestSavedData::load), ID); }
    public List<Request> all() { return List.copyOf(requests); }
    public Request get(String id) { return requests.stream().filter(request -> request.id().equals(id)).findFirst().orElse(null); }
    public void add(Request request) { if (request != null) { requests.add(request); setDirty(); } }
    public void update(Request request) { for (int i = 0; i < requests.size(); i++) if (requests.get(i).id().equals(request.id())) { requests.set(i, request); setDirty(); return; } }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) { ListTag list = new ListTag(); for (Request r : requests) { CompoundTag v = new CompoundTag(); v.putString("id", r.id()); v.putString("business", r.businessId()); v.putLong("periodEnd", r.periodEnd()); v.putLong("revenue", r.revenue()); v.putLong("expenses", r.expenses()); v.putString("reason", r.reason()); v.putUUID("applicant", r.applicant()); v.putLong("createdAt", r.createdAt()); v.putString("status", r.status()); v.putString("reviewer", r.reviewer()); v.putLong("reviewedAt", r.reviewedAt()); v.putString("reviewReason", r.reviewReason()); list.add(v); } tag.put("requests", list); return tag; }
    public static TaxCorrectionRequestSavedData load(CompoundTag tag, HolderLookup.Provider registries) { TaxCorrectionRequestSavedData data = new TaxCorrectionRequestSavedData(); ListTag list = tag.getList("requests", 10); for (int i = 0; i < list.size(); i++) { CompoundTag v = list.getCompound(i); if (!v.hasUUID("applicant")) continue; data.requests.add(new Request(v.getString("id"), v.getString("business"), v.getLong("periodEnd"), v.getLong("revenue"), v.getLong("expenses"), v.getString("reason"), v.getUUID("applicant"), v.getLong("createdAt"), v.getString("status"), v.getString("reviewer"), v.getLong("reviewedAt"), v.getString("reviewReason"))); } return data; }
}
