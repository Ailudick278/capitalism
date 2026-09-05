package com.ailudick.capitalismmod.tax;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.ArrayList;
import java.util.List;

/** Permanent audit trail for administrator tax-period corrections. */
public final class TaxCorrectionAuditSavedData extends SavedData {
    private static final String ID = "capitalismmod_tax_correction_audit";
    public record Entry(String id, String businessId, long periodEnd, long oldRevenue, long oldExpenses,
                        long newRevenue, long newExpenses, long oldTax, long newTax, long difference,
                        String administrator, long occurredAt, String reason) {}
    private final List<Entry> entries = new ArrayList<>();
    private TaxCorrectionAuditSavedData() {}
    public static TaxCorrectionAuditSavedData get(MinecraftServer server) { return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(TaxCorrectionAuditSavedData::new, TaxCorrectionAuditSavedData::load), ID); }
    public List<Entry> all() { return List.copyOf(entries); }
    public void add(Entry entry) { if (entry != null) { entries.add(entry); setDirty(); } }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) { ListTag list = new ListTag(); for (Entry e : entries) { CompoundTag v = new CompoundTag(); v.putString("id", e.id()); v.putString("business", e.businessId()); v.putLong("periodEnd", e.periodEnd()); v.putLong("oldRevenue", e.oldRevenue()); v.putLong("oldExpenses", e.oldExpenses()); v.putLong("newRevenue", e.newRevenue()); v.putLong("newExpenses", e.newExpenses()); v.putLong("oldTax", e.oldTax()); v.putLong("newTax", e.newTax()); v.putLong("difference", e.difference()); v.putString("administrator", e.administrator()); v.putLong("occurredAt", e.occurredAt()); v.putString("reason", e.reason()); list.add(v); } tag.put("entries", list); return tag; }
    public static TaxCorrectionAuditSavedData load(CompoundTag tag, HolderLookup.Provider registries) { TaxCorrectionAuditSavedData data = new TaxCorrectionAuditSavedData(); ListTag list = tag.getList("entries", 10); for (int i = 0; i < list.size(); i++) { CompoundTag v = list.getCompound(i); data.entries.add(new Entry(v.getString("id"), v.getString("business"), v.getLong("periodEnd"), v.getLong("oldRevenue"), v.getLong("oldExpenses"), v.getLong("newRevenue"), v.getLong("newExpenses"), v.getLong("oldTax"), v.getLong("newTax"), v.getLong("difference"), v.getString("administrator"), v.getLong("occurredAt"), v.getString("reason"))); } return data; }
}
