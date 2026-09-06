package com.ailudick.capitalismmod.government;

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

/** Persistent fiscal policy, reserve balance, and transfer audit trail. */
public final class GovernmentPolicySavedData extends SavedData {
    private static final String ID = "capitalismmod_government_policy";
    private static final int MAX_TRANSACTIONS = 8192;
    private long treasuryMinor;
    private long dailyBenefitMinor;
    private int regionalSupportRatePercent = 10;
    private int policyRateBasisPoints;
    private boolean automaticInflationPolicy;
    private int inflationTargetIndexBps = 10200;
    private long lastAutomaticInflationDay = -1L;
    private final List<Transaction> transactions = new ArrayList<>();
    private final List<TaxRevenue> taxRevenues = new ArrayList<>();
    private final List<RentRevenue> rentRevenues = new ArrayList<>();
    private final Set<String> depositReceipts = new HashSet<>();

    public record Transaction(String id, long day, String householdId, long amount, long balanceAfter) {}
    public record TaxRevenue(String id, long day, String subject, String taxType,
                             String currencyId, long originalAmount, long convertedAmount,
                             long balanceAfter) {}
    public record RentRevenue(String id, long day, String householdId, String region,
                              long amount, long balanceAfter) {}

    private GovernmentPolicySavedData() {}

    public static GovernmentPolicySavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(GovernmentPolicySavedData::new, GovernmentPolicySavedData::load), ID);
    }

    public long treasuryMinor() { return treasuryMinor; }
    public long dailyBenefitMinor() { return dailyBenefitMinor; }
    public int regionalSupportRatePercent() { return regionalSupportRatePercent; }
    public int policyRateBasisPoints() { return policyRateBasisPoints; }
    public boolean automaticInflationPolicy() { return automaticInflationPolicy; }
    public int inflationTargetIndexBps() { return inflationTargetIndexBps; }
    public long lastAutomaticInflationDay() { return lastAutomaticInflationDay; }
    public List<Transaction> transactions() { return List.copyOf(transactions); }
    public List<TaxRevenue> taxRevenues() { return List.copyOf(taxRevenues); }
    public List<RentRevenue> rentRevenues() { return List.copyOf(rentRevenues); }

    public boolean setDailyBenefit(long amount) {
        if (amount < 0L || amount > 1_000_000_000L) return false;
        dailyBenefitMinor = amount; setDirty(); return true;
    }

    public boolean setRegionalSupportRatePercent(int percent) {
        if (percent < 0 || percent > 30) return false;
        regionalSupportRatePercent = percent; setDirty(); return true;
    }

    public boolean setPolicyRateBasisPoints(int basisPoints) {
        if (basisPoints < -10000 || basisPoints > 20000) return false;
        policyRateBasisPoints = basisPoints; setDirty(); return true;
    }

    public boolean setAutomaticInflationPolicy(boolean enabled) {
        automaticInflationPolicy = enabled; setDirty(); return true;
    }

    public boolean setInflationTargetIndexBps(int target) {
        if (target < 9000 || target > 12000) return false;
        inflationTargetIndexBps = target; setDirty(); return true;
    }

    public boolean adjustPolicyRate(int delta) {
        return setPolicyRateBasisPoints(Math.max(-10000, Math.min(20000, policyRateBasisPoints + delta)));
    }

    public boolean adjustPolicyRateOnce(long day, int delta) {
        if (!InflationEconomics.automaticAdjustmentDue(day, lastAutomaticInflationDay)) return false;
        lastAutomaticInflationDay = day;
        if (delta != 0) adjustPolicyRate(delta);
        setDirty();
        return true;
    }

    public boolean deposit(long amount) {
        if (amount <= 0L || treasuryMinor > Long.MAX_VALUE - amount) return false;
        treasuryMinor += amount; setDirty(); return true;
    }

    /** Deposits non-tax government revenue at most once for a durable source. */
    public boolean depositOnce(long amount, String sourceId) {
        if (amount <= 0L || sourceId == null || sourceId.isBlank() || depositReceipts.contains(sourceId)
                || treasuryMinor > Long.MAX_VALUE - amount) return false;
        treasuryMinor += amount;
        depositReceipts.add(sourceId);
        while (depositReceipts.size() > MAX_TRANSACTIONS) {
            depositReceipts.remove(depositReceipts.iterator().next());
        }
        setDirty();
        return true;
    }

    public boolean hasDeposit(String sourceId) {
        return sourceId != null && !sourceId.isBlank() && depositReceipts.contains(sourceId);
    }

    /** Collects a tax bill in the government's base currency, once per bill. */
    public boolean collectTax(String billId, long day, String subject, String taxType,
                              String currencyId, long originalAmount, long convertedAmount) {
        if (billId == null || billId.isBlank() || subject == null || subject.isBlank()
                || taxType == null || taxType.isBlank() || currencyId == null || currencyId.isBlank()
                || originalAmount <= 0L || convertedAmount <= 0L
                || treasuryMinor > Long.MAX_VALUE - convertedAmount
                || taxRevenues.stream().anyMatch(t -> billId.equals(t.id()))) return false;
        treasuryMinor += convertedAmount;
        taxRevenues.add(new TaxRevenue(billId, day, subject, taxType, currencyId,
                originalAmount, convertedAmount, treasuryMinor));
        while (taxRevenues.size() > MAX_TRANSACTIONS) taxRevenues.remove(0);
        setDirty();
        return true;
    }

    public boolean hasRent(String paymentId) { return rentRevenues.stream().anyMatch(r -> r.id().equals(paymentId)); }
    public boolean collectRent(String paymentId, long day, String householdId, String region, long amount) {
        if (paymentId == null || paymentId.isBlank() || householdId == null || householdId.isBlank()
                || region == null || region.isBlank() || amount <= 0L || hasRent(paymentId)
                || treasuryMinor > Long.MAX_VALUE - amount) return false;
        treasuryMinor += amount;
        rentRevenues.add(new RentRevenue(paymentId, day, householdId, region, amount, treasuryMinor));
        while (rentRevenues.size() > MAX_TRANSACTIONS) rentRevenues.remove(0);
        setDirty(); return true;
    }

    public boolean spend(String householdId, long day, long amount, String transactionId) {
        if (householdId == null || householdId.isBlank() || amount <= 0L || amount > treasuryMinor
                || transactionId == null || transactionId.isBlank()
                || transactions.stream().anyMatch(t -> transactionId.equals(t.id()))) return false;
        treasuryMinor -= amount;
        transactions.add(new Transaction(transactionId, day, householdId, amount, treasuryMinor));
        while (transactions.size() > MAX_TRANSACTIONS) transactions.remove(0);
        setDirty(); return true;
    }

    /** Returns whether a durable government spending receipt already exists. */
    public boolean hasSpending(String transactionId) {
        return transactionId != null && !transactionId.isBlank()
                && transactions.stream().anyMatch(t -> transactionId.equals(t.id()));
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putLong("treasury", treasuryMinor); tag.putLong("benefit", dailyBenefitMinor);
        tag.putInt("regionalSupportRate", regionalSupportRatePercent);
        tag.putInt("policyRateBps", policyRateBasisPoints);
        tag.putBoolean("automaticInflationPolicy", automaticInflationPolicy);
        tag.putInt("inflationTargetIndexBps", inflationTargetIndexBps);
        tag.putLong("lastAutomaticInflationDay", lastAutomaticInflationDay);
        ListTag list = new ListTag();
        for (Transaction t : transactions) {
            CompoundTag e = new CompoundTag(); e.putString("id", t.id()); e.putLong("day", t.day());
            e.putString("household", t.householdId()); e.putLong("amount", t.amount());
            e.putLong("balance", t.balanceAfter()); list.add(e);
        }
        tag.put("transactions", list);
        ListTag revenues = new ListTag();
        for (TaxRevenue revenue : taxRevenues) {
            CompoundTag e = new CompoundTag(); e.putString("id", revenue.id()); e.putLong("day", revenue.day());
            e.putString("subject", revenue.subject()); e.putString("taxType", revenue.taxType());
            e.putString("currency", revenue.currencyId()); e.putLong("original", revenue.originalAmount());
            e.putLong("converted", revenue.convertedAmount()); e.putLong("balance", revenue.balanceAfter());
            revenues.add(e);
        }
        tag.put("taxRevenues", revenues);
        ListTag rents = new ListTag();
        for (RentRevenue revenue : rentRevenues) {
            CompoundTag e = new CompoundTag(); e.putString("id", revenue.id()); e.putLong("day", revenue.day());
            e.putString("household", revenue.householdId()); e.putString("region", revenue.region());
            e.putLong("amount", revenue.amount()); e.putLong("balance", revenue.balanceAfter()); rents.add(e);
        }
        tag.put("rentRevenues", rents);
        ListTag deposits = new ListTag();
        for (String source : depositReceipts) {
            CompoundTag entry = new CompoundTag();
            entry.putString("source", source);
            deposits.add(entry);
        }
        tag.put("depositReceipts", deposits);
        return tag;
    }

    public static GovernmentPolicySavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        GovernmentPolicySavedData data = new GovernmentPolicySavedData();
        data.treasuryMinor = Math.max(0L, tag.getLong("treasury"));
        data.dailyBenefitMinor = Math.max(0L, tag.getLong("benefit"));
        data.regionalSupportRatePercent = Math.max(0, Math.min(30, tag.contains("regionalSupportRate")
                ? tag.getInt("regionalSupportRate") : 10));
        data.policyRateBasisPoints = Math.max(-10000, Math.min(20000, tag.getInt("policyRateBps")));
        data.automaticInflationPolicy = tag.getBoolean("automaticInflationPolicy");
        data.inflationTargetIndexBps = Math.max(9000, Math.min(12000,
                tag.contains("inflationTargetIndexBps") ? tag.getInt("inflationTargetIndexBps") : 10200));
        data.lastAutomaticInflationDay = tag.contains("lastAutomaticInflationDay")
                ? tag.getLong("lastAutomaticInflationDay") : -1L;
        ListTag deposits = tag.getList("depositReceipts", Tag.TAG_COMPOUND);
        for (int i = 0; i < deposits.size(); i++) {
            String source = deposits.getCompound(i).getString("source");
            if (!source.isBlank()) data.depositReceipts.add(source);
        }
        ListTag list = tag.getList("transactions", Tag.TAG_COMPOUND);
        for (int i = Math.max(0, list.size() - MAX_TRANSACTIONS); i < list.size(); i++) {
            CompoundTag e = list.getCompound(i);
            if (!e.getString("id").isBlank() && !e.getString("household").isBlank() && e.getLong("amount") > 0L) {
                data.transactions.add(new Transaction(e.getString("id"), e.getLong("day"),
                        e.getString("household"), e.getLong("amount"), Math.max(0L, e.getLong("balance"))));
            }
        }
        ListTag revenues = tag.getList("taxRevenues", Tag.TAG_COMPOUND);
        for (int i = Math.max(0, revenues.size() - MAX_TRANSACTIONS); i < revenues.size(); i++) {
            CompoundTag e = revenues.getCompound(i);
            if (!e.getString("id").isBlank() && !e.getString("subject").isBlank()
                    && !e.getString("taxType").isBlank() && !e.getString("currency").isBlank()
                    && e.getLong("original") > 0L && e.getLong("converted") > 0L) {
                data.taxRevenues.add(new TaxRevenue(e.getString("id"), e.getLong("day"),
                        e.getString("subject"), e.getString("taxType"), e.getString("currency"),
                        e.getLong("original"), e.getLong("converted"), Math.max(0L, e.getLong("balance"))));
            }
        }
        ListTag rents = tag.getList("rentRevenues", Tag.TAG_COMPOUND);
        for (int i = Math.max(0, rents.size() - MAX_TRANSACTIONS); i < rents.size(); i++) {
            CompoundTag e = rents.getCompound(i);
            if (!e.getString("id").isBlank() && !e.getString("household").isBlank()
                    && !e.getString("region").isBlank() && e.getLong("amount") > 0L) {
                data.rentRevenues.add(new RentRevenue(e.getString("id"), e.getLong("day"),
                        e.getString("household"), e.getString("region"), e.getLong("amount"),
                        Math.max(0L, e.getLong("balance"))));
            }
        }
        return data;
    }
}
