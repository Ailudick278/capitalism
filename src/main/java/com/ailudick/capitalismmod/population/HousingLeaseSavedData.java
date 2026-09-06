package com.ailudick.capitalismmod.population;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

/** Persistent public-housing leases, arrears, and idempotent rent payments. */
public final class HousingLeaseSavedData extends SavedData {
    private static final String ID = "capitalismmod_housing_leases";
    private static final int MAX_PAYMENTS = 16384;
    private final List<Lease> leases = new ArrayList<>();
    private final List<Payment> payments = new ArrayList<>();

    public record Lease(String householdId, String region, long dailyRentMinor,
                        long arrearsMinor, long lastPaymentDay) {}
    public record Payment(String id, long day, String householdId, String region,
                          long dueMinor, long paidMinor, long arrearsAfter) {}

    private HousingLeaseSavedData() {}

    public static HousingLeaseSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(HousingLeaseSavedData::new, HousingLeaseSavedData::load), ID);
    }

    public List<Lease> leases() { return List.copyOf(leases); }
    public List<Payment> payments() { return List.copyOf(payments); }
    public Lease lease(String householdId) { return leases.stream()
            .filter(l -> l.householdId().equals(householdId)).findFirst().orElse(null); }
    public Payment payment(String id) { return payments.stream()
            .filter(p -> p.id().equals(id)).findFirst().orElse(null); }

    /** Records one rent attempt and returns the existing result when retried. */
    public Payment settleRent(String householdId, String region, long day,
                              long dailyRentMinor, long dueMinor, long availableMinor) {
        String paymentId = "rent:" + householdId + ":" + day;
        Payment existing = payment(paymentId);
        if (existing != null) return existing;
        Lease previous = lease(householdId);
        long priorArrears = previous == null ? 0L : previous.arrearsMinor();
        long paid = Math.max(0L, Math.min(Math.max(0L, availableMinor), Math.max(0L, dueMinor)));
        long unpaid = dueMinor > paid ? dueMinor - paid : 0L;
        long arrears = add(priorArrears, unpaid);
        Payment result = new Payment(paymentId, day, householdId, region,
                Math.max(0L, dueMinor), paid, arrears);
        payments.add(result);
        while (payments.size() > MAX_PAYMENTS) payments.remove(0);
        leases.removeIf(l -> l.householdId().equals(householdId));
        leases.add(new Lease(householdId, region, Math.max(0L, dailyRentMinor), arrears, paid > 0L ? day :
                (previous == null ? -1L : previous.lastPaymentDay())));
        setDirty();
        return result;
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag leaseList = new ListTag();
        for (Lease l : leases) { CompoundTag e = new CompoundTag(); e.putString("household", l.householdId());
            e.putString("region", l.region()); e.putLong("rent", l.dailyRentMinor());
            e.putLong("arrears", l.arrearsMinor()); e.putLong("lastPayment", l.lastPaymentDay()); leaseList.add(e); }
        tag.put("leases", leaseList);
        ListTag paymentList = new ListTag();
        for (Payment p : payments) { CompoundTag e = new CompoundTag(); e.putString("id", p.id()); e.putLong("day", p.day());
            e.putString("household", p.householdId()); e.putString("region", p.region()); e.putLong("due", p.dueMinor());
            e.putLong("paid", p.paidMinor()); e.putLong("arrears", p.arrearsAfter()); paymentList.add(e); }
        tag.put("payments", paymentList); return tag;
    }

    public static HousingLeaseSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        HousingLeaseSavedData data = new HousingLeaseSavedData();
        ListTag ls = tag.getList("leases", Tag.TAG_COMPOUND);
        for (int i = 0; i < ls.size(); i++) { CompoundTag e = ls.getCompound(i);
            if (!e.getString("household").isBlank() && !e.getString("region").isBlank()) data.leases.add(new Lease(
                    e.getString("household"), e.getString("region"), Math.max(0L, e.getLong("rent")),
                    Math.max(0L, e.getLong("arrears")), e.getLong("lastPayment"))); }
        ListTag ps = tag.getList("payments", Tag.TAG_COMPOUND);
        for (int i = Math.max(0, ps.size() - MAX_PAYMENTS); i < ps.size(); i++) { CompoundTag e = ps.getCompound(i);
            if (!e.getString("id").isBlank() && !e.getString("household").isBlank()) data.payments.add(new Payment(
                    e.getString("id"), e.getLong("day"), e.getString("household"), e.getString("region"),
                    Math.max(0L, e.getLong("due")), Math.max(0L, e.getLong("paid")), Math.max(0L, e.getLong("arrears")))); }
        return data;
    }

    private static long add(long left, long right) {
        try { return Math.addExact(left, right); } catch (ArithmeticException e) { return Long.MAX_VALUE; }
    }
}
