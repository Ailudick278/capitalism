package com.ailudick.capitalismmod.tax;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** The single persistent ledger for tax bills from all subsystems. */
public final class TaxLedgerSavedData extends SavedData {
    private static final String ID = "capitalismmod_tax_ledger";
    private static final int CURRENT_VERSION = 6;
    private final List<TaxBill> bills = new ArrayList<>();
    private final List<TaxPayment> payments = new ArrayList<>();
    private final List<String> externalSettlementSources = new ArrayList<>();

    private TaxLedgerSavedData() {}

    public static TaxLedgerSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(TaxLedgerSavedData::new, TaxLedgerSavedData::load), ID);
    }

    public List<TaxBill> bills() { return List.copyOf(bills); }
    public List<TaxPayment> payments() { return List.copyOf(payments); }

    public boolean hasPayment(String paymentId) {
        return paymentId != null && !paymentId.isBlank()
                && payments.stream().anyMatch(payment -> payment.id().equals(paymentId));
    }

    public boolean hasExternalSettlement(String sourceId) {
        return sourceId != null && externalSettlementSources.contains(sourceId);
    }

    public void recordExternalSettlement(String sourceId) {
        if (sourceId == null || sourceId.isBlank() || externalSettlementSources.contains(sourceId)) return;
        externalSettlementSources.add(sourceId);
        setDirty();
    }

    public List<TaxPayment> paymentsFor(UUID taxpayerUuid) {
        return payments.stream().filter(payment -> payment.taxpayerUuid().equals(taxpayerUuid)).toList();
    }

    public List<TaxBill> allFor(UUID taxpayerUuid) {
        return bills.stream().filter(bill -> bill.subject().taxpayerUuid().equals(taxpayerUuid)).toList();
    }

    public List<TaxPayment> paymentsForBill(String billId) {
        return payments.stream().filter(payment -> payment.billId().equals(billId)).toList();
    }

    public void add(TaxBill bill) {
        if (bill == null || bill.id() == null || bill.id().isBlank() || bill.subject() == null
                || bill.currencyId() == null || bill.currencyId().isBlank() || bill.amount() <= 0L) return;
        if (bills.stream().anyMatch(existing -> existing.id().equals(bill.id()))) return;
        if (!bill.sourceEventId().isBlank() && findBySourceEvent(bill.sourceEventId()) != null) return;
        bills.add(bill);
        setDirty();
    }

    public void addPayment(TaxPayment payment) {
        if (payment == null || payments.stream().anyMatch(existing -> existing.id().equals(payment.id()))) return;
        payments.add(payment);
        setDirty();
    }

    public void replace(TaxBill bill) {
        for (int i = 0; i < bills.size(); i++) {
            if (bills.get(i).id().equals(bill.id())) {
                bills.set(i, bill);
                setDirty();
                return;
            }
        }
        add(bill);
    }

    public TaxBill get(String id) {
        return bills.stream().filter(bill -> bill.id().equals(id)).findFirst().orElse(null);
    }

    public TaxBill findBySourceEvent(String sourceEventId) {
        if (sourceEventId == null || sourceEventId.isBlank()) return null;
        return bills.stream().filter(bill -> sourceEventId.equals(bill.sourceEventId())).findFirst().orElse(null);
    }

    public List<TaxBill> outstandingFor(UUID taxpayerUuid) {
        return bills.stream().filter(bill -> bill.subject().taxpayerUuid().equals(taxpayerUuid) && !bill.paid()).toList();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (TaxBill bill : bills) {
            CompoundTag entry = new CompoundTag();
            entry.putString("id", bill.id());
            entry.putString("type", bill.subject().type().id());
            entry.putString("subjectId", bill.subject().subjectId());
            entry.putUUID("taxpayer", bill.subject().taxpayerUuid());
            entry.putString("currency", bill.currencyId());
            entry.putLong("amount", bill.amount());
            entry.putLong("paid", bill.paidAmount());
            entry.putLong("createdAt", bill.createdAt());
            entry.putLong("dueAt", bill.dueAt());
            entry.putLong("graceUntil", bill.graceUntil());
            entry.putString("sourceEventId", bill.sourceEventId());
            entry.putLong("periodStart", bill.periodStart());
            entry.putLong("periodEnd", bill.periodEnd());
            entry.putLong("taxableBase", bill.taxableBase());
            entry.putInt("rateBps", bill.rateBasisPoints());
            entry.putLong("declarationDueAt", bill.declarationDueAt());
            entry.putLong("declaredAt", bill.declaredAt());
            entry.putString("declaredBy", bill.declaredBy());
            entry.putLong("lateFee", bill.lateFeeAmount());
            entry.putLong("lateFeeUpdatedAt", bill.lateFeeUpdatedAt());
            list.add(entry);
        }
        tag.putInt("version", CURRENT_VERSION);
        tag.put("bills", list);
        ListTag paymentList = new ListTag();
        for (TaxPayment payment : payments) {
            CompoundTag entry = new CompoundTag();
            entry.putString("id", payment.id());
            entry.putString("billId", payment.billId());
            entry.putUUID("taxpayer", payment.taxpayerUuid());
            entry.putString("currency", payment.currencyId());
            entry.putLong("amount", payment.amount());
            entry.putLong("paidAt", payment.paidAt());
            if (!payment.settlementReference().isBlank()) entry.putString("settlementReference", payment.settlementReference());
            paymentList.add(entry);
        }
        tag.put("payments", paymentList);
        ListTag settlementList = new ListTag();
        for (String source : externalSettlementSources) {
            CompoundTag entry = new CompoundTag();
            entry.putString("source", source);
            settlementList.add(entry);
        }
        tag.put("externalSettlements", settlementList);
        return tag;
    }

    public static TaxLedgerSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        TaxLedgerSavedData data = new TaxLedgerSavedData();
        int version = tag.contains("version") ? tag.getInt("version") : 1;
        ListTag list = tag.getList("bills", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (!entry.hasUUID("taxpayer")) continue;
            TaxType type = TaxType.byId(entry.getString("type"));
            long amount = entry.getLong("amount");
            long paid = entry.getLong("paid");
            // Version 1 stored land liabilities in whole CNY while the other
            // migrated liabilities were already minor-unit values.
            if (version < 2 && type == TaxType.LAND) {
                amount = com.ailudick.capitalismmod.currency.Money.toMinorSaturated(amount);
                paid = com.ailudick.capitalismmod.currency.Money.toMinorSaturated(paid);
            }
            long createdAt = entry.getLong("createdAt");
            long declarationDueAt = entry.getLong("declarationDueAt");
            long declaredAt = version < 4 ? createdAt : entry.getLong("declaredAt");
            String declaredBy = version < 4 ? "legacy" : entry.getString("declaredBy");
            TaxBill bill = new TaxBill(entry.getString("id"),
                    new TaxSubject(type, entry.getString("subjectId"), entry.getUUID("taxpayer")),
                    entry.getString("currency"), amount, paid,
                    createdAt, entry.getLong("dueAt"), entry.getLong("graceUntil"),
                    entry.getString("sourceEventId"), entry.getLong("periodStart"),
                    entry.getLong("periodEnd"), entry.getLong("taxableBase"), entry.getInt("rateBps"),
                    declarationDueAt, declaredAt, declaredBy, entry.getLong("lateFee"),
                    entry.getLong("lateFeeUpdatedAt"));
            if (bill.id() != null && !bill.id().isBlank()
                    && !data.bills.stream().anyMatch(existing -> existing.id().equals(bill.id()))
                    && (bill.sourceEventId().isBlank()
                    || data.bills.stream().noneMatch(existing -> existing.sourceEventId().equals(bill.sourceEventId())))) {
                data.bills.add(bill);
            }
        }
        ListTag paymentList = tag.getList("payments", 10);
        for (int i = 0; i < paymentList.size(); i++) {
            CompoundTag entry = paymentList.getCompound(i);
            if (!entry.hasUUID("taxpayer") || entry.getLong("amount") <= 0L) continue;
            TaxPayment payment = new TaxPayment(entry.getString("id"), entry.getString("billId"),
                    entry.getUUID("taxpayer"), entry.getString("currency"), entry.getLong("amount"),
                    entry.getLong("paidAt"), entry.getString("settlementReference"));
            if (data.payments.stream().noneMatch(existing -> existing.id().equals(payment.id()))) {
                data.payments.add(payment);
            }
        }
        ListTag settlementList = tag.getList("externalSettlements", 10);
        for (int i = 0; i < settlementList.size(); i++) {
            String source = settlementList.getCompound(i).getString("source");
            if (!source.isBlank()) data.externalSettlementSources.add(source);
        }
        return data;
    }
}
