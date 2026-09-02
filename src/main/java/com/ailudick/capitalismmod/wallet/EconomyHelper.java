package com.ailudick.capitalismmod.wallet;

import com.ailudick.capitalismmod.bank.BankAccount;
import com.ailudick.capitalismmod.bank.BankAccountHelper;
import com.ailudick.capitalismmod.currency.Currency;
import com.ailudick.capitalismmod.currency.CurrencyItem;
import com.ailudick.capitalismmod.event.WalletChangedEvent;
import com.ailudick.capitalismmod.economy.EconomyLogSavedData;
import com.ailudick.capitalismmod.util.EconomyMath;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;

import java.util.HashMap;
import java.util.List;
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
                long value = EconomyMath.multiply(item.value(), stack.getCount());
                total = value < 0 ? Long.MAX_VALUE : EconomyMath.add(total, value);
                if (total < 0) {
                    return Long.MAX_VALUE;
                }
            }
        }
        return total;
    }

    /** Total balance of a currency across all bank accounts. */
    public static long totalAccountBalance(Player player, Currency currency) {
        long total = 0;
        for (BankAccount account : BankAccountHelper.getAccounts(player).values()) {
            long balance = account.getBalance(currency.id());
            total = EconomyMath.add(total, balance);
            if (total < 0) {
                return Long.MAX_VALUE;
            }
        }
        return total;
    }

    /** Total available balance: physical items + bank accounts. */
    public static long getBalance(Player player, Currency currency) {
        long items = countItems(player, currency);
        long accounts = totalAccountBalance(player, currency);
        long total = EconomyMath.add(items, accounts);
        return total < 0 ? Long.MAX_VALUE : total;
    }

    /** Pays {@code amount}: consumes currency items first, then falls back to bank accounts. */
    public static boolean tryPay(Player player, Currency currency, long amount) {
        if (amount < 0) {
            return false;
        }
        if (amount == 0) {
            return true;
        }
        long items = countItems(player, currency);
        if (items >= amount) {
            consumeItems(player, currency, amount);
            postChanged(player, currency);
            log(player, "支付", currency, amount);
            return true;
        }
        long remaining = amount - items;
        // Check the account side before consuming any physical currency. This
        // keeps a failed payment atomic instead of destroying the item portion
        // when the combined balance is insufficient.
        if (totalAccountBalance(player, currency) < remaining) {
            return false;
        }
        if (items > 0) {
            consumeItems(player, currency, items);
        }
        boolean success = trySpendFromAccounts(player, currency, remaining);
        if (success) {
            postChanged(player, currency);
            log(player, "支付", currency, amount);
        }
        return success;
    }

    /**
     * Removes physical currency worth at least {@code amount} and returns the
     * excess as change. This is used for deposits, where a player may only have
     * a denomination larger than the requested amount.
     */
    public static boolean consumeItemsWithChange(Player player, Currency currency, long amount) {
        if (amount <= 0) {
            return false;
        }

        Map<Item, Long> plan = new HashMap<>();
        long selected = 0L;
        List<Map.Entry<Long, Item>> denominations = CurrencyItem.denominations(currency.id());
        // Deposit from smaller denominations first so exact payment is preferred.
        for (int i = denominations.size() - 1; i >= 0; i--) {
            Map.Entry<Long, Item> denomination = denominations.get(i);
            long value = denomination.getKey();
            long available = 0L;
            for (ItemStack stack : player.getInventory().items) {
                if (stack.getItem() == denomination.getValue()) {
                    available += stack.getCount();
                }
            }
            if (available <= 0) {
                continue;
            }

            long needed = amount - selected;
            long take = needed / value + (needed % value == 0 ? 0 : 1);
            take = Math.min(take, available);
            if (take > 0) {
                plan.put(denomination.getValue(), take);
                try {
                    selected = Math.addExact(selected, Math.multiplyExact(take, value));
                } catch (ArithmeticException e) {
                    return false;
                }
            }
            if (selected >= amount) {
                break;
            }
        }
        if (selected < amount) {
            return false;
        }

        for (Map.Entry<Item, Long> entry : plan.entrySet()) {
            long remaining = entry.getValue();
            for (ItemStack stack : player.getInventory().items) {
                if (remaining <= 0) {
                    break;
                }
                if (stack.getItem() == entry.getKey()) {
                    int take = (int) Math.min(remaining, stack.getCount());
                    stack.shrink(take);
                    remaining -= take;
                }
            }
        }

        long change = selected - amount;
        if (change > 0) {
            giveMoney(player, currency, change);
        }
        return true;
    }

    /** Gives {@code amount} of {@code currency} as physical items (greedy denominations). */
    public static void giveMoney(Player player, Currency currency, long amount) {
        if (amount <= 0) {
            return;
        }
        long remaining = amount;
        // Always issue change greedily: largest denominations first.
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
        log(player, "收入", currency, amount);
    }

    private static void log(Player player, String action, Currency currency, long amount) {
        if (player.getServer() != null) {
            EconomyLogSavedData.get(player.getServer()).append(
                    player.getServer().overworld().getGameTime(), player.getUUID(), action, currency.id(), amount);
        }
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
