package com.ailudick.capitalismmod.bank;

import com.ailudick.capitalismmod.Config;
import com.ailudick.capitalismmod.currency.Currencies;
import com.ailudick.capitalismmod.currency.Currency;
import com.ailudick.capitalismmod.currency.ExchangeRateProvider;
import com.ailudick.capitalismmod.currency.Money;
import com.ailudick.capitalismmod.event.AccountOpenedEvent;
import com.ailudick.capitalismmod.event.LoanTakenEvent;
import com.ailudick.capitalismmod.init.ModAttachments;
import com.ailudick.capitalismmod.util.EconomyMath;
import com.ailudick.capitalismmod.wallet.EconomyHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Central helpers for the player's bank accounts (open / deposit / withdraw / loan / repay / interest / term deposits / transfer).
 */
public final class BankAccountHelper {
    /** Minecraft days until a loan matures; overdue loans incur a penalty rate. */
    private static final int LOAN_TERM_DAYS = 30;
    /** Transfer fee divisor: amount / 1000 = 0.1%, minimum 1. */
    private static final long TRANSFER_FEE_DIVISOR = 1000L;
    /** Interest multiplier applied once a loan is overdue. */
    private static final double OVERDUE_RATE_MULTIPLIER = 2.0;

    private BankAccountHelper() {
    }

    public static Map<String, BankAccount> getAccounts(Player player) {
        return player.getData(ModAttachments.BANK_ACCOUNTS);
    }

    public static void setAccounts(Player player, Map<String, BankAccount> accounts) {
        player.setData(ModAttachments.BANK_ACCOUNTS, accounts);
    }

    public static BankAccount openAccount(Player player, boolean credit) {
        Map<String, BankAccount> accounts = getAccounts(player);
        String id;
        do {
            id = BankCardNumber.generate();
        } while (accounts.containsKey(id));

        BankAccount account = BankAccount.create(id, credit);
        Map<String, BankAccount> updated = new HashMap<>(accounts);
        updated.put(account.id(), account);
        setAccounts(player, updated);
        NeoForge.EVENT_BUS.post(new AccountOpenedEvent(player, account.id(), credit));
        return account;
    }

    public static BankAccount getAccount(Player player, String accountId) {
        return getAccounts(player).get(accountId);
    }

    /**
     * Applies one Minecraft day of interest to every account: deposit interest on balances,
     * loan interest on debts, and a tick on each term deposit (maturing it when due).
     * Rates are annual ({@link Config}), compounded daily.
     */
    public static void applyDailyInterest(Player player) {
        double depositRate = Config.DEPOSIT_RATE_PER_YEAR.get() / 365.0;
        double loanRate = Config.LOAN_RATE_PER_YEAR.get() / 365.0;
        Map<String, BankAccount> accounts = getAccounts(player);
        if (accounts.isEmpty()) {
            return;
        }

        Map<String, BankAccount> updated = new HashMap<>(accounts);
        boolean changed = false;
        for (BankAccount account : new HashMap<>(accounts).values()) {
            Map<String, Long> newBalances = new HashMap<>(account.balances());
            Map<String, Long> newDebts = new HashMap<>(account.debts());
            List<BankTransaction> txs = new ArrayList<>(account.transactions());
            for (Map.Entry<String, Long> entry : newBalances.entrySet()) {
                long interest = (long) (entry.getValue() * depositRate);
                if (interest > 0) {
                    entry.setValue(entry.getValue() + interest);
                    txs.add(new BankTransaction("interest", entry.getKey(), interest));
                    changed = true;
                }
            }
            boolean hasDebt = newDebts.values().stream().anyMatch(v -> v > 0);
            int loanDaysRemaining = account.loanDaysRemaining();
            if (hasDebt) {
                loanDaysRemaining--;
                changed = true;
            }
            boolean overdue = hasDebt && loanDaysRemaining < 0;
            double effectiveLoanRate = overdue ? loanRate * OVERDUE_RATE_MULTIPLIER : loanRate;
            for (Map.Entry<String, Long> entry : newDebts.entrySet()) {
                long interest = (long) (entry.getValue() * effectiveLoanRate);
                if (interest > 0) {
                    entry.setValue(entry.getValue() + interest);
                    txs.add(new BankTransaction("interest", entry.getKey(), -interest));
                    changed = true;
                }
            }
            // Tick term deposits; mature any that have reached their due date.
            List<TermDeposit> newTerms = new ArrayList<>();
            for (TermDeposit term : account.termDeposits()) {
                TermDeposit ticked = term.tick();
                if (ticked.daysRemaining() <= 0) {
                    long payout = term.principal() + term.interest();
                    newBalances.merge(term.currencyId(), payout, Long::sum);
                    txs.add(new BankTransaction("term_maturity", term.currencyId(), payout));
                } else {
                    newTerms.add(ticked);
                }
                changed = true;
            }
            updated.put(account.id(), account.withBalancesAndDebts(newBalances, newDebts)
                    .withTransactions(txs).withTermDeposits(newTerms).withLoanDaysRemaining(loanDaysRemaining));
        }
        if (changed) {
            setAccounts(player, updated);
        }
    }

    /** Transfers {@code amount} of {@code currency} between physical items and the account. deposit=true moves items -> account. */
    public static boolean transfer(Player player, String accountId, Currency currency, long amount, boolean deposit) {
        if (amount <= 0) {
            return false;
        }
        BankAccount account = getAccount(player, accountId);
        if (account == null) {
            return false;
        }
        long accountBalance = account.getBalance(currency.id());

        if (deposit) {
            // physical items -> account
            if (EconomyHelper.countItems(player, currency) < amount) {
                return false;
            }
            EconomyHelper.consumeItems(player, currency, amount);
            account = account.withBalance(currency.id(), accountBalance + amount)
                    .withTransaction(new BankTransaction("deposit", currency.id(), amount));
        } else {
            // account -> physical items
            if (accountBalance < amount) {
                return false;
            }
            account = account.withBalance(currency.id(), accountBalance - amount)
                    .withTransaction(new BankTransaction("withdraw", currency.id(), -amount));
            EconomyHelper.giveMoney(player, currency, amount);
        }

        updateAccount(player, account);
        return true;
    }

    /** Takes a loan of {@code amount} of {@code currency} on a credit account, subject to the credit limit. */
    public static boolean loan(Player player, String accountId, Currency currency, long amount) {
        if (amount <= 0) {
            return false;
        }
        BankAccount account = getAccount(player, accountId);
        if (account == null || !account.credit()) {
            return false;
        }

        long newDebtInBase = EconomyMath.multiply(amount, ExchangeRateProvider.effective(currency)) / Money.MINOR_UNITS_PER_UNIT;
        if (newDebtInBase < 0) {
            return false;
        }
        long existingDebtInBase = totalDebtInBase(account);
        long combined = EconomyMath.add(existingDebtInBase, newDebtInBase);
        if (combined < 0 || combined > Config.CREDIT_LIMIT.get()) {
            return false;
        }

        int loanDays = existingDebtInBase == 0 ? LOAN_TERM_DAYS : account.loanDaysRemaining();
        EconomyHelper.giveMoney(player, currency, amount);
        updateAccount(player, account.withDebt(currency.id(), account.getDebt(currency.id()) + amount)
                .withLoanDaysRemaining(loanDays)
                .withTransaction(new BankTransaction("loan", currency.id(), amount)));
        NeoForge.EVENT_BUS.post(new LoanTakenEvent(player, accountId, currency.id(), amount));
        return true;
    }

    /** Repays {@code amount} of {@code currency} from items/accounts toward the account's debt. */
    public static boolean repay(Player player, String accountId, Currency currency, long amount) {
        if (amount <= 0) {
            return false;
        }
        BankAccount account = getAccount(player, accountId);
        if (account == null) {
            return false;
        }
        long debt = account.getDebt(currency.id());
        if (debt < amount) {
            return false;
        }
        if (!EconomyHelper.tryPay(player, currency, amount)) {
            return false;
        }
        BankAccount updated = account.withDebt(currency.id(), debt - amount)
                .withTransaction(new BankTransaction("repay", currency.id(), -amount));
        if (totalDebtInBase(updated) == 0) {
            updated = updated.withLoanDaysRemaining(0);
        }
        updateAccount(player, updated);
        return true;
    }

    /** Opens a fixed-term deposit, moving {@code amount} out of the demand balance for {@code termDays} days. */
    public static boolean openTermDeposit(Player player, String accountId, String currencyId, long amount, int termDays) {
        if (amount <= 0 || termDays <= 0 || !Currencies.exists(currencyId)) {
            return false;
        }
        BankAccount account = getAccount(player, accountId);
        if (account == null || account.getBalance(currencyId) < amount) {
            return false;
        }
        double dailyRate = Config.TERM_DEPOSIT_RATE_PER_YEAR.get() / 365.0;
        long interest = (long) (amount * dailyRate * termDays);
        List<TermDeposit> terms = new ArrayList<>(account.termDeposits());
        terms.add(new TermDeposit(currencyId, amount, interest, termDays));
        account = account.withBalance(currencyId, account.getBalance(currencyId) - amount)
                .withTermDeposits(terms)
                .withTransaction(new BankTransaction("term_deposit", currencyId, -amount));
        updateAccount(player, account);
        return true;
    }

    /** Withdraws a term deposit early, returning the principal only (forfeiting interest). */
    public static boolean withdrawTermDeposit(Player player, String accountId, int index) {
        BankAccount account = getAccount(player, accountId);
        if (account == null || index < 0 || index >= account.termDeposits().size()) {
            return false;
        }
        TermDeposit term = account.termDeposits().get(index);
        List<TermDeposit> terms = new ArrayList<>(account.termDeposits());
        terms.remove(index);
        account = account.withBalance(term.currencyId(), account.getBalance(term.currencyId()) + term.principal())
                .withTermDeposits(terms)
                .withTransaction(new BankTransaction("term_withdraw", term.currencyId(), term.principal()));
        updateAccount(player, account);
        return true;
    }

    /** Transfers {@code amount} between two accounts by number. Both players must be online. */
    public static boolean transferBetween(ServerPlayer sender, String fromAccountId, String targetAccountId, String currencyId, long amount) {
        if (amount <= 0 || !Currencies.exists(currencyId) || fromAccountId.equals(targetAccountId)) {
            return false;
        }
        ServerPlayer target = findAccountOwner(sender.getServer(), targetAccountId);
        if (target == null) {
            return false;
        }
        BankAccount from = getAccount(sender, fromAccountId);
        BankAccount to = getAccount(target, targetAccountId);
        long fee = transferFee(amount);
        long total = EconomyMath.add(amount, fee);
        if (from == null || to == null || total < 0 || from.getBalance(currencyId) < total) {
            return false;
        }
        updateAccount(sender, from.withBalance(currencyId, from.getBalance(currencyId) - total)
                .withTransaction(new BankTransaction("transfer_out", currencyId, -total)));
        updateAccount(target, to.withBalance(currencyId, to.getBalance(currencyId) + amount)
                .withTransaction(new BankTransaction("transfer_in", currencyId, amount)));
        return true;
    }

    private static long transferFee(long amount) {
        return Math.max(1L, amount / TRANSFER_FEE_DIVISOR);
    }

    private static ServerPlayer findAccountOwner(MinecraftServer server, String accountId) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (getAccounts(player).containsKey(accountId)) {
                return player;
            }
        }
        return null;
    }

    private static long totalDebtInBase(BankAccount account) {
        long total = 0;
        for (Map.Entry<String, Long> entry : account.debts().entrySet()) {
            if (Currencies.exists(entry.getKey())) {
                long product = EconomyMath.multiply(entry.getValue(), ExchangeRateProvider.effective(Currencies.byId(entry.getKey()))) / Money.MINOR_UNITS_PER_UNIT;
                if (product < 0) {
                    return Long.MAX_VALUE;
                }
                total = EconomyMath.add(total, product);
                if (total < 0) {
                    return Long.MAX_VALUE;
                }
            }
        }
        return total;
    }

    private static void updateAccount(Player player, BankAccount updated) {
        Map<String, BankAccount> accounts = new HashMap<>(getAccounts(player));
        accounts.put(updated.id(), updated);
        setAccounts(player, accounts);
    }
}
