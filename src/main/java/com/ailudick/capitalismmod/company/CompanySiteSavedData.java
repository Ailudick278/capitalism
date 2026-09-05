package com.ailudick.capitalismmod.company;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

/** Persistent registered operating locations for companies. */
public final class CompanySiteSavedData extends SavedData {
    private static final String ID = "capitalismmod_company_sites";
    private final Map<String, Site> sites = new HashMap<>();

    public record Site(String companyId, String dimension, int chunkX, int chunkZ) {}

    private CompanySiteSavedData() {}

    public static CompanySiteSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(CompanySiteSavedData::new, CompanySiteSavedData::load), ID);
    }

    public Site get(String companyId) {
        return companyId == null ? null : sites.get(companyId);
    }

    public void set(Site site) {
        if (site == null || site.companyId() == null || site.companyId().isBlank()
                || site.dimension() == null || site.dimension().isBlank()) return;
        sites.put(site.companyId(), site);
        setDirty();
    }

    public void remove(String companyId) {
        if (companyId != null && sites.remove(companyId) != null) setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Site site : sites.values()) {
            CompoundTag entry = new CompoundTag();
            entry.putString("company", site.companyId());
            entry.putString("dimension", site.dimension());
            entry.putInt("x", site.chunkX());
            entry.putInt("z", site.chunkZ());
            list.add(entry);
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
                data.sites.put(company, new Site(company, dimension, entry.getInt("x"), entry.getInt("z")));
            }
        }
        return data;
    }
}
