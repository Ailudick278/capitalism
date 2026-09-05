package com.ailudick.capitalismmod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Unified in-game help for players learning the economy systems. */
public final class CapitalismCommand {
    private CapitalismCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("capitalism")
                .then(Commands.literal("help")
                        .executes(ctx -> help(ctx.getSource().getPlayerOrException(), "general"))
                        .then(Commands.argument("topic", StringArgumentType.word())
                                .executes(ctx -> help(ctx.getSource().getPlayerOrException(),
                                        StringArgumentType.getString(ctx, "topic"))))));
    }

    private static int help(ServerPlayer player, String topic) {
        player.sendSystemMessage(Component.literal("=== 资本主义模组帮助 ==="));
        switch (topic.toLowerCase()) {
            case "bank", "银行" -> {
                player.sendSystemMessage(Component.literal("银行：先摆放银行方块，再使用借记卡/信用卡开户。"));
                player.sendSystemMessage(Component.literal("可进行存取款、定期存款、贷款、转账和外汇兑换。"));
                player.sendSystemMessage(Component.literal("贷款逾期会提高利息；银行流水可在界面或 /economylog 查看。"));
            }
            case "company", "公司" -> {
                player.sendSystemMessage(Component.literal("公司：在工商局注册公司，把原料放入仓库后按产业链生产。"));
                player.sendSystemMessage(Component.literal("公司每个生产周期会计入税款和维护费，原料不足会停产。"));
                player.sendSystemMessage(Component.literal("使用 /company list、/company upgrade、/company withdraw 管理公司。"));
            }
            case "business", "个体户" -> {
                player.sendSystemMessage(Component.literal("个体户：先登记经营名称和经营范围，再使用独立经营账户管理经营资金。"));
                player.sendSystemMessage(Component.literal("/business register <名称> <经营范围>，/business info 查看登记信息。"));
                player.sendSystemMessage(Component.literal("/business deposit <金额> 存入现金，/business withdraw <金额> 进行业主提款。"));
            }
            case "market", "市场" -> {
                player.sendSystemMessage(Component.literal("市场：股票和商品支持限价单、部分成交和取消订单。"));
                player.sendSystemMessage(Component.literal("使用 /marketorders 查看活动订单，/markettrades 查看成交记录。"));
                player.sendSystemMessage(Component.literal("管理员可用 /economystats 和 /marketrepair scan 排查经济异常。"));
            }
            default -> {
                player.sendSystemMessage(Component.literal("/balance 查看余额，/pay 转账，/fx 查看汇率。"));
                player.sendSystemMessage(Component.literal("/capitalism help bank|company|market 查看专题帮助。"));
                player.sendSystemMessage(Component.literal("/company list 管理公司，/loans 查看个人借贷。"));
            }
        }
        return 1;
    }
}
