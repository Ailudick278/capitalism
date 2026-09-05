package com.ailudick.capitalismmod.network.payload;

import com.ailudick.capitalismmod.CapitalismMod;
import com.ailudick.capitalismmod.tax.TaxBill;
import com.ailudick.capitalismmod.tax.TaxPayment;
import com.ailudick.capitalismmod.tax.TaxSubject;
import com.ailudick.capitalismmod.tax.TaxType;
import com.ailudick.capitalismmod.tax.TaxRefundSavedData;
import com.ailudick.capitalismmod.tax.TaxRefundAllocation;
import com.ailudick.capitalismmod.tax.TaxRefundAuditSavedData;
import com.ailudick.capitalismmod.tax.TaxRefundNotificationSavedData;
import com.ailudick.capitalismmod.tax.TaxAnnualReport;
import com.ailudick.capitalismmod.tax.IndividualTaxPeriodSavedData;
import com.ailudick.capitalismmod.tax.TaxExpenseLedgerSavedData;
import com.ailudick.capitalismmod.tax.TaxIncomeVoucherLedgerSavedData;
import com.ailudick.capitalismmod.tax.TaxCorrectionAuditSavedData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.ArrayList;
import java.util.List;

public record SyncTaxBillsPayload(List<TaxBill> bills, List<TaxPayment> payments, long creditBalance,
                                  List<TaxRefundSavedData.Request> refunds,
                                  List<TaxRefundAuditSavedData.Event> refundAudit,
                                  List<TaxRefundNotificationSavedData.Notification> notifications,
                                  TaxAnnualReport annualReport,
                                  List<IndividualTaxPeriodSavedData.Entry> individualPeriods,
                                  List<TaxExpenseLedgerSavedData.Expense> individualExpenses,
                                  List<TaxIncomeVoucherLedgerSavedData.Voucher> individualIncomes,
                                  List<TaxCorrectionAuditSavedData.Entry> correctionAudits) implements CustomPacketPayload {
    public SyncTaxBillsPayload(List<TaxBill> bills) { this(bills, List.of(), 0L, List.of(), List.of(), List.of(), null, List.of(), List.of(), List.of(), List.of()); }
    public SyncTaxBillsPayload(List<TaxBill> bills, List<TaxPayment> payments) { this(bills, payments, 0L, List.of(), List.of(), List.of(), null, List.of(), List.of(), List.of(), List.of()); }
    public SyncTaxBillsPayload(List<TaxBill> bills, List<TaxPayment> payments, long creditBalance) { this(bills, payments, creditBalance, List.of(), List.of(), List.of(), null, List.of(), List.of(), List.of(), List.of()); }
    public SyncTaxBillsPayload(List<TaxBill> bills, List<TaxPayment> payments, long creditBalance, List<TaxRefundSavedData.Request> refunds) { this(bills, payments, creditBalance, refunds, List.of(), List.of(), null, List.of(), List.of(), List.of(), List.of()); }
    public SyncTaxBillsPayload(List<TaxBill> bills, List<TaxPayment> payments, long creditBalance, List<TaxRefundSavedData.Request> refunds, List<TaxRefundAuditSavedData.Event> audit, List<TaxRefundNotificationSavedData.Notification> notifications) { this(bills, payments, creditBalance, refunds, audit, notifications, null, List.of(), List.of(), List.of(), List.of()); }
    public static final Type<SyncTaxBillsPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CapitalismMod.MODID, "sync_tax_bills"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncTaxBillsPayload> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> {
                buf.writeVarInt(value.bills().size());
                for (TaxBill bill : value.bills()) {
                    buf.writeUtf(bill.id()); buf.writeUtf(bill.subject().type().id()); buf.writeUtf(bill.subject().subjectId()); buf.writeUUID(bill.subject().taxpayerUuid());
                    buf.writeUtf(bill.currencyId()); buf.writeVarLong(bill.amount()); buf.writeVarLong(bill.paidAmount()); buf.writeVarLong(bill.createdAt());
                    buf.writeVarLong(bill.dueAt()); buf.writeVarLong(bill.graceUntil()); buf.writeUtf(bill.sourceEventId()); buf.writeVarLong(bill.periodStart());
                    buf.writeVarLong(bill.periodEnd()); buf.writeVarLong(bill.taxableBase()); buf.writeVarInt(bill.rateBasisPoints());
                    buf.writeVarLong(bill.declarationDueAt()); buf.writeVarLong(bill.declaredAt()); buf.writeUtf(bill.declaredBy());
                    buf.writeVarLong(bill.lateFeeAmount()); buf.writeVarLong(bill.lateFeeUpdatedAt());
                }
                buf.writeVarInt(value.payments().size());
                for (TaxPayment payment : value.payments()) {
                    buf.writeUtf(payment.id()); buf.writeUtf(payment.billId()); buf.writeUUID(payment.taxpayerUuid()); buf.writeUtf(payment.currencyId());
                    buf.writeVarLong(payment.amount()); buf.writeVarLong(payment.paidAt());
                }
                buf.writeVarLong(value.creditBalance());
                int refundSize = Math.min(value.refunds().size(), 128);
                buf.writeVarInt(refundSize);
                for (int i = 0; i < refundSize; i++) {
                    TaxRefundSavedData.Request refund = value.refunds().get(i);
                    buf.writeUtf(refund.id()); buf.writeUUID(refund.taxpayerUuid()); buf.writeUtf(refund.currencyId());
                    buf.writeVarLong(refund.amount()); buf.writeVarLong(refund.requestedAt()); buf.writeUtf(refund.status());
                    buf.writeVarLong(refund.reviewedAt()); buf.writeUtf(refund.reviewer()); buf.writeUtf(refund.reason()); buf.writeUtf(refund.sourceSummary()); buf.writeUtf(refund.allocations());
                    int detailSize = Math.min(refund.allocationDetails().size(), 32);
                    buf.writeVarInt(detailSize);
                    for (int j = 0; j < detailSize; j++) {
                        TaxRefundAllocation detail = refund.allocationDetails().get(j);
                        buf.writeUtf(detail.sourceId()); buf.writeUtf(detail.subjectType()); buf.writeUtf(detail.subjectId());
                        buf.writeVarLong(detail.periodStart()); buf.writeVarLong(detail.periodEnd()); buf.writeVarLong(detail.originalCredit()); buf.writeVarLong(detail.refundAmount());
                    }
                }
                int auditSize = Math.min(value.refundAudit().size(), 512);
                buf.writeVarInt(auditSize);
                for (int i = 0; i < auditSize; i++) {
                    TaxRefundAuditSavedData.Event event = value.refundAudit().get(i);
                    buf.writeUtf(event.requestId(), 64); buf.writeUUID(event.taxpayerUuid()); buf.writeUtf(event.action(), 32); buf.writeUtf(event.actor(), 64);
                    buf.writeUtf(event.currencyId(), 16); buf.writeVarLong(event.amount()); buf.writeVarLong(event.time()); buf.writeUtf(event.result(), 32);
                    buf.writeUtf(event.reason(), 256); buf.writeUtf(event.sourceSummary(), 512);
                }
                int notificationSize = Math.min(value.notifications().size(), 128); buf.writeVarInt(notificationSize);
                for (int i = 0; i < notificationSize; i++) { var notification = value.notifications().get(i); buf.writeUUID(notification.playerUuid()); buf.writeUtf(notification.requestId(), 64); buf.writeVarLong(notification.time()); buf.writeUtf(notification.message(), 512); buf.writeBoolean(notification.read()); }
                int periodSize = Math.min(value.individualPeriods().size(), 64); buf.writeVarInt(periodSize);
                for (int i = 0; i < periodSize; i++) { var period = value.individualPeriods().get(i); buf.writeUtf(period.businessId(), 128); buf.writeUUID(period.ownerUuid()); buf.writeUtf(period.currencyId(), 16); buf.writeVarLong(period.revenue()); buf.writeVarLong(period.expenses()); buf.writeVarLong(period.periodStart()); buf.writeVarLong(period.periodEnd()); }
                int expenseSize = Math.min(value.individualExpenses().size(), 128); buf.writeVarInt(expenseSize);
                for (int i = 0; i < expenseSize; i++) { var expense = value.individualExpenses().get(i); buf.writeUtf(expense.id(), 64); buf.writeUUID(expense.taxpayerUuid()); buf.writeUtf(expense.subjectId(), 128); buf.writeUtf(expense.category(), 64); buf.writeUtf(expense.currencyId(), 16); buf.writeVarLong(expense.amount()); buf.writeVarLong(expense.occurredAt()); buf.writeUtf(expense.sourceId(), 128); buf.writeBoolean(expense.deductible()); buf.writeUtf(expense.details(), 256); }
                int incomeSize = Math.min(value.individualIncomes().size(), 128); buf.writeVarInt(incomeSize);
                for (int i = 0; i < incomeSize; i++) { var income = value.individualIncomes().get(i); buf.writeUtf(income.id(), 64); buf.writeUUID(income.taxpayerUuid()); buf.writeUtf(income.subjectId(), 128); buf.writeUtf(income.category(), 64); buf.writeUtf(income.currencyId(), 16); buf.writeVarLong(income.amount()); buf.writeVarLong(income.occurredAt()); buf.writeUtf(income.sourceId(), 128); buf.writeUtf(income.details(), 256); }
                int correctionSize = Math.min(value.correctionAudits().size(), 64); buf.writeVarInt(correctionSize);
                for (int i = 0; i < correctionSize; i++) { var audit = value.correctionAudits().get(i); buf.writeUtf(audit.id(), 64); buf.writeUtf(audit.businessId(), 128); buf.writeVarLong(audit.periodEnd()); buf.writeVarLong(audit.oldRevenue()); buf.writeVarLong(audit.oldExpenses()); buf.writeVarLong(audit.newRevenue()); buf.writeVarLong(audit.newExpenses()); buf.writeVarLong(audit.oldTax()); buf.writeVarLong(audit.newTax()); buf.writeVarLong(audit.difference()); buf.writeUtf(audit.administrator(), 64); buf.writeVarLong(audit.occurredAt()); buf.writeUtf(audit.reason(), 256); }
                buf.writeBoolean(value.annualReport() != null);
                if (value.annualReport() != null) { var report = value.annualReport(); buf.writeVarLong(report.year()); buf.writeVarLong(report.yearStart()); buf.writeVarLong(report.yearEnd()); buf.writeVarLong(report.taxableBase()); buf.writeVarLong(report.assessedTax()); buf.writeVarLong(report.paidTax()); buf.writeVarLong(report.refunds()); buf.writeVarLong(report.outstandingTax()); buf.writeVarLong(report.currentCreditBalance()); buf.writeVarLong(report.refundActions()); }
            },
            buf -> {
                int size = Math.min(buf.readVarInt(), 256);
                List<TaxBill> bills = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    bills.add(new TaxBill(buf.readUtf(64), new TaxSubject(TaxType.byId(buf.readUtf(64)), buf.readUtf(128), buf.readUUID()),
                            buf.readUtf(16), buf.readVarLong(), buf.readVarLong(), buf.readVarLong(), buf.readVarLong(), buf.readVarLong(),
                            buf.readUtf(128), buf.readVarLong(), buf.readVarLong(), buf.readVarLong(), buf.readVarInt(), buf.readVarLong(),
                            buf.readVarLong(), buf.readUtf(64), buf.readVarLong(), buf.readVarLong()));
                }
                int paymentSize = Math.min(buf.readVarInt(), 512);
                List<TaxPayment> payments = new ArrayList<>(paymentSize);
                for (int i = 0; i < paymentSize; i++) {
                    payments.add(new TaxPayment(buf.readUtf(64), buf.readUtf(64), buf.readUUID(), buf.readUtf(16), buf.readVarLong(), buf.readVarLong()));
                }
                long creditBalance = buf.readVarLong();
                int refundSize = Math.min(buf.readVarInt(), 128);
                List<TaxRefundSavedData.Request> refunds = new ArrayList<>(refundSize);
                for (int i = 0; i < refundSize; i++) {
                    refunds.add(new TaxRefundSavedData.Request(buf.readUtf(64), buf.readUUID(), buf.readUtf(16),
                            buf.readVarLong(), buf.readVarLong(), buf.readUtf(32), buf.readVarLong(), buf.readUtf(64), buf.readUtf(256), buf.readUtf(512), buf.readUtf(768), readAllocations(buf)));
                }
                int auditSize = Math.min(buf.readVarInt(), 512);
                List<TaxRefundAuditSavedData.Event> audit = new ArrayList<>(auditSize);
                for (int i = 0; i < auditSize; i++) {
                    audit.add(new TaxRefundAuditSavedData.Event(buf.readUtf(64), buf.readUUID(), buf.readUtf(32), buf.readUtf(64),
                            buf.readUtf(16), buf.readVarLong(), buf.readVarLong(), buf.readUtf(32), buf.readUtf(256), buf.readUtf(512)));
                }
                int notificationSize = Math.min(buf.readVarInt(), 128); List<TaxRefundNotificationSavedData.Notification> notifications = new ArrayList<>(notificationSize);
                for (int i = 0; i < notificationSize; i++) notifications.add(new TaxRefundNotificationSavedData.Notification(buf.readUUID(), buf.readUtf(64), buf.readVarLong(), buf.readUtf(512), buf.readBoolean()));
                int periodSize = Math.min(buf.readVarInt(), 64); List<IndividualTaxPeriodSavedData.Entry> periods = new ArrayList<>(periodSize);
                for (int i = 0; i < periodSize; i++) periods.add(new IndividualTaxPeriodSavedData.Entry(buf.readUtf(128), buf.readUUID(), buf.readUtf(16), buf.readVarLong(), buf.readVarLong(), buf.readVarLong(), buf.readVarLong()));
                int expenseSize = Math.min(buf.readVarInt(), 128); List<TaxExpenseLedgerSavedData.Expense> expenses = new ArrayList<>(expenseSize);
                for (int i = 0; i < expenseSize; i++) expenses.add(new TaxExpenseLedgerSavedData.Expense(buf.readUtf(64), buf.readUUID(), buf.readUtf(128), buf.readUtf(64), buf.readUtf(16), buf.readVarLong(), buf.readVarLong(), buf.readUtf(128), buf.readBoolean(), buf.readUtf(256)));
                int incomeSize = Math.min(buf.readVarInt(), 128); List<TaxIncomeVoucherLedgerSavedData.Voucher> incomes = new ArrayList<>(incomeSize);
                for (int i = 0; i < incomeSize; i++) incomes.add(new TaxIncomeVoucherLedgerSavedData.Voucher(buf.readUtf(64), buf.readUUID(), buf.readUtf(128), buf.readUtf(64), buf.readUtf(16), buf.readVarLong(), buf.readVarLong(), buf.readUtf(128), buf.readUtf(256)));
                int correctionSize = Math.min(buf.readVarInt(), 64); List<TaxCorrectionAuditSavedData.Entry> corrections = new ArrayList<>(correctionSize);
                for (int i = 0; i < correctionSize; i++) corrections.add(new TaxCorrectionAuditSavedData.Entry(buf.readUtf(64), buf.readUtf(128), buf.readVarLong(), buf.readVarLong(), buf.readVarLong(), buf.readVarLong(), buf.readVarLong(), buf.readVarLong(), buf.readVarLong(), buf.readVarLong(), buf.readUtf(64), buf.readVarLong(), buf.readUtf(256)));
                TaxAnnualReport report = null;
                if (buf.readBoolean()) report = new TaxAnnualReport(buf.readVarLong(), buf.readVarLong(), buf.readVarLong(), buf.readVarLong(), buf.readVarLong(), buf.readVarLong(), buf.readVarLong(), buf.readVarLong(), buf.readVarLong(), buf.readVarLong());
                return new SyncTaxBillsPayload(List.copyOf(bills), List.copyOf(payments), creditBalance, List.copyOf(refunds), List.copyOf(audit), List.copyOf(notifications), report, List.copyOf(periods), List.copyOf(expenses), List.copyOf(incomes), List.copyOf(corrections));
            });
    private static List<TaxRefundAllocation> readAllocations(RegistryFriendlyByteBuf buf) {
        int size = Math.min(buf.readVarInt(), 32);
        List<TaxRefundAllocation> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            result.add(new TaxRefundAllocation(buf.readUtf(256), buf.readUtf(64), buf.readUtf(128),
                    buf.readVarLong(), buf.readVarLong(), buf.readVarLong(), buf.readVarLong()));
        }
        return List.copyOf(result);
    }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
