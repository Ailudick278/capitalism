package com.ailudick.capitalismmod.company;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Optional site-level allocation of a company's machines and workforce. */
public final class CompanySiteAllocationSavedData extends SavedData {
    private static final String ID = "capitalismmod_company_site_allocations";
    private final Map<String, Allocation> allocations = new HashMap<>();

    public record Allocation(String companyId, String dimension, int chunkX, int chunkZ,
                             Map<String, Integer> machines, int workers) {
        private static final Codec<Allocation> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("companyId").forGetter(Allocation::companyId),
                Codec.STRING.fieldOf("dimension").forGetter(Allocation::dimension),
                Codec.INT.fieldOf("chunkX").forGetter(Allocation::chunkX),
                Codec.INT.fieldOf("chunkZ").forGetter(Allocation::chunkZ),
                Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("machines", Map.of())
                        .forGetter(Allocation::machines),
                Codec.INT.optionalFieldOf("workers", 0).forGetter(Allocation::workers)
        ).apply(instance, Allocation::new));

        public Allocation {
            companyId = companyId == null ? "" : companyId;
            dimension = dimension == null ? "" : dimension;
            Map<String, Integer> normalized = new HashMap<>();
            if (machines != null) {
                machines.forEach((type, count) -> {
                    if (type != null && !type.isBlank() && count != null && count > 0) {
                        normalized.put(type, count);
                    }
                });
            }
            machines = Map.copyOf(normalized);
            workers = Math.max(0, workers);
        }

        public boolean empty() {
            return machines.isEmpty() && workers <= 0;
        }
    }

    private record State(Map<String, Allocation> allocations) {
        private static final Codec<State> CODEC = Codec.unboundedMap(Codec.STRING, Allocation.CODEC)
                .xmap(State::new, State::allocations);
    }

    private CompanySiteAllocationSavedData() {}

    public static CompanySiteAllocationSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(CompanySiteAllocationSavedData::new, CompanySiteAllocationSavedData::load), ID);
    }

    private static String key(String companyId, CompanySiteSavedData.Site site) {
        return companyId + "|" + site.dimension() + "|" + site.chunkX() + "|" + site.chunkZ();
    }

    public Allocation get(String companyId, CompanySiteSavedData.Site site) {
        if (companyId == null || companyId.isBlank() || site == null) return null;
        return allocations.get(key(companyId, site));
    }

    /** Returns whether this company opted into site-level machine allocation for a type. */
    public boolean hasMachineAllocation(String companyId, String machineType) {
        if (companyId == null || machineType == null || machineType.isBlank()) return false;
        return allocations.values().stream().anyMatch(allocation -> companyId.equals(allocation.companyId())
                && allocation.machines().containsKey(machineType));
    }

    /** Returns whether this company opted into site-level workforce allocation. */
    public boolean hasWorkerAllocation(String companyId) {
        if (companyId == null || companyId.isBlank()) return false;
        return allocations.values().stream().anyMatch(allocation -> companyId.equals(allocation.companyId())
                && allocation.workers() > 0);
    }

    /** Returns the assigned machine count, or -1 when legacy company-wide allocation applies. */
    public int machineCount(String companyId, CompanySiteSavedData.Site site, String machineType) {
        if (!hasMachineAllocation(companyId, machineType)) return -1;
        Allocation allocation = get(companyId, site);
        return allocation == null ? 0 : allocation.machines().getOrDefault(machineType, 0);
    }

    /** Returns the assigned worker count, or -1 when legacy company-wide allocation applies. */
    public int workerCount(String companyId, CompanySiteSavedData.Site site) {
        if (!hasWorkerAllocation(companyId)) return -1;
        Allocation allocation = get(companyId, site);
        return allocation == null ? 0 : allocation.workers();
    }

    public int totalMachineAllocation(String companyId, String machineType) {
        long total = 0L;
        for (Allocation allocation : allocations.values()) {
            if (companyId.equals(allocation.companyId())) {
                total += Math.max(0, allocation.machines().getOrDefault(machineType, 0));
            }
        }
        return (int) Math.min(Integer.MAX_VALUE, total);
    }

    public int totalWorkerAllocation(String companyId) {
        long total = 0L;
        for (Allocation allocation : allocations.values()) {
            if (companyId.equals(allocation.companyId())) total += Math.max(0, allocation.workers());
        }
        return (int) Math.min(Integer.MAX_VALUE, total);
    }

    public int maxMachineCount(String companyId, String machineType) {
        int maximum = 0;
        for (Allocation allocation : allocations.values()) {
            if (companyId.equals(allocation.companyId())) {
                maximum = Math.max(maximum, allocation.machines().getOrDefault(machineType, 0));
            }
        }
        return maximum;
    }

    public int maxWorkerCount(String companyId) {
        int maximum = 0;
        for (Allocation allocation : allocations.values()) {
            if (companyId.equals(allocation.companyId())) maximum = Math.max(maximum, allocation.workers());
        }
        return maximum;
    }

    /** Removes machine allocations that can no longer be backed by company assets. */
    public void trimMachineCount(String companyId, String machineType, int maximum) {
        if (companyId == null || machineType == null || maximum < 0) return;
        int remaining = maximum;
        boolean changed = false;
        for (Map.Entry<String, Allocation> entry : new ArrayList<>(allocations.entrySet())) {
            Allocation allocation = entry.getValue();
            if (!companyId.equals(allocation.companyId())) continue;
            int current = allocation.machines().getOrDefault(machineType, 0);
            if (current <= 0) continue;
            int kept = Math.min(current, remaining);
            remaining -= kept;
            if (kept == current) continue;
            Map<String, Integer> machines = new HashMap<>(allocation.machines());
            if (kept <= 0) machines.remove(machineType); else machines.put(machineType, kept);
            if (machines.isEmpty() && allocation.workers() <= 0) allocations.remove(entry.getKey());
            else allocations.put(entry.getKey(), new Allocation(allocation.companyId(), allocation.dimension(),
                    allocation.chunkX(), allocation.chunkZ(), machines, allocation.workers()));
            changed = true;
        }
        if (changed) setDirty();
    }

    /** Removes worker allocations that can no longer be backed by active contracts. */
    public void trimWorkerCount(String companyId, int maximum) {
        if (companyId == null || maximum < 0) return;
        int remaining = maximum;
        boolean changed = false;
        for (Map.Entry<String, Allocation> entry : new ArrayList<>(allocations.entrySet())) {
            Allocation allocation = entry.getValue();
            if (!companyId.equals(allocation.companyId()) || allocation.workers() <= 0) continue;
            int kept = Math.min(allocation.workers(), remaining);
            remaining -= kept;
            if (kept == allocation.workers()) continue;
            if (allocation.machines().isEmpty() && kept <= 0) allocations.remove(entry.getKey());
            else allocations.put(entry.getKey(), new Allocation(allocation.companyId(), allocation.dimension(),
                    allocation.chunkX(), allocation.chunkZ(), allocation.machines(), kept));
            changed = true;
        }
        if (changed) setDirty();
    }

    public List<Allocation> forCompany(String companyId) {
        if (companyId == null || companyId.isBlank()) return List.of();
        return allocations.values().stream().filter(value -> companyId.equals(value.companyId())).toList();
    }

    public boolean setMachineCount(String companyId, CompanySiteSavedData.Site site, String machineType,
                                   int count, int installedCount) {
        if (companyId == null || companyId.isBlank() || site == null || machineType == null
                || machineType.isBlank() || count < 0 || count > installedCount) return false;
        int otherSites = totalMachineAllocation(companyId, machineType)
                - Math.max(0, machineCountAtSite(companyId, site, machineType));
        if (otherSites > installedCount - count) return false;
        Allocation current = get(companyId, site);
        Map<String, Integer> machines = new HashMap<>(current == null ? Map.of() : current.machines());
        if (count == 0) machines.remove(machineType); else machines.put(machineType, count);
        put(new Allocation(companyId, site.dimension(), site.chunkX(), site.chunkZ(), machines,
                current == null ? 0 : current.workers()));
        return true;
    }

    public boolean setWorkerCount(String companyId, CompanySiteSavedData.Site site,
                                  int count, int activeWorkers) {
        if (companyId == null || companyId.isBlank() || site == null || count < 0 || count > activeWorkers) {
            return false;
        }
        Allocation current = get(companyId, site);
        int otherSites = totalWorkerAllocation(companyId) - (current == null ? 0 : current.workers());
        if (otherSites > activeWorkers - count) return false;
        put(new Allocation(companyId, site.dimension(), site.chunkX(), site.chunkZ(),
                current == null ? Map.of() : current.machines(), count));
        return true;
    }

    private int machineCountAtSite(String companyId, CompanySiteSavedData.Site site, String machineType) {
        Allocation current = get(companyId, site);
        return current == null ? 0 : current.machines().getOrDefault(machineType, 0);
    }

    private void put(Allocation allocation) {
        String id = key(allocation.companyId(), new CompanySiteSavedData.Site(
                allocation.companyId(), allocation.dimension(), allocation.chunkX(), allocation.chunkZ()));
        if (allocation.empty()) allocations.remove(id); else allocations.put(id, allocation);
        setDirty();
    }

    public void removeAt(String companyId, String dimension, int chunkX, int chunkZ) {
        if (companyId == null || dimension == null) return;
        String id = key(companyId, new CompanySiteSavedData.Site(companyId, dimension, chunkX, chunkZ));
        if (allocations.remove(id) != null) setDirty();
    }

    public void remove(String companyId) {
        if (companyId == null || companyId.isBlank()) return;
        if (allocations.entrySet().removeIf(entry -> companyId.equals(entry.getValue().companyId()))) {
            setDirty();
        }
    }

    public void transferCompany(String sourceId, String targetId) {
        transferCompany(sourceId, targetId, List.of());
    }

    /** Transfers only allocations whose sites survived the target's site limit. */
    public void transferCompany(String sourceId, String targetId, List<CompanySiteSavedData.Site> allowedSites) {
        if (sourceId == null || targetId == null || sourceId.isBlank() || targetId.isBlank()
                || sourceId.equals(targetId)) return;
        List<Allocation> source = new ArrayList<>();
        allocations.values().removeIf(allocation -> {
            if (!sourceId.equals(allocation.companyId())) return false;
            source.add(allocation);
            return true;
        });
        for (Allocation incoming : source) {
            CompanySiteSavedData.Site site = new CompanySiteSavedData.Site(targetId, incoming.dimension(),
                    incoming.chunkX(), incoming.chunkZ());
            if (!allowedSites.isEmpty() && allowedSites.stream().noneMatch(existing ->
                    existing.dimension().equals(site.dimension()) && existing.chunkX() == site.chunkX()
                            && existing.chunkZ() == site.chunkZ())) {
                continue;
            }
            Allocation existing = get(targetId, site);
            Map<String, Integer> machines = new HashMap<>(existing == null ? Map.of() : existing.machines());
            incoming.machines().forEach((type, count) -> machines.merge(type, count,
                    (left, right) -> left > Integer.MAX_VALUE - right ? Integer.MAX_VALUE : left + right));
            long workers = (long) (existing == null ? 0 : existing.workers()) + incoming.workers();
            put(new Allocation(targetId, site.dimension(), site.chunkX(), site.chunkZ(), machines,
                    (int) Math.min(Integer.MAX_VALUE, workers)));
        }
        if (!source.isEmpty()) setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        State.CODEC.encodeStart(NbtOps.INSTANCE, new State(allocations)).result()
                .ifPresent(encoded -> tag.put("data", encoded));
        return tag;
    }

    public static CompanySiteAllocationSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        CompanySiteAllocationSavedData data = new CompanySiteAllocationSavedData();
        if (tag.contains("data")) {
            State.CODEC.parse(NbtOps.INSTANCE, tag.get("data")).result()
                    .ifPresent(state -> data.allocations.putAll(state.allocations()));
        }
        return data;
    }
}
