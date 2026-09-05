package com.ailudick.capitalismmod.company;

import com.ailudick.capitalismmod.util.EconomyMath;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

/** Weighted-average cost layers for company-owned warehouse inventory. */
public final class CompanyInventoryCostSavedData extends SavedData {
    private static final String ID = "capitalismmod_company_inventory_cost";
    private final Map<String, Map<String, CostLayer>> layers = new HashMap<>();
    private final Set<String> freightSources = new HashSet<>();

    public record CostLayer(int quantity, long totalCost) {
        private static final Codec<CostLayer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("quantity").forGetter(CostLayer::quantity),
                Codec.LONG.fieldOf("totalCost").forGetter(CostLayer::totalCost)
        ).apply(instance, CostLayer::new));
    }

    public record Consumption(int quantity, long cost) {
    }

    private record State(Map<String, Map<String, CostLayer>> layers, Set<String> freightSources) {
        private static final Codec<State> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.unboundedMap(Codec.STRING, Codec.unboundedMap(Codec.STRING, CostLayer.CODEC))
                        .fieldOf("layers").forGetter(State::layers),
                Codec.STRING.listOf().xmap(values -> (Set<String>) new HashSet<String>(values),
                                values -> new ArrayList<>(values))
                        .optionalFieldOf("freightSources", Set.of())
                        .forGetter(State::freightSources)
        ).apply(instance, State::new));
    }

    private CompanyInventoryCostSavedData() {
    }

    public static CompanyInventoryCostSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(CompanyInventoryCostSavedData::new, CompanyInventoryCostSavedData::load), ID);
    }

    public CostLayer layer(String companyId, String itemId) {
        if (companyId == null || itemId == null) return null;
        return layers.getOrDefault(companyId, Map.of()).get(itemId);
    }

    /** Adds an acquired batch to the weighted-average cost layer. */
    public void add(String companyId, String itemId, int quantity, long totalCost) {
        if (companyId == null || companyId.isBlank() || itemId == null || itemId.isBlank()
                || quantity <= 0 || totalCost < 0L) return;
        CostLayer previous = layer(companyId, itemId);
        WeightedAverageCost next = new WeightedAverageCost(previous == null ? 0 : previous.quantity(),
                previous == null ? 0L : previous.totalCost()).add(quantity, totalCost);
        layers.computeIfAbsent(companyId, ignored -> new HashMap<>())
                .put(itemId, new CostLayer(next.quantity(), next.totalCost()));
        setDirty();
    }

    /** Adds an inbound freight estimate exactly once for a shipment. */
    public boolean addFreightCost(String companyId, String itemId, long totalCost, String sourceId) {
        if (companyId == null || companyId.isBlank() || itemId == null || itemId.isBlank()
                || totalCost < 0L || sourceId == null || sourceId.isBlank()
                || freightSources.contains(sourceId)) return false;
        CostLayer previous = layer(companyId, itemId);
        long nextCost = addSaturated(previous == null ? 0L : previous.totalCost(), totalCost);
        layers.computeIfAbsent(companyId, ignored -> new HashMap<>())
                .put(itemId, new CostLayer(previous == null ? 0 : previous.quantity(), nextCost));
        freightSources.add(sourceId);
        setDirty();
        return true;
    }

    /** Consumes the tracked portion of a batch and returns its weighted-average cost. */
    public Consumption consume(String companyId, String itemId, int requested) {
        if (companyId == null || itemId == null || requested <= 0) return new Consumption(0, 0L);
        CostLayer previous = layer(companyId, itemId);
        if (previous == null || previous.quantity() <= 0) return new Consumption(0, 0L);
        WeightedAverageCost.Consumption result = new WeightedAverageCost(
                previous.quantity(), previous.totalCost()).consume(requested);
        int taken = result.quantity();
        long cost = result.cost();
        int remaining = result.remaining().quantity();
        Map<String, CostLayer> companyLayers = layers.get(companyId);
        if (remaining <= 0) {
            companyLayers.remove(itemId);
        } else {
            companyLayers.put(itemId, new CostLayer(remaining, result.remaining().totalCost()));
        }
        if (companyLayers.isEmpty()) layers.remove(companyId);
        setDirty();
        return new Consumption(taken, cost);
    }

    /** Moves all tracked cost layers during a company merger. */
    public void transferCompany(String sourceId, String targetId) {
        if (sourceId == null || targetId == null || sourceId.equals(targetId)) return;
        Map<String, CostLayer> source = layers.remove(sourceId);
        if (source == null || source.isEmpty()) return;
        Map<String, CostLayer> target = layers.computeIfAbsent(targetId, ignored -> new HashMap<>());
        source.forEach((itemId, layer) -> {
            CostLayer existing = target.get(itemId);
            if (existing == null) target.put(itemId, layer);
            else target.put(itemId, new CostLayer(safeQuantity(existing.quantity(), layer.quantity()),
                    addSaturated(existing.totalCost(), layer.totalCost())));
        });
        setDirty();
    }

    private static long addSaturated(long left, long right) {
        long result = EconomyMath.add(Math.max(0L, left), Math.max(0L, right));
        return result < 0L ? Long.MAX_VALUE : result;
    }

    private static int safeQuantity(int left, int right) {
        return right > Integer.MAX_VALUE - Math.max(0, left)
                ? Integer.MAX_VALUE : Math.max(0, left) + Math.max(0, right);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        State.CODEC.encodeStart(NbtOps.INSTANCE, new State(layers, freightSources)).result()
                .ifPresent(encoded -> tag.put("data", encoded));
        return tag;
    }

    public static CompanyInventoryCostSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        CompanyInventoryCostSavedData data = new CompanyInventoryCostSavedData();
        if (tag.contains("data")) {
            State.CODEC.parse(NbtOps.INSTANCE, tag.get("data")).result().ifPresent(state -> {
                state.layers().forEach((companyId, values) ->
                        data.layers.put(companyId, new HashMap<>(values)));
                data.freightSources.addAll(state.freightSources());
            });
        }
        return data;
    }
}
