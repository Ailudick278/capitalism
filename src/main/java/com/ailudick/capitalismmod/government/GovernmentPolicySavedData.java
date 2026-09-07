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
import java.util.Map;
import java.util.HashMap;

/** Persistent fiscal policy, reserve balance, and transfer audit trail. */
public final class GovernmentPolicySavedData extends SavedData {
    private static final String ID = "capitalismmod_government_policy";
    private static final int MAX_TRANSACTIONS = 8192;
    private long treasuryMinor;
    private long dailyBenefitMinor;
    private int regionalSupportRatePercent = 10;
    private int policyRateBasisPoints;
    private boolean automaticInflationPolicy;
    private int inflationTargetBps = 167;
    private long lastAutomaticInflationDay = -1L;
    private boolean automaticOpenMarketPolicy;
    private long lastAutomaticOpenMarketDay = -1L;
    private final List<Transaction> transactions = new ArrayList<>();
    private final List<TaxRevenue> taxRevenues = new ArrayList<>();
    private final List<RentRevenue> rentRevenues = new ArrayList<>();
    private final List<ConsumptionTaxRevenue> consumptionTaxRevenues = new ArrayList<>();
    private final Set<String> depositReceipts = new HashSet<>();

    public record Transaction(String id, long day, String householdId, long amount, long balanceAfter) {}
    public record TaxRevenue(String id, long day, String subject, String taxType,
                             String currencyId, long originalAmount, long convertedAmount,
                             long balanceAfter) {}
    public record RentRevenue(String id, long day, String householdId, String region,
                              long amount, long balanceAfter) {}
    public record ConsumptionTaxRevenue(String id, long day, String householdId, String sellerId,
                                        long grossAmount, long taxAmount, long balanceAfter) {}

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
    public int inflationTargetBps() { return inflationTargetBps; }
    public long lastAutomaticInflationDay() { return lastAutomaticInflationDay; }
    public boolean automaticOpenMarketPolicy() { return automaticOpenMarketPolicy; }
    public long lastAutomaticOpenMarketDay() { return lastAutomaticOpenMarketDay; }
    public List<Transaction> transactions() { return List.copyOf(transactions); }
    public List<TaxRevenue> taxRevenues() { return List.copyOf(taxRevenues); }
    public List<RentRevenue> rentRevenues() { return List.copyOf(rentRevenues); }
    public List<ConsumptionTaxRevenue> consumptionTaxRevenues() { return List.copyOf(consumptionTaxRevenues); }

    public Map<GovernmentBudgetCategory, Long> spendingByCategory(long day) {
        Map<GovernmentBudgetCategory, Long> totals = new HashMap<>();
        for (Transaction transaction : transactions) {
            if (transaction.day() != day) continue;
            GovernmentBudgetCategory category = GovernmentBudgetCategory.fromTransactionId(transaction.id());
            long previous = totals.getOrDefault(category, 0L);
            totals.put(category, previous > Long.MAX_VALUE - transaction.amount()
                    ? Long.MAX_VALUE : previous + transaction.amount());
        }
        return Map.copyOf(totals);
    }

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

    public boolean setInflationTargetBps(int target) {
        if (target < 0 || target > 2000) return false;
        inflationTargetBps = target; setDirty(); return true;
    }

    public boolean setAutomaticOpenMarketPolicy(boolean enabled) {
        automaticOpenMarketPolicy = enabled; setDirty(); return true;
    }

    public boolean automaticOpenMarketDue(long day) {
        return day >= 0L && day > lastAutomaticOpenMarketDay;
    }

    public void markAutomaticOpenMarketDay(long day) {
        if (day > lastAutomaticOpenMarketDay) {
            lastAutomaticOpenMarketDay = day;
            setDirty();
        }
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

    public boolean collectConsumptionTax(String paymentId, long day, String householdId,
                                         String sellerId, long grossAmount, long taxAmount) {
        if (paymentId == null || paymentId.isBlank() || householdId == null || householdId.isBlank()
                || sellerId == null || sellerId.isBlank() || grossAmount <= 0L || taxAmount <= 0L
                || taxAmount >= grossAmount) return false;
        ConsumptionTaxRevenue existing = consumptionTaxRevenues.stream()
                .filter(revenue -> paymentId.equals(revenue.id())).findFirst().orElse(null);
        if (existing != null) {
            return existing.grossAmount() == grossAmount && existing.taxAmount() == taxAmount
                    && existing.householdId().equals(householdId) && existing.sellerId().equals(sellerId);
        }
        if (treasuryMinor > Long.MAX_VALUE - taxAmount) return false;
        treasuryMinor += taxAmount;
        consumptionTaxRevenues.add(new ConsumptionTaxRevenue(paymentId, day, householdId, sellerId,
                grossAmount, taxAmount, treasuryMinor));
        while (consumptionTaxRevenues.size() > MAX_TRANSACTIONS) consumptionTaxRevenues.remove(0);
        setDirty();
        return true;
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
        tag.putInt("inflationTargetBps", inflationTargetBps);
        tag.putLong("lastAutomaticInflationDay", lastAutomaticInflationDay);
        tag.putBoolean("automaticOpenMarketPolicy", automaticOpenMarketPolicy);
        tag.putLong("lastAutomaticOpenMarketDay", lastAutomaticOpenMarketDay);
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
        ListTag consumptionTaxes = new ListTag();
        for (ConsumptionTaxRevenue revenue : consumptionTaxRevenues) {
            CompoundTag e = new CompoundTag(); e.putString("id", revenue.id()); e.putLong("day", revenue.day());
            e.putString("household", revenue.householdId()); e.putString("seller", revenue.sellerId());
            e.putLong("gross", revenue.grossAmount()); e.putLong("tax", revenue.taxAmount());
            e.putLong("balance", revenue.balanceAfter()); consumptionTaxes.add(e);
        }
        tag.put("consumptionTaxRevenues", consumptionTaxes);
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
        if (tag.contains("inflationTargetBps")) {
            data.inflationTargetBps = Math.max(0, Math.min(2000, tag.getInt("inflationTargetBps")));
        } else {
            data.inflationTargetBps = 167;
        }
        data.lastAutomaticInflationDay = tag.contains("lastAutomaticInflationDay")
                ? tag.getLong("lastAutomaticInflationDay") : -1L;
        data.automaticOpenMarketPolicy = tag.getBoolean("automaticOpenMarketPolicy");
        data.lastAutomaticOpenMarketDay = tag.contains("lastAutomaticOpenMarketDay")
                ? tag.getLong("lastAutomaticOpenMarketDay") : -1L;
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
        ListTag consumptionTaxes = tag.getList("consumptionTaxRevenues", Tag.TAG_COMPOUND);
        for (int i = Math.max(0, consumptionTaxes.size() - MAX_TRANSACTIONS); i < consumptionTaxes.size(); i++) {
            CompoundTag e = consumptionTaxes.getCompound(i);
            if (!e.getString("id").isBlank() && !e.getString("household").isBlank()
                    && !e.getString("seller").isBlank() && e.getLong("gross") > e.getLong("tax")
                    && e.getLong("tax") > 0L && e.getLong("balance") >= 0L) {
                data.consumptionTaxRevenues.add(new ConsumptionTaxRevenue(e.getString("id"),
                        e.getLong("day"), e.getString("household"), e.getString("seller"),
                        e.getLong("gross"), e.getLong("tax"), e.getLong("balance")));
            }
        }
        return data;
    }
}
