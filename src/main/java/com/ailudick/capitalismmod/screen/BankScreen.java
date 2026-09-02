package com.ailudick.capitalismmod.screen;

import com.ailudick.capitalismmod.bank.BankAccount;
import com.ailudick.capitalismmod.bank.BankCardNumber;
import com.ailudick.capitalismmod.bank.BankTransaction;
import com.ailudick.capitalismmod.bank.TermDeposit;
import com.ailudick.capitalismmod.client.GuiStyles;
import com.ailudick.capitalismmod.currency.Currencies;
import com.ailudick.capitalismmod.currency.Currency;
import com.ailudick.capitalismmod.currency.Money;
import com.ailudick.capitalismmod.menu.BankMenu;
import com.ailudick.capitalismmod.network.payload.BankTransactionPayload;
import com.ailudick.capitalismmod.network.payload.ExchangePayload;
import com.ailudick.capitalismmod.network.payload.LoanPayload;
import com.ailudick.capitalismmod.network.payload.OpenAccountPayload;
import com.ailudick.capitalismmod.network.payload.ReplaceCardPayload;
import com.ailudick.capitalismmod.network.payload.TransferPayload;
import com.ailudick.capitalismmod.network.payload.OpenTermDepositPayload;
import com.ailudick.capitalismmod.network.payload.WithdrawTermDepositPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public class BankScreen extends AbstractContainerScreen<BankMenu> {
    private enum View { LOBBY, OPEN_ACCOUNT, DEPOSIT, TERM, TRANSFER, EXCHANGE, LOAN, REPLACE, TRANSACTIONS }

    private View view = View.LOBBY;

    private final List<String> accountIds = new ArrayList<>();
    private int selectedIndex = -1;
    private Currency selectedCurrency = Currencies.USD;
    private EditBox amountField;
    private Currency fromCurrency = Currencies.USD;
    private Currency toCurrency = Currencies.CNY;
    private EditBox exchangeAmountField;
    private EditBox targetAccountField;
    private int selectedTerm = 3;
    private int lastTermCount = -1;

    public BankScreen(BankMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageHeight = 245;
    }

    @Override
    protected void init() {
        super.init();
        rebuildView();
    }

    private void rebuildView() {
        clearWidgets();
        this.amountField = null;
        this.exchangeAmountField = null;
        this.targetAccountField = null;
        switch (view) {
            case LOBBY -> buildLobby();
            case OPEN_ACCOUNT -> buildOpenAccount();
            case DEPOSIT -> buildDeposit();
            case TERM -> buildTerm();
            case TRANSFER -> buildTransfer();
            case EXCHANGE -> buildExchange();
            case LOAN -> buildLoan();
            case REPLACE -> buildReplace();
            case TRANSACTIONS -> buildTransactions();
        }
    }

    private void buildLobby() {
        addRenderableWidget(Button.builder(Component.translatable("gui.capitalismmod.open_account"),
                        btn -> { view = View.OPEN_ACCOUNT; rebuildView(); })
                .bounds(leftPos + 8, topPos + 16, 160, 24).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.capitalismmod.deposit"),
                        btn -> { view = View.DEPOSIT; rebuildView(); })
                .bounds(leftPos + 8, topPos + 42, 160, 24).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.capitalismmod.term_deposit"),
                        btn -> { view = View.TERM; rebuildView(); })
                .bounds(leftPos + 8, topPos + 68, 160, 24).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.capitalismmod.transfer"),
                        btn -> { view = View.TRANSFER; rebuildView(); })
                .bounds(leftPos + 8, topPos + 94, 160, 24).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.capitalismmod.exchange"),
                        btn -> { view = View.EXCHANGE; rebuildView(); })
                .bounds(leftPos + 8, topPos + 120, 160, 24).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.capitalismmod.loan"),
                        btn -> { view = View.LOAN; rebuildView(); })
                .bounds(leftPos + 8, topPos + 146, 160, 24).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.capitalismmod.report_loss"),
                        btn -> { view = View.REPLACE; rebuildView(); })
                .bounds(leftPos + 8, topPos + 172, 160, 24).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.capitalismmod.transactions"),
                        btn -> { view = View.TRANSACTIONS; rebuildView(); })
                .bounds(leftPos + 8, topPos + 198, 160, 24).build());
    }

    private void buildOpenAccount() {
        addRenderableWidget(Button.builder(Component.translatable("gui.capitalismmod.open_debit"), btn -> openAccount(false))
                .bounds(leftPos + 8, topPos + 30, 160, 24).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.capitalismmod.open_credit"), btn -> openAccount(true))
                .bounds(leftPos + 8, topPos + 60, 160, 24).build());
        addBackButton();
    }

    private void buildDeposit() {
        buildAccountSwitcher();
        buildCurrencySelection(50);
        this.amountField = new EditBox(font, leftPos + 8, topPos + 80, 60, 20, Component.literal("1"));
        this.amountField.setFilter(s -> s.isEmpty() || s.matches("\\d+"));
        this.amountField.setValue("1");
        addRenderableWidget(this.amountField);
        addRenderableWidget(Button.builder(Component.translatable("gui.capitalismmod.deposit"), btn -> doTransaction(true))
                .bounds(leftPos + 76, topPos + 80, 45, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.capitalismmod.withdraw"), btn -> doTransaction(false))
                .bounds(leftPos + 123, topPos + 80, 45, 20).build());
        addBackButton();
    }

    private void buildExchange() {
        for (int i = 0; i < Currencies.ALL.size(); i++) {
            Currency currency = Currencies.ALL.get(i);
            addRenderableWidget(Button.builder(Component.translatable(currency.nameKey()), btn -> this.fromCurrency = currency)
                    .bounds(leftPos + 8 + i * 42, topPos + 30, 40, 20).build());
        }
        for (int i = 0; i < Currencies.ALL.size(); i++) {
            Currency currency = Currencies.ALL.get(i);
            addRenderableWidget(Button.builder(Component.translatable(currency.nameKey()), btn -> this.toCurrency = currency)
                    .bounds(leftPos + 8 + i * 42, topPos + 60, 40, 20).build());
        }
        this.exchangeAmountField = new EditBox(font, leftPos + 8, topPos + 90, 60, 20, Component.literal("1"));
        this.exchangeAmountField.setFilter(s -> s.isEmpty() || s.matches("\\d+"));
        this.exchangeAmountField.setValue("1");
        addRenderableWidget(this.exchangeAmountField);
        addRenderableWidget(Button.builder(Component.translatable("gui.capitalismmod.exchange"), btn -> doExchange())
                .bounds(leftPos + 76, topPos + 90, 80, 20).build());
        addBackButton();
    }

    private void buildLoan() {
        buildAccountSwitcher();
        buildCurrencySelection(50);
        this.amountField = new EditBox(font, leftPos + 8, topPos + 80, 60, 20, Component.literal("1"));
        this.amountField.setFilter(s -> s.isEmpty() || s.matches("\\d+"));
        this.amountField.setValue("1");
        addRenderableWidget(this.amountField);
        addRenderableWidget(Button.builder(Component.translatable("gui.capitalismmod.loan"), btn -> doLoan(false))
                .bounds(leftPos + 76, topPos + 80, 45, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.capitalismmod.repay"), btn -> doLoan(true))
                .bounds(leftPos + 123, topPos + 80, 45, 20).build());
        addBackButton();
    }

    private void buildAccountSwitcher() {
        addRenderableWidget(Button.builder(Component.literal("<"), btn -> select(-1))
                .bounds(leftPos + 8, topPos + 20, 20, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), btn -> select(1))
                .bounds(leftPos + 34, topPos + 20, 20, 20).build());
    }

    private void buildCurrencySelection(int y) {
        for (int i = 0; i < Currencies.ALL.size(); i++) {
            Currency currency = Currencies.ALL.get(i);
            addRenderableWidget(Button.builder(Component.translatable(currency.nameKey()), btn -> this.selectedCurrency = currency)
                    .bounds(leftPos + 8 + i * 42, topPos + y, 40, 20).build());
        }
    }

    private void addBackButton() {
        addRenderableWidget(Button.builder(Component.translatable("gui.capitalismmod.back"),
                        btn -> { view = View.LOBBY; rebuildView(); })
                .bounds(leftPos + 8, topPos + 220, 60, 20).build());
    }

    private void buildReplace() {
        buildAccountSwitcher();
        addRenderableWidget(Button.builder(Component.translatable("gui.capitalismmod.report_loss"), btn -> doReplace())
                .bounds(leftPos + 8, topPos + 50, 160, 24).build());
        addBackButton();
    }

    private void buildTransactions() {
        buildAccountSwitcher();
        addBackButton();
    }

    private void buildTerm() {
        buildAccountSwitcher();
        buildCurrencySelection(46);
        this.amountField = new EditBox(font, leftPos + 8, topPos + 74, 60, 20, Component.literal("1"));
        this.amountField.setFilter(s -> s.isEmpty() || s.matches("\\d+"));
        this.amountField.setValue("1");
        addRenderableWidget(this.amountField);
        int[] terms = {3, 7, 30};
        for (int i = 0; i < terms.length; i++) {
            int days = terms[i];
            addRenderableWidget(Button.builder(Component.translatable("gui.capitalismmod.days", days), btn -> this.selectedTerm = days)
                    .bounds(leftPos + 8 + i * 42, topPos + 100, 40, 20).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.capitalismmod.open_term"), btn -> openTerm())
                .bounds(leftPos + 8, topPos + 126, 100, 20).build());
        BankAccount account = selectedAccount();
        if (account != null) {
            int y = topPos + 150;
            for (int i = 0; i < Math.min(4, account.termDeposits().size()); i++) {
                final int index = i;
                addRenderableWidget(Button.builder(Component.translatable("gui.capitalismmod.term_withdraw"), btn -> withdrawTerm(index))
                        .bounds(leftPos + 118, y + i * 16, 50, 16).build());
            }
        }
        addBackButton();
    }

    private void buildTransfer() {
        buildAccountSwitcher();
        buildCurrencySelection(46);
        this.targetAccountField = new EditBox(font, leftPos + 8, topPos + 74, 160, 20, Component.literal(""));
        this.targetAccountField.setMaxLength(19);
        this.targetAccountField.setFilter(s -> s.matches("\\d*"));
        addRenderableWidget(this.targetAccountField);
        this.amountField = new EditBox(font, leftPos + 8, topPos + 100, 60, 20, Component.literal("1"));
        this.amountField.setFilter(s -> s.isEmpty() || s.matches("\\d+"));
        this.amountField.setValue("1");
        addRenderableWidget(this.amountField);
        addRenderableWidget(Button.builder(Component.translatable("gui.capitalismmod.transfer"), btn -> doTransfer())
                .bounds(leftPos + 76, topPos + 100, 60, 20).build());
        addBackButton();
    }

    private void openTerm() {
        String accountId = selectedAccountId();
        if (accountId == null || this.amountField == null) {
            return;
        }
        try {
            long amount = Long.parseLong(this.amountField.getValue());
            if (amount > 0) {
                PacketDistributor.sendToServer(new OpenTermDepositPayload(accountId, selectedCurrency.id(), amount, this.selectedTerm));
            }
        } catch (NumberFormatException ignored) {
        }
    }

    private void withdrawTerm(int index) {
        String accountId = selectedAccountId();
        if (accountId == null) {
            return;
        }
        PacketDistributor.sendToServer(new WithdrawTermDepositPayload(accountId, index));
    }

    private void doTransfer() {
        String fromId = selectedAccountId();
        if (fromId == null || this.amountField == null || this.targetAccountField == null) {
            return;
        }
        String targetId = this.targetAccountField.getValue();
        if (targetId.isEmpty()) {
            return;
        }
        try {
            long amount = Long.parseLong(this.amountField.getValue());
            if (amount > 0) {
                PacketDistributor.sendToServer(new TransferPayload(fromId, targetId, selectedCurrency.id(), amount));
            }
        } catch (NumberFormatException ignored) {
        }
    }

    private void doReplace() {
        String accountId = selectedAccountId();
        if (accountId == null) {
            return;
        }
        Minecraft.getInstance().setScreen(new ConfirmScreen(
                confirmed -> {
                    if (confirmed) {
                        PacketDistributor.sendToServer(new ReplaceCardPayload(accountId));
                    }
                    Minecraft.getInstance().setScreen(this);
                },
                Component.translatable("gui.capitalismmod.report_loss"),
                Component.translatable("gui.capitalismmod.report_loss_confirm", BankCardNumber.format(accountId))
        ));
    }

    private void refreshAccountIds() {
        accountIds.clear();
        accountIds.addAll(menu.getAccounts().keySet());
        if (accountIds.isEmpty()) {
            selectedIndex = -1;
        } else if (selectedIndex < 0) {
            selectedIndex = 0;
        } else if (selectedIndex >= accountIds.size()) {
            selectedIndex = accountIds.size() - 1;
        }
    }

    private void select(int delta) {
        refreshAccountIds();
        if (accountIds.isEmpty()) {
            return;
        }
        selectedIndex = (selectedIndex + delta + accountIds.size()) % accountIds.size();
    }

    private String selectedAccountId() {
        refreshAccountIds();
        if (selectedIndex < 0 || selectedIndex >= accountIds.size()) {
            return null;
        }
        return accountIds.get(selectedIndex);
    }

    private BankAccount selectedAccount() {
        String id = selectedAccountId();
        if (id == null) {
            return null;
        }
        return menu.getAccounts().get(id);
    }

    private void doTransaction(boolean deposit) {
        String accountId = selectedAccountId();
        if (accountId == null || this.amountField == null) {
            return;
        }
        try {
            long amount = Long.parseLong(this.amountField.getValue());
            if (amount > 0) {
                PacketDistributor.sendToServer(new BankTransactionPayload(accountId, selectedCurrency.id(), amount, deposit));
            }
        } catch (NumberFormatException ignored) {
        }
    }

    private void doLoan(boolean repay) {
        String accountId = selectedAccountId();
        if (accountId == null || this.amountField == null) {
            return;
        }
        try {
            long amount = Long.parseLong(this.amountField.getValue());
            if (amount <= 0) {
                return;
            }
            if (!repay) {
                Minecraft.getInstance().setScreen(new ConfirmScreen(
                        confirmed -> {
                            if (confirmed) {
                                PacketDistributor.sendToServer(new LoanPayload(accountId, selectedCurrency.id(), amount, false));
                            }
                            Minecraft.getInstance().setScreen(this);
                        },
                        Component.translatable("gui.capitalismmod.loan"),
                        Component.translatable("gui.capitalismmod.loan_confirm", amount, Component.translatable(selectedCurrency.nameKey()))
                ));
            } else {
                PacketDistributor.sendToServer(new LoanPayload(accountId, selectedCurrency.id(), amount, true));
            }
        } catch (NumberFormatException ignored) {
        }
    }

    private void doExchange() {
        if (this.exchangeAmountField == null) {
            return;
        }
        try {
            long amount = Long.parseLong(this.exchangeAmountField.getValue());
            if (amount > 0 && !this.fromCurrency.equals(this.toCurrency)) {
                PacketDistributor.sendToServer(new ExchangePayload(this.fromCurrency.id(), this.toCurrency.id(), amount));
            }
        } catch (NumberFormatException ignored) {
        }
    }

    private void openAccount(boolean credit) {
        if (credit) {
            Minecraft.getInstance().setScreen(new ConfirmScreen(
                    confirmed -> {
                        if (confirmed) {
                            PacketDistributor.sendToServer(new OpenAccountPayload(true));
                        }
                        Minecraft.getInstance().setScreen(this);
                    },
                    Component.translatable("gui.capitalismmod.open_credit"),
                    Component.translatable("gui.capitalismmod.open_credit_confirm")
            ));
        } else {
            PacketDistributor.sendToServer(new OpenAccountPayload(false));
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        GuiStyles.drawBackground(graphics, leftPos, topPos, imageWidth, imageHeight);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (view == View.TERM) {
            BankAccount account = selectedAccount();
            int count = account != null ? account.termDeposits().size() : 0;
            if (count != lastTermCount) {
                lastTermCount = count;
                rebuildView();
            }
        }
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);

        graphics.drawString(font, title, leftPos + 8, topPos + 6, GuiStyles.ACCENT, false);

        if (view == View.LOBBY) {
            BankAccount account = selectedAccount();
            if (account != null) {
                Component type = Component.translatable(account.credit() ? "gui.capitalismmod.credit" : "gui.capitalismmod.debit");
                graphics.drawString(font, type.copy().append(Component.literal("  " + BankCardNumber.format(account.id()))),
                        leftPos + 8, topPos + 180, GuiStyles.TEXT_DIM, false);
                for (int i = 0; i < Currencies.ALL.size(); i++) {
                    Currency currency = Currencies.ALL.get(i);
                    graphics.drawString(font, Component.translatable(currency.nameKey()).copy()
                                    .append(Component.literal(" " + Money.format(account.getBalance(currency.id())))),
                            leftPos + 90, topPos + 180 + i * 12, GuiStyles.TEXT, false);
                }
            } else {
                graphics.drawString(font, Component.translatable("gui.capitalismmod.no_account"),
                        leftPos + 8, topPos + 180, GuiStyles.TEXT_DIM, false);
            }
        } else if (view == View.TRANSACTIONS) {
            BankAccount account = selectedAccount();
            if (account != null && !account.transactions().isEmpty()) {
                List<BankTransaction> txs = account.transactions();
                int start = Math.max(0, txs.size() - 8);
                int y = topPos + 50;
                for (int i = start; i < txs.size(); i++) {
                    graphics.drawString(font, formatTransaction(txs.get(i)), leftPos + 8, y, GuiStyles.TEXT, false);
                    y += 12;
                }
            } else {
                graphics.drawString(font, Component.translatable("gui.capitalismmod.no_transactions"),
                        leftPos + 8, topPos + 50, GuiStyles.TEXT_DIM, false);
            }
        } else if (view == View.TERM) {
            BankAccount account = selectedAccount();
            if (account != null) {
                List<TermDeposit> terms = account.termDeposits();
                int y = topPos + 152;
                for (int i = 0; i < Math.min(4, terms.size()); i++) {
                    TermDeposit td = terms.get(i);
                    String currencyName = Currencies.exists(td.currencyId())
                            ? Component.translatable(Currencies.byId(td.currencyId()).nameKey()).getString()
                            : td.currencyId();
                    graphics.drawString(font, Money.format(td.principal()) + " " + currencyName + " " + td.daysRemaining() + "d",
                            leftPos + 8, y, GuiStyles.TEXT, false);
                    y += 16;
                }
            }
        } else if (view == View.TRANSFER) {
            graphics.drawString(font, Component.translatable("gui.capitalismmod.target_account"),
                    leftPos + 8, topPos + 62, GuiStyles.TEXT, false);
        } else if (view == View.LOAN) {
            BankAccount account = selectedAccount();
            if (account != null && account.getDebt(selectedCurrency.id()) > 0) {
                long debt = account.getDebt(selectedCurrency.id());
                String currencyName = Component.translatable(selectedCurrency.nameKey()).getString();
                String status = account.loanDaysRemaining() < 0
                        ? Component.translatable("gui.capitalismmod.loan_overdue").getString()
                        : Component.translatable("gui.capitalismmod.loan_due", account.loanDaysRemaining()).getString();
                graphics.drawString(font, Component.literal(Money.format(debt) + " " + currencyName + " · " + status),
                        leftPos + 8, topPos + 130, GuiStyles.TEXT_DIM, false);
            }
        }
    }

    private Component formatTransaction(BankTransaction tx) {
        String typeName = Component.translatable("gui.capitalismmod." + tx.type()).getString();
        String currencyName = Currencies.exists(tx.currencyId())
                ? Component.translatable(Currencies.byId(tx.currencyId()).nameKey()).getString()
                : tx.currencyId();
        String amount = (tx.amount() >= 0 ? "+" : "") + Money.format(tx.amount());
        return Component.literal(typeName + " " + amount + " " + currencyName);
    }
}
