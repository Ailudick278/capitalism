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
    private static final int MAX_TERMINATIONS = 4096;
    private final List<Lease> leases = new ArrayList<>();
    private final List<Payment> payments = new ArrayList<>();
    private final List<Termination> terminations = new ArrayList<>();

    public record Lease(String householdId, String region, long dailyRentMinor,
                        long arrearsMinor, long lastPaymentDay, int missedDays, long noticeDay,
                        long depositDueMinor, long depositHeldMinor) {
        public String status() {
            if (missedDays >= 90) return "termination_eligible";
            if (missedDays >= 30) return "notice";
            if (missedDays > 0) return "grace";
            return "current";
        }
    }
    public record Payment(String id, long day, String householdId, String region,
                          long dueMinor, long paidMinor, long rentPaidMinor,
                          long depositPaidMinor, long arrearsAfter) {}
    public record Termination(String id, long day, String householdId, String region,
                              long depositReleasedMinor, long residualArrearsMinor, String reason) {}

    private HousingLeaseSavedData() {}

    public static HousingLeaseSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(HousingLeaseSavedData::new, HousingLeaseSavedData::load), ID);
    }

    public List<Lease> leases() { return List.copyOf(leases); }
    public List<Payment> payments() { return List.copyOf(payments); }
    public List<Termination> terminations() { return List.copyOf(terminations); }
    public Lease lease(String householdId) { return leases.stream()
            .filter(l -> l.householdId().equals(householdId)).findFirst().orElse(null); }
    public Payment payment(String id) { return payments.stream()
            .filter(p -> p.id().equals(id)).findFirst().orElse(null); }

    /** Ends only a lease that has reached the 90-day termination threshold. */
    public Termination terminate(String householdId, long day, String reason) {
        Lease lease = lease(householdId);
        if (lease == null || lease.missedDays() < 90) return null;
        HousingLeaseEconomics.Termination result = HousingLeaseEconomics.terminate(
                lease.depositHeldMinor(), lease.arrearsMinor());
        Termination termination = new Termination("lease-end:" + householdId + ":" + day,
                day, householdId, lease.region(), result.refund(), result.residualArrears(),
                reason == null || reason.isBlank() ? "arrears" : reason);
        leases.removeIf(l -> l.householdId().equals(householdId));
        terminations.removeIf(t -> t.householdId().equals(householdId));
        terminations.add(termination);
        while (terminations.size() > MAX_TERMINATIONS) terminations.remove(0);
        setDirty();
        return termination;
    }

    /** Creates a fresh lease after termination, clearing the old arrears while retaining the audit history. */
    public boolean rehouse(String householdId, String region, long day, long dailyRentMinor) {
        if (householdId == null || householdId.isBlank() || region == null || region.isBlank()
                || dailyRentMinor < 0L || lease(householdId) != null) return false;
        leases.add(new Lease(householdId, region, dailyRentMinor, 0L, -1L, 0, -1L,
                HousingLeaseEconomics.securityDeposit(dailyRentMinor), 0L));
        setDirty(); return true;
    }

    /** Moves an active lease with the household while preserving arrears and held deposit. */
    public boolean relocate(String householdId, String region, long dailyRentMinor) {
        if (householdId == null || householdId.isBlank() || region == null || region.isBlank()
                || dailyRentMinor < 0L) return false;
        Lease previous = lease(householdId);
        if (previous == null) {
            return rehouse(householdId, region, 0L, dailyRentMinor);
        }
        long depositDue = HousingLeaseEconomics.securityDeposit(dailyRentMinor);
        long depositHeld = Math.min(previous.depositHeldMinor(), depositDue);
        leases.removeIf(l -> l.householdId().equals(householdId));
        leases.add(new Lease(householdId, region, dailyRentMinor, previous.arrearsMinor(),
                previous.lastPaymentDay(), previous.missedDays(), previous.noticeDay(),
                depositDue, depositHeld));
        setDirty();
        return true;
    }

    /** Records one rent attempt and returns the existing result when retried. */
    public Payment settleRent(String householdId, String region, long day,
                              long dailyRentMinor, long dueMinor, long availableMinor) {
        String paymentId = "rent:" + householdId + ":" + day;
        Payment existing = payment(paymentId);
        if (existing != null) return existing;
        Lease previous = lease(householdId);
        long priorArrears = previous == null ? 0L : previous.arrearsMinor();
        long safeDailyRent = Math.max(0L, dailyRentMinor);
        long depositDue = previous == null ? HousingLeaseEconomics.securityDeposit(safeDailyRent)
                : Math.max(0L, previous.depositDueMinor() - previous.depositHeldMinor());
        long rentDue = Math.max(0L, dueMinor);
        long available = Math.max(0L, availableMinor);
        long rentTotalDue = add(priorArrears, rentDue);
        long depositPaid = depositDue > 0L && available >= add(depositDue, rentTotalDue) ? depositDue : 0L;
        HousingLeaseEconomics.Settlement settlement = HousingLeaseEconomics.settle(priorArrears,
                previous == null ? 0 : previous.missedDays(),
                previous == null ? -1L : previous.noticeDay(), day, rentDue, available - depositPaid);
        long paid = settlement.paid();
        long arrears = settlement.arrears();
        int missedDays = settlement.missedDays();
        long noticeDay = settlement.noticeDay();
        Payment result = new Payment(paymentId, day, householdId, region,
                add(depositDue, settlement.totalDue()), add(depositPaid, paid), paid, depositPaid, arrears);
        payments.add(result);
        while (payments.size() > MAX_PAYMENTS) payments.remove(0);
        leases.removeIf(l -> l.householdId().equals(householdId));
        leases.add(new Lease(householdId, region, safeDailyRent, arrears, paid > 0L ? day :
                (previous == null ? -1L : previous.lastPaymentDay()), missedDays, noticeDay,
                previous == null ? depositDue : previous.depositDueMinor(),
                add(previous == null ? 0L : previous.depositHeldMinor(), depositPaid)));
        setDirty();
        return result;
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag leaseList = new ListTag();
        for (Lease l : leases) { CompoundTag e = new CompoundTag(); e.putString("household", l.householdId());
            e.putString("region", l.region()); e.putLong("rent", l.dailyRentMinor());
            e.putLong("arrears", l.arrearsMinor()); e.putLong("lastPayment", l.lastPaymentDay());
            e.putInt("missedDays", l.missedDays()); e.putLong("noticeDay", l.noticeDay());
            e.putLong("depositDue", l.depositDueMinor()); e.putLong("depositHeld", l.depositHeldMinor()); leaseList.add(e); }
        tag.put("leases", leaseList);
        ListTag paymentList = new ListTag();
        for (Payment p : payments) { CompoundTag e = new CompoundTag(); e.putString("id", p.id()); e.putLong("day", p.day());
            e.putString("household", p.householdId()); e.putString("region", p.region()); e.putLong("due", p.dueMinor());
            e.putLong("paid", p.paidMinor()); e.putLong("rentPaid", p.rentPaidMinor());
            e.putLong("depositPaid", p.depositPaidMinor()); e.putLong("arrears", p.arrearsAfter()); paymentList.add(e); }
        tag.put("payments", paymentList);
        ListTag terminationList = new ListTag();
        for (Termination t : terminations) { CompoundTag e = new CompoundTag(); e.putString("id", t.id());
            e.putLong("day", t.day()); e.putString("household", t.householdId()); e.putString("region", t.region());
            e.putLong("refund", t.depositReleasedMinor()); e.putLong("residualArrears", t.residualArrearsMinor());
            e.putString("reason", t.reason()); terminationList.add(e); }
        tag.put("terminations", terminationList); return tag;
    }

    public static HousingLeaseSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        HousingLeaseSavedData data = new HousingLeaseSavedData();
        ListTag ls = tag.getList("leases", Tag.TAG_COMPOUND);
        for (int i = 0; i < ls.size(); i++) { CompoundTag e = ls.getCompound(i);
            if (!e.getString("household").isBlank() && !e.getString("region").isBlank()) data.leases.add(new Lease(
                    e.getString("household"), e.getString("region"), Math.max(0L, e.getLong("rent")),
                    Math.max(0L, e.getLong("arrears")), e.getLong("lastPayment"),
                    Math.max(0, e.getInt("missedDays")), e.contains("noticeDay") ? e.getLong("noticeDay") : -1L,
                    Math.max(0L, e.getLong("depositDue")), Math.max(0L, e.getLong("depositHeld")))); }
        ListTag ps = tag.getList("payments", Tag.TAG_COMPOUND);
        for (int i = Math.max(0, ps.size() - MAX_PAYMENTS); i < ps.size(); i++) { CompoundTag e = ps.getCompound(i);
            if (!e.getString("id").isBlank() && !e.getString("household").isBlank()) data.payments.add(new Payment(
                    e.getString("id"), e.getLong("day"), e.getString("household"), e.getString("region"),
                    Math.max(0L, e.getLong("due")), Math.max(0L, e.getLong("paid")),
                    Math.max(0L, e.contains("rentPaid") ? e.getLong("rentPaid") : e.getLong("paid")),
                    Math.max(0L, e.getLong("depositPaid")), Math.max(0L, e.getLong("arrears")))); }
        ListTag ts = tag.getList("terminations", Tag.TAG_COMPOUND);
        for (int i = Math.max(0, ts.size() - MAX_TERMINATIONS); i < ts.size(); i++) { CompoundTag e = ts.getCompound(i);
            if (!e.getString("id").isBlank() && !e.getString("household").isBlank()) data.terminations.add(new Termination(
                    e.getString("id"), e.getLong("day"), e.getString("household"), e.getString("region"),
                    Math.max(0L, e.getLong("refund")), Math.max(0L, e.getLong("residualArrears")),
                    e.getString("reason"))); }
        return data;
    }

    private static long add(long left, long right) {
        try { return Math.addExact(left, right); } catch (ArithmeticException e) { return Long.MAX_VALUE; }
    }
}
