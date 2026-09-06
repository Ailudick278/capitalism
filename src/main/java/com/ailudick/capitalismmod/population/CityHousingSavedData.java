package com.ailudick.capitalismmod.population;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

/** Persists the administered base rent for each stable trade region. */
public final class CityHousingSavedData extends SavedData {
    private static final String ID = "capitalismmod_city_housing";
    private final Map<String, Long> baseRentPerResident = new HashMap<>();
    private final Map<String, String> landlordByRegion = new HashMap<>();

    private CityHousingSavedData() {}

    public static CityHousingSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(CityHousingSavedData::new, CityHousingSavedData::load), ID);
    }

    public long baseRent(String region) { return baseRentPerResident.getOrDefault(region, 0L); }
    public String landlord(String region) { return landlordByRegion.getOrDefault(region, "government"); }

    public boolean setBaseRent(String region, long rent) {
        if (region == null || region.isBlank() || rent < 0L || rent > 1_000_000_000L) return false;
        if (rent == 0L) baseRentPerResident.remove(region);
        else baseRentPerResident.put(region, rent);
        setDirty();
        return true;
    }

    public boolean setLandlord(String region, String landlordId) {
        if (region == null || region.isBlank() || landlordId == null || landlordId.isBlank()) return false;
        if ("government".equals(landlordId)) landlordByRegion.remove(region);
        else landlordByRegion.put(region, landlordId);
        setDirty(); return true;
    }

    public long dailyRent(String region, int residents, int housingUnits) {
        return HousingEconomics.dailyRent(baseRent(region), residents, housingUnits);
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        baseRentPerResident.forEach((region, rent) -> {
            CompoundTag entry = new CompoundTag(); entry.putString("region", region); entry.putLong("rent", rent); list.add(entry);
        });
        tag.put("rents", list);
        ListTag landlords = new ListTag();
        landlordByRegion.forEach((region, landlord) -> { CompoundTag entry = new CompoundTag();
            entry.putString("region", region); entry.putString("landlord", landlord); landlords.add(entry); });
        tag.put("landlords", landlords); return tag;
    }

    public static CityHousingSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        CityHousingSavedData data = new CityHousingSavedData();
        ListTag list = tag.getList("rents", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            String region = entry.getString("region"); long rent = entry.getLong("rent");
            if (!region.isBlank() && rent > 0L && rent <= 1_000_000_000L) data.baseRentPerResident.put(region, rent);
        }
        ListTag landlords = tag.getList("landlords", Tag.TAG_COMPOUND);
        for (int i = 0; i < landlords.size(); i++) { CompoundTag entry = landlords.getCompound(i);
            String region = entry.getString("region"), landlord = entry.getString("landlord");
            if (!region.isBlank() && !landlord.isBlank() && !"government".equals(landlord)) data.landlordByRegion.put(region, landlord);
        }
        return data;
    }
}
