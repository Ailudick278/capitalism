package com.ailudick.capitalismmod.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Debug stick with an extensible right-click action. The default action is a no-op.
 * Reuses the vanilla stick model/texture.
 */
public class DebugStick extends Item {
    private static DebugStickAction action = (level, player, hand, stack) -> {
    };

    public DebugStick(Properties properties) {
        super(properties);
    }

    /** Replaces the debug stick behavior for development or test integrations. */
    public static void setAction(DebugStickAction newAction) {
        action = newAction == null ? (level, player, hand, stack) -> {
        } : newAction;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            action.execute(level, player, hand, stack);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
