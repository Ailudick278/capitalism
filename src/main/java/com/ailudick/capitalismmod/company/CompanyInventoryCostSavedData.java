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
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Weighted-average cost layers for company-owned warehouse inventory. */
public final class CompanyInventoryCostSavedData extends SavedData {
    private static final String ID = "capitalismmod_company_inventory_cost";
    private static final Codec<FifoInventoryCost.Batch> BATCH_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("quantity").forGetter(FifoInventoryCost.Batch::quantity),
            Codec.LONG.fieldOf("totalCost").forGetter(FifoInventoryCost.Batch::totalCost)
    ).apply(instance, FifoInventoryCost.Batch::new));
    private final Map<String, Map<String, CostLayer>> layers = new HashMap<>();
    private final Map<String, Map<String, ArrayList<FifoInventoryCost.Batch>>> batches = new HashMap<>();
    private final Set<String> freightSources = new HashSet<>();
    private final Set<String> inboundSources = new HashSet<>();
    private final Set<String> inventorySaleSources = new HashSet<>();
    private final Set<String> inventoryLossSources = new HashSet<>();
    private final Set<String> inventoryConsumptionSources = new HashSet<>();
    private final Map<String, Consumption> inventoryConsumptionReceipts = new HashMap<>();

    public record CostLayer(int quantity, long totalCost) {
        private static final Codec<CostLayer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("quantity").forGetter(CostLayer::quantity),
                Codec.LONG.fieldOf("totalCost").forGetter(CostLayer::totalCost)
        ).apply(instance, CostLayer::new));
    }

    public record Consumption(int quantity, long cost) {
    }

    private static final Codec<Consumption> CONSUMPTION_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("quantity").forGetter(Consumption::quantity),
            Codec.LONG.fieldOf("cost").forGetter(Consumption::cost)
    ).apply(instance, Consumption::new));

    private record State(Map<String, Map<String, CostLayer>> layers,
                         Map<String, Map<String, List<FifoInventoryCost.Batch>>> batches,
                         Set<String> freightSources, Set<String> inboundSources,
                         Set<String> inventorySaleSources, Set<String> inventoryLossSources,
                         Set<String> inventoryConsumptionSources,
                         Map<String, Consumption> inventoryConsumptionReceipts) {
        private static final Codec<State> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.unboundedMap(Codec.STRING, Codec.unboundedMap(Codec.STRING, CostLayer.CODEC))
                        .fieldOf("layers").forGetter(State::layers),
                Codec.unboundedMap(Codec.STRING, Codec.unboundedMap(Codec.STRING, BATCH_CODEC.listOf()))
                        .optionalFieldOf("batches", Map.of()).forGetter(State::batches),
                Codec.STRING.listOf().xmap(values -> (Set<String>) new HashSet<String>(values),
                                values -> new ArrayList<>(values))
                        .optionalFieldOf("freightSources", Set.of())
                        .forGetter(State::freightSources),
                Codec.STRING.listOf().xmap(values -> (Set<String>) new HashSet<String>(values),
                        values -> new ArrayList<>(values))
                        .optionalFieldOf("inboundSources", Set.of())
                        .forGetter(State::inboundSources),
                Codec.STRING.listOf().xmap(values -> (Set<String>) new HashSet<String>(values),
                        values -> new ArrayList<>(values))
                        .optionalFieldOf("inventorySaleSources", Set.of())
                        .forGetter(State::inventorySaleSources),
                Codec.STRING.listOf().xmap(values -> (Set<String>) new HashSet<String>(values),
                        values -> new ArrayList<>(values))
                        .optionalFieldOf("inventoryLossSources", Set.of())
                        .forGetter(State::inventoryLossSources),
                Codec.STRING.listOf().xmap(values -> (Set<String>) new HashSet<String>(values),
                        values -> new ArrayList<>(values))
                        .optionalFieldOf("inventoryConsumptionSources", Set.of())
                        .forGetter(State::inventoryConsumptionSources),
                Codec.unboundedMap(Codec.STRING, CONSUMPTION_CODEC)
                        .optionalFieldOf("inventoryConsumptionReceipts", Map.of())
                        .forGetter(State::inventoryConsumptionReceipts)
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

    public boolean hasInventorySale(String sourceId) {
        return sourceId != null && !sourceId.isBlank() && inventorySaleSources.contains(sourceId);
    }

    public void recordInventorySale(String sourceId) {
        if (sourceId == null || sourceId.isBlank() || !inventorySaleSources.add(sourceId)) return;
        setDirty();
    }

    public boolean hasInventoryLoss(String sourceId) {
        return sourceId != null && !sourceId.isBlank() && inventoryLossSources.contains(sourceId);
    }

    public boolean recordInventoryLoss(String sourceId) {
        if (sourceId == null || sourceId.isBlank() || !inventoryLossSources.add(sourceId)) return false;
        setDirty();
        return true;
    }

    /** Adds an acquired batch to the weighted-average cost layer. */
    public void add(String companyId, String itemId, int quantity, long totalCost) {
        if (companyId == null || companyId.isBlank() || itemId == null || itemId.isBlank()
                || quantity <= 0 || totalCost < 0L) return;
        List<FifoInventoryCost.Batch> current = batchesFor(companyId, itemId);
        List<FifoInventoryCost.Batch> next = FifoInventoryCost.add(current, quantity, totalCost);
        batches.computeIfAbsent(companyId, ignored -> new HashMap<>())
                .put(itemId, new ArrayList<>(next));
        rebuildLayer(companyId, itemId, next);
        setDirty();
    }

    /** Adds an inbound inventory cost layer once for a durable delivery source. */
    public boolean addInboundOnce(String companyId, String itemId, int quantity, long totalCost,
                                  String sourceId) {
        if (sourceId == null || sourceId.isBlank() || inboundSources.contains(sourceId)
                || companyId == null || companyId.isBlank() || itemId == null || itemId.isBlank()
                || quantity <= 0 || totalCost < 0L) return false;
        add(companyId, itemId, quantity, totalCost);
        inboundSources.add(sourceId);
        trimSources(inboundSources);
        setDirty();
        return true;
    }

    /** Adds an inbound freight estimate exactly once for a shipment. */
    public boolean addFreightCost(String companyId, String itemId, long totalCost, String sourceId) {
        if (companyId == null || companyId.isBlank() || itemId == null || itemId.isBlank()
                || totalCost < 0L || sourceId == null || sourceId.isBlank()
                || freightSources.contains(sourceId)) return false;
        List<FifoInventoryCost.Batch> current = new ArrayList<>(batchesFor(companyId, itemId));
        if (current.isEmpty()) return false;
        FifoInventoryCost.Batch last = current.remove(current.size() - 1);
        current.add(new FifoInventoryCost.Batch(last.quantity(), addSaturated(last.totalCost(), totalCost)));
        batches.computeIfAbsent(companyId, ignored -> new HashMap<>())
                .put(itemId, new ArrayList<>(current));
        rebuildLayer(companyId, itemId, current);
        freightSources.add(sourceId);
        setDirty();
        return true;
    }

    /** Consumes the tracked portion of a batch and returns its weighted-average cost. */
    public Consumption consume(String companyId, String itemId, int requested) {
        if (companyId == null || itemId == null || requested <= 0) return new Consumption(0, 0L);
        List<FifoInventoryCost.Batch> current = batchesFor(companyId, itemId);
        FifoInventoryCost.Consumption result = FifoInventoryCost.consume(current, requested);
        int taken = result.quantity();
        long cost = result.cost();
        Map<String, ArrayList<FifoInventoryCost.Batch>> companyBatches = batches.get(companyId);
        if (result.remaining().isEmpty()) {
            if (companyBatches != null) companyBatches.remove(itemId);
            if (companyBatches != null && companyBatches.isEmpty()) batches.remove(companyId);
            Map<String, CostLayer> companyLayers = layers.get(companyId);
            if (companyLayers != null) {
                companyLayers.remove(itemId);
                if (companyLayers.isEmpty()) layers.remove(companyId);
            }
        } else {
            batches.computeIfAbsent(companyId, ignored -> new HashMap<>())
                    .put(itemId, new ArrayList<>(result.remaining()));
            rebuildLayer(companyId, itemId, result.remaining());
        }
        setDirty();
        return new Consumption(taken, cost);
    }

    /** Consumes tracked inventory cost at most once for a durable external source. */
    public Consumption consumeOnce(String companyId, String itemId, int requested, String sourceId) {
        if (sourceId == null || sourceId.isBlank()) {
            return new Consumption(0, 0L);
        }
        Consumption recorded = inventoryConsumptionReceipts.get(sourceId);
        if (recorded != null) return recorded;
        // Compatibility with saves written before result receipts existed.
        if (inventoryConsumptionSources.contains(sourceId)) return new Consumption(0, 0L);
        Consumption result = consume(companyId, itemId, requested);
        inventoryConsumptionSources.add(sourceId);
        inventoryConsumptionReceipts.put(sourceId, result);
        trimSources(inventoryConsumptionSources);
        while (inventoryConsumptionReceipts.size() > 8192) {
            String first = inventoryConsumptionReceipts.keySet().iterator().next();
            inventoryConsumptionReceipts.remove(first);
        }
        setDirty();
        return result;
    }

    /** Moves all tracked cost layers during a company merger. */
    public void transferCompany(String sourceId, String targetId) {
        if (sourceId == null || targetId == null || sourceId.equals(targetId)) return;
        Map<String, CostLayer> source = layers.remove(sourceId);
        Map<String, ArrayList<FifoInventoryCost.Batch>> sourceBatches = batches.remove(sourceId);
        if (source == null && (sourceBatches == null || sourceBatches.isEmpty())) return;
        Map<String, CostLayer> target = layers.computeIfAbsent(targetId, ignored -> new HashMap<>());
        if (source != null) source.forEach((itemId, layer) -> {
                CostLayer existing = target.get(itemId);
                if (existing == null) target.put(itemId, layer);
                else target.put(itemId, new CostLayer(safeQuantity(existing.quantity(), layer.quantity()),
                        addSaturated(existing.totalCost(), layer.totalCost())));
            });
        if (sourceBatches != null) sourceBatches.forEach((itemId, values) -> {
            ArrayList<FifoInventoryCost.Batch> targetValues = new ArrayList<>(batchesFor(targetId, itemId));
            targetValues.addAll(values);
            batches.computeIfAbsent(targetId, ignored -> new HashMap<>()).put(itemId, targetValues);
            rebuildLayer(targetId, itemId, targetValues);
        });
        setDirty();
    }

    /** Returns FIFO batches, converting a legacy aggregate layer on first access. */
    private List<FifoInventoryCost.Batch> batchesFor(String companyId, String itemId) {
        Map<String, ArrayList<FifoInventoryCost.Batch>> company =
                batches.computeIfAbsent(companyId, ignored -> new HashMap<>());
        ArrayList<FifoInventoryCost.Batch> existing = company.get(itemId);
        if (existing != null) return existing;
        CostLayer legacy = layers.getOrDefault(companyId, Map.of()).get(itemId);
        ArrayList<FifoInventoryCost.Batch> migrated = new ArrayList<>();
        if (legacy != null && legacy.quantity() > 0) {
            migrated.add(new FifoInventoryCost.Batch(legacy.quantity(), legacy.totalCost()));
        }
        company.put(itemId, migrated);
        return migrated;
    }

    private void rebuildLayer(String companyId, String itemId, List<FifoInventoryCost.Batch> values) {
        int quantity = FifoInventoryCost.quantity(values);
        if (quantity <= 0) {
            Map<String, CostLayer> company = layers.get(companyId);
            if (company != null) {
                company.remove(itemId);
                if (company.isEmpty()) layers.remove(companyId);
            }
            return;
        }
        layers.computeIfAbsent(companyId, ignored -> new HashMap<>())
                .put(itemId, new CostLayer(quantity, FifoInventoryCost.totalCost(values)));
    }

    private static long addSaturated(long left, long right) {
        long result = EconomyMath.add(Math.max(0L, left), Math.max(0L, right));
        return result < 0L ? Long.MAX_VALUE : result;
    }

    private static void trimSources(Set<String> sources) {
        while (sources.size() > 8192) sources.remove(sources.iterator().next());
    }

    private static int safeQuantity(int left, int right) {
        return right > Integer.MAX_VALUE - Math.max(0, left)
                ? Integer.MAX_VALUE : Math.max(0, left) + Math.max(0, right);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        Map<String, Map<String, List<FifoInventoryCost.Batch>>> savedBatches = new HashMap<>();
        batches.forEach((companyId, values) -> savedBatches.put(companyId, new HashMap<>(values)));
        State.CODEC.encodeStart(NbtOps.INSTANCE, new State(layers, savedBatches, freightSources, inboundSources,
                inventorySaleSources, inventoryLossSources, inventoryConsumptionSources,
                inventoryConsumptionReceipts)).result()
                .ifPresent(encoded -> tag.put("data", encoded));
        return tag;
    }

    public static CompanyInventoryCostSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        CompanyInventoryCostSavedData data = new CompanyInventoryCostSavedData();
        if (tag.contains("data")) {
            State.CODEC.parse(NbtOps.INSTANCE, tag.get("data")).result().ifPresent(state -> {
                state.layers().forEach((companyId, values) ->
                        data.layers.put(companyId, new HashMap<>(values)));
                state.batches().forEach((companyId, values) -> {
                    Map<String, ArrayList<FifoInventoryCost.Batch>> company = new HashMap<>();
                    values.forEach((itemId, list) -> company.put(itemId, new ArrayList<>(list)));
                    data.batches.put(companyId, company);
                });
                data.freightSources.addAll(state.freightSources());
                data.inboundSources.addAll(state.inboundSources());
                trimSources(data.inboundSources);
                data.inventorySaleSources.addAll(state.inventorySaleSources());
                data.inventoryLossSources.addAll(state.inventoryLossSources());
                data.inventoryConsumptionSources.addAll(state.inventoryConsumptionSources());
                trimSources(data.inventoryConsumptionSources);
                data.inventoryConsumptionReceipts.putAll(state.inventoryConsumptionReceipts());
                while (data.inventoryConsumptionReceipts.size() > 8192) {
                    String first = data.inventoryConsumptionReceipts.keySet().iterator().next();
                    data.inventoryConsumptionReceipts.remove(first);
                }
            });
        }
        return data;
    }
}
