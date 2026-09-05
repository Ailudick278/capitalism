package com.ailudick.capitalismmod.company;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

/** Persistent weighted quality ledger for newly manufactured company goods. */
public final class CompanyQualitySavedData extends SavedData {
    private static final String ID = "capitalismmod_company_quality";
    private final Map<String, Map<String, ProductQuality>> products = new HashMap<>();

    public record ProductQuality(long units, long qualityTotal) {
        private static final Codec<ProductQuality> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.LONG.fieldOf("units").forGetter(ProductQuality::units),
                Codec.LONG.fieldOf("qualityTotal").forGetter(ProductQuality::qualityTotal)
        ).apply(instance, ProductQuality::new));

        public int averageScore() {
            if (units <= 0L) return 0;
            return (int) Math.max(0L, Math.min(100L, qualityTotal / units));
        }
    }

    private record State(Map<String, Map<String, ProductQuality>> products) {
        private static final Codec<State> CODEC = Codec.unboundedMap(Codec.STRING,
                Codec.unboundedMap(Codec.STRING, ProductQuality.CODEC)).xmap(State::new, State::products);
    }

    private CompanyQualitySavedData() {
    }

    public static CompanyQualitySavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(CompanyQualitySavedData::new, CompanyQualitySavedData::load), ID);
    }

    public ProductQuality get(String companyId, String itemId) {
        if (companyId == null || itemId == null) return null;
        return products.getOrDefault(companyId, Map.of()).get(itemId);
    }

    public Map<String, ProductQuality> all(String companyId) {
        return Map.copyOf(products.getOrDefault(companyId, Map.of()));
    }

    public int averageScore(String companyId) {
        Map<String, ProductQuality> company = products.getOrDefault(companyId, Map.of());
        long units = 0L;
        long total = 0L;
        for (ProductQuality quality : company.values()) {
            if (quality == null || quality.units() <= 0L) continue;
            units = addSaturated(units, quality.units());
            total = addSaturated(total, quality.qualityTotal());
        }
        return units <= 0L ? 0 : (int) Math.max(0L, Math.min(100L, total / units));
    }

    /** Records a manufactured batch as a weighted average, without changing item counts. */
    public void record(String companyId, String itemId, int units, int score) {
        if (companyId == null || companyId.isBlank() || itemId == null || itemId.isBlank()
                || units <= 0) return;
        int normalizedScore = Math.max(0, Math.min(100, score));
        Map<String, ProductQuality> company = new HashMap<>(products.getOrDefault(companyId, Map.of()));
        ProductQuality previous = company.get(itemId);
        long addedUnits = units;
        long addedTotal = multiplySaturated(addedUnits, normalizedScore);
        if (previous != null) {
            addedUnits = addSaturated(previous.units(), addedUnits);
            addedTotal = addSaturated(previous.qualityTotal(), addedTotal);
        }
        company.put(itemId, new ProductQuality(addedUnits, addedTotal));
        products.put(companyId, company);
        setDirty();
    }

    /** Removes quality lots proportionally when company stock is sold. */
    public void consume(String companyId, String itemId, int units) {
        if (companyId == null || itemId == null || units <= 0) return;
        Map<String, ProductQuality> company = products.get(companyId);
        if (company == null) return;
        ProductQuality previous = company.get(itemId);
        if (previous == null || previous.units() <= 0L) return;
        long used = Math.min((long) units, previous.units());
        long usedTotal = proportional(previous.qualityTotal(), used, previous.units());
        long remaining = previous.units() - used;
        if (remaining <= 0L) company.remove(itemId);
        else company.put(itemId, new ProductQuality(remaining, Math.max(0L, previous.qualityTotal() - usedTotal)));
        if (company.isEmpty()) products.remove(companyId);
        setDirty();
    }

    /** Transfers weighted quality history during a company merger. */
    public void transferCompany(String sourceId, String targetId) {
        if (sourceId == null || targetId == null || sourceId.equals(targetId)) return;
        Map<String, ProductQuality> source = products.remove(sourceId);
        if (source == null || source.isEmpty()) {
            if (source != null) setDirty();
            return;
        }
        Map<String, ProductQuality> target = new HashMap<>(products.getOrDefault(targetId, Map.of()));
        source.forEach((itemId, incoming) -> {
            ProductQuality existing = target.get(itemId);
            target.put(itemId, existing == null ? incoming : new ProductQuality(
                    addSaturated(existing.units(), incoming.units()),
                    addSaturated(existing.qualityTotal(), incoming.qualityTotal())));
        });
        products.put(targetId, target);
        setDirty();
    }

    private static long proportional(long total, long part, long denominator) {
        if (total <= 0L || part <= 0L || denominator <= 0L) return 0L;
        if (total == Long.MAX_VALUE) return Long.MAX_VALUE;
        long whole = total / denominator;
        long remainder = total % denominator;
        return addSaturated(multiplySaturated(whole, part), (remainder * part) / denominator);
    }

    private static long multiplySaturated(long left, long right) {
        if (left <= 0L || right <= 0L) return 0L;
        if (left > Long.MAX_VALUE / right) return Long.MAX_VALUE;
        return left * right;
    }

    private static long addSaturated(long left, long right) {
        if (left < 0L || right < 0L || left > Long.MAX_VALUE - right) return Long.MAX_VALUE;
        return left + right;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        State.CODEC.encodeStart(NbtOps.INSTANCE, new State(products)).result()
                .ifPresent(encoded -> tag.put("data", encoded));
        return tag;
    }

    public static CompanyQualitySavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        CompanyQualitySavedData data = new CompanyQualitySavedData();
        if (tag.contains("data")) {
            State.CODEC.parse(NbtOps.INSTANCE, tag.get("data")).result()
                    .ifPresent(state -> state.products().forEach((companyId, values) ->
                            data.products.put(companyId, new HashMap<>(values))));
        }
        return data;
    }
}
