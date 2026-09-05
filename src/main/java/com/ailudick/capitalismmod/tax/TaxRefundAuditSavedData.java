package com.ailudick.capitalismmod.tax;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Immutable audit trail for every tax refund action. */
public final class TaxRefundAuditSavedData extends SavedData {
    private static final String ID = "capitalismmod_tax_refund_audit";
    private static final int MAX_EVENTS = 1024;
    private final List<Event> events = new ArrayList<>();
    public record Event(String requestId, UUID taxpayerUuid, String action, String actor, String currencyId,
                        long amount, long time, String result, String reason, String sourceSummary) {}

    private TaxRefundAuditSavedData() {}
    public static TaxRefundAuditSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(TaxRefundAuditSavedData::new, TaxRefundAuditSavedData::load), ID);
    }
    public void log(Event event) {
        events.add(event);
        while (events.size() > MAX_EVENTS) events.remove(0);
        setDirty();
    }
    public List<Event> all() { return List.copyOf(events); }
    public List<Event> forRequest(String requestId) { return events.stream().filter(event -> event.requestId().equals(requestId)).toList(); }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Event event : events) {
            CompoundTag value = new CompoundTag(); value.putString("request", event.requestId()); value.putUUID("taxpayer", event.taxpayerUuid());
            value.putString("action", event.action()); value.putString("actor", event.actor()); value.putString("currency", event.currencyId());
            value.putLong("amount", event.amount()); value.putLong("time", event.time()); value.putString("result", event.result());
            value.putString("reason", event.reason()); value.putString("source", event.sourceSummary()); list.add(value);
        }
        tag.put("events", list); return tag;
    }
    public static TaxRefundAuditSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        TaxRefundAuditSavedData data = new TaxRefundAuditSavedData(); ListTag list = tag.getList("events", 10);
        for (int i = 0; i < list.size(); i++) { CompoundTag value = list.getCompound(i); if (value.hasUUID("taxpayer")) data.events.add(new Event(value.getString("request"), value.getUUID("taxpayer"), value.getString("action"), value.getString("actor"), value.getString("currency"), value.getLong("amount"), value.getLong("time"), value.getString("result"), value.getString("reason"), value.getString("source"))); }
        while (data.events.size() > MAX_EVENTS) data.events.remove(0);
        return data;
    }
}
