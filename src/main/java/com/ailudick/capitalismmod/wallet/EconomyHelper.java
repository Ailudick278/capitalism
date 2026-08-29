package com.ailudick.capitalismmod.wallet;

import com.ailudick.capitalismmod.bank.BankAccount;
import com.ailudick.capitalismmod.bank.BankAccountHelper;
import com.ailudick.capitalismmod.currency.Currency;
import com.ailudick.capitalismmod.currency.CurrencyItem;
import com.ailudick.capitalismmod.event.WalletChangedEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;

import java.util.HashMap;
import java.util.Map;

/**
 * Settlement helpers. There is no standalone wallet anymore; payments consume
 * physical currency items first, then fall back to bank account balances.
 */
public final class EconomyHelper {
    private EconomyHelper() {
    }

    /** Total face value of a currency held as physical items in the inventory. */
    public static long countItems(Player player, Currency currency) {
        long total = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof CurrencyItem item && item.currency().equals(currency)) {
                total += item.value() * stack.getCount();
            }
        }
        return total;
    }

    /** Total balance of a currency across all bank accounts. */
    public static long totalAccountBalance(Player player, Currency currency) {
        long total = 0;
        for (BankAccount account : BankAccountHelper.getAccounts(player).values()) {
            total += account.getBalance(currency.id());
        }
        return total;
    }

    /** Total available balance: physical items + bank accounts. */
    public static long getBalance(Player player, Currency currency) {
        return countItems(player, currency) + totalAccountBalance(player, currency);
    }

    /** Pays {@code amount}: consumes currency items first, then falls back to bank accounts. */
    public static boolean tryPay(Player player, Currency currency, long amount) {
        if (amount <= 0) {
            return true;
        }
        long items = countItems(player, currency);
        if (items >= amount) {
            consumeItems(player, currency, amount);
            postChanged(player, currency);
            return true;
        }
        long remaining = amount - items;
        if (items > 0) {
            consumeItems(player, currency, items);
        }
        boolean success = trySpendFromAccounts(player, currency, remaining);
        if (success) {
            postChanged(player, currency);
        }
        return success;
    }

    /** Gives {@code amount} of {@code currency} as physical items (greedy denominations). */
    public static void giveMoney(Player player, Currency currency, long amount) {
        if (amount <= 0) {
            return;
        }
        long remaining = amount;
        for (Map.Entry<Long, Item> denom : CurrencyItem.denominations(currency.id())) {
            if (remaining <= 0) {
                break;
            }
            long value = denom.getKey();
            long count = remaining / value;
            if (count == 0) {
                continue;
            }
            Item item = denom.getValue();
            while (count > 0) {
                int stackSize = (int) Math.min(count, item.getDefaultMaxStackSize());
                ItemStack stack = new ItemStack(item, stackSize);
                count -= stackSize;
                remaining -= stackSize * value;
                if (!player.getInventory().add(stack)) {
                    player.drop(stack, false);
                }
            }
        }
        postChanged(player, currency);
    }

    /** Consumes exactly {@code amount} worth of currency items (caller must ensure enough). */
    public static void consumeItems(Player player, Currency currency, long amount) {
        long remaining = amount;
        for (Map.Entry<Long, Item> denom : CurrencyItem.denominations(currency.id())) {
            if (remaining <= 0) {
                break;
            }
            long value = denom.getKey();
            if (value > remaining) {
                continue;
            }
            Item item = denom.getValue();
            long count = remaining / value;
            for (ItemStack stack : player.getInventory().items) {
                if (count <= 0) {
                    break;
                }
                if (stack.getItem() == item) {
                    int take = (int) Math.min(count, stack.getCount());
                    stack.shrink(take);
                    count -= take;
                    remaining -= take * value;
                }
            }
        }
    }

    private static boolean trySpendFromAccounts(Player player, Currency currency, long amount) {
        if (totalAccountBalance(player, currency) < amount) {
            return false;
        }
        long remaining = amount;
        Map<String, BankAccount> accounts = new HashMap<>(BankAccountHelper.getAccounts(player));
        for (Map.Entry<String, BankAccount> entry : accounts.entrySet()) {
            BankAccount account = entry.getValue();
            long balance = account.getBalance(currency.id());
            long take = Math.min(balance, remaining);
            if (take > 0) {
                entry.setValue(account.withBalance(currency.id(), balance - take));
                remaining -= take;
            }
            if (remaining <= 0) {
                break;
            }
        }
        BankAccountHelper.setAccounts(player, accounts);
        return remaining <= 0;
    }

    private static void postChanged(Player player, Currency currency) {
        NeoForge.EVENT_BUS.post(new WalletChangedEvent(player, currency.id(), getBalance(player, currency)));
    }
}
