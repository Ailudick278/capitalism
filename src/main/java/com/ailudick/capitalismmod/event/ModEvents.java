package com.ailudick.capitalismmod.event;

import com.ailudick.capitalismmod.CapitalismMod;
import com.ailudick.capitalismmod.command.BalanceCommand;
import com.ailudick.capitalismmod.command.CompanyCommand;
import com.ailudick.capitalismmod.command.ExchangeCommand;
import com.ailudick.capitalismmod.command.LoanCommand;
import com.ailudick.capitalismmod.command.RankingCommand;
import com.ailudick.capitalismmod.command.TransferSharesCommand;
import com.ailudick.capitalismmod.command.FxCommand;
import com.ailudick.capitalismmod.command.MoneyCommand;
import com.ailudick.capitalismmod.command.OfferCommand;
import com.ailudick.capitalismmod.command.PayCommand;
import com.ailudick.capitalismmod.command.EconomyLogCommand;
import com.ailudick.capitalismmod.command.MarketOrdersCommand;
import com.ailudick.capitalismmod.command.EconomyStatsCommand;
import com.ailudick.capitalismmod.command.MarketRepairCommand;
import com.ailudick.capitalismmod.command.MarketTradesCommand;
import com.ailudick.capitalismmod.command.LogisticsCommand;
import com.ailudick.capitalismmod.command.CapitalismCommand;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = CapitalismMod.MODID)
public class ModEvents {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        BalanceCommand.register(dispatcher);
        PayCommand.register(dispatcher);
        MoneyCommand.register(dispatcher);
        ExchangeCommand.register(dispatcher);
        FxCommand.register(dispatcher);
        CompanyCommand.register(dispatcher);
        RankingCommand.register(dispatcher);
        TransferSharesCommand.register(dispatcher);
        LoanCommand.register(dispatcher);
        OfferCommand.register(dispatcher);
        EconomyLogCommand.register(dispatcher);
        MarketOrdersCommand.register(dispatcher);
        EconomyStatsCommand.register(dispatcher);
        MarketRepairCommand.register(dispatcher);
        MarketTradesCommand.register(dispatcher);
        LogisticsCommand.register(dispatcher);
        CapitalismCommand.register(dispatcher);
    }
}
