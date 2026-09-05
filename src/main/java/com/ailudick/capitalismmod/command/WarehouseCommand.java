package com.ailudick.capitalismmod.command;

import com.ailudick.capitalismmod.market.InventoryOwner;
import com.ailudick.capitalismmod.market.WarehouseSavedData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** Administrative inspection and recovery commands for owner-scoped warehouses. */
public final class WarehouseCommand {
    private WarehouseCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = Commands.literal("warehouse").requires(source -> source.hasPermission(2));
        root.then(Commands.literal("audit").executes(ctx -> audit(ctx.getSource())));
        var transfer = Commands.literal("transfer");
        var from = Commands.argument("from", StringArgumentType.word());
        var to = Commands.argument("to", StringArgumentType.word());
        var item = Commands.argument("item", StringArgumentType.word());
        item.then(Commands.argument("count", IntegerArgumentType.integer(1)).executes(ctx -> transfer(ctx.getSource(),
                StringArgumentType.getString(ctx, "from"), StringArgumentType.getString(ctx, "to"),
                StringArgumentType.getString(ctx, "item"), IntegerArgumentType.getInteger(ctx, "count"))));
        to.then(item);
        from.then(to);
        transfer.then(from);
        root.then(transfer);
        dispatcher.register(root);
    }

    private static int audit(CommandSourceStack source) {
        var entries = WarehouseSavedData.get(source.getServer()).audit();
        source.sendSuccess(() -> Component.literal("仓库审计记录：" + entries.size()), false);
        entries.stream().skip(Math.max(0, entries.size() - 10)).forEach(entry ->
                source.sendSuccess(() -> Component.literal(entry.action() + " " + entry.from() + " -> "
                        + entry.to() + " " + entry.itemId() + " x" + entry.count()), false));
        return entries.size();
    }

    private static int transfer(CommandSourceStack source, String fromKey, String toKey, String itemId, int count) {
        InventoryOwner from = InventoryOwner.parse(fromKey);
        InventoryOwner to = InventoryOwner.parse(toKey);
        Item item;
        try { item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId)); }
        catch (IllegalArgumentException ignored) { item = Items.AIR; }
        if (from == null || to == null || item == null || item == Items.AIR
                || !WarehouseSavedData.get(source.getServer()).transfer(from, to, item, count)) {
            source.sendFailure(Component.literal("仓库主体、物品不存在或库存不足。"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("仓库转移完成。"), true);
        return 1;
    }
}
