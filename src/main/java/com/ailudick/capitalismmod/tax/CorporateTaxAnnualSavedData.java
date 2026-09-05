package com.ailudick.capitalismmod.tax;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Stores annual corporate income totals and completed settlement results. */
public final class CorporateTaxAnnualSavedData extends SavedData {
    private static final String ID = "capitalismmod_corporate_tax_annual";
    private final Map<String, Entry> entries = new HashMap<>();
    private final Map<String, Settlement> settlements = new HashMap<>();
    private final Map<String, Long> prepaid = new HashMap<>();
    public record Entry(String companyId, String currencyId, long revenue, long expenses, long yearStart, long yearEnd) {
        public Entry(String companyId, String currencyId, long revenue, long yearStart, long yearEnd) {
            this(companyId, currencyId, revenue, 0L, yearStart, yearEnd);
        }
    }
    public record Settlement(String companyId, long yearEnd, long annualTax, long prepaidTax,
                             long balance, long credit, long settledAt) {}
    private CorporateTaxAnnualSavedData() {}

    public static CorporateTaxAnnualSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(CorporateTaxAnnualSavedData::new, CorporateTaxAnnualSavedData::load), ID);
    }

    public void record(String companyId, String currencyId, long revenue, long yearStart, long yearEnd) {
        if (revenue <= 0L) return;
        String key = companyId + ":" + yearEnd;
        Entry old = entries.get(key);
        long total = old == null ? revenue : old.revenue() > Long.MAX_VALUE - revenue
                ? Long.MAX_VALUE : old.revenue() + revenue;
        entries.put(key, new Entry(companyId, currencyId, total, old == null ? 0L : old.expenses(), yearStart, yearEnd));
        setDirty();
    }

    public void recordExpense(String companyId, String currencyId, long expense, long yearStart, long yearEnd) {
        if (expense <= 0L) return;
        String key = companyId + ":" + yearEnd;
        Entry old = entries.get(key);
        long revenue = old == null ? 0L : old.revenue();
        long expenses = old == null ? expense : old.expenses() > Long.MAX_VALUE - expense
                ? Long.MAX_VALUE : old.expenses() + expense;
        entries.put(key, new Entry(companyId, currencyId, revenue, expenses, yearStart, yearEnd));
        setDirty();
    }

    public void recordPrepaid(String companyId, long yearEnd, long amount) {
        if (amount <= 0L) return;
        String key = companyId + ":" + yearEnd;
        long old = prepaid.getOrDefault(key, 0L);
        prepaid.put(key, old > Long.MAX_VALUE - amount ? Long.MAX_VALUE : old + amount);
        setDirty();
    }

    public long prepaidFor(String companyId, long yearEnd) {
        return prepaid.getOrDefault(companyId + ":" + yearEnd, 0L);
    }

    public List<Entry> due(long now) {
        return new ArrayList<>(entries.values().stream().filter(entry -> entry.yearEnd() <= now
                && !settlements.containsKey(entry.companyId() + ":" + entry.yearEnd())).toList());
    }

    public void settle(Entry entry, Settlement settlement) {
        entries.remove(entry.companyId() + ":" + entry.yearEnd());
        settlements.put(entry.companyId() + ":" + entry.yearEnd(), settlement);
        prepaid.remove(entry.companyId() + ":" + entry.yearEnd());
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag entryList = new ListTag();
        entries.values().forEach(entry -> {
            CompoundTag value = new CompoundTag(); value.putString("companyId", entry.companyId());
            value.putString("currency", entry.currencyId()); value.putLong("revenue", entry.revenue());
            value.putLong("expenses", entry.expenses());
            value.putLong("yearStart", entry.yearStart()); value.putLong("yearEnd", entry.yearEnd()); entryList.add(value);
        });
        tag.put("entries", entryList);
        ListTag settlementList = new ListTag();
        settlements.values().forEach(settlement -> {
            CompoundTag value = new CompoundTag(); value.putString("companyId", settlement.companyId());
            value.putLong("yearEnd", settlement.yearEnd()); value.putLong("annualTax", settlement.annualTax());
            value.putLong("prepaidTax", settlement.prepaidTax()); value.putLong("balance", settlement.balance());
            value.putLong("credit", settlement.credit()); value.putLong("settledAt", settlement.settledAt()); settlementList.add(value);
        });
        tag.put("settlements", settlementList);
        ListTag prepaidList = new ListTag();
        prepaid.forEach((key, amount) -> { CompoundTag value = new CompoundTag(); value.putString("key", key); value.putLong("amount", amount); prepaidList.add(value); });
        tag.put("prepaid", prepaidList);
        return tag;
    }

    public static CorporateTaxAnnualSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        CorporateTaxAnnualSavedData data = new CorporateTaxAnnualSavedData();
        ListTag entries = tag.getList("entries", 10);
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag value = entries.getCompound(i);
            Entry entry = new Entry(value.getString("companyId"), value.getString("currency"), value.getLong("revenue"),
                    value.getLong("expenses"), value.getLong("yearStart"), value.getLong("yearEnd"));
            if (!entry.companyId().isBlank()) data.entries.put(entry.companyId() + ":" + entry.yearEnd(), entry);
        }
        ListTag settlements = tag.getList("settlements", 10);
        for (int i = 0; i < settlements.size(); i++) {
            CompoundTag value = settlements.getCompound(i);
            Settlement settlement = new Settlement(value.getString("companyId"), value.getLong("yearEnd"),
                    value.getLong("annualTax"), value.getLong("prepaidTax"), value.getLong("balance"),
                    value.getLong("credit"), value.getLong("settledAt"));
            if (!settlement.companyId().isBlank()) data.settlements.put(settlement.companyId() + ":" + settlement.yearEnd(), settlement);
        }
        ListTag prepaid = tag.getList("prepaid", 10);
        for (int i = 0; i < prepaid.size(); i++) {
            CompoundTag value = prepaid.getCompound(i);
            if (!value.getString("key").isBlank() && value.getLong("amount") > 0L) data.prepaid.put(value.getString("key"), value.getLong("amount"));
        }
        return data;
    }
}
