package com.ailudick.capitalismmod.currency;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * A physical denomination of a currency. There is no wallet; currency must be
 * deposited into a bank account at a bank.
 */
public class CurrencyItem extends Item {
    private static final Map<String, TreeMap<Long, Item>> DENOMINATIONS = new HashMap<>();

    private final Currency currency;
    private final long value;

    public CurrencyItem(Currency currency, long value, Properties properties) {
        super(properties);
        this.currency = currency;
        this.value = value;
        DENOMINATIONS.computeIfAbsent(currency.id(), k -> new TreeMap<>(Comparator.reverseOrder()))
                .put(value, this);
    }

    public Currency currency() {
        return currency;
    }

    public long value() {
        return value;
    }

    /** All denomination entries for a currency, highest value first. */
    public static List<Map.Entry<Long, Item>> denominations(String currencyId) {
        TreeMap<Long, Item> map = DENOMINATIONS.get(currencyId);
        if (map == null) {
            return List.of();
        }
        return new ArrayList<>(map.entrySet());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
