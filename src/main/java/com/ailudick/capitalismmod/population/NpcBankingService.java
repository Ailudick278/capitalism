package com.ailudick.capitalismmod.population;

import net.minecraft.server.MinecraftServer;

/** Daily NPC banking: liquidity buffer, emergency credit, repayment, and savings. */
public final class NpcBankingService {
    private NpcBankingService() {}
    public record Result(Household household, long bankNetCashMinor) {}

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
        long limit = Math.min(Long.MAX_VALUE, multiply(need, 30L));
        if (cash < need && account.debtMinor() < limit) {
            long amount = Math.min(need - cash, limit - account.debtMinor());
            if (amount > 0L && data.transact(household.id(), day, "loan", 0L, amount, 30) != null) {
                cash = add(cash, amount); net = add(net, amount);
            }
        }
        return new Result(household.withCash(cash), net);
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
