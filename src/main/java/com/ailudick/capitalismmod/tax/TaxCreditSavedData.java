package com.ailudick.capitalismmod.tax;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

/** Carry-forward tax credits, keyed by taxpayer, tax subject and currency. */
public final class TaxCreditSavedData extends SavedData {
    private static final String ID = "capitalismmod_tax_credits";
    private final Map<String, Long> credits = new HashMap<>();
    private final List<CreditLot> lots = new ArrayList<>();
    public record CreditLot(UUID taxpayerUuid, String currencyId, String subjectType, String subjectId,
                            String sourceId, long periodStart, long periodEnd, long createdAt, long amount) {}
    private TaxCreditSavedData() {}

    public static TaxCreditSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(TaxCreditSavedData::new, TaxCreditSavedData::load), ID);
    }

    private static String key(TaxSubject subject, String currencyId) {
        return subject.type().id() + ":" + subject.subjectId() + ":" + subject.taxpayerUuid() + ":" + currencyId;
    }

    public long balance(TaxSubject subject, String currencyId) {
        return credits.getOrDefault(key(subject, currencyId), 0L);
    }

    public long totalFor(UUID taxpayerUuid) {
        String marker = ":" + taxpayerUuid + ":";
        return credits.entrySet().stream().filter(entry -> entry.getKey().contains(marker))
                .mapToLong(Map.Entry::getValue).reduce(0L, TaxCreditSavedData::addSaturated);
    }

    public long totalFor(UUID taxpayerUuid, String currencyId) {
        String marker = ":" + taxpayerUuid + ":" + currencyId;
        return credits.entrySet().stream().filter(entry -> entry.getKey().endsWith(marker))
                .mapToLong(Map.Entry::getValue).reduce(0L, TaxCreditSavedData::addSaturated);
    }

    public long consumeFor(UUID taxpayerUuid, String currencyId, long amount) {
        if (amount <= 0L) return 0L;
        if (totalFor(taxpayerUuid, currencyId) < amount) return 0L;
        long remaining = amount;
        for (String key : new java.util.ArrayList<>(credits.keySet())) {
            if (!key.endsWith(":" + taxpayerUuid + ":" + currencyId) || remaining <= 0L) continue;
            long used = Math.min(remaining, credits.get(key));
            remaining -= used;
            if (used == credits.get(key)) credits.remove(key); else credits.put(key, credits.get(key) - used);
        }
        long used = amount - remaining;
        if (used > 0L) {
            consumeLotMetadata(taxpayerUuid, currencyId, used);
            setDirty();
        }
        return used;
    }

    public String sourceSummaryFor(UUID taxpayerUuid, String currencyId) {
        String summary = lots.stream()
                .filter(lot -> lot.taxpayerUuid().equals(taxpayerUuid) && lot.currencyId().equals(currencyId) && lot.amount() > 0L)
                .map(lot -> lot.sourceId() + " [" + lot.periodStart() + "-" + lot.periodEnd() + "]")
                .distinct().limit(4).reduce((left, right) -> left + ", " + right).orElse("");
        return summary.isBlank() ? "Legacy or unclassified tax credit" : summary;
    }

    public String allocationSummaryFor(UUID taxpayerUuid, String currencyId, long amount) {
        long remaining = amount;
        StringBuilder result = new StringBuilder();
        for (CreditLot lot : lots) {
            if (remaining <= 0L) break;
            if (!lot.taxpayerUuid().equals(taxpayerUuid) || !lot.currencyId().equals(currencyId) || lot.amount() <= 0L) continue;
            long used = Math.min(remaining, lot.amount());
            if (result.length() > 0) result.append("; ");
            result.append(lot.sourceId()).append("=").append(used)
                    .append("[").append(lot.periodStart()).append("-").append(lot.periodEnd()).append("]");
            remaining -= used;
        }
        if (remaining > 0L) {
            if (result.length() > 0) result.append("; ");
            result.append("legacy/unclassified=").append(remaining);
        }
        return result.toString();
    }

    public List<TaxRefundAllocation> allocationsFor(UUID taxpayerUuid, String currencyId, long amount) {
        long remaining = amount;
        List<TaxRefundAllocation> result = new ArrayList<>();
        for (CreditLot lot : lots) {
            if (remaining <= 0L) break;
            if (!lot.taxpayerUuid().equals(taxpayerUuid) || !lot.currencyId().equals(currencyId) || lot.amount() <= 0L) continue;
            long used = Math.min(remaining, lot.amount());
            result.add(new TaxRefundAllocation(lot.sourceId(), lot.subjectType(), lot.subjectId(), lot.periodStart(), lot.periodEnd(), lot.amount(), used));
            remaining -= used;
        }
        if (remaining > 0L) result.add(new TaxRefundAllocation("legacy/unclassified", "legacy", "", 0L, 0L, remaining, remaining));
        return List.copyOf(result);
    }

    private static long addSaturated(long left, long right) {
        return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
    }

    public void add(TaxSubject subject, String currencyId, long amount) {
        add(subject, currencyId, amount, "legacy:" + subject.subjectId(), 0L, 0L, 0L);
    }

    public void add(TaxSubject subject, String currencyId, long amount, String sourceId,
                    long periodStart, long periodEnd, long createdAt) {
        if (amount <= 0L) return;
        String key = key(subject, currencyId);
        long old = credits.getOrDefault(key, 0L);
        credits.put(key, old > Long.MAX_VALUE - amount ? Long.MAX_VALUE : old + amount);
        lots.add(new CreditLot(subject.taxpayerUuid(), currencyId, subject.type().id(), subject.subjectId(),
                sourceId == null ? "" : sourceId, periodStart, periodEnd, createdAt, amount));
        setDirty();
    }

    public long consume(TaxSubject subject, String currencyId, long amount) {
        if (amount <= 0L) return 0L;
        String key = key(subject, currencyId);
        long used = Math.min(amount, credits.getOrDefault(key, 0L));
        if (used > 0L) {
            long left = credits.get(key) - used;
            if (left == 0L) credits.remove(key); else credits.put(key, left);
            consumeLotMetadata(subject.taxpayerUuid(), currencyId, used);
            setDirty();
        }
        return used;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        credits.forEach((key, amount) -> {
            CompoundTag entry = new CompoundTag(); entry.putString("key", key); entry.putLong("amount", amount); list.add(entry);
        });
        tag.put("credits", list);
        ListTag lotList = new ListTag();
        lots.forEach(lot -> {
            CompoundTag entry = new CompoundTag(); entry.putUUID("taxpayer", lot.taxpayerUuid()); entry.putString("currency", lot.currencyId());
            entry.putString("subjectType", lot.subjectType()); entry.putString("subjectId", lot.subjectId()); entry.putString("source", lot.sourceId());
            entry.putLong("periodStart", lot.periodStart()); entry.putLong("periodEnd", lot.periodEnd()); entry.putLong("createdAt", lot.createdAt()); entry.putLong("amount", lot.amount());
            lotList.add(entry);
        });
        tag.put("lots", lotList);
        return tag;
    }

    public static TaxCreditSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        TaxCreditSavedData data = new TaxCreditSavedData();
        ListTag list = tag.getList("credits", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (!entry.getString("key").isBlank() && entry.getLong("amount") > 0L) data.credits.put(entry.getString("key"), entry.getLong("amount"));
        }
        ListTag lotList = tag.getList("lots", 10);
        for (int i = 0; i < lotList.size(); i++) {
            CompoundTag entry = lotList.getCompound(i);
            if (entry.hasUUID("taxpayer") && entry.getLong("amount") > 0L) data.lots.add(new CreditLot(entry.getUUID("taxpayer"), entry.getString("currency"),
                    entry.getString("subjectType"), entry.getString("subjectId"), entry.getString("source"), entry.getLong("periodStart"),
                    entry.getLong("periodEnd"), entry.getLong("createdAt"), entry.getLong("amount")));
        }
        return data;
    }

    private void consumeLotMetadata(UUID taxpayerUuid, String currencyId, long amount) {
        long remaining = amount;
        for (int i = 0; i < lots.size() && remaining > 0L; i++) {
            CreditLot lot = lots.get(i);
            if (!lot.taxpayerUuid().equals(taxpayerUuid) || !lot.currencyId().equals(currencyId)) continue;
            long used = Math.min(remaining, lot.amount()); remaining -= used;
            if (used == lot.amount()) lots.remove(i--);
            else lots.set(i, new CreditLot(lot.taxpayerUuid(), lot.currencyId(), lot.subjectType(), lot.subjectId(), lot.sourceId(), lot.periodStart(), lot.periodEnd(), lot.createdAt(), lot.amount() - used));
        }
    }
}
