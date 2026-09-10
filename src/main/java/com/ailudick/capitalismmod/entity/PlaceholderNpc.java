package com.ailudick.capitalismmod.entity;

import com.ailudick.capitalismmod.init.ModEntities;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import com.ailudick.capitalismmod.population.Household;
import com.ailudick.capitalismmod.population.PopulationSavedData;
import com.ailudick.capitalismmod.population.NpcProfession;
import net.minecraft.core.BlockPos;
import java.util.UUID;
import java.util.EnumSet;

public class PlaceholderNpc extends PathfinderMob {
    private static final String HOUSEHOLD_ID_TAG = "HouseholdId";
    private String householdId = "";
    private String professionId = NpcProfession.UNEMPLOYED.id();
    private long workMachine = Long.MIN_VALUE;
    private int skill = 50;
    private int workAnimationTicks;
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.5D);
    }

    public PlaceholderNpc(EntityType<? extends PlaceholderNpc> type, Level level) {
        super(type, level);
    }

    public String householdId() {
        return householdId;
    }

    public boolean isBound() {
        return !householdId.isBlank();
    }
    public String professionId() { return professionId; }
    public NpcProfession profession() { return NpcProfession.parse(professionId); }
    public int skill() { return skill; }
    public boolean hasWorkMachine() { return workMachine != Long.MIN_VALUE; }
    public BlockPos workMachine() { return hasWorkMachine() ? BlockPos.of(workMachine) : null; }
    public void assignProfession(NpcProfession profession) { professionId = profession == null ? "unemployed" : profession.id(); }
    public void assignWorkMachine(BlockPos pos) { workMachine = pos == null ? Long.MIN_VALUE : pos.asLong(); }
    public void clearWorkMachine() { workMachine = Long.MIN_VALUE; }

    public void bindHousehold(String id) {
        householdId = id == null ? "" : id.trim();
        refreshDisplayName();
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount % 40 == 0) refreshDisplayName();
    }

    private void refreshDisplayName() {
        if (!(level() instanceof ServerLevel serverLevel) || householdId.isBlank()) return;
        Household household = PopulationSavedData.get(serverLevel.getServer()).find(householdId);
        if (household == null) return;
        setCustomName(Component.literal("居民 · " + household.region() + " · " + household.size() + "人"));
        setCustomNameVisible(true);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (!householdId.isBlank()) tag.putString(HOUSEHOLD_ID_TAG, householdId);
        tag.putString("Profession", professionId); tag.putLong("WorkMachine", workMachine); tag.putInt("Skill", skill);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        householdId = tag.getString(HOUSEHOLD_ID_TAG);
        professionId = tag.contains("Profession") ? tag.getString("Profession") : "unemployed";
        workMachine = tag.contains("WorkMachine") ? tag.getLong("WorkMachine") : Long.MIN_VALUE;
        skill = Math.max(0, Math.min(100, tag.contains("Skill") ? tag.getInt("Skill") : 50));
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(2, new WorkAtMachineGoal(this));
        goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.6D));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    /** Moves a staffed NPC to its physical workstation and gives the player feedback. */
    private static final class WorkAtMachineGoal extends Goal {
        private final PlaceholderNpc npc;
        private BlockPos target;
        private WorkAtMachineGoal(PlaceholderNpc npc) {
            this.npc = npc;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }
        @Override public boolean canUse() {
            target = npc.workMachine();
            return target != null && npc.level().getBlockEntity(target) != null;
        }
        @Override public boolean canContinueToUse() {
            return npc.hasWorkMachine() && target != null && npc.level().getBlockEntity(target) != null;
        }
        @Override public void stop() {
            npc.getNavigation().stop();
            target = null;
        }
        @Override public void tick() {
            if (target == null) return;
            double distance = npc.distanceToSqr(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D);
            if (distance > 9.0D) {
                npc.getNavigation().moveTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, 1.0D);
            } else {
                npc.getNavigation().stop();
                npc.getLookControl().setLookAt(target.getX() + 0.5D, target.getY() + 0.8D, target.getZ() + 0.5D);
                if (++npc.workAnimationTicks % 40 == 0) npc.swing(InteractionHand.MAIN_HAND);
            }
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND && !level().isClientSide) {
            Household household = householdId.isBlank() || !(level() instanceof ServerLevel serverLevel)
                    ? null : PopulationSavedData.get(serverLevel.getServer()).find(householdId);
            if (household == null) {
                player.sendSystemMessage(Component.literal("这是一个尚未绑定家庭数据的居民。"));
            } else {
                player.sendSystemMessage(Component.literal("家庭 " + household.id()
                        + "｜地区: " + household.region() + "｜人数: " + household.size()
                        + "｜满意度: " + household.satisfaction() + "%"));
            }
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    public static PlaceholderNpc create(Level level) {
        return ModEntities.PLACEHOLDER_NPC.get().create(level);
    }
}
