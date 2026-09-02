package com.ailudick.capitalismmod.item;

import com.ailudick.capitalismmod.bank.BankCardNumber;
import com.ailudick.capitalismmod.init.ModDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A bank card, either a debit card or a credit card. A bound card records its account number.
 */
public class BankCard extends Item {
    private final boolean credit;

    public BankCard(boolean credit, Properties properties) {
        super(properties);
        this.credit = credit;
    }

    public boolean isCredit() {
        return credit;
    }

    public static boolean isBound(ItemStack stack) {
        return stack.has(ModDataComponents.ACCOUNT_ID.get());
    }

    public static String getAccountId(ItemStack stack) {
        return stack.get(ModDataComponents.ACCOUNT_ID.get());
    }

    public static void bind(ItemStack stack, String accountId) {
        stack.set(ModDataComponents.ACCOUNT_ID.get(), accountId);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            if (isBound(stack)) {
                player.sendSystemMessage(Component.translatable("message.capitalismmod.card_bound", BankCardNumber.format(getAccountId(stack))));
            } else {
                player.sendSystemMessage(Component.translatable(credit
                        ? "message.capitalismmod.credit_card_blank"
                        : "message.capitalismmod.debit_card_blank"));
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
