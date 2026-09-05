package com.ailudick.capitalismmod.block;

import com.ailudick.capitalismmod.business.BusinessTypes;
import com.ailudick.capitalismmod.business.IndividualBusiness;
import com.ailudick.capitalismmod.business.IndividualBusinessHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Testing and service block for the sole proprietor system. */
public class IndividualBusinessBlock extends Block {
    public IndividualBusinessBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hitResult) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            IndividualBusiness business = IndividualBusinessHelper.get(serverPlayer);
            if (business == null) {
                serverPlayer.sendSystemMessage(Component.literal(
                        "尚未登记个体户。使用 /business register <名称> <经营范围ID> 登记。"));
            } else {
                serverPlayer.sendSystemMessage(Component.literal("=== 个体户服务台 ==="));
                serverPlayer.sendSystemMessage(Component.literal("名称：" + business.name()
                        + " | ID：" + business.businessId()));
                serverPlayer.sendSystemMessage(Component.literal("经营范围："
                        + BusinessTypes.displayName(business.scope()) + " (" + business.scope() + ")"));
                serverPlayer.sendSystemMessage(Component.literal("状态：" + business.status()
                        + " | USD 经营账户：" + business.balance("usd")
                        + " | 欠税：" + business.taxOwed()));
                serverPlayer.sendSystemMessage(Component.literal(
                        "订单：/business order list | 账本：/business ledger"));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
