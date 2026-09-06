package com.ailudick.capitalismmod.bank;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** A persistent immutable bank account. Interest remainders preserve fractional minor units. */
public record BankAccount(String id, boolean credit, Map<String, Long> balances, Map<String, Long> debts,
                          List<BankTransaction> transactions, List<TermDeposit> termDeposits,
                          int loanDaysRemaining, Map<String, Long> depositInterestRemainders,
                          Map<String, Long> loanInterestRemainders) {
    private static final int MAX_TRANSACTIONS = 50;

    /** Lazily initialized so pure account calculations do not load Minecraft's data-fixer runtime. */
    public static Codec<BankAccount> codec() { return Codecs.CODEC; }

    private static final class Codecs {
        private static final Codec<BankAccount> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("id").forGetter(BankAccount::id),
                Codec.BOOL.fieldOf("credit").forGetter(BankAccount::credit),
                Codec.unboundedMap(Codec.STRING, Codec.LONG).fieldOf("balances").forGetter(BankAccount::balances),
                Codec.unboundedMap(Codec.STRING, Codec.LONG).fieldOf("debts").forGetter(BankAccount::debts),
                BankTransaction.codec().listOf().fieldOf("transactions").forGetter(BankAccount::transactions),
                TermDeposit.codec().listOf().fieldOf("termDeposits").forGetter(BankAccount::termDeposits),
                Codec.INT.fieldOf("loanDaysRemaining").forGetter(BankAccount::loanDaysRemaining),
                Codec.unboundedMap(Codec.STRING, Codec.LONG).optionalFieldOf("depositInterestRemainders", Map.of())
                        .forGetter(BankAccount::depositInterestRemainders),
                Codec.unboundedMap(Codec.STRING, Codec.LONG).optionalFieldOf("loanInterestRemainders", Map.of())
                        .forGetter(BankAccount::loanInterestRemainders)
        ).apply(instance, (id, credit, balances, debts, transactions, terms, days, depositRemainders, loanRemainders) ->
                new BankAccount(id, credit, balances, debts, transactions, terms, days, depositRemainders, loanRemainders)));
    }

    public static BankAccount create(String id, boolean credit) {
        return new BankAccount(id, credit, new HashMap<>(), new HashMap<>(), new ArrayList<>(), new ArrayList<>(),
                0, new HashMap<>(), new HashMap<>());
    }

    /** Compatibility constructor for older callers and old serialized accounts. */
    public BankAccount(String id, boolean credit, Map<String, Long> balances, Map<String, Long> debts,
                       List<BankTransaction> transactions, List<TermDeposit> termDeposits, int loanDaysRemaining) {
        this(id, credit, balances, debts, transactions, termDeposits, loanDaysRemaining, new HashMap<>(), new HashMap<>());
    }

    public long getBalance(String currencyId) { return balances.getOrDefault(currencyId, 0L); }
    public long getDebt(String currencyId) { return debts.getOrDefault(currencyId, 0L); }

    public BankAccount withBalance(String currencyId, long amount) {
        Map<String, Long> copy = new HashMap<>(balances);
        copy.put(currencyId, amount);
        Map<String, Long> remainders = new HashMap<>(depositInterestRemainders);
        if (amount <= 0L) remainders.remove(currencyId);
        return copyWith(copy, debts, transactions, termDeposits, loanDaysRemaining,
                remainders, loanInterestRemainders);
    }

    public BankAccount withDebt(String currencyId, long amount) {
        Map<String, Long> copy = new HashMap<>(debts);
        copy.put(currencyId, amount);
        Map<String, Long> remainders = new HashMap<>(loanInterestRemainders);
        if (amount <= 0L) remainders.remove(currencyId);
        return copyWith(balances, copy, transactions, termDeposits, loanDaysRemaining,
                depositInterestRemainders, remainders);
    }

    public BankAccount withBalancesAndDebts(Map<String, Long> newBalances, Map<String, Long> newDebts) {
        return copyWith(newBalances, newDebts, transactions, termDeposits, loanDaysRemaining,
                depositInterestRemainders, loanInterestRemainders);
    }

    public BankAccount withBalancesAndDebtsAndInterest(Map<String, Long> newBalances, Map<String, Long> newDebts,
                                                        Map<String, Long> newDepositRemainders,
                                                        Map<String, Long> newLoanRemainders) {
        return copyWith(newBalances, newDebts, transactions, termDeposits, loanDaysRemaining,
                newDepositRemainders, newLoanRemainders);
    }

    public BankAccount withTermDeposits(List<TermDeposit> newTermDeposits) {
        return copyWith(balances, debts, transactions, newTermDeposits, loanDaysRemaining,
                depositInterestRemainders, loanInterestRemainders);
    }

    public BankAccount withLoanDaysRemaining(int days) {
        return copyWith(balances, debts, transactions, termDeposits, days,
                depositInterestRemainders, loanInterestRemainders);
    }

    public BankAccount withTransaction(BankTransaction transaction) {
        List<BankTransaction> copy = new ArrayList<>(transactions);
        copy.add(transaction);
        while (copy.size() > MAX_TRANSACTIONS) copy.remove(0);
        return copyWith(balances, debts, copy, termDeposits, loanDaysRemaining,
                depositInterestRemainders, loanInterestRemainders);
    }

    public BankAccount withTransactions(List<BankTransaction> newTransactions) {
        List<BankTransaction> copy = new ArrayList<>(newTransactions);
        while (copy.size() > MAX_TRANSACTIONS) copy.remove(0);
        return copyWith(balances, debts, copy, termDeposits, loanDaysRemaining,
                depositInterestRemainders, loanInterestRemainders);
    }

    /** Returns whether this account already contains a transaction with the given stable reference. */
    public boolean hasTransactionReference(String reference) {
        if (reference == null || reference.isBlank()) return false;
        return transactions.stream().anyMatch(transaction -> reference.equals(transaction.reference()));
    }

    private BankAccount copyWith(Map<String, Long> newBalances, Map<String, Long> newDebts,
                                 List<BankTransaction> newTransactions, List<TermDeposit> newTerms,
                                 int newLoanDays, Map<String, Long> newDepositRemainders,
                                 Map<String, Long> newLoanRemainders) {
        return new BankAccount(id, credit, newBalances, newDebts, newTransactions, newTerms, newLoanDays,
                newDepositRemainders, newLoanRemainders);
    }
}
