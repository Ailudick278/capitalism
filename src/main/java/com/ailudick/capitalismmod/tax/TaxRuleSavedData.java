package com.ailudick.capitalismmod.tax;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Persistent server-owned tax rule table. */
public final class TaxRuleSavedData extends SavedData {
    private static final String ID = "capitalismmod_tax_rules";
    private final Map<TaxType, List<TaxRule>> rules = new EnumMap<>(TaxType.class);

    private TaxRuleSavedData() {}

    public static TaxRuleSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(TaxRuleSavedData::new, TaxRuleSavedData::load), ID);
    }

    public TaxRule get(TaxType type) {
        return rules.getOrDefault(type, List.of()).stream()
                .max(Comparator.comparingLong(TaxRule::effectiveFrom).thenComparingLong(TaxRule::createdAt))
                .orElse(null);
    }

    public TaxRule effective(TaxType type, long at) {
        return rules.getOrDefault(type, List.of()).stream()
                .filter(rule -> rule.effectiveFrom() <= at)
                .max(Comparator.comparingLong(TaxRule::effectiveFrom).thenComparingLong(TaxRule::createdAt))
                .orElse(null);
    }

    public List<TaxRule> history(TaxType type) {
        return rules.getOrDefault(type, List.of()).stream()
                .sorted(Comparator.comparingLong(TaxRule::effectiveFrom).reversed())
                .toList();
    }

    public Map<TaxType, TaxRule> all() {
        Map<TaxType, TaxRule> latest = new EnumMap<>(TaxType.class);
        rules.forEach((type, entries) -> {
            TaxRule rule = entries.stream().max(Comparator.comparingLong(TaxRule::effectiveFrom)
                    .thenComparingLong(TaxRule::createdAt)).orElse(null);
            if (rule != null) latest.put(type, rule);
        });
        return Map.copyOf(latest);
    }

    public void put(TaxRule rule) {
        rules.computeIfAbsent(rule.type(), ignored -> new ArrayList<>()).add(rule);
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (List<TaxRule> versions : rules.values()) for (TaxRule rule : versions) {
            CompoundTag entry = new CompoundTag();
            entry.putString("type", rule.type().id());
            entry.putInt("rateBps", rule.rateBasisPoints());
            entry.putLong("threshold", rule.thresholdMinor());
            entry.putLong("exemption", rule.exemptionMinor());
            entry.putLong("effectiveFrom", rule.effectiveFrom());
            entry.putBoolean("enabled", rule.enabled());
            entry.putString("versionId", rule.versionId());
            entry.putLong("createdAt", rule.createdAt());
            entry.putString("createdBy", rule.createdBy());
            list.add(entry);
        }
        tag.put("rules", list);
        return tag;
    }

    public static TaxRuleSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        TaxRuleSavedData data = new TaxRuleSavedData();
        ListTag list = tag.getList("rules", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            TaxType type = TaxType.byId(entry.getString("type"));
            TaxRule rule = new TaxRule(type, entry.getInt("rateBps"), entry.getLong("threshold"),
                    entry.getLong("exemption"), entry.getLong("effectiveFrom"), entry.getBoolean("enabled"),
                    entry.getString("versionId"), entry.getLong("createdAt"), entry.getString("createdBy"));
            data.rules.computeIfAbsent(type, ignored -> new ArrayList<>()).add(rule);
        }
        return data;
    }
}
