package com.ailudick.capitalismmod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** External input/output connection point for factory logistics. */
public final class FactoryPortBlock extends Block {
    private final boolean input;

    public FactoryPortBlock(Properties properties, boolean input) {
        super(properties);
        this.input = input;
    }

    public boolean isInput() { return input; }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hit) {
        if (!level.isClientSide) player.sendSystemMessage(Component.literal(input
                ? "工厂输入接口：等待外部物流送入原料。"
                : "工厂输出接口：等待外部物流取走成品。"));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
