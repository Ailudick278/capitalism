package com.ailudick.capitalismmod.block;

import com.ailudick.capitalismmod.blockentity.FactoryMachineBlockEntity;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import com.ailudick.capitalismmod.init.ModBlockEntities;
import net.minecraft.world.phys.BlockHitResult;

/** Generic physical machine placeholder; recipe-specific machines can extend this later. */
public final class FactoryMachineBlock extends Block implements EntityBlock {
    public FactoryMachineBlock(Properties properties) { super(properties); }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FactoryMachineBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                    BlockEntityType<T> type) {
        if (level.isClientSide || type != ModBlockEntities.FACTORY_MACHINE.get()) return null;
        return (BlockEntityTicker<T>) ((level1, pos1, state1, blockEntity) ->
                FactoryMachineBlockEntity.serverTick(level1, pos1, state1,
                        (FactoryMachineBlockEntity) blockEntity));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            FactoryMachineBlockEntity machine = level.getBlockEntity(pos) instanceof FactoryMachineBlockEntity value
                    ? value : null;
            player.sendSystemMessage(Component.literal(machine == null ? "工厂机器未初始化。"
                    : "工厂机器｜类型: " + machine.machineType() + "｜配方: " + machine.recipeId()
                    + "｜状态: " + machine.status() + "｜进度: " + machine.progress() + "/" + machine.maxProgress()));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, net.minecraft.util.RandomSource random) {
        if (random.nextInt(5) == 0) level.addParticle(ParticleTypes.SMOKE,
                pos.getX() + 0.5D, pos.getY() + 1.05D, pos.getZ() + 0.5D,
                0.0D, 0.03D, 0.0D);
    }
}
