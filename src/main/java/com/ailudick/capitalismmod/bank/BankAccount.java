package com.ailudick.capitalismmod.bank;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A single bank account owned by a player. Immutable; mutations return a new instance.
 *
 * @param id               account number (19-digit card number)
 * @param credit           whether this is a credit account (can take loans)
 * @param balances         currency id -> balance
 * @param debts            currency id -> debt (credit accounts only)
 * @param transactions     recent transaction history, newest last
 * @param termDeposits     fixed-term deposits held by this account
 * @param loanDaysRemaining Minecraft days until the active loan matures (0 = no active loan term; negative = overdue)
 */
public record BankAccount(String id, boolean credit, Map<String, Long> balances, Map<String, Long> debts, List<BankTransaction> transactions, List<TermDeposit> termDeposits, int loanDaysRemaining) {

    private static final int MAX_TRANSACTIONS = 50;

    public static final Codec<BankAccount> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(BankAccount::id),
            Codec.BOOL.fieldOf("credit").forGetter(BankAccount::credit),
            Codec.unboundedMap(Codec.STRING, Codec.LONG).fieldOf("balances").forGetter(BankAccount::balances),
            Codec.unboundedMap(Codec.STRING, Codec.LONG).fieldOf("debts").forGetter(BankAccount::debts),
            BankTransaction.CODEC.listOf().fieldOf("transactions").forGetter(BankAccount::transactions),
            TermDeposit.CODEC.listOf().fieldOf("termDeposits").forGetter(BankAccount::termDeposits),
            Codec.INT.fieldOf("loanDaysRemaining").forGetter(BankAccount::loanDaysRemaining)
    ).apply(instance, BankAccount::new));

    public static BankAccount create(String id, boolean credit) {
        return new BankAccount(id, credit, new HashMap<>(), new HashMap<>(), new ArrayList<>(), new ArrayList<>(), 0);
    }

    public long getBalance(String currencyId) {
        return balances.getOrDefault(currencyId, 0L);
    }

    public long getDebt(String currencyId) {
        return debts.getOrDefault(currencyId, 0L);
    }

    public BankAccount withBalance(String currencyId, long amount) {
        Map<String, Long> copy = new HashMap<>(balances);
        copy.put(currencyId, amount);
        return new BankAccount(id, credit, copy, debts, transactions, termDeposits, loanDaysRemaining);
    }

    public BankAccount withDebt(String currencyId, long amount) {
        Map<String, Long> copy = new HashMap<>(debts);
        copy.put(currencyId, amount);
        return new BankAccount(id, credit, balances, copy, transactions, termDeposits, loanDaysRemaining);
    }

    public BankAccount withBalancesAndDebts(Map<String, Long> newBalances, Map<String, Long> newDebts) {
        return new BankAccount(id, credit, newBalances, newDebts, transactions, termDeposits, loanDaysRemaining);
    }

    public BankAccount withTermDeposits(List<TermDeposit> newTermDeposits) {
        return new BankAccount(id, credit, balances, debts, transactions, newTermDeposits, loanDaysRemaining);
    }

    public BankAccount withLoanDaysRemaining(int days) {
        return new BankAccount(id, credit, balances, debts, transactions, termDeposits, days);
    }

    public BankAccount withTransaction(BankTransaction transaction) {
        List<BankTransaction> copy = new ArrayList<>(transactions);
        copy.add(transaction);
        while (copy.size() > MAX_TRANSACTIONS) {
            copy.remove(0);
        }
        return new BankAccount(id, credit, balances, debts, copy, termDeposits, loanDaysRemaining);
    }

    public BankAccount withTransactions(List<BankTransaction> newTransactions) {
        List<BankTransaction> copy = new ArrayList<>(newTransactions);
        while (copy.size() > MAX_TRANSACTIONS) {
            copy.remove(0);
        }
        return new BankAccount(id, credit, balances, debts, copy, termDeposits, loanDaysRemaining);
    }
}
