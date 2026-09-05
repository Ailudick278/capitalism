package com.ailudick.capitalismmod.entity;

import com.ailudick.capitalismmod.init.ModEntities;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class PlaceholderNpc extends PathfinderMob {
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.5D);
    }

    public PlaceholderNpc(EntityType<? extends PlaceholderNpc> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.6D));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND && !level().isClientSide) {
            player.sendSystemMessage(Component.translatable("entity.capitalismmod.placeholder_npc.coming_soon"));
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    public static PlaceholderNpc create(Level level) {
        return ModEntities.PLACEHOLDER_NPC.get().create(level);
    }
}
