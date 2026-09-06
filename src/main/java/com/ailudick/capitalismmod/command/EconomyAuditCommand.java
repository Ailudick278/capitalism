package com.ailudick.capitalismmod.command;

import com.ailudick.capitalismmod.economy.audit.EconomyAuditService;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/** Administrator-only, read-only economic consistency report. */
public final class EconomyAuditCommand {
    private EconomyAuditCommand() {}
    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("economy-audit").requires(s -> s.hasPermission(2)).executes(c -> audit(c.getSource())));
    }
    private static int audit(CommandSourceStack source) {
        var issues = EconomyAuditService.audit(source.getServer());
        source.sendSuccess(() -> Component.literal("economic audit issues=" + issues.size()), false);
        issues.stream().limit(20).forEach(issue -> source.sendFailure(Component.literal(issue)));
        return issues.isEmpty() ? 1 : 0;
    }
}
