package com.ailudick.capitalismmod.company;

import com.ailudick.capitalismmod.Config;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;

/** Persistent registered operating locations for companies. */
public final class CompanySiteSavedData extends SavedData {
    private static final String ID = "capitalismmod_company_sites";
    private final Map<String, List<Site>> sites = new HashMap<>();

    public record Site(String companyId, String dimension, int chunkX, int chunkZ) {}

    private CompanySiteSavedData() {}

    public static CompanySiteSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(CompanySiteSavedData::new, CompanySiteSavedData::load), ID);
    }

    public Site get(String companyId) {
        List<Site> companySites = sites(companyId);
        return companySites.isEmpty() ? null : companySites.get(0);
    }

    public List<Site> sites(String companyId) {
        if (companyId == null || companyId.isBlank()) return List.of();
        return List.copyOf(sites.getOrDefault(companyId, List.of()));
    }

    public List<Site> sitesInDimension(String dimension) {
        if (dimension == null || dimension.isBlank()) return List.of();
        return sites.values().stream().flatMap(List::stream)
                .filter(site -> dimension.equals(site.dimension())).toList();
    }

    public boolean set(Site site) {
        if (site == null || site.companyId() == null || site.companyId().isBlank()
                || site.dimension() == null || site.dimension().isBlank()) return false;
        List<Site> companySites = new ArrayList<>(sites.getOrDefault(site.companyId(), List.of()));
        boolean alreadyRegistered = companySites.stream().anyMatch(existing -> existing.dimension().equals(site.dimension())
                && existing.chunkX() == site.chunkX() && existing.chunkZ() == site.chunkZ());
        if (!alreadyRegistered && companySites.size() >= Config.MAX_COMPANY_SITES.get()) return false;
        companySites.removeIf(existing -> existing.dimension().equals(site.dimension())
                && existing.chunkX() == site.chunkX() && existing.chunkZ() == site.chunkZ());
        companySites.add(site);
        sites.put(site.companyId(), companySites);
        setDirty();
        return true;
    }

    public void remove(String companyId) {
        if (companyId != null && sites.remove(companyId) != null) setDirty();
    }

    public boolean removeAt(String companyId, String dimension, int chunkX, int chunkZ) {
        if (companyId == null || dimension == null || dimension.isBlank()) return false;
        List<Site> companySites = sites.get(companyId);
        if (companySites == null) return false;
        boolean changed = companySites.removeIf(site -> dimension.equals(site.dimension())
                && site.chunkX() == chunkX && site.chunkZ() == chunkZ);
        if (!changed) return false;
        if (companySites.isEmpty()) sites.remove(companyId);
        setDirty();
        return true;
    }

    /** Transfers every registered operating site during a company merger. */
    public void transferCompany(String sourceId, String targetId) {
        if (sourceId == null || targetId == null || sourceId.isBlank()
                || targetId.isBlank() || sourceId.equals(targetId)) return;
        List<Site> source = sites.remove(sourceId);
        if (source == null || source.isEmpty()) return;
        List<Site> target = new ArrayList<>(sites.getOrDefault(targetId, List.of()));
        for (Site site : source) {
            Site rebound = new Site(targetId, site.dimension(), site.chunkX(), site.chunkZ());
            boolean duplicate = target.stream().anyMatch(existing -> existing.dimension().equals(rebound.dimension())
                    && existing.chunkX() == rebound.chunkX() && existing.chunkZ() == rebound.chunkZ());
            if (duplicate) continue;
            if (target.size() >= Config.MAX_COMPANY_SITES.get()) break;
            target.add(rebound);
        }
        sites.put(targetId, target);
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (List<Site> companySites : sites.values()) {
            for (Site site : companySites) {
                CompoundTag entry = new CompoundTag();
                entry.putString("company", site.companyId());
                entry.putString("dimension", site.dimension());
                entry.putInt("x", site.chunkX());
                entry.putInt("z", site.chunkZ());
                list.add(entry);
            }
        }
        tag.put("sites", list);
        return tag;
    }

    public static CompanySiteSavedData load(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        CompanySiteSavedData data = new CompanySiteSavedData();
        ListTag list = tag.getList("sites", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            String company = entry.getString("company");
            String dimension = entry.getString("dimension");
            if (!company.isBlank() && !dimension.isBlank()) {
                data.sites.computeIfAbsent(company, ignored -> new ArrayList<>())
                        .add(new Site(company, dimension, entry.getInt("x"), entry.getInt("z")));
            }
        }
        return data;
    }
}
