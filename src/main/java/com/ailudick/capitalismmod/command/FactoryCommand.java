package com.ailudick.capitalismmod.command;

import com.ailudick.capitalismmod.company.Company;
import com.ailudick.capitalismmod.company.CompanySavedData;
import com.ailudick.capitalismmod.factory.FactoryRegionSavedData;
import com.ailudick.capitalismmod.factory.FactoryRegionService;
import com.ailudick.capitalismmod.blockentity.FactoryMachineBlockEntity;
import com.ailudick.capitalismmod.company.Industries;
import com.ailudick.capitalismmod.company.IndustrySpec;
import com.ailudick.capitalismmod.company.ProductionRecipe;
import com.ailudick.capitalismmod.entity.PlaceholderNpc;
import com.ailudick.capitalismmod.population.NpcProfession;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

/** Administrative commands for physical factory areas. */
public final class FactoryCommand {
    private FactoryCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("factory").requires(source -> source.hasPermission(2))
                .then(Commands.literal("create")
                        .then(Commands.argument("company", StringArgumentType.word())
                                .then(Commands.argument("radius", IntegerArgumentType.integer(4, 64))
                                        .executes(context -> create(context.getSource(),
                                                StringArgumentType.getString(context, "company"),
                                                IntegerArgumentType.getInteger(context, "radius"))))))
                .then(Commands.literal("list").executes(context -> list(context.getSource())))
                .then(Commands.literal("scan").executes(context -> scan(context.getSource())))
                .then(Commands.literal("machine")
                        .then(Commands.literal("configure")
                                .then(Commands.argument("industry", StringArgumentType.word())
                                        .then(Commands.argument("recipe", StringArgumentType.word())
                                                .executes(context -> configureMachine(context.getSource(),
                                                        StringArgumentType.getString(context, "industry"),
                                                        StringArgumentType.getString(context, "recipe"))))))
                        .then(Commands.literal("feed")
                                .then(Commands.argument("item", StringArgumentType.string())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 4096))
                                                .executes(context -> feedMachine(context.getSource(),
                                                        StringArgumentType.getString(context, "item"),
                                                IntegerArgumentType.getInteger(context, "amount"))))))
                        .then(Commands.literal("form").executes(context -> formMachine(context.getSource())))
                        .then(Commands.literal("assign")
                                .then(Commands.argument("profession", StringArgumentType.word())
                                        .executes(context -> assignWorker(context.getSource(),
                                                StringArgumentType.getString(context, "profession")))))
                        .then(Commands.literal("unassign").executes(context -> unassignWorker(context.getSource())))
                        .then(Commands.literal("status").executes(context -> machineStatus(context.getSource()))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .executes(context -> remove(context.getSource(), StringArgumentType.getString(context, "id"))))));
    }

    private static int create(CommandSourceStack source, String companyName, int radius) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("必须由玩家在工厂控制器附近执行。")); return 0;
        }
        Company company = CompanySavedData.get(source.getServer()).companies().values().stream()
                .filter(candidate -> candidate.name().equals(companyName) && candidate.ownerUuid().equals(player.getUUID()))
                .findFirst().orElse(null);
        if (company == null) { source.sendFailure(Component.literal("找不到属于你的公司: " + companyName)); return 0; }
        FactoryRegionSavedData.Region region = FactoryRegionService.create(source.getServer(), player.serverLevel(),
                player.blockPosition(), company.companyId(), radius);
        if (region == null) { source.sendFailure(Component.literal("工厂区域创建失败。")); return 0; }
        source.sendSuccess(() -> Component.literal("工厂区域已创建: " + region.id()
                + "，范围半径 " + radius + " 格"), true);
        return 1;
    }

    private static int list(CommandSourceStack source) {
        int count = 0;
        for (FactoryRegionSavedData.Region region : FactoryRegionSavedData.get(source.getServer()).regions()) {
            source.sendSuccess(() -> Component.literal(region.id() + " | company=" + region.companyId()
                    + " | " + region.dimension() + " | volume=" + region.volume()), false);
            count++;
        }
        return count;
    }

    private static int scan(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) return 0;
        FactoryRegionSavedData.Region region = FactoryRegionSavedData.get(source.getServer()).at(
                player.level().dimension().location().toString(), player.getBlockX(), player.getBlockY(), player.getBlockZ());
        if (region == null) { source.sendFailure(Component.literal("你不在工厂区域内。")); return 0; }
        FactoryRegionService.Scan scan = FactoryRegionService.scan(player.serverLevel(), region);
        source.sendSuccess(() -> Component.literal("工厂 " + region.id() + " | 仓库=" + scan.warehouses()
                + " | 机器=" + scan.machines() + " | 物流节点=" + scan.logisticsNodes()), false);
        return 1;
    }

    private static int remove(CommandSourceStack source, String id) {
        if (!FactoryRegionSavedData.get(source.getServer()).remove(id)) {
            source.sendFailure(Component.literal("工厂区域不存在: " + id)); return 0;
        }
        source.sendSuccess(() -> Component.literal("已移除工厂区域: " + id), true);
        return 1;
    }

    private static FactoryMachineBlockEntity nearestMachine(ServerPlayer player) {
        BlockPos origin = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-4, -4, -4), origin.offset(4, 4, 4))) {
            if (player.level().getBlockEntity(pos) instanceof FactoryMachineBlockEntity machine) return machine;
        }
        return null;
    }

    private static int configureMachine(CommandSourceStack source, String industryId, String recipeId) {
        if (!(source.getEntity() instanceof ServerPlayer player)) return 0;
        IndustrySpec industry = Industries.byId(industryId);
        if (industry == null) { source.sendFailure(Component.literal("未知产业: " + industryId)); return 0; }
        ProductionRecipe recipe = industry.recipe(recipeId);
        if (recipe == null || recipe.outputs().isEmpty()) {
            source.sendFailure(Component.literal("未知或不可加工配方: " + recipeId)); return 0;
        }
        FactoryMachineBlockEntity machine = nearestMachine(player);
        if (machine == null) { source.sendFailure(Component.literal("4格范围内没有实体机器")); return 0; }
        machine.configure(industryId, recipe.machineType(), recipe.id());
        source.sendSuccess(() -> Component.literal("机器已配置: " + industryId + "/" + recipe.id()
                + " | 工序=" + recipe.machineType()), true);
        return 1;
    }

    private static int feedMachine(CommandSourceStack source, String itemId, int amount) {
        if (!(source.getEntity() instanceof ServerPlayer player)) return 0;
        FactoryMachineBlockEntity machine = nearestMachine(player);
        if (machine == null) { source.sendFailure(Component.literal("4格范围内没有实体机器")); return 0; }
        int moved = machine.insertInput(itemId, amount);
        source.sendSuccess(() -> Component.literal("已送入 " + moved + " 个 " + itemId), false);
        return moved;
    }

    private static int machineStatus(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) return 0;
        FactoryMachineBlockEntity machine = nearestMachine(player);
        if (machine == null) { source.sendFailure(Component.literal("4格范围内没有实体机器")); return 0; }
        source.sendSuccess(() -> Component.literal("机器=" + machine.machineType() + " | 配方="
                + machine.industryId() + "/" + machine.recipeId() + " | 状态=" + machine.status()
                + " | 进度=" + machine.progress() + "/" + machine.maxProgress()
                + " | 结构=" + machine.formed() + "(" + machine.structure().width() + "x"
                + machine.structure().height() + "x" + machine.structure().depth() + ")"
                + " | 工人=" + (machine.worker() == null ? "none" : machine.worker().professionId()
                + "/skill=" + machine.worker().skill())
                + " | 输入=" + machine.inputBuffer() + " | 输出=" + machine.outputBuffer()), false);
        return 1;
    }

    private static int formMachine(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) return 0;
        FactoryMachineBlockEntity machine = nearestMachine(player);
        if (machine == null) { source.sendFailure(Component.literal("4格范围内没有实体机器")); return 0; }
        boolean formed = machine.formStructure();
        source.sendSuccess(() -> Component.literal(formed ? "多方块结构完整，机器已成型" :
                "结构不完整：请按机器尺寸用外壳方块搭建，内部保持空心"), true);
        return formed ? 1 : 0;
    }

    private static int assignWorker(CommandSourceStack source, String professionId) {
        if (!(source.getEntity() instanceof ServerPlayer player)) return 0;
        NpcProfession profession = NpcProfession.parse(professionId);
        if (profession == null || profession == NpcProfession.UNEMPLOYED) {
            source.sendFailure(Component.literal("未知职业，可用: metallurgist, chemical_engineer, semiconductor_engineer, assembly_technician 等"));
            return 0;
        }
        FactoryMachineBlockEntity machine = nearestMachine(player);
        if (machine == null) { source.sendFailure(Component.literal("4格范围内没有实体机器")); return 0; }
        PlaceholderNpc npc = player.level().getEntitiesOfClass(PlaceholderNpc.class,
                        new AABB(player.blockPosition()).inflate(8.0D)).stream().findFirst().orElse(null);
        if (npc == null) { source.sendFailure(Component.literal("8格范围内没有 NPC")); return 0; }
        npc.assignProfession(profession);
        if (!machine.assignWorker(npc)) {
            source.sendFailure(Component.literal("该职业不能操作机器: " + machine.machineType()));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("NPC 已绑定岗位: " + profession.id() + " -> " + machine.machineType()), true);
        return 1;
    }

    private static int unassignWorker(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) return 0;
        FactoryMachineBlockEntity machine = nearestMachine(player);
        if (machine == null) { source.sendFailure(Component.literal("4格范围内没有实体机器")); return 0; }
        machine.clearWorker();
        source.sendSuccess(() -> Component.literal("机器岗位已释放，NPC 回到待业状态"), true);
        return 1;
    }
}
