package com.ailudick.capitalismmod.population;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

/** World-persisted resident households. It intentionally stores simulation state, not entities. */
public final class PopulationSavedData extends SavedData {
    private static final String ID = "capitalismmod_population";
    private static final int MAX_HOUSEHOLDS = 16384;
    private final List<Household> households = new ArrayList<>();
    private PopulationSavedData() {}
    public static PopulationSavedData get(MinecraftServer server) { return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(PopulationSavedData::new, PopulationSavedData::load), ID); }
    public List<Household> households() { return List.copyOf(households); }
    public Household find(String id) { return households.stream().filter(h -> h.id().equals(id)).findFirst().orElse(null); }
    public void upsert(Household household) { if (household == null) return; households.removeIf(h -> h.id().equals(household.id())); households.add(household); while (households.size() > MAX_HOUSEHOLDS) households.remove(0); setDirty(); }
    public int population(String region) { return households.stream().filter(h -> h.region().equals(region)).mapToInt(Household::size).sum(); }
    public long dailyDemand(String region) { return households.stream().filter(h -> h.region().equals(region)).mapToLong(h -> h.dailyNeedMinor() * (long) h.size()).reduce(0L, PopulationSavedData::add); }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) { ListTag list = new ListTag(); for (Household h : households) { CompoundTag e = new CompoundTag(); e.putString("id",h.id()); e.putString("region",h.region()); e.putInt("size",h.size()); e.putInt("workingAge",h.workingAge()); e.putLong("cash",h.cashMinor()); e.putLong("need",h.dailyNeedMinor()); e.putInt("satisfaction",h.satisfaction()); e.putLong("day",h.lastSettlementDay()); list.add(e); } tag.put("households",list); return tag; }
    public static PopulationSavedData load(CompoundTag tag, HolderLookup.Provider registries) { PopulationSavedData data=new PopulationSavedData(); ListTag list=tag.getList("households", Tag.TAG_COMPOUND); for(int i=Math.max(0,list.size()-MAX_HOUSEHOLDS);i<list.size();i++){CompoundTag e=list.getCompound(i); try{data.households.add(new Household(e.getString("id"),e.getString("region"),e.getInt("size"),e.getInt("workingAge"),Math.max(0L,e.getLong("cash")),Math.max(0L,e.getLong("need")),e.getInt("satisfaction"),e.getLong("day")));}catch(IllegalArgumentException ignored){}} return data; }
    private static long add(long a,long b){try{return Math.addExact(a,b);}catch(ArithmeticException e){return Long.MAX_VALUE;}}
}
