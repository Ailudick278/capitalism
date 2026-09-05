package com.ailudick.capitalismmod.menu;

import com.ailudick.capitalismmod.company.Company;
import com.ailudick.capitalismmod.tax.TaxBill;
import com.ailudick.capitalismmod.tax.TaxPayment;
import com.ailudick.capitalismmod.tax.TaxRefundSavedData;
import com.ailudick.capitalismmod.tax.TaxRefundAuditSavedData;
import com.ailudick.capitalismmod.tax.TaxRefundNotificationSavedData;
import com.ailudick.capitalismmod.tax.TaxAnnualReport;
import com.ailudick.capitalismmod.tax.IndividualTaxPeriodSavedData;
import com.ailudick.capitalismmod.tax.TaxExpenseLedgerSavedData;
import com.ailudick.capitalismmod.tax.TaxIncomeVoucherLedgerSavedData;
import com.ailudick.capitalismmod.tax.TaxCorrectionAuditSavedData;
import com.ailudick.capitalismmod.tax.TaxCorrectionRequestSavedData;
import com.ailudick.capitalismmod.init.ModMenuTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class TaxBureauMenu extends AbstractContainerMenu {
    private Map<String, Company> companies = new HashMap<>();
    private List<TaxBill> bills = List.of();
    private List<TaxPayment> payments = List.of();
    private long creditBalance;
    private List<TaxRefundSavedData.Request> refunds = List.of();
    private List<TaxRefundAuditSavedData.Event> refundAudit = List.of();
    private List<TaxRefundNotificationSavedData.Notification> refundNotifications = List.of();
    private TaxAnnualReport annualReport;
    private List<IndividualTaxPeriodSavedData.Entry> individualPeriods = List.of();
    private List<TaxExpenseLedgerSavedData.Expense> individualExpenses = List.of();
    private List<TaxIncomeVoucherLedgerSavedData.Voucher> individualIncomes = List.of();
    private List<TaxCorrectionAuditSavedData.Entry> correctionAudits = List.of();
    private List<TaxCorrectionRequestSavedData.Request> correctionRequests = List.of();

    public TaxBureauMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.TAX_BUREAU_MENU.get(), containerId);
    }

    public Map<String, Company> getCompanies() {
        return companies;
    }

    public void setCompanies(Map<String, Company> companies) {
        this.companies = companies;
    }

    public List<TaxBill> getTaxBills() { return bills; }
    public void setTaxBills(List<TaxBill> bills) { this.bills = bills; }
    public List<TaxPayment> getTaxPayments() { return payments; }
    public void setTaxPayments(List<TaxPayment> payments) { this.payments = payments; }
    public long getCreditBalance() { return creditBalance; }
    public void setCreditBalance(long creditBalance) { this.creditBalance = Math.max(0L, creditBalance); }
    public List<TaxRefundSavedData.Request> getRefunds() { return refunds; }
    public void setRefunds(List<TaxRefundSavedData.Request> refunds) { this.refunds = refunds; }
    public List<TaxRefundAuditSavedData.Event> getRefundAudit() { return refundAudit; }
    public void setRefundAudit(List<TaxRefundAuditSavedData.Event> refundAudit) { this.refundAudit = refundAudit; }
    public List<TaxRefundNotificationSavedData.Notification> getRefundNotifications() { return refundNotifications; }
    public void setRefundNotifications(List<TaxRefundNotificationSavedData.Notification> notifications) { this.refundNotifications = notifications; }
    public TaxAnnualReport getAnnualReport() { return annualReport; }
    public void setAnnualReport(TaxAnnualReport annualReport) { this.annualReport = annualReport; }
    public List<IndividualTaxPeriodSavedData.Entry> getIndividualPeriods() { return individualPeriods; }
    public void setIndividualPeriods(List<IndividualTaxPeriodSavedData.Entry> periods) { this.individualPeriods = periods; }
    public List<TaxExpenseLedgerSavedData.Expense> getIndividualExpenses() { return individualExpenses; }
    public void setIndividualExpenses(List<TaxExpenseLedgerSavedData.Expense> expenses) { this.individualExpenses = expenses; }
    public List<TaxIncomeVoucherLedgerSavedData.Voucher> getIndividualIncomes() { return individualIncomes; }
    public void setIndividualIncomes(List<TaxIncomeVoucherLedgerSavedData.Voucher> incomes) { this.individualIncomes = incomes; }
    public List<TaxCorrectionAuditSavedData.Entry> getCorrectionAudits() { return correctionAudits; }
    public void setCorrectionAudits(List<TaxCorrectionAuditSavedData.Entry> audits) { this.correctionAudits = audits; }
    public List<TaxCorrectionRequestSavedData.Request> getCorrectionRequests() { return correctionRequests; }
    public void setCorrectionRequests(List<TaxCorrectionRequestSavedData.Request> requests) { this.correctionRequests = requests; }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
