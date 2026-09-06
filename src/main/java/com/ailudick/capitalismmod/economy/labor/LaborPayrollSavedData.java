package com.ailudick.capitalismmod.economy.labor;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

/** Crash-safe per-employment payroll marker and unpaid wage liability. */
public final class LaborPayrollSavedData extends SavedData {
    public record Account(long unpaid, long lastSettlementDay) {}
    private static final String ID = "capitalismmod_labor_payroll";
    private final Map<String, Account> accounts = new HashMap<>();
    private LaborPayrollSavedData() {}
    public static LaborPayrollSavedData get(MinecraftServer server) { return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(LaborPayrollSavedData::new, LaborPayrollSavedData::load), ID); }
    public Account account(String id) { return accounts.getOrDefault(id, new Account(0L, -1L)); }
    public Map<String, Account> accounts() { return Map.copyOf(accounts); }
    public void settle(String id, long day, long unpaid) { accounts.put(id, new Account(Math.max(0L, unpaid), day)); setDirty(); }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) { ListTag list = new ListTag(); accounts.forEach((id, a) -> { CompoundTag e = new CompoundTag(); e.putString("id", id); e.putLong("unpaid", a.unpaid()); e.putLong("day", a.lastSettlementDay()); list.add(e); }); tag.put("accounts", list); return tag; }
    public static LaborPayrollSavedData load(CompoundTag tag, HolderLookup.Provider registries) { LaborPayrollSavedData data = new LaborPayrollSavedData(); ListTag list = tag.getList("accounts", Tag.TAG_COMPOUND); for (int i=0;i<list.size();i++) { CompoundTag e=list.getCompound(i); if (!e.getString("id").isBlank()) data.accounts.put(e.getString("id"), new Account(Math.max(0L,e.getLong("unpaid")), e.getLong("day"))); } return data; }
}
