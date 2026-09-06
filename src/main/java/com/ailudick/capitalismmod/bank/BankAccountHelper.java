package com.ailudick.capitalismmod.bank;

import com.ailudick.capitalismmod.Config;
import com.ailudick.capitalismmod.calendar.PerpetualCalendar;
import com.ailudick.capitalismmod.currency.Currencies;
import com.ailudick.capitalismmod.currency.Currency;
import com.ailudick.capitalismmod.currency.ExchangeRateProvider;
import com.ailudick.capitalismmod.currency.Money;
import com.ailudick.capitalismmod.economy.EconomyTransferSavedData;
import com.ailudick.capitalismmod.government.GovernmentPolicySavedData;
import com.ailudick.capitalismmod.government.MonetaryPolicyEconomics;
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
import java.util.UUID;

/**
 * Central helpers for the player's bank accounts (open / deposit / withdraw / loan / repay / interest / term deposits / transfer).
 */
public final class BankAccountHelper {
    /** Fractional minor-unit precision carried between daily settlements. */
    private static final long INTEREST_SCALE = 1_000_000L;
    /** Minecraft days until a loan matures; overdue loans incur a penalty rate. */
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

    public static boolean canOpenAccount(Player player, boolean credit) {
        long count = getAccounts(player).values().stream()
                .filter(account -> account.credit() == credit)
                .count();
        int limit = credit ? Config.MAX_CREDIT_ACCOUNTS.get() : Config.MAX_DEBIT_ACCOUNTS.get();
        return count < limit;
    }

    /**
     * Applies the settlement for one Minecraft day exactly once: deposit interest on balances,
     * loan interest on debts, and a tick on each term deposit (maturing it when due).
     * Rates are annual ({@link Config}), compounded daily. The day marker makes this operation
     * idempotent if a login event, tick event, or a future subsystem requests the same settlement
     * more than once.
     */
    public static void applyDailyInterest(Player player, long settlementDay) {
        long lastSettlementDay = player.getData(ModAttachments.LAST_BANK_SETTLEMENT_DAY);
        if (lastSettlementDay >= settlementDay) {
            return;
        }
        int policyRateBps = GovernmentPolicySavedData.get(player.getServer()).policyRateBasisPoints();
        double depositRate = MonetaryPolicyEconomics.adjustedAnnualRate(Config.DEPOSIT_RATE_PER_YEAR.get(), policyRateBps) / 365.0;
        double loanRate = MonetaryPolicyEconomics.adjustedAnnualRate(Config.LOAN_RATE_PER_YEAR.get(), policyRateBps) / 365.0;
        Map<String, BankAccount> accounts = getAccounts(player);
        if (accounts.isEmpty()) {
            player.setData(ModAttachments.LAST_BANK_SETTLEMENT_DAY, settlementDay);
            return;
        }

        Map<String, BankAccount> updated = new HashMap<>(accounts);
        long settlementTick = PerpetualCalendar.ticksForDays(settlementDay);
        boolean changed = false;
        for (BankAccount account : new HashMap<>(accounts).values()) {
            Map<String, Long> newBalances = new HashMap<>(account.balances());
            Map<String, Long> newDebts = new HashMap<>(account.debts());
            Map<String, Long> depositRemainders = new HashMap<>(account.depositInterestRemainders());
            Map<String, Long> loanRemainders = new HashMap<>(account.loanInterestRemainders());
            List<BankTransaction> txs = new ArrayList<>(account.transactions());
            for (Map.Entry<String, Long> entry : newBalances.entrySet()) {
                if (entry.getValue() <= 0) {
                    depositRemainders.remove(entry.getKey());
                    continue;
                }
                // Persist fractional interest even when it has not yet
                // reached one minor currency unit. Otherwise small balances
                // would permanently lose sub-unit accrual on quiet days.
                changed = true;
                InterestAccrual accrual = accrueInterest(entry.getValue(), depositRate,
                        depositRemainders.getOrDefault(entry.getKey(), 0L));
                depositRemainders.put(entry.getKey(), accrual.remainder());
                long interest = accrual.wholeMinorUnits();
                if (interest > 0) {
                    entry.setValue(safeAdd(entry.getValue(), interest));
                    txs.add(BankTransaction.atTick(settlementTick, "interest", entry.getKey(), interest));
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
                if (entry.getValue() <= 0) {
                    loanRemainders.remove(entry.getKey());
                    continue;
                }
                InterestAccrual accrual = accrueInterest(entry.getValue(), effectiveLoanRate,
                        loanRemainders.getOrDefault(entry.getKey(), 0L));
                loanRemainders.put(entry.getKey(), accrual.remainder());
                long interest = accrual.wholeMinorUnits();
                if (interest > 0) {
                    entry.setValue(safeAdd(entry.getValue(), interest));
                    txs.add(BankTransaction.atTick(settlementTick, "interest", entry.getKey(), -interest));
                    changed = true;
                }
            }
            // Tick term deposits; mature any that have reached their due date.
            List<TermDeposit> newTerms = new ArrayList<>();
            for (TermDeposit term : account.termDeposits()) {
                TermDeposit ticked = term.tick();
                if (ticked.daysRemaining() <= 0) {
                    long payout = safeAdd(term.principal(), term.interest());
                    long balance = newBalances.getOrDefault(term.currencyId(), 0L);
                    newBalances.put(term.currencyId(), safeAdd(balance, payout));
                    txs.add(BankTransaction.atTick(settlementTick, "term_maturity", term.currencyId(), payout));
                } else {
                    newTerms.add(ticked);
                }
                changed = true;
            }
            updated.put(account.id(), account.withBalancesAndDebtsAndInterest(newBalances, newDebts,
                            depositRemainders, loanRemainders)
                    .withTransactions(txs).withTermDeposits(newTerms).withLoanDaysRemaining(loanDaysRemaining));
        }
        if (changed) {
            setAccounts(player, updated);
        }
        player.setData(ModAttachments.LAST_BANK_SETTLEMENT_DAY, settlementDay);
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
            long newBalance = safeAdd(accountBalance, amount);
            if (newBalance == Long.MAX_VALUE && accountBalance != Long.MAX_VALUE) {
                return false;
            }
            if (!EconomyHelper.consumeItemsWithChange(player, currency, amount)) {
                return false;
            }
            account = account.withBalance(currency.id(), newBalance)
                    .withTransaction(BankTransaction.now(player, "deposit", currency.id(), amount,
                            "cash_deposit", "wallet"));
        } else {
            // account -> physical items
            if (accountBalance < amount) {
                return false;
            }
            account = account.withBalance(currency.id(), accountBalance - amount)
                    .withTransaction(BankTransaction.now(player, "withdraw", currency.id(), -amount,
                            "cash_withdraw", "wallet"));
            EconomyHelper.giveMoney(player, currency, amount);
        }

        updateAccount(player, account);
        return true;
    }

    /** Exchanges funds held by one selected bank account between two currencies. */
    public static boolean exchange(Player player, String accountId, Currency from, Currency to,
                                   long amount, long converted) {
        if (amount <= 0 || converted <= 0 || from.equals(to)) {
            return false;
        }
        BankAccount account = getAccount(player, accountId);
        if (account == null || account.getBalance(from.id()) < amount) {
            return false;
        }
        long targetBalance = account.getBalance(to.id());
        if (Long.MAX_VALUE - targetBalance < converted) {
            return false;
        }
        BankAccount updated = account.withBalance(from.id(), account.getBalance(from.id()) - amount)
                .withBalance(to.id(), targetBalance + converted)
                .withTransaction(BankTransaction.now(player, "exchange", from.id(), -amount))
                .withTransaction(BankTransaction.now(player, "exchange", to.id(), converted));
        updateAccount(player, updated);
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
        long newDebt = safeAdd(account.getDebt(currency.id()), amount);
        if (newDebt == Long.MAX_VALUE && account.getDebt(currency.id()) != Long.MAX_VALUE) {
            return false;
        }
        long combined = EconomyMath.add(existingDebtInBase, newDebtInBase);
        CreditAssessment assessment = CreditAssessment.evaluate(account, existingDebtInBase, Config.CREDIT_LIMIT.get());
        if (assessment.overdue() || combined < 0 || combined > assessment.approvedLimit()) {
            return false;
        }

        int loanDays = existingDebtInBase == 0 ? Config.LOAN_TERM_DAYS.get() : account.loanDaysRemaining();
        EconomyHelper.giveMoney(player, currency, amount);
        updateAccount(player, account.withDebt(currency.id(), newDebt)
                .withLoanDaysRemaining(loanDays)
                .withTransaction(BankTransaction.now(player, "loan", currency.id(), amount)));
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
                .withTransaction(BankTransaction.now(player, "repay", currency.id(), -amount));
        if (totalDebtInBase(updated) == 0) {
            updated = updated.withLoanDaysRemaining(0);
        }
        updateAccount(player, updated);
        return true;
    }

    /** Opens a fixed-term deposit, moving {@code amount} out of the demand balance for {@code termDays} days. */
    public static boolean openTermDeposit(Player player, String accountId, String currencyId, long amount, int termDays) {
        if (amount <= 0 || termDays <= 0 || termDays > Config.TERM_DEPOSIT_MAX_DAYS.get()
                || !Currencies.exists(currencyId)) {
            return false;
        }
        BankAccount account = getAccount(player, accountId);
        if (account == null || account.getBalance(currencyId) < amount) {
            return false;
        }
        double dailyRate = Config.TERM_DEPOSIT_RATE_PER_YEAR.get() / 365.0;
        long interest = safeInterest(amount, dailyRate * termDays);
        List<TermDeposit> terms = new ArrayList<>(account.termDeposits());
        terms.add(new TermDeposit(currencyId, amount, interest, termDays));
        account = account.withBalance(currencyId, account.getBalance(currencyId) - amount)
                .withTermDeposits(terms)
                .withTransaction(BankTransaction.now(player, "term_deposit", currencyId, -amount));
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
        account = account.withBalance(term.currencyId(), safeAdd(account.getBalance(term.currencyId()), term.principal()))
                .withTermDeposits(terms)
                .withTransaction(BankTransaction.now(player, "term_withdraw", term.currencyId(), term.principal()));
        updateAccount(player, account);
        return true;
    }

    /** Transfers {@code amount} between two accounts by number. Both players must be online. */
    public static boolean transferBetween(ServerPlayer sender, String fromAccountId, String targetAccountId, String currencyId, long amount) {
        return transferBetween(sender, null, fromAccountId, targetAccountId, currencyId, amount);
    }

    /** Transfers between accounts with a persistent client request receipt. */
    public static boolean transferBetween(ServerPlayer sender, UUID requestId, String fromAccountId,
                                          String targetAccountId, String currencyId, long amount) {
        if (amount <= 0 || !Currencies.exists(currencyId)
                || java.util.Objects.equals(fromAccountId, targetAccountId)) {
            return false;
        }
        EconomyTransferSavedData transferReceipts = requestId == null ? null
                : EconomyTransferSavedData.get(sender.getServer());
        if (transferReceipts != null) {
            EconomyTransferSavedData.Receipt existing = transferReceipts.find(requestId);
            if (existing != null) {
                return sender.getUUID().equals(existing.senderId())
                        && existing.fromAccountId().equals(fromAccountId)
                        && existing.targetAccountId().equals(targetAccountId)
                        && existing.currencyId().equals(currencyId)
                        && existing.amount() == amount;
            }
        }
        ServerPlayer target = findAccountOwner(sender.getServer(), targetAccountId);
        if (target == null) {
            return false;
        }
        BankAccount from = getAccount(sender, fromAccountId);
        BankAccount to = getAccount(target, targetAccountId);
        long fee = transferFee(amount);
        long total = EconomyMath.add(amount, fee);
        long targetBalance = to == null ? -1L : EconomyMath.add(to.getBalance(currencyId), amount);
        if (from == null || to == null || fee < 0L || total < 0L
                || targetBalance < 0L || from.getBalance(currencyId) < total) {
            return false;
        }
        BankAccount senderUpdated = from.withBalance(currencyId, from.getBalance(currencyId) - total)
                .withTransaction(BankTransaction.now(sender, "transfer_out", currencyId, -amount,
                        "bank_transfer", targetAccountId));
        if (fee > 0L) {
            senderUpdated = senderUpdated.withTransaction(BankTransaction.now(sender, "transfer_fee", currencyId, -fee,
                    "transfer_fee", "bank"));
        }
        updateAccount(sender, senderUpdated);
        updateAccount(target, to.withBalance(currencyId, targetBalance)
                .withTransaction(BankTransaction.now(sender, "transfer_in", currencyId, amount,
                        "bank_transfer", fromAccountId)));
        if (transferReceipts != null) {
            transferReceipts.record(new EconomyTransferSavedData.Receipt(requestId, sender.getUUID(),
                    fromAccountId, targetAccountId, currencyId, amount,
                    sender.getServer().overworld().getGameTime()));
        }
        return true;
    }

    private static long transferFee(long amount) {
        double calculated = amount * Config.TRANSFER_FEE_RATE.get();
        if (!Double.isFinite(calculated) || calculated >= Long.MAX_VALUE) {
            return -1L;
        }
        if (calculated <= 0.0) {
            return 0L;
        }
        return Math.max(1L, (long) calculated);
    }

    private static long safeInterest(long principal, double rate) {
        double value = principal * rate;
        if (!Double.isFinite(value) || value >= Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }
        return Math.max(0L, (long) value);
    }

    private static InterestAccrual accrueInterest(long principal, double rate, long remainder) {
        if (principal <= 0L || !Double.isFinite(rate) || rate <= 0.0) {
            return new InterestAccrual(0L, Math.max(0L, remainder));
        }
        double scaled = principal * rate * INTEREST_SCALE;
        if (!Double.isFinite(scaled) || scaled >= Long.MAX_VALUE) {
            return new InterestAccrual(Long.MAX_VALUE, 0L);
        }
        long scaledWhole = Math.max(0L, (long) scaled);
        long totalScaled = scaledWhole > Long.MAX_VALUE - remainder
                ? Long.MAX_VALUE : scaledWhole + remainder;
        return new InterestAccrual(totalScaled / INTEREST_SCALE, totalScaled % INTEREST_SCALE);
    }

    private record InterestAccrual(long wholeMinorUnits, long remainder) {}

    private static long safeAdd(long left, long right) {
        try {
            return Math.addExact(left, right);
        } catch (ArithmeticException e) {
            return Long.MAX_VALUE;
        }
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
