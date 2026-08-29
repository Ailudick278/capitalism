package com.ailudick.capitalismmod.item;

import com.ailudick.capitalismmod.currency.Currencies;
import com.ailudick.capitalismmod.currency.Money;
import com.ailudick.capitalismmod.wallet.EconomyHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Debug stick: right-click to grant 100 USD, for testing the economy.
 * Reuses the vanilla stick model/texture.
 */
public class DebugStick extends Item {
    public DebugStick(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            EconomyHelper.giveMoney(player, Currencies.USD, Money.toMinor(100));
            player.sendSystemMessage(Component.translatable("message.capitalismmod.debug_stick",
                    Money.format(EconomyHelper.getBalance(player, Currencies.USD))));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
