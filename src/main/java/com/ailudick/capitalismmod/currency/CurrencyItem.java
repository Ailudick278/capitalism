package com.ailudick.capitalismmod.currency;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.TooltipFlag;
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
    public enum Form { COIN, BANKNOTE }

    private static final Map<String, TreeMap<Long, List<Item>>> DENOMINATIONS = new HashMap<>();

    private final Currency currency;
    private final long value;
    private final Form form;
    private final String circulationStatus;

    public CurrencyItem(Currency currency, long value, Properties properties) {
        this(currency, value, Form.BANKNOTE, "current", properties);
    }

    public CurrencyItem(Currency currency, long value, Form form, String circulationStatus, Properties properties) {
        super(properties);
        this.currency = currency;
        this.value = value;
        this.form = form;
        this.circulationStatus = circulationStatus;
        DENOMINATIONS.computeIfAbsent(currency.id(), k -> new TreeMap<>(Comparator.reverseOrder()))
                .computeIfAbsent(value, ignored -> new ArrayList<>()).add(this);
    }

    public Currency currency() {
        return currency;
    }

    public long value() {
        return value;
    }

    public Form form() {
        return form;
    }

    public String circulationStatus() {
        return circulationStatus;
    }

    public boolean isCommonCirculation() {
        return "current".equals(circulationStatus);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents,
                                TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        if (form == Form.COIN) {
            tooltipComponents.add(Component.literal("硬币").withStyle(ChatFormatting.GRAY));
        }
        if (!"current".equals(circulationStatus)) {
            tooltipComponents.add(Component.literal("历史/少见/停止生产面额：" + circulationStatus)
                    .withStyle(ChatFormatting.YELLOW));
        }
    }

    /** All denomination entries for a currency, highest value first. */
    public static List<Map.Entry<Long, Item>> denominations(String currencyId) {
        TreeMap<Long, List<Item>> map = DENOMINATIONS.get(currencyId);
        if (map == null) {
            return List.of();
        }
        List<Map.Entry<Long, Item>> entries = new ArrayList<>();
        map.forEach((value, items) -> items.forEach(item -> entries.add(Map.entry(value, item))));
        entries.sort((left, right) -> {
            int valueOrder = Long.compare(right.getKey(), left.getKey());
            if (valueOrder != 0) return valueOrder;
            boolean leftCommon = ((CurrencyItem) left.getValue()).isCommonCirculation();
            boolean rightCommon = ((CurrencyItem) right.getValue()).isCommonCirculation();
            return Boolean.compare(rightCommon, leftCommon);
        });
        return entries;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
