package com.ailudick.capitalismmod.population;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

/** World-persisted resident households. It intentionally stores simulation state, not entities. */
public final class PopulationSavedData extends SavedData {
    private static final String ID = "capitalismmod_population";
    private static final int MAX_HOUSEHOLDS = 16384;
    private final List<Household> households = new ArrayList<>();
    private final Set<String> creditedSources = new HashSet<>();
    private PopulationSavedData() {}
    public static PopulationSavedData get(MinecraftServer server) { return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(PopulationSavedData::new, PopulationSavedData::load), ID); }
    public List<Household> households() { return List.copyOf(households); }
    public Household find(String id) { return households.stream().filter(h -> h.id().equals(id)).findFirst().orElse(null); }
    public void upsert(Household household) { if (household == null) return; households.removeIf(h -> h.id().equals(household.id())); households.add(household); while (households.size() > MAX_HOUSEHOLDS) households.remove(0); setDirty(); }
    public boolean addCash(String id, long amount) { Household h = find(id); if (h == null || amount <= 0L) return false; upsert(h.withCash(add(h.cashMinor(), amount))); return true; }
    public boolean addCashOnce(String id, long amount, String source) { if (source == null || source.isBlank() || creditedSources.contains(source)) return false; if (!addCash(id, amount)) return false; creditedSources.add(source); while (creditedSources.size() > 8192) creditedSources.remove(creditedSources.iterator().next()); setDirty(); return true; }
    public boolean move(String id, String region, long day) { Household h = find(id); if (h == null || region == null || region.isBlank() || h.region().equals(region)) return false; upsert(h.withRegion(region, day)); return true; }
    public int population(String region) { return households.stream().filter(h -> h.region().equals(region)).mapToInt(Household::size).sum(); }
    public long dailyDemand(String region) { return households.stream().filter(h -> h.region().equals(region)).mapToLong(h -> h.dailyNeedMinor() * (long) h.size()).reduce(0L, PopulationSavedData::add); }
    /** Returns simulated daily unit demand for essential commodities at a major-unit price. */
    public long demandUnits(String itemId, long priceMajor) {
        if (itemId == null || itemId.isBlank() || !isEssential(itemId)) return 0L;
        long unitCostMinor = Math.max(1L, Math.min(Long.MAX_VALUE / 100L, Math.max(1L, priceMajor)) * 100L);
        long units = 0L;
        for (Household household : households) {
            long affordable = Math.max(0L, household.dailyNeedMinor() / unitCostMinor);
            long weighted = affordable * (long) household.size() * household.satisfaction() / 100L;
            units = add(units, weighted);
        }
        return Math.min(1_000_000L, units);
    }
    private static boolean isEssential(String itemId) {
        String id = itemId.toLowerCase(java.util.Locale.ROOT);
        return id.contains("bread") || id.contains("potato") || id.contains("carrot") || id.contains("apple")
                || id.contains("wheat") || id.contains("beef") || id.contains("pork") || id.contains("coal")
                || id.contains("torch") || id.contains("planks");
    }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) { ListTag list = new ListTag(); for (Household h : households) { CompoundTag e = new CompoundTag(); e.putString("id",h.id()); e.putString("region",h.region()); e.putInt("size",h.size()); e.putInt("workingAge",h.workingAge()); e.putLong("cash",h.cashMinor()); e.putLong("need",h.dailyNeedMinor()); e.putInt("satisfaction",h.satisfaction()); e.putLong("day",h.lastSettlementDay()); e.putLong("migrationDay",h.lastMigrationDay()); e.putInt("health",h.health()); e.putInt("education",h.education()); e.putInt("unemploymentDays",h.unemploymentDays()); e.putInt("employmentDays",h.employmentDays()); list.add(e); } tag.put("households",list); ListTag sources = new ListTag(); for (String source : creditedSources) { CompoundTag e = new CompoundTag(); e.putString("source", source); sources.add(e); } tag.put("creditedSources", sources); return tag; }
    public static PopulationSavedData load(CompoundTag tag, HolderLookup.Provider registries) { PopulationSavedData data=new PopulationSavedData(); ListTag list=tag.getList("households", Tag.TAG_COMPOUND); for(int i=Math.max(0,list.size()-MAX_HOUSEHOLDS);i<list.size();i++){CompoundTag e=list.getCompound(i); try{data.households.add(new Household(e.getString("id"),e.getString("region"),e.getInt("size"),e.getInt("workingAge"),Math.max(0L,e.getLong("cash")),Math.max(0L,e.getLong("need")),e.getInt("satisfaction"),e.getLong("day"),e.getLong("migrationDay"),e.contains("health") ? e.getInt("health") : 70,e.contains("education") ? e.getInt("education") : 0,e.contains("unemploymentDays") ? Math.max(0,e.getInt("unemploymentDays")) : 0,e.contains("employmentDays") ? Math.max(0,e.getInt("employmentDays")) : 0));}catch(IllegalArgumentException ignored){}} ListTag sources=tag.getList("creditedSources",Tag.TAG_COMPOUND); for(int i=0;i<sources.size();i++){String source=sources.getCompound(i).getString("source"); if(!source.isBlank()) data.creditedSources.add(source);} return data; }
    private static long add(long a,long b){try{return Math.addExact(a,b);}catch(ArithmeticException e){return Long.MAX_VALUE;}}
}
