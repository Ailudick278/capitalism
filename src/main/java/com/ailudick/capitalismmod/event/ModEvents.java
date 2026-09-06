package com.ailudick.capitalismmod.event;

import com.ailudick.capitalismmod.CapitalismMod;
import com.ailudick.capitalismmod.command.BalanceCommand;
import com.ailudick.capitalismmod.command.CompanyCommand;
import com.ailudick.capitalismmod.command.BusinessCommand;
import com.ailudick.capitalismmod.command.LandCommand;
import com.ailudick.capitalismmod.command.ExchangeCommand;
import com.ailudick.capitalismmod.command.LoanCommand;
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
import com.ailudick.capitalismmod.command.CalendarCommand;
import com.ailudick.capitalismmod.command.WarehouseCommand;
import com.ailudick.capitalismmod.command.TaxRefundCommand;
import com.ailudick.capitalismmod.command.TaxReportCommand;
import com.ailudick.capitalismmod.command.TaxRuleCommand;
import com.ailudick.capitalismmod.command.TaxCorrectionCommand;
import com.ailudick.capitalismmod.command.TaxTransactionCommand;
import com.ailudick.capitalismmod.command.TaxExpenseCommand;
import com.ailudick.capitalismmod.command.TaxInvoiceCommand;
import com.ailudick.capitalismmod.command.BankStatementCommand;
import com.ailudick.capitalismmod.command.LaborCommand;
import com.ailudick.capitalismmod.command.PopulationCommand;
import com.ailudick.capitalismmod.command.EconomyAuditCommand;
import com.ailudick.capitalismmod.command.CityCommand;
import com.ailudick.capitalismmod.command.GovernmentCommand;
import com.ailudick.capitalismmod.command.ContractDisputeCommand;
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
        BusinessCommand.register(dispatcher);
        LandCommand.register(dispatcher);
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
        CalendarCommand.register(dispatcher);
        WarehouseCommand.register(dispatcher);
        TaxRefundCommand.register(dispatcher);
        TaxReportCommand.register(dispatcher);
        TaxRuleCommand.register(dispatcher);
        TaxCorrectionCommand.register(dispatcher);
        TaxTransactionCommand.register(dispatcher);
        TaxExpenseCommand.register(dispatcher);
        TaxInvoiceCommand.register(dispatcher);
        BankStatementCommand.register(dispatcher);
        LaborCommand.register(dispatcher);
        PopulationCommand.register(dispatcher);
        EconomyAuditCommand.register(dispatcher);
        CityCommand.register(dispatcher);
        GovernmentCommand.register(dispatcher);
        ContractDisputeCommand.register(dispatcher);
    }
}
