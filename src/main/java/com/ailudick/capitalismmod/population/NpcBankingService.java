package com.ailudick.capitalismmod.population;

import com.ailudick.capitalismmod.Config;
import com.ailudick.capitalismmod.bank.BankCapitalEconomics;
import com.ailudick.capitalismmod.bank.BankCapitalSavedData;
import com.ailudick.capitalismmod.bank.BankBadDebtEconomics;
import com.ailudick.capitalismmod.government.GovernmentPolicySavedData;
import com.ailudick.capitalismmod.government.MonetaryPolicyEconomics;
import net.minecraft.server.MinecraftServer;

/** Daily NPC banking: liquidity buffer, emergency credit, repayment, and savings. */
public final class NpcBankingService {
    private NpcBankingService() {}
    public record Result(Household household, long bankNetCashMinor) {}

    /** Applies one idempotent daily interest pass to all NPC accounts. */
    public static void applyDailyInterest(MinecraftServer server, long day) {
        NpcBankingSavedData data = NpcBankingSavedData.get(server);
        if (data.lastInterestDay() >= day) return;
        int policyRateBps = GovernmentPolicySavedData.get(server).policyRateBasisPoints();
        double depositRate = MonetaryPolicyEconomics.adjustedAnnualRate(Config.DEPOSIT_RATE_PER_YEAR.get(), policyRateBps) / 365.0;
        double loanRate = MonetaryPolicyEconomics.adjustedAnnualRate(Config.LOAN_RATE_PER_YEAR.get(), policyRateBps) / 365.0;
        BankCapitalSavedData capital = BankCapitalSavedData.get(server);
        if (!capital.initialized()) capital.initialize(Config.BANK_INITIAL_CAPITAL_MINOR.get());
        for (NpcBankingSavedData.Account account : data.accounts()) {
            long depositInterest = interest(account.balanceMinor(), depositRate);
            if (depositInterest > 0L && data.transact(account.householdId(), day, "deposit_interest",
                    depositInterest, 0L, account.loanDaysRemaining()) != null) {
                capital.applyTransactionOnce("npc-bank-interest:" + account.householdId() + ":" + day + ":deposit",
                        0L, depositInterest);
            }
            NpcBankingSavedData.Account current = data.find(account.householdId());
            if (current == null || current.debtMinor() <= 0L) continue;
            double effectiveRate = current.loanDaysRemaining() < 0 ? loanRate * 2.0 : loanRate;
            long loanInterest = interest(current.debtMinor(), effectiveRate);
            if (loanInterest > 0L && data.transact(account.householdId(), day, "loan_interest",
                    0L, loanInterest, current.loanDaysRemaining()) != null) {
                capital.applyTransactionOnce("npc-bank-interest:" + account.householdId() + ":" + day + ":loan",
                        loanInterest, 0L);
            }
        }
        data.markInterestDay(day);
    }

    /** Writes off NPC debt after the configured grace period, once against bank equity. */
    public static int writeOffBadDebts(MinecraftServer server, long day) {
        NpcBankingSavedData data = NpcBankingSavedData.get(server);
        BankCapitalSavedData capital = BankCapitalSavedData.get(server);
        if (!capital.initialized()) capital.initialize(Config.BANK_INITIAL_CAPITAL_MINOR.get());
        int writtenOff = 0;
        for (NpcBankingSavedData.Account account : data.accounts()) {
            if (!BankBadDebtEconomics.eligible(account.loanDaysRemaining(), Config.BANK_BAD_DEBT_WRITE_OFF_DAYS.get())
                    || account.debtMinor() <= 0L) continue;
            String source = "npc-bank-bad-debt:" + account.householdId() + ":" + day;
            if (!capital.hasWriteOff(source)) capital.writeOffOnce(account.debtMinor(), source);
            if (!capital.hasWriteOff(source)) continue;
            if (data.transact(account.householdId(), day, "bad_debt_writeoff", 0L,
                    -account.debtMinor(), 0) != null) writtenOff++;
        }
        return writtenOff;
    }

    public static Result prepare(MinecraftServer server, Household household, long day) {
        if (household == null || !household.id().startsWith("npc-")) return new Result(household, 0L);
        NpcBankingSavedData data = NpcBankingSavedData.get(server); NpcBankingSavedData.Account account = data.advanceLoanDay(household.id());
        long need = multiply(household.dailyNeedMinor(), household.size());
        long cash = household.cashMinor(); long net = 0L;
        if (cash < need && account.balanceMinor() > 0L) {
            long amount = Math.min(need - cash, account.balanceMinor());
            if (data.transact(household.id(), day, "withdrawal", -amount, 0L, account.loanDaysRemaining()) != null) {
                cash = add(cash, amount); net = add(net, amount);
            }
        }
        account = data.find(household.id());
        BankCapitalSavedData capital = BankCapitalSavedData.get(server);
        if (!capital.initialized()) capital.initialize(Config.BANK_INITIAL_CAPITAL_MINOR.get());
        long totalCapacity = BankCapitalEconomics.capitalBackedLoanCapacity(capital.capitalMinor());
        long availableCapacity = totalCapacity > data.totalDebt() ? totalCapacity - data.totalDebt() : 0L;
        long limit = Math.min(multiply(need, 30L), availableCapacity);
        if (cash < need && account.debtMinor() < limit) {
            long amount = Math.min(need - cash, limit - account.debtMinor());
            if (amount > 0L && data.transact(household.id(), day, "loan", 0L, amount, 30) != null) {
                cash = add(cash, amount); net = add(net, amount);
            }
        }
        return new Result(household.withCash(cash), net);
    }

    private static long interest(long principal, double dailyRate) {
        if (principal <= 0L || !Double.isFinite(dailyRate) || dailyRate <= 0.0) return 0L;
        double value = principal * dailyRate;
        return !Double.isFinite(value) || value >= Long.MAX_VALUE ? Long.MAX_VALUE : (long) value;
    }

    public static Result finish(MinecraftServer server, Household household, long day) {
        if (household == null || !household.id().startsWith("npc-")) return new Result(household, 0L);
        NpcBankingSavedData data = NpcBankingSavedData.get(server); NpcBankingSavedData.Account account = data.ensure(household.id());
        long need = multiply(household.dailyNeedMinor(), household.size());
        long buffer = multiply(need, 2L); long cash = household.cashMinor(); long net = 0L;
        long surplus = cash > buffer ? cash - buffer : 0L;
        long repayment = Math.min(account.debtMinor(), surplus / 2L);
        if (repayment > 0L && data.transact(household.id(), day, "repayment", 0L, -repayment, account.loanDaysRemaining()) != null) {
            cash -= repayment; surplus -= repayment; net = subtract(net, repayment);
        }
        long deposit = Math.min(surplus, Math.max(0L, cash - buffer));
        if (deposit > 0L && data.transact(household.id(), day, "deposit", deposit, 0L, account.loanDaysRemaining()) != null) {
            cash -= deposit; net = subtract(net, deposit);
        }
        return new Result(household.withCash(cash), net);
    }
    private static long multiply(long a, long b) { try { return Math.multiplyExact(a, b); } catch (ArithmeticException e) { return Long.MAX_VALUE; } }
    private static long add(long a, long b) { try { return Math.addExact(a, b); } catch (ArithmeticException e) { return Long.MAX_VALUE; } }
    private static long subtract(long a, long b) { try { return Math.subtractExact(a, b); } catch (ArithmeticException e) { return Long.MIN_VALUE; } }
}
