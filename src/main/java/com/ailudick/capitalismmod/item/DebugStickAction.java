package com.ailudick.capitalismmod.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Extension point for the debug stick's right-click behavior.
 * The default action is intentionally a no-op.
 */
@FunctionalInterface
public interface DebugStickAction {
    void execute(Level level, Player player, InteractionHand hand, ItemStack stack);
}
