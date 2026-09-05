package com.ailudick.capitalismmod.company;

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

/** Persistent employee wages and employer labor-cost liabilities and payment audit. */
public final class CompanyPayrollSavedData extends SavedData {
    private static final String ID = "capitalismmod_company_payroll";
    private static final int MAX_PAYMENTS = 4096;
    private final Map<String, Account> accounts = new HashMap<>();
    private final List<Payment> payments = new ArrayList<>();

    public record Account(String companyId, long unpaidWages, long lastSettlementDay) {
    }

    public record Payment(String companyId, long settlementDay, long amount, long unpaidBefore, long unpaidAfter) {
    }

    private CompanyPayrollSavedData() {
    }

    public static CompanyPayrollSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(CompanyPayrollSavedData::new, CompanyPayrollSavedData::load), ID);
    }

    public long unpaid(String companyId) {
        return Math.max(0L, accounts.getOrDefault(companyId, new Account(companyId, 0L, -1L)).unpaidWages());
    }

    public Account account(String companyId) {
        return accounts.getOrDefault(companyId, new Account(companyId, 0L, -1L));
    }

    public List<Payment> payments() {
        return List.copyOf(payments);
    }

    public void settle(String companyId, long settlementDay, long paid, long unpaidAfter) {
        long before = unpaid(companyId);
        accounts.put(companyId, new Account(companyId, Math.max(0L, unpaidAfter), settlementDay));
        payments.add(new Payment(companyId, settlementDay, Math.max(0L, paid), before,
                Math.max(0L, unpaidAfter)));
        while (payments.size() > MAX_PAYMENTS) payments.remove(0);
        setDirty();
    }

    /**
     * Moves payroll liabilities and their audit trail during a legal merger.
     * The surviving company must inherit unpaid wages; otherwise a removed
     * company could leave employees with an uncollectable liability.
     */
    public void transferCompany(String sourceId, String targetId) {
        if (sourceId == null || targetId == null || sourceId.isBlank()
                || targetId.isBlank() || sourceId.equals(targetId)) return;
        Account source = accounts.remove(sourceId);
        Account target = accounts.get(targetId);
        if (source != null) {
            long sourceUnpaid = Math.max(0L, source.unpaidWages());
            long targetUnpaid = target == null ? 0L : Math.max(0L, target.unpaidWages());
            accounts.put(targetId, new Account(targetId, add(targetUnpaid, sourceUnpaid),
                    Math.max(source.lastSettlementDay(), target == null ? -1L : target.lastSettlementDay())));
        }
        boolean movedPayment = false;
        for (int i = 0; i < payments.size(); i++) {
            Payment payment = payments.get(i);
            if (!sourceId.equals(payment.companyId())) continue;
            payments.set(i, new Payment(targetId, payment.settlementDay(), payment.amount(),
                    payment.unpaidBefore(), payment.unpaidAfter()));
            movedPayment = true;
        }
        if (source != null || movedPayment) setDirty();
    }

    private static long add(long left, long right) {
        if (left >= Long.MAX_VALUE - right) return Long.MAX_VALUE;
        return left + right;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag accountList = new ListTag();
        for (Account account : accounts.values()) {
            CompoundTag entry = new CompoundTag();
            entry.putString("companyId", account.companyId());
            entry.putLong("unpaid", account.unpaidWages());
            entry.putLong("lastDay", account.lastSettlementDay());
            accountList.add(entry);
        }
        tag.put("accounts", accountList);
        ListTag paymentList = new ListTag();
        for (Payment payment : payments) {
            CompoundTag entry = new CompoundTag();
            entry.putString("companyId", payment.companyId());
            entry.putLong("day", payment.settlementDay());
            entry.putLong("amount", payment.amount());
            entry.putLong("before", payment.unpaidBefore());
            entry.putLong("after", payment.unpaidAfter());
            paymentList.add(entry);
        }
        tag.put("payments", paymentList);
        return tag;
    }

    public static CompanyPayrollSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        CompanyPayrollSavedData data = new CompanyPayrollSavedData();
        ListTag accountList = tag.getList("accounts", Tag.TAG_COMPOUND);
        for (int i = 0; i < accountList.size(); i++) {
            CompoundTag entry = accountList.getCompound(i);
            if (!entry.getString("companyId").isBlank()) {
                data.accounts.put(entry.getString("companyId"), new Account(entry.getString("companyId"),
                        Math.max(0L, entry.getLong("unpaid")), entry.getLong("lastDay")));
            }
        }
        ListTag paymentList = tag.getList("payments", Tag.TAG_COMPOUND);
        for (int i = 0; i < paymentList.size(); i++) {
            CompoundTag entry = paymentList.getCompound(i);
            if (!entry.getString("companyId").isBlank()) {
                data.payments.add(new Payment(entry.getString("companyId"), entry.getLong("day"),
                        Math.max(0L, entry.getLong("amount")), Math.max(0L, entry.getLong("before")),
                        Math.max(0L, entry.getLong("after"))));
            }
        }
        while (data.payments.size() > MAX_PAYMENTS) data.payments.remove(0);
        return data;
    }
}
