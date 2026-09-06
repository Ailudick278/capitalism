package com.ailudick.capitalismmod.command;

import com.ailudick.capitalismmod.economy.contract.ContractDisputeSavedData;
import com.ailudick.capitalismmod.economy.contract.ContractStatus;
import com.ailudick.capitalismmod.economy.contract.EconomicContractSavedData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/** Administrator-controlled contract dispute filing and resolution. */
public final class ContractDisputeCommand {
    private ContractDisputeCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("contract-dispute")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("contractId", StringArgumentType.word())
                        .then(Commands.argument("reason", StringArgumentType.greedyString())
                                .executes(context -> open(context.getSource(),
                                        StringArgumentType.getString(context, "contractId"),
                                        StringArgumentType.getString(context, "reason"))))));
        dispatcher.register(Commands.literal("contract-resolve")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("disputeId", StringArgumentType.word())
                        .then(Commands.argument("result", StringArgumentType.word())
                                .then(Commands.argument("resolution", StringArgumentType.greedyString())
                                        .executes(context -> resolve(context.getSource(),
                                                StringArgumentType.getString(context, "disputeId"),
                                                StringArgumentType.getString(context, "result"),
                                                StringArgumentType.getString(context, "resolution")))))));
    }

    private static int open(CommandSourceStack source, String contractId, String reason) {
        String disputeId = "dispute-" + java.util.UUID.randomUUID();
        long now = source.getServer().overworld().getGameTime();
        boolean opened = EconomicContractSavedData.get(source.getServer())
                .openDispute(source.getServer(), contractId, disputeId, now, reason);
        if (!opened) {
            source.sendFailure(Component.literal("合同不存在、不可争议或已有未结争议。"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("争议已登记：" + disputeId), false);
        return 1;
    }

    private static int resolve(CommandSourceStack source, String disputeId, String result, String resolution) {
        ContractDisputeSavedData.Dispute dispute = ContractDisputeSavedData.get(source.getServer()).find(disputeId);
        ContractStatus status = switch (result.toLowerCase(java.util.Locale.ROOT)) {
            case "complete", "completed" -> ContractStatus.COMPLETED;
            case "cancel", "cancelled" -> ContractStatus.CANCELLED;
            case "breach", "breached" -> ContractStatus.BREACHED;
            default -> null;
        };
        if (dispute == null || status == null || !"OPEN".equals(dispute.status())) {
            source.sendFailure(Component.literal("争议不存在、已处理，或裁决结果无效（complete/cancel/breach）。"));
            return 0;
        }
        long now = source.getServer().overworld().getGameTime();
        if (!EconomicContractSavedData.get(source.getServer()).transition(dispute.contractId(), status, now)
                || !ContractDisputeSavedData.get(source.getServer()).resolve(disputeId, result.toUpperCase(java.util.Locale.ROOT), now, resolution)) {
            source.sendFailure(Component.literal("合同当前状态不允许该裁决。"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("争议已裁决：" + disputeId + " -> " + status.name()), false);
        return 1;
    }
}
