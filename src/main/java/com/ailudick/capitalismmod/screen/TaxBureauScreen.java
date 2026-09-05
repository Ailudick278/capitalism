package com.ailudick.capitalismmod.screen;

import com.ailudick.capitalismmod.client.GuiStyles;
import com.ailudick.capitalismmod.Config;
import com.ailudick.capitalismmod.currency.Money;
import com.ailudick.capitalismmod.menu.TaxBureauMenu;
import com.ailudick.capitalismmod.network.payload.PayUnifiedTaxPayload;
import com.ailudick.capitalismmod.network.payload.DeclareTaxPayload;
import com.ailudick.capitalismmod.network.payload.RequestTaxRefundPayload;
import com.ailudick.capitalismmod.network.payload.ReviewTaxRefundPayload;
import com.ailudick.capitalismmod.network.payload.ManageTaxRefundNotificationsPayload;
import com.ailudick.capitalismmod.network.payload.ReviewTaxCorrectionRequestPayload;
import com.ailudick.capitalismmod.tax.TaxBill;
import com.ailudick.capitalismmod.tax.TaxPayment;
import com.ailudick.capitalismmod.tax.TaxRefundSavedData;
import com.ailudick.capitalismmod.tax.TaxRefundNotificationSavedData;
import com.ailudick.capitalismmod.tax.TaxAnnualReport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public class TaxBureauScreen extends AbstractContainerScreen<TaxBureauMenu> {
    private String lastTaxSignature = "";
    private String selectedBillId = "";
    private boolean adminReview;
    private EditBox rejectReasonInput;
    private int refundPage;
    private boolean showAllRefunds;
    private String selectedRefundId = "";
    private boolean showAnnualReport;

    public TaxBureauScreen(TaxBureauMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 320;
        imageHeight = 820;
        adminReview = Minecraft.getInstance().player != null && Minecraft.getInstance().player.hasPermissions(2);
        showAllRefunds = !adminReview;
    }

    @Override
    protected void init() {
        super.init();
        refreshWidgets();
    }

    private void refreshWidgets() {
        clearWidgets();
        int row = 0;
        for (TaxBill bill : menu.getTaxBills()) {
            if (row >= 7) break;
            addRenderableWidget(Button.builder(Component.literal("Detail"), button -> selectBill(bill))
                    .bounds(leftPos + 205, topPos + 28 + row * 24, 52, 20).build());
            if (bill.declared() && !bill.paid()) {
                addRenderableWidget(Button.builder(Component.literal("Pay"), button -> payTax(bill))
                        .bounds(leftPos + 262, topPos + 28 + row * 24, 42, 20).build());
            } else {
                addRenderableWidget(Button.builder(Component.literal("Declare"), button -> declareTax(bill))
                        .bounds(leftPos + 262, topPos + 28 + row * 24, 54, 20).build());
            }
            row++;
        }
        addRenderableWidget(Button.builder(Component.literal("Request " + Config.defaultCurrencyId().toUpperCase() + " Refund"), button -> requestRefund())
                .bounds(leftPos + 8, topPos + 196, 150, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Annual report"), button -> { showAnnualReport = !showAnnualReport; })
                .bounds(leftPos + 8, topPos + 520, 92, 20).build());
        if (adminReview) {
            rejectReasonInput = new EditBox(font, leftPos + 8, topPos + 370, 150, 20, Component.literal("Reject reason"));
            rejectReasonInput.setMaxLength(256);
            rejectReasonInput.setHint(Component.literal("Reason for rejection"));
            addRenderableWidget(rejectReasonInput);
            var visibleRefunds = visibleRefunds();
            for (int refundRow = 0; refundRow < visibleRefunds.size(); refundRow++) {
                TaxRefundSavedData.Request refund = visibleRefunds.get(refundRow);
                if (!"PENDING".equals(refund.status())) continue;
                int y = topPos + 396 + refundRow * 42;
                addRenderableWidget(Button.builder(Component.literal("Approve"), button -> review(refund, true)).bounds(leftPos + 8, y, 70, 20).build());
                addRenderableWidget(Button.builder(Component.literal("Reject"), button -> review(refund, false)).bounds(leftPos + 82, y, 70, 20).build());
                addRenderableWidget(Button.builder(Component.literal("Detail"), button -> selectedRefundId = refund.id()).bounds(leftPos + 156, y, 60, 20).build());
            }
        }
        addRenderableWidget(Button.builder(Component.literal(adminReview ? (showAllRefunds ? "Pending only" : "All requests") : "Refund history"),
                        button -> { if (adminReview) { showAllRefunds = !showAllRefunds; refundPage = 0; refreshWidgets(); } })
                .bounds(leftPos + 168, topPos + 360, 92, 20).build());
        addRenderableWidget(Button.builder(Component.literal("<"), button -> { if (refundPage > 0) { refundPage--; refreshWidgets(); } })
                .bounds(leftPos + 264, topPos + 360, 24, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), button -> { if ((refundPage + 1) * 3 < visibleRefunds().size()) { refundPage++; refreshWidgets(); } })
                .bounds(leftPos + 292, topPos + 360, 24, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Read all"), button -> manageNotifications("read", ""))
                .bounds(leftPos + 168, topPos + 520, 72, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Clear read"), button -> manageNotifications("clear_read", ""))
                .bounds(leftPos + 244, topPos + 520, 72, 20).build());
        for (int notificationRow = 0; notificationRow < Math.min(7, menu.getRefundNotifications().size()); notificationRow++) {
            var notification = menu.getRefundNotifications().get(notificationRow);
            if (notification.requestId().isBlank()) continue;
            addRenderableWidget(Button.builder(Component.literal("Open"), button -> openNotification(notification))
                    .bounds(leftPos + 100, topPos + 536 + notificationRow * 18, 52, 18).build());
            addRenderableWidget(Button.builder(Component.literal("X"), button -> manageNotifications("delete_one", notification.requestId()))
                    .bounds(leftPos + 154, topPos + 536 + notificationRow * 18, 18, 18).build());
        }
        if (adminReview) {
            int correctionRow = 0;
            for (var request : menu.getCorrectionRequests()) {
                if (!"PENDING".equals(request.status()) || correctionRow >= 3) continue;
                int y = topPos + 620 + correctionRow * 22;
                addRenderableWidget(Button.builder(Component.literal("Approve"), button -> reviewCorrection(request.id(), true)).bounds(leftPos + 210, y, 55, 18).build());
                addRenderableWidget(Button.builder(Component.literal("Reject"), button -> reviewCorrection(request.id(), false)).bounds(leftPos + 268, y, 48, 18).build());
                correctionRow++;
            }
        }
        lastTaxSignature = taxSignature();
    }

    private String taxSignature() {
        return menu.getTaxBills().stream()
                .map(bill -> bill.id() + ":" + bill.paidAmount() + ":" + bill.amount())
                .reduce("", (left, right) -> left + right)
                + menu.getRefunds().stream().map(refund -> refund.id() + ":" + refund.status() + ":" + refund.reason()).reduce("", String::concat)
                + menu.getIndividualPeriods().stream().map(period -> period.businessId() + ":" + period.periodEnd()
                + ":" + period.revenue() + ":" + period.expenses()).reduce("", String::concat);
    }

    private java.util.List<TaxRefundSavedData.Request> visibleRefunds() {
        var filtered = menu.getRefunds().stream()
                .filter(refund -> !adminReview || showAllRefunds || "PENDING".equals(refund.status()))
                .toList();
        int start = Math.min(refundPage * 3, Math.max(0, filtered.size()));
        return filtered.subList(start, Math.min(start + 3, filtered.size()));
    }

    private void selectBill(TaxBill bill) { selectedBillId = bill.id(); }

    private void payTax(TaxBill bill) {
        PacketDistributor.sendToServer(new PayUnifiedTaxPayload(bill.id()));
    }

    private void declareTax(TaxBill bill) {
        PacketDistributor.sendToServer(new DeclareTaxPayload(bill.id()));
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, Component.literal("Tax Bureau"), 8, 6, GuiStyles.ACCENT, false);
        graphics.drawString(font, Component.literal("Tax bills"), 8, 17, GuiStyles.TEXT_DIM, false);
        long now = Minecraft.getInstance().level == null ? 0L : Minecraft.getInstance().level.getGameTime();
        int row = 0;
        for (TaxBill bill : menu.getTaxBills()) {
            if (row >= 7) break;
            String subject = bill.subject().subjectId();
            if (subject.length() > 16) subject = subject.substring(0, 16);
            String amount = bill.currencyId().toUpperCase() + " " + Money.format(bill.outstanding());
            TaxBill.Status status = bill.status(now);
            graphics.drawString(font, Component.literal(bill.subject().type().displayName() + " / " + subject), 8, 30 + row * 24, GuiStyles.TEXT, false);
            graphics.drawString(font, Component.literal(amount + " / " + statusName(status)), 8, 41 + row * 24, status == TaxBill.Status.FROZEN ? 0xFFFF6666 : GuiStyles.TEXT_DIM, false);
            row++;
        }
        if (menu.getTaxBills().isEmpty()) graphics.drawString(font, Component.literal("No tax records"), 8, 34, GuiStyles.TEXT_DIM, false);
        graphics.drawString(font, Component.literal("Tax credit balance: " + Money.format(menu.getCreditBalance())),
                8, 188, GuiStyles.ACCENT, false);
        if (adminReview) graphics.drawString(font, Component.literal("Reject reason"), 8, 362, GuiStyles.TEXT_DIM, false);
        graphics.drawString(font, Component.literal("Rules: auto <= " + Money.format(Config.TAX_REFUND_AUTO_APPROVAL_LIMIT.get())
                        + ", single <= " + Money.format(Config.TAX_REFUND_MAX_SINGLE_AMOUNT.get())
                        + ", " + Config.TAX_REFUND_MAX_REQUESTS_PER_PERIOD.get() + " requests / "
                        + Config.TAX_REFUND_PERIOD_DAYS.get() + " days"),
                8, 210, GuiStyles.TEXT_DIM, false);
        renderDetails(graphics);
        renderRefunds(graphics);
        if (showAnnualReport) renderAnnualReport(graphics);
        else renderRefundDetail(graphics);
        renderNotifications(graphics);
        renderCorrectionAudits(graphics);
        renderCorrectionRequests(graphics);
    }

    private void renderCorrectionRequests(GuiGraphics graphics) {
        int y = 610;
        graphics.drawString(font, Component.literal("Correction applications"), 168, y, GuiStyles.ACCENT, false);
        int row = 0;
        for (var request : menu.getCorrectionRequests()) {
            if (row >= 3) break;
            String line = request.id().substring(0, Math.min(8, request.id().length())) + " / " + request.status()
                    + " / period " + request.periodEnd();
            graphics.drawString(font, Component.literal(line), 168, y + 14 + row * 22, GuiStyles.TEXT, false);
            row++;
        }
        if (row == 0) graphics.drawString(font, Component.literal("No applications"), 168, y + 14, GuiStyles.TEXT_DIM, false);
    }

    private void renderCorrectionAudits(GuiGraphics graphics) {
        int y = 690;
        graphics.drawString(font, Component.literal("Tax corrections"), 8, y, GuiStyles.ACCENT, false);
        var audits = menu.getCorrectionAudits();
        if (audits.isEmpty()) {
            graphics.drawString(font, Component.literal("No correction records"), 8, y + 14, GuiStyles.TEXT_DIM, false);
            return;
        }
        for (int i = 0; i < Math.min(4, audits.size()); i++) {
            var audit = audits.get(i);
            String reason = audit.reason().isBlank() ? "-" : audit.reason();
            if (reason.length() > 34) reason = reason.substring(0, 34);
            graphics.drawString(font, Component.literal("period " + audit.periodEnd() + " / tax diff "
                    + Money.format(Math.abs(audit.difference())) + " / " + audit.administrator()),
                    8, y + 14 + i * 28, GuiStyles.TEXT, false);
            graphics.drawString(font, Component.literal("收入 " + audit.oldRevenue() + " -> " + audit.newRevenue()
                    + "，成本 " + audit.oldExpenses() + " -> " + audit.newExpenses() + " / " + reason),
                    8, y + 26 + i * 28, GuiStyles.TEXT_DIM, false);
        }
    }

    private void requestRefund() {
        if (menu.getCreditBalance() > 0L) {
            PacketDistributor.sendToServer(new RequestTaxRefundPayload(Config.defaultCurrencyId(), menu.getCreditBalance()));
        }
    }

    private void review(TaxRefundSavedData.Request refund, boolean approve) {
        String reason = rejectReasonInput == null ? "" : rejectReasonInput.getValue().trim();
        PacketDistributor.sendToServer(new ReviewTaxRefundPayload(refund.id(), approve, approve ? "" : reason));
    }

    private void manageNotifications(String action, String requestId) {
        PacketDistributor.sendToServer(new ManageTaxRefundNotificationsPayload(action, requestId));
    }

    private void reviewCorrection(String id, boolean approve) {
        String reason = approve ? "Tax bureau approved" : (rejectReasonInput == null ? "" : rejectReasonInput.getValue().trim());
        if (!approve && reason.isBlank()) reason = "No reason provided";
        PacketDistributor.sendToServer(new ReviewTaxCorrectionRequestPayload(id, approve,
                reason));
    }

    private void openNotification(TaxRefundNotificationSavedData.Notification notification) {
        selectedRefundId = notification.requestId();
        if (!notification.requestId().isBlank()) manageNotifications("read_one", notification.requestId());
    }

    private void renderDetails(GuiGraphics graphics) {
        TaxBill bill = menu.getTaxBills().stream().filter(candidate -> candidate.id().equals(selectedBillId))
                .findFirst().orElse(menu.getTaxBills().isEmpty() ? null : menu.getTaxBills().get(0));
        if (bill == null) return;
        int y = 224;
        graphics.drawString(font, Component.literal("Tax bill details"), 8, y, GuiStyles.ACCENT, false);
        graphics.drawString(font, Component.literal("Period: " + bill.periodStart() + " - " + bill.periodEnd()), 8, y + 14, GuiStyles.TEXT_DIM, false);
        graphics.drawString(font, Component.literal("Tax base: " + bill.currencyId().toUpperCase() + " " + Money.format(bill.taxableBase())), 8, y + 28, GuiStyles.TEXT_DIM, false);
        graphics.drawString(font, Component.literal("Rate: " + (bill.rateBasisPoints() / 100.0) + "%"), 8, y + 42, GuiStyles.TEXT_DIM, false);
        boolean hasBusinessPeriod = bill.subject().type() == com.ailudick.capitalismmod.tax.TaxType.INDIVIDUAL_BUSINESS_INCOME
                && menu.getIndividualPeriods().stream().anyMatch(candidate -> candidate.businessId().equals(bill.subject().subjectId())
                && candidate.periodStart() == bill.periodStart() && candidate.periodEnd() == bill.periodEnd());
        int detailShift = hasBusinessPeriod ? 140 : 0;
        if (bill.subject().type() == com.ailudick.capitalismmod.tax.TaxType.INDIVIDUAL_BUSINESS_INCOME) {
            var period = menu.getIndividualPeriods().stream()
                    .filter(candidate -> candidate.businessId().equals(bill.subject().subjectId())
                            && candidate.periodStart() == bill.periodStart()
                            && candidate.periodEnd() == bill.periodEnd())
                    .findFirst().orElse(null);
            if (period != null) {
                long profit = Math.max(0L, period.revenue() - Math.min(period.revenue(), period.expenses()));
                graphics.drawString(font, Component.literal("Business revenue: USD " + period.revenue()), 8, y + 56, GuiStyles.TEXT_DIM, false);
                graphics.drawString(font, Component.literal("Deductible procurement: USD " + period.expenses()), 8, y + 70, GuiStyles.TEXT_DIM, false);
                graphics.drawString(font, Component.literal("Taxable business income: USD " + profit), 8, y + 84, GuiStyles.TEXT_DIM, false);
                var vouchers = menu.getIndividualExpenses().stream()
                        .filter(expense -> expense.subjectId().equals(bill.subject().subjectId())
                                && expense.occurredAt() >= bill.periodStart() && expense.occurredAt() < bill.periodEnd())
                        .toList();
                graphics.drawString(font, Component.literal("Expense vouchers: " + vouchers.size()), 8, y + 98, GuiStyles.TEXT_DIM, false);
                for (int row = 0; row < Math.min(3, vouchers.size()); row++) {
                    var voucher = vouchers.get(row);
                    String details = voucher.details().isBlank() ? voucher.sourceId() : voucher.details();
                    if (details.length() > 52) details = details.substring(0, 52);
                    graphics.drawString(font, Component.literal("Voucher: " + details + " / USD " + voucher.amount()),
                            8, y + 112 + row * 12, GuiStyles.TEXT_DIM, false);
                }
                var incomes = menu.getIndividualIncomes().stream()
                        .filter(income -> income.subjectId().equals(bill.subject().subjectId())
                                && income.occurredAt() >= bill.periodStart() && income.occurredAt() < bill.periodEnd())
                        .toList();
                graphics.drawString(font, Component.literal("Income vouchers: " + incomes.size()), 8, y + 152, GuiStyles.TEXT_DIM, false);
                for (int row = 0; row < Math.min(2, incomes.size()); row++) {
                    var income = incomes.get(row);
                    String details = income.details().isBlank() ? income.sourceId() : income.details();
                    if (details.length() > 52) details = details.substring(0, 52);
                    graphics.drawString(font, Component.literal("Income: " + details + " / USD " + income.amount()),
                            8, y + 164 + row * 12, GuiStyles.TEXT_DIM, false);
                }
            }
        }
        graphics.drawString(font, Component.literal("Source: " + bill.sourceEventId()), 8, y + 56 + detailShift, GuiStyles.TEXT_DIM, false);
        graphics.drawString(font, Component.literal("Paid: " + bill.currencyId().toUpperCase() + " " + Money.format(bill.paidAmount())), 8, y + 70 + detailShift, GuiStyles.TEXT_DIM, false);
        graphics.drawString(font, Component.literal("Late fee: " + bill.currencyId().toUpperCase() + " " + Money.format(bill.lateFeeAmount())), 8, y + 84 + detailShift, GuiStyles.TEXT_DIM, false);
        graphics.drawString(font, Component.literal("Declaration due: " + bill.declarationDueAt()), 8, y + 98 + detailShift, GuiStyles.TEXT_DIM, false);
        graphics.drawString(font, Component.literal("Payment due: " + bill.dueAt()), 8, y + 112 + detailShift, GuiStyles.TEXT_DIM, false);
        long detailNow = Minecraft.getInstance().level == null ? 0L : Minecraft.getInstance().level.getGameTime();
        if (bill.status(detailNow) == TaxBill.Status.DECLARATION_OVERDUE) {
            graphics.drawString(font, Component.literal("Declaration overdue by "
                    + bill.declarationDaysLate(detailNow) + " day(s)"),
                    8, y + 126 + detailShift, 0xFFFF6666, false);
        }
        int paymentY = y + 140 + detailShift;
        graphics.drawString(font, Component.literal("Payment history"), 8, paymentY, GuiStyles.ACCENT, false);
        var payments = menu.getTaxPayments().stream().filter(payment -> payment.billId().equals(bill.id())).toList();
        if (payments.isEmpty()) graphics.drawString(font, Component.literal("No payments"), 8, paymentY + 14, GuiStyles.TEXT_DIM, false);
        else for (int row = 0; row < Math.min(3, payments.size()); row++) {
            TaxPayment payment = payments.get(row);
            graphics.drawString(font, Component.literal(payment.currencyId().toUpperCase() + " " + Money.format(payment.amount()) + " @ " + payment.paidAt()), 8, paymentY + 14 + row * 14, GuiStyles.TEXT_DIM, false);
        }
    }

    private void renderRefunds(GuiGraphics graphics) {
        int y = 382;
        graphics.drawString(font, Component.literal("Refund applications"), 168, y, GuiStyles.ACCENT, false);
        var visibleRefunds = visibleRefunds();
        if (visibleRefunds.isEmpty()) {
            graphics.drawString(font, Component.literal("No refund applications"), 168, y + 14, GuiStyles.TEXT_DIM, false);
            return;
        }
        int row = 0;
        for (TaxRefundSavedData.Request refund : visibleRefunds) {
            String status = refund.status();
            String line = refund.currencyId().toUpperCase() + " " + Money.format(refund.amount()) + " / " + status;
            graphics.drawString(font, Component.literal(line), 168, y + 14 + row * 28, GuiStyles.TEXT, false);
            String reason = refund.reason();
            if (reason.length() > 28) reason = reason.substring(0, 28);
            graphics.drawString(font, Component.literal(reason), 168, y + 26 + row * 42, GuiStyles.TEXT_DIM, false);
            String source = refund.sourceSummary();
            if (source.length() > 28) source = source.substring(0, 28);
            graphics.drawString(font, Component.literal("Source: " + source), 168, y + 38 + row * 42, GuiStyles.TEXT_DIM, false);
            String allocations = refund.allocations();
            if (!refund.allocationDetails().isEmpty()) {
                var detail = refund.allocationDetails().get(0);
                allocations = detail.sourceId() + " -> " + Money.format(detail.refundAmount());
                if (refund.allocationDetails().size() > 1) allocations += " +" + (refund.allocationDetails().size() - 1);
            }
            if (allocations.length() > 28) allocations = allocations.substring(0, 28);
            graphics.drawString(font, Component.literal("Used: " + allocations), 168, y + 50 + row * 42, GuiStyles.TEXT_DIM, false);
            row++;
        }
    }

    private void renderRefundDetail(GuiGraphics graphics) {
        TaxRefundSavedData.Request refund = menu.getRefunds().stream()
                .filter(candidate -> candidate.id().equals(selectedRefundId)).findFirst().orElse(null);
        if (refund == null) {
            if (!selectedRefundId.isBlank()) graphics.drawString(font, Component.literal("Refund application unavailable"), 8, 540, 0xFFFF6666, false);
            return;
        }
        int y = 540;
        graphics.drawString(font, Component.literal("Refund application detail"), 8, y, GuiStyles.ACCENT, false);
        graphics.drawString(font, Component.literal("Applicant: " + refund.taxpayerUuid()), 8, y + 14, GuiStyles.TEXT_DIM, false);
        graphics.drawString(font, Component.literal("Amount: " + refund.currencyId().toUpperCase() + " " + Money.format(refund.amount())
                + " / " + refund.status()), 8, y + 28, GuiStyles.TEXT_DIM, false);
        graphics.drawString(font, Component.literal("Requested: " + refund.requestedAt() + "  Reviewed: " + refund.reviewedAt()), 8, y + 42, GuiStyles.TEXT_DIM, false);
        graphics.drawString(font, Component.literal("Reviewer: " + refund.reviewer()), 8, y + 56, GuiStyles.TEXT_DIM, false);
        String reason = refund.reason();
        if (reason.length() > 90) reason = reason.substring(0, 90);
        graphics.drawString(font, Component.literal("Reason: " + reason), 8, y + 70, GuiStyles.TEXT_DIM, false);
        graphics.drawString(font, Component.literal("Allocations"), 8, y + 86, GuiStyles.ACCENT, false);
        int row = 0;
        for (var allocation : refund.allocationDetails()) {
            if (row >= 4) break;
            graphics.drawString(font, Component.literal(allocation.sourceId() + " / " + allocation.subjectType()
                    + " / period " + allocation.periodStart() + "-" + allocation.periodEnd()
                    + " / original " + Money.format(allocation.originalCredit())
                    + " / refund " + Money.format(allocation.refundAmount())), 8, y + 100 + row * 14, GuiStyles.TEXT_DIM, false);
            row++;
        }
        int auditY = y + 160;
        graphics.drawString(font, Component.literal("Audit timeline"), 8, auditY, GuiStyles.ACCENT, false);
        int auditRow = 0;
        for (var event : menu.getRefundAudit().stream().filter(event -> event.requestId().equals(refund.id())).toList()) {
            if (auditRow >= 5) break;
            String line = event.time() + " / " + event.action() + " / " + event.actor() + " / " + event.result();
            if (line.length() > 100) line = line.substring(0, 100);
            graphics.drawString(font, Component.literal(line), 8, auditY + 14 + auditRow * 14, GuiStyles.TEXT_DIM, false);
            auditRow++;
        }
    }

    private void renderAnnualReport(GuiGraphics graphics) {
        TaxAnnualReport report = menu.getAnnualReport();
        int y = 540;
        graphics.drawString(font, Component.literal("Annual tax report"), 8, y, GuiStyles.ACCENT, false);
        if (report == null) {
            graphics.drawString(font, Component.literal("Report unavailable"), 8, y + 16, GuiStyles.TEXT_DIM, false);
            return;
        }
        graphics.drawString(font, Component.literal("Year: " + report.year() + " (" + report.yearStart() + " - " + report.yearEnd() + ")"), 8, y + 16, GuiStyles.TEXT_DIM, false);
        graphics.drawString(font, Component.literal("Tax base: " + Money.format(report.taxableBase())), 8, y + 32, GuiStyles.TEXT_DIM, false);
        graphics.drawString(font, Component.literal("Assessed: " + Money.format(report.assessedTax())), 8, y + 48, GuiStyles.TEXT_DIM, false);
        graphics.drawString(font, Component.literal("Paid: " + Money.format(report.paidTax())), 8, y + 64, GuiStyles.TEXT_DIM, false);
        graphics.drawString(font, Component.literal("Refunds: " + Money.format(report.refunds()) + " (" + report.refundActions() + ")"), 8, y + 80, GuiStyles.TEXT_DIM, false);
        graphics.drawString(font, Component.literal("Outstanding: " + Money.format(report.outstandingTax())), 8, y + 96, GuiStyles.TEXT_DIM, false);
        graphics.drawString(font, Component.literal("Current credit: " + Money.format(report.currentCreditBalance())), 8, y + 112, GuiStyles.TEXT_DIM, false);
    }

    private void renderNotifications(GuiGraphics graphics) {
        int y = 540;
        long unread = menu.getRefundNotifications().stream().filter(notification -> !notification.read()).count();
        graphics.drawString(font, Component.literal("Refund notifications (unread: " + unread + ")"), 168, y, GuiStyles.ACCENT, false);
        if (menu.getRefundNotifications().isEmpty()) {
            graphics.drawString(font, Component.literal("No notifications"), 168, y + 14, GuiStyles.TEXT_DIM, false);
            return;
        }
        int row = 0;
        for (var notification : menu.getRefundNotifications()) {
            if (row >= 7) break;
            String message = notification.message();
            if (message.length() > 34) message = message.substring(0, 34);
            graphics.drawString(font, Component.literal((notification.read() ? "" : "* ") + message), 168, y + 14 + row * 18,
                    notification.read() ? GuiStyles.TEXT_DIM : GuiStyles.ACCENT, false);
            row++;
        }
    }

    private static String statusName(TaxBill.Status status) {
        return switch (status) {
            case OPEN -> "OPEN";
            case OVERDUE -> "OVERDUE";
            case DELINQUENT -> "DELINQUENT";
            case FROZEN -> "FROZEN";
            case PAID -> "PAID";
            case DECLARATION_REQUIRED -> "DECLARE";
            case DECLARATION_OVERDUE -> "DECLARATION OVERDUE";
        };
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        GuiStyles.drawBackground(graphics, leftPos, topPos, imageWidth, imageHeight);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!taxSignature().equals(lastTaxSignature)) refreshWidgets();
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
