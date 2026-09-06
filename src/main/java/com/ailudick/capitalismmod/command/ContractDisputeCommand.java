package com.ailudick.capitalismmod.command;

import com.ailudick.capitalismmod.economy.contract.ContractDisputeSavedData;
import com.ailudick.capitalismmod.economy.contract.ContractStatus;
import com.ailudick.capitalismmod.economy.contract.EconomicContractSavedData;
import com.ailudick.capitalismmod.business.BusinessOrder;
import com.ailudick.capitalismmod.business.BusinessOrderEscrowSavedData;
import com.ailudick.capitalismmod.business.BusinessOrderSavedData;
import com.ailudick.capitalismmod.population.PopulationSavedData;
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
        if (status == ContractStatus.COMPLETED
                && BusinessOrderSavedData.get(source.getServer()).get(dispute.contractId()) != null) {
            source.sendFailure(Component.literal("个人企业订单必须通过订单交付服务完成，不能由通用裁决直接完成。"));
            return 0;
        }
        if ((status == ContractStatus.CANCELLED || status == ContractStatus.BREACHED)
                && !refundBusinessOrderEscrow(source, dispute)) {
            source.sendFailure(Component.literal("订单托管退款未完成，裁决暂不生效。"));
            return 0;
        }
        if (!EconomicContractSavedData.get(source.getServer()).transition(dispute.contractId(), status, now)
                || !ContractDisputeSavedData.get(source.getServer()).resolve(disputeId, result.toUpperCase(java.util.Locale.ROOT), now, resolution)) {
            source.sendFailure(Component.literal("合同当前状态不允许该裁决。"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("争议已裁决：" + disputeId + " -> " + status.name()), false);
        return 1;
    }

    private static boolean refundBusinessOrderEscrow(CommandSourceStack source,
                                                     ContractDisputeSavedData.Dispute dispute) {
        BusinessOrder order = BusinessOrderSavedData.get(source.getServer()).get(dispute.contractId());
        if (order == null) return true;
        PopulationSavedData population = PopulationSavedData.get(source.getServer());
        BusinessOrderEscrowSavedData escrowData = BusinessOrderEscrowSavedData.get(source.getServer());
        for (BusinessOrderEscrowSavedData.Escrow escrow : escrowData.escrows()) {
            if (!escrow.orderId().equals(order.id()) || escrow.heldMinor() <= 0L) continue;
            String refundSource = "contract-dispute-refund:" + dispute.id() + ":" + escrow.batchId();
            if (!population.hasCreditedSource(refundSource)
                    && !population.addCashOnce(escrow.buyerId(), escrow.heldMinor(), refundSource)) return false;
            if (!escrowData.refundOnce(escrow.orderId(), escrow.batchId(), escrow.heldMinor())) return false;
        }
        BusinessOrderSavedData.get(source.getServer()).put(order.withStatus("cancelled"));
        return true;
    }
}
