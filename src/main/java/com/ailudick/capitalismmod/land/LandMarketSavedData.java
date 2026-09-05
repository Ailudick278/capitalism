package com.ailudick.capitalismmod.land;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.ArrayList;
import java.util.List;

public final class LandMarketSavedData extends SavedData {
    private static final String ID = "capitalismmod_land_market";
    private static final int MAX_TRANSACTIONS = 1024;
    private final List<Transaction> transactions = new ArrayList<>();
    public record Transaction(long time, String dimension, int chunkX, int chunkZ, String purpose, long price) {}
    private LandMarketSavedData() {}
    public static LandMarketSavedData get(MinecraftServer server) { return server.overworld().getDataStorage().computeIfAbsent(
            new Factory<>(LandMarketSavedData::new, LandMarketSavedData::load), ID); }
    public void record(Transaction transaction) { transactions.add(transaction); while (transactions.size() > MAX_TRANSACTIONS) transactions.remove(0); setDirty(); }
    public long count(String dimension) { return transactions.stream().filter(t -> t.dimension().equals(dimension)).count(); }
    public long average(String dimension) { long[] values = transactions.stream().filter(t -> t.dimension().equals(dimension)).mapToLong(Transaction::price).toArray();
        if (values.length == 0) return 0L; long total = 0L; for (long value : values) total = total > Long.MAX_VALUE - value ? Long.MAX_VALUE : total + value; return total / values.length; }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) { ListTag list = new ListTag();
        for (Transaction t : transactions) { CompoundTag n = new CompoundTag(); n.putLong("time", t.time()); n.putString("dimension", t.dimension()); n.putInt("x", t.chunkX()); n.putInt("z", t.chunkZ()); n.putString("purpose", t.purpose()); n.putLong("price", t.price()); list.add(n); } tag.put("transactions", list); return tag; }
    public static LandMarketSavedData load(CompoundTag tag, HolderLookup.Provider registries) { LandMarketSavedData data = new LandMarketSavedData(); ListTag list = tag.getList("transactions", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) { CompoundTag n = list.getCompound(i); data.transactions.add(new Transaction(n.getLong("time"), n.getString("dimension"), n.getInt("x"), n.getInt("z"), n.getString("purpose"), n.getLong("price"))); } while (data.transactions.size() > MAX_TRANSACTIONS) data.transactions.remove(0); return data; }
}
