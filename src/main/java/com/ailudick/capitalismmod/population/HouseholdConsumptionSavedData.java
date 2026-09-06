package com.ailudick.capitalismmod.population;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

/** Durable virtual-consumer purchases made by simulated households. */
public final class HouseholdConsumptionSavedData extends SavedData {
    public record Consumption(String id, String householdId, long day, String category,
                              String itemId, long quantity, long unitPriceMinor, long totalCostMinor) {}
    private static final String ID = "capitalismmod_household_consumption";
    private static final int MAX_RECORDS = 32768;
    private final List<Consumption> records = new ArrayList<>();
    private HouseholdConsumptionSavedData() {}
    public static HouseholdConsumptionSavedData get(MinecraftServer server) { return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(HouseholdConsumptionSavedData::new, HouseholdConsumptionSavedData::load), ID); }
    public List<Consumption> records() { return List.copyOf(records); }
    public boolean record(Consumption consumption) { if (consumption == null || consumption.id().isBlank() || consumption.householdId().isBlank() || consumption.itemId().isBlank() || consumption.quantity() <= 0L || consumption.unitPriceMinor() <= 0L || consumption.totalCostMinor() <= 0L || records.stream().anyMatch(r -> r.id().equals(consumption.id()))) return false; records.add(consumption); while(records.size()>MAX_RECORDS) records.remove(0); setDirty(); return true; }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) { ListTag list=new ListTag(); for(Consumption c:records){CompoundTag e=new CompoundTag();e.putString("id",c.id());e.putString("household",c.householdId());e.putLong("day",c.day());e.putString("category",c.category());e.putString("item",c.itemId());e.putLong("quantity",c.quantity());e.putLong("unitPrice",c.unitPriceMinor());e.putLong("total",c.totalCostMinor());list.add(e);} tag.put("records",list); return tag; }
    public static HouseholdConsumptionSavedData load(CompoundTag tag, HolderLookup.Provider registries) { HouseholdConsumptionSavedData data=new HouseholdConsumptionSavedData(); ListTag list=tag.getList("records",Tag.TAG_COMPOUND); for(int i=Math.max(0,list.size()-MAX_RECORDS);i<list.size();i++){CompoundTag e=list.getCompound(i); if(!e.getString("id").isBlank()&&!e.getString("household").isBlank()&&!e.getString("item").isBlank()&&e.getLong("quantity")>0L&&e.getLong("unitPrice")>0L&&e.getLong("total")>0L)data.records.add(new Consumption(e.getString("id"),e.getString("household"),e.getLong("day"),e.getString("category"),e.getString("item"),e.getLong("quantity"),e.getLong("unitPrice"),e.getLong("total")));} return data; }
}
