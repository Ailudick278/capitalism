package com.ailudick.capitalismmod.block;

import com.ailudick.capitalismmod.company.Company;
import com.ailudick.capitalismmod.company.CompanySavedData;
import com.ailudick.capitalismmod.company.CompanySiteAllocationSavedData;
import com.ailudick.capitalismmod.company.CompanySiteSavedData;
import com.ailudick.capitalismmod.company.CompanyEconomy;
import com.ailudick.capitalismmod.company.ProductionRecipe;
import com.ailudick.capitalismmod.company.CompanyProductionSavedData;
import com.ailudick.capitalismmod.compat.IndustrialCompat;
import com.ailudick.capitalismmod.blockentity.FactoryBlockEntity;
import com.ailudick.capitalismmod.factory.FactoryRegionSavedData;
import com.ailudick.capitalismmod.factory.FactoryRegionService;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Physical visualization anchor for an existing company operating site. */
public final class FactoryBlock extends Block implements EntityBlock {
    public FactoryBlock(Properties properties) { super(properties); }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FactoryBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            Company company = companyAt(serverPlayer, pos);
            if (company == null) {
                serverPlayer.sendSystemMessage(Component.literal(
                        "该工厂尚未绑定公司。先在当前区块执行 /company site <公司名>。"));
            } else {
                ProductionRecipe recipe = CompanyEconomy.recipe(company);
                FactoryRegionSavedData.Region region = FactoryRegionSavedData.get(serverPlayer.getServer()).at(
                        level.dimension().location().toString(), pos.getX(), pos.getY(), pos.getZ());
                if (region != null) {
                    FactoryRegionService.Scan scan = FactoryRegionService.scan(serverPlayer.serverLevel(), region);
                    CompanyProductionSavedData.ProductionState production =
                            CompanyProductionSavedData.get(serverPlayer.getServer()).get(company.companyId());
                    String status = production == null ? "未初始化" : production.failedCycles() > production.successfulCycles()
                            ? "需要检修或补充原料" : "运行正常";
                    serverPlayer.sendSystemMessage(Component.literal("区域 " + region.id() + "｜状态: " + status
                            + "｜仓库 " + scan.warehouses() + "｜机器 " + scan.machines()
                            + "｜输入接口 " + scan.inputPorts() + "｜输出接口 " + scan.outputPorts()
                            + "｜物流节点 " + scan.logisticsNodes()));
                }
                CompanySiteSavedData.Site site = new CompanySiteSavedData.Site(company.companyId(),
                        level.dimension().location().toString(), pos.getX() >> 4, pos.getZ() >> 4);
                CompanySiteAllocationSavedData.Allocation allocation =
                        CompanySiteAllocationSavedData.get(serverPlayer.getServer()).get(company.companyId(), site);
                serverPlayer.sendSystemMessage(Component.literal("工厂 " + company.name()
                        + "｜配方: " + recipe.id() + "｜原料: " + recipe.inputs()
                        + "｜产出: " + recipe.outputs() + "｜设备: "
                        + (allocation == null ? "公司级" : allocation.machines()) + "｜工人: "
                        + (allocation == null ? "公司级" : allocation.workers())));
                serverPlayer.sendSystemMessage(Component.literal("物流后端: "
                        + IndustrialCompat.transportBackend()
                        + "（工厂端口已预留，当前仍使用模组仓储结算）"));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, net.minecraft.util.RandomSource random) {
        if (random.nextInt(3) != 0) return;
        level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.5D,
                pos.getY() + 1.05D, pos.getZ() + 0.5D, 0.0D, 0.04D, 0.0D);
        if (random.nextBoolean()) level.addParticle(ParticleTypes.FLAME,
                pos.getX() + 0.5D, pos.getY() + 1.02D, pos.getZ() + 0.5D,
                0.0D, 0.02D, 0.0D);
    }

    private static Company companyAt(ServerPlayer player, BlockPos pos) {
        String dimension = player.level().dimension().location().toString();
        int chunkX = pos.getX() >> 4, chunkZ = pos.getZ() >> 4;
        for (Company company : CompanySavedData.get(player.getServer()).companies().values()) {
            if (!company.ownerUuid().equals(player.getUUID())) continue;
            boolean present = CompanySiteSavedData.get(player.getServer()).sites(company.companyId()).stream()
                    .anyMatch(site -> dimension.equals(site.dimension()) && site.chunkX() == chunkX && site.chunkZ() == chunkZ);
            if (present) return company;
        }
        return null;
    }
}
