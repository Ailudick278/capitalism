package com.ailudick.capitalismmod.item;

import com.ailudick.capitalismmod.init.ModDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A business license (营业执照). Company registration happens at the business
 * bureau (工商局), which issues a bound license; a blank license merely hints
 * the player toward the bureau, and a bound license shows its company info.
 */
public class BusinessLicense extends Item {
    public BusinessLicense(Properties properties) {
        super(properties);
    }

    public static boolean isBound(ItemStack stack) {
        return stack.has(ModDataComponents.COMPANY_NAME.get());
    }

    public static String getCompanyName(ItemStack stack) {
        return stack.get(ModDataComponents.COMPANY_NAME.get());
    }

    public static String getCompanyType(ItemStack stack) {
        return stack.get(ModDataComponents.COMPANY_TYPE.get());
    }

    public static void bind(ItemStack stack, String name, String type) {
        stack.set(ModDataComponents.COMPANY_NAME.get(), name);
        stack.set(ModDataComponents.COMPANY_TYPE.get(), type);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            if (isBound(stack)) {
                player.sendSystemMessage(Component.translatable("message.capitalismmod.company_info",
                        getCompanyName(stack), Component.translatable("company_type.capitalismmod." + getCompanyType(stack))));
            } else {
                player.sendSystemMessage(Component.translatable("message.capitalismmod.register_at_bureau"));
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
