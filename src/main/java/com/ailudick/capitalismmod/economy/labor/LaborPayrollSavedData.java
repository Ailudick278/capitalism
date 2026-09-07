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
    public record Payment(String source, String employmentId, String workerId, long day,
                          long amountMinor, String householdSource) {}
    private static final String ID = "capitalismmod_labor_payroll";
    private final Map<String, Account> accounts = new HashMap<>();
    private final Map<String, Payment> payments = new HashMap<>();
    private LaborPayrollSavedData() {}
    public static LaborPayrollSavedData get(MinecraftServer server) { return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(LaborPayrollSavedData::new, LaborPayrollSavedData::load), ID); }
    public Account account(String id) { return accounts.getOrDefault(id, new Account(0L, -1L)); }
    public Map<String, Account> accounts() { return Map.copyOf(accounts); }
    public Map<String, Payment> payments() { return Map.copyOf(payments); }
    public void recordPayment(Payment payment) {
        if (payment == null || payment.source() == null || payment.source().isBlank()
                || payment.amountMinor() <= 0L) return;
        payments.putIfAbsent(payment.source(), payment);
        setDirty();
    }
    public void settle(String id, long day, long unpaid) { accounts.put(id, new Account(Math.max(0L, unpaid), day)); setDirty(); }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        accounts.forEach((id, a) -> { CompoundTag e = new CompoundTag(); e.putString("id", id); e.putLong("unpaid", a.unpaid()); e.putLong("day", a.lastSettlementDay()); list.add(e); });
        tag.put("accounts", list);
        ListTag paymentList = new ListTag();
        payments.values().forEach(payment -> { CompoundTag e = new CompoundTag(); e.putString("source", payment.source()); e.putString("employment", payment.employmentId()); e.putString("worker", payment.workerId()); e.putLong("day", payment.day()); e.putLong("amount", payment.amountMinor()); e.putString("householdSource", payment.householdSource()); paymentList.add(e); });
        tag.put("payments", paymentList);
        return tag;
    }
    public static LaborPayrollSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        LaborPayrollSavedData data = new LaborPayrollSavedData();
        ListTag list = tag.getList("accounts", Tag.TAG_COMPOUND);
        for (int i=0;i<list.size();i++) { CompoundTag e=list.getCompound(i); if (!e.getString("id").isBlank()) data.accounts.put(e.getString("id"), new Account(Math.max(0L,e.getLong("unpaid")), e.getLong("day"))); }
        ListTag paymentList = tag.getList("payments", Tag.TAG_COMPOUND);
        for (int i=0;i<paymentList.size();i++) { CompoundTag e = paymentList.getCompound(i); String source = e.getString("source"); if (!source.isBlank() && e.getLong("amount") > 0L) data.payments.put(source, new Payment(source, e.getString("employment"), e.getString("worker"), e.getLong("day"), e.getLong("amount"), e.getString("householdSource"))); }
        return data;
    }
}
