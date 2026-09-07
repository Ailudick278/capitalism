package com.ailudick.capitalismmod.population;

import com.ailudick.capitalismmod.Config;
import com.ailudick.capitalismmod.currency.Currencies;
import com.ailudick.capitalismmod.currency.ExchangeRates;
import com.ailudick.capitalismmod.currency.Money;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

/** World-persisted resident households. It intentionally stores simulation state, not entities. */
public final class PopulationSavedData extends SavedData {
    private static final String ID = "capitalismmod_population";
    private static final int MAX_HOUSEHOLDS = 16384;
    private static final int MAX_MIGRATIONS = 16384;
    private final List<Household> households = new ArrayList<>();
    private final List<Migration> migrations = new ArrayList<>();
    private final Set<String> creditedSources = new HashSet<>();
    private final Map<String, String> chargedSources = new HashMap<>();
    public record Migration(String id, long day, String householdId, String fromRegion,
                            String toRegion, long costMinor, long cashAfter) {}
    private PopulationSavedData() {}
    public static PopulationSavedData get(MinecraftServer server) { return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(PopulationSavedData::new, PopulationSavedData::load), ID); }
    public List<Household> households() { return List.copyOf(households); }
    public List<Migration> migrations() { return List.copyOf(migrations); }
    public Household find(String id) { return households.stream().filter(h -> h.id().equals(id)).findFirst().orElse(null); }
    public void upsert(Household household) { if (household == null) return; households.removeIf(h -> h.id().equals(household.id())); households.add(household); while (households.size() > MAX_HOUSEHOLDS) households.remove(0); setDirty(); }
    public boolean addCash(String id, long amount) { Household h = find(id); if (h == null || amount <= 0L) return false; upsert(h.withCash(add(h.cashMinor(), amount))); return true; }
    public boolean hasCreditedSource(String source) { return source != null && creditedSources.contains(source); }
    public boolean addCashOnce(String id, long amount, String source) { if (source == null || source.isBlank() || creditedSources.contains(source)) return false; if (!addCash(id, amount)) return false; creditedSources.add(source); while (creditedSources.size() > 8192) creditedSources.remove(creditedSources.iterator().next()); setDirty(); return true; }
    /** Charges a household once and remembers which household funded the purchase for recovery. */
    public boolean chargeCashOnce(String id, long amount, String source) {
        if (id == null || id.isBlank() || amount <= 0L || source == null || source.isBlank()) return false;
        if (chargedSources.containsKey(source)) return chargedSources.get(source).equals(id);
        Household household = find(id);
        if (household == null || household.cashMinor() < amount) return false;
        upsert(household.withCash(household.cashMinor() - amount));
        chargedSources.put(source, id);
        while (chargedSources.size() > 8192) chargedSources.remove(chargedSources.keySet().iterator().next());
        setDirty();
        return true;
    }
    public String chargedHousehold(String source) { return source == null ? null : chargedSources.get(source); }
    public boolean move(String id, String region, long day) { Household h = find(id); if (h == null || region == null || region.isBlank() || h.region().equals(region)) return false; upsert(h.withRegion(region, day)); return true; }
    /** Charges a one-time relocation cost and changes region atomically. */
    public boolean migrate(String id, String region, long day, long cost) {
        Household h = find(id);
        if (h == null || region == null || region.isBlank() || h.region().equals(region)
                || cost < 0L || h.cashMinor() < cost) return false;
        Household moved = h.withCash(h.cashMinor() - cost).withRegion(region, day);
        upsert(moved);
        String migrationId = migrationId(id, day, region);
        migrations.removeIf(migration -> migration.id().equals(migrationId));
        migrations.add(new Migration(migrationId, day, id, h.region(), region, cost, moved.cashMinor()));
        while (migrations.size() > MAX_MIGRATIONS) migrations.remove(0);
        setDirty();
        return true;
    }
    /** Reverts the latest migration when the dependent employment transaction fails. */
    public boolean rollbackMigration(String id, String fromRegion, String toRegion, long day, long cost) {
        Household current = find(id);
        String migrationId = migrationId(id, day, toRegion);
        if (current == null || fromRegion == null || fromRegion.isBlank() || toRegion == null
                || toRegion.isBlank() || current.region().equals(fromRegion)
                || !current.region().equals(toRegion) || cost < 0L
                || current.cashMinor() > Long.MAX_VALUE - cost) return false;
        upsert(current.withCash(current.cashMinor() + cost).withRegion(fromRegion, day));
        boolean removed = migrations.removeIf(migration -> migration.id().equals(migrationId));
        if (removed) setDirty();
        return removed;
    }
    public boolean remove(String id) { if (id == null || id.isBlank()) return false; boolean removed = households.removeIf(h -> h.id().equals(id)); if (removed) setDirty(); return removed; }
    public boolean merge(String sourceId, String targetId) {
        Household source = find(sourceId), target = find(targetId);
        if (source == null || target == null || source.id().equals(target.id())
                || !source.id().startsWith("npc-") || !target.id().startsWith("npc-")) return false;
        Household merged = target.mergeWith(source);
        if (merged == null) return false;
        remove(source.id()); upsert(merged); return true;
    }
    public int population(String region) { return households.stream().filter(h -> h.region().equals(region)).mapToInt(Household::size).sum(); }
    public long dailyDemand(String region) { return households.stream().filter(h -> h.region().equals(region)).mapToLong(h -> h.dailyNeedMinor() * (long) h.size()).reduce(0L, PopulationSavedData::add); }
    /** Returns simulated daily unit demand for essential commodities at a major-unit price. */
    public long demandUnits(String itemId, long priceMajor) {
        if (itemId == null || itemId.isBlank() || !isEssential(itemId)) return 0L;
        long unitCostMinor = Math.max(1L, ExchangeRates.convert(Money.toMinorSaturated(Math.max(1L, priceMajor)),
                Currencies.USD, Config.defaultCurrency()));
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
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) { ListTag list = new ListTag(); for (Household h : households) { CompoundTag e = new CompoundTag(); e.putString("id",h.id()); e.putString("region",h.region()); e.putInt("size",h.size()); e.putInt("workingAge",h.workingAge()); e.putLong("cash",h.cashMinor()); e.putLong("need",h.dailyNeedMinor()); e.putInt("satisfaction",h.satisfaction()); e.putLong("day",h.lastSettlementDay()); e.putLong("migrationDay",h.lastMigrationDay()); e.putInt("health",h.health()); e.putInt("education",h.education()); e.putInt("unemploymentDays",h.unemploymentDays()); e.putInt("employmentDays",h.employmentDays()); e.putInt("averageAge",h.averageAge()); e.putInt("children",h.children()); e.putInt("elderly",h.elderly()); list.add(e); } tag.put("households",list); ListTag migrationList = new ListTag(); for (Migration migration : migrations) { CompoundTag e = new CompoundTag(); e.putString("id", migration.id()); e.putLong("day", migration.day()); e.putString("household", migration.householdId()); e.putString("from", migration.fromRegion()); e.putString("to", migration.toRegion()); e.putLong("cost", migration.costMinor()); e.putLong("cashAfter", migration.cashAfter()); migrationList.add(e); } tag.put("migrations", migrationList); ListTag sources = new ListTag(); for (String source : creditedSources) { CompoundTag e = new CompoundTag(); e.putString("source", source); sources.add(e); } tag.put("creditedSources", sources); ListTag charges = new ListTag(); chargedSources.forEach((source, household) -> { CompoundTag e = new CompoundTag(); e.putString("source", source); e.putString("household", household); charges.add(e); }); tag.put("chargedSources", charges); return tag; }
    public static PopulationSavedData load(CompoundTag tag, HolderLookup.Provider registries) { PopulationSavedData data=new PopulationSavedData(); ListTag list=tag.getList("households", Tag.TAG_COMPOUND); for(int i=Math.max(0,list.size()-MAX_HOUSEHOLDS);i<list.size();i++){CompoundTag e=list.getCompound(i); try{int size=e.getInt("size"), working=e.getInt("workingAge"); int children=e.contains("children") ? Math.max(0,e.getInt("children")) : Math.max(0,size-working); int elderly=e.contains("elderly") ? Math.max(0,e.getInt("elderly")) : 0; if(children+working+elderly != size){ children=Math.max(0,size-working); elderly=0; } data.households.add(new Household(e.getString("id"),e.getString("region"),size,working,Math.max(0L,e.getLong("cash")),Math.max(0L,e.getLong("need")),e.getInt("satisfaction"),e.getLong("day"),e.getLong("migrationDay"),e.contains("health") ? e.getInt("health") : 70,e.contains("education") ? e.getInt("education") : 0,e.contains("unemploymentDays") ? Math.max(0,e.getInt("unemploymentDays")) : 0,e.contains("employmentDays") ? Math.max(0,e.getInt("employmentDays")) : 0,e.contains("averageAge") ? Math.max(0,Math.min(120,e.getInt("averageAge"))) : 30,children,elderly));}catch(IllegalArgumentException ignored){}} ListTag migrationList=tag.getList("migrations",Tag.TAG_COMPOUND); for(int i=Math.max(0,migrationList.size()-MAX_MIGRATIONS);i<migrationList.size();i++){CompoundTag e=migrationList.getCompound(i); if(!e.getString("id").isBlank() && !e.getString("household").isBlank() && !e.getString("from").isBlank() && !e.getString("to").isBlank() && e.getLong("day") >= 0L && e.getLong("cost") >= 0L && e.getLong("cashAfter") >= 0L) data.migrations.add(new Migration(e.getString("id"),e.getLong("day"),e.getString("household"),e.getString("from"),e.getString("to"),e.getLong("cost"),e.getLong("cashAfter")));} ListTag sources=tag.getList("creditedSources",Tag.TAG_COMPOUND); for(int i=0;i<sources.size();i++){String source=sources.getCompound(i).getString("source"); if(!source.isBlank()) data.creditedSources.add(source);} ListTag charges=tag.getList("chargedSources",Tag.TAG_COMPOUND); for(int i=0;i<charges.size();i++){CompoundTag e=charges.getCompound(i); String source=e.getString("source"), household=e.getString("household"); if(!source.isBlank() && !household.isBlank()) data.chargedSources.put(source, household);} return data; }
    private static String migrationId(String householdId, long day, String region) { return "migration:" + householdId + ":" + day + ":" + region; }
    private static long add(long a,long b){try{return Math.addExact(a,b);}catch(ArithmeticException e){return Long.MAX_VALUE;}}
}
