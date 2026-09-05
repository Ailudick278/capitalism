package com.ailudick.capitalismmod.command;

import com.ailudick.capitalismmod.company.Company;
import com.ailudick.capitalismmod.company.AcquisitionSavedData;
import com.ailudick.capitalismmod.company.CompanyHelper;
import com.ailudick.capitalismmod.company.PublicTakeoverSavedData;
import com.ailudick.capitalismmod.company.CompanyTypes;
import com.ailudick.capitalismmod.company.CompanyLedgerSavedData;
import com.ailudick.capitalismmod.company.CompanyLaborSavedData;
import com.ailudick.capitalismmod.company.CompanyEquipmentSavedData;
import com.ailudick.capitalismmod.company.CompanyOperatingSnapshot;
import com.ailudick.capitalismmod.company.Industries;
import com.ailudick.capitalismmod.company.CompanyLifecycleService;
import com.ailudick.capitalismmod.company.CompanySiteSavedData;
import com.ailudick.capitalismmod.company.OilFieldSavedData;
import com.ailudick.capitalismmod.land.LandHelper;
import com.ailudick.capitalismmod.supply.SupplyMarket;
import com.ailudick.capitalismmod.loan.CompanyLoan;
import com.ailudick.capitalismmod.loan.CompanyLoanHelper;
import com.ailudick.capitalismmod.loan.CompanyLoanSavedData;
import com.ailudick.capitalismmod.loan.CompanyCreditSnapshot;
import com.ailudick.capitalismmod.economy.EconomySavedData;
import com.ailudick.capitalismmod.currency.Currencies;
import com.ailudick.capitalismmod.currency.Currency;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.MinecraftServer;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

import java.util.Map;

public class CompanyCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = Commands.literal("company");
        root.then(Commands.literal("list").executes(ctx -> list(ctx.getSource())));
        root.then(Commands.literal("site")
                .then(Commands.argument("name", StringArgumentType.word())
                        .executes(ctx -> registerSite(ctx.getSource(), StringArgumentType.getString(ctx, "name")))));
        root.then(Commands.literal("status")
                .then(Commands.argument("name", StringArgumentType.word())
                        .executes(ctx -> status(ctx.getSource(), StringArgumentType.getString(ctx, "name")))));
        root.then(Commands.literal("suspend")
                .then(Commands.argument("name", StringArgumentType.word())
                        .executes(ctx -> suspend(ctx.getSource(), StringArgumentType.getString(ctx, "name")))));
        root.then(Commands.literal("resume")
                .then(Commands.argument("name", StringArgumentType.word())
                        .executes(ctx -> resume(ctx.getSource(), StringArgumentType.getString(ctx, "name")))));
        root.then(Commands.literal("liquidate")
                .then(Commands.argument("name", StringArgumentType.word())
                        .executes(ctx -> liquidate(ctx.getSource(), StringArgumentType.getString(ctx, "name")))
                        .then(Commands.literal("settle")
                                .executes(ctx -> settleLiquidation(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "name"))))));
        root.then(Commands.literal("statement")
                .then(Commands.argument("name", StringArgumentType.word())
                        .executes(ctx -> statement(ctx.getSource(), StringArgumentType.getString(ctx, "name")))));
        root.then(Commands.literal("metrics")
                .then(Commands.argument("name", StringArgumentType.word())
                        .executes(ctx -> metrics(ctx.getSource(), StringArgumentType.getString(ctx, "name"), 90L))
                        .then(Commands.argument("days", IntegerArgumentType.integer(1, 360))
                                .executes(ctx -> metrics(ctx.getSource(), StringArgumentType.getString(ctx, "name"),
                                IntegerArgumentType.getInteger(ctx, "days"))))));
        root.then(Commands.literal("credit")
                .then(Commands.argument("name", StringArgumentType.word())
                        .executes(ctx -> credit(ctx.getSource(), StringArgumentType.getString(ctx, "name")))));
        root.then(Commands.literal("contribute")
                .then(Commands.argument("name", StringArgumentType.word())
                        .then(Commands.argument("amount", LongArgumentType.longArg(1))
                                .executes(ctx -> contribute(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "name"),
                                        LongArgumentType.getLong(ctx, "amount"))))));
        root.then(Commands.literal("dividend")
                .then(Commands.argument("name", StringArgumentType.word())
                        .then(Commands.argument("amountPerShare", LongArgumentType.longArg(1))
                                .executes(ctx -> dividend(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "name"),
                                        LongArgumentType.getLong(ctx, "amountPerShare"))))));
        root.then(Commands.literal("borrow")
                .then(Commands.argument("name", StringArgumentType.word())
                        .then(Commands.argument("amount", LongArgumentType.longArg(1))
                                .then(Commands.argument("days", IntegerArgumentType.integer(1, 3650))
                                        .then(Commands.argument("rate", DoubleArgumentType.doubleArg(0.0, 100.0))
                                                .executes(ctx -> borrow(ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "name"),
                                                        LongArgumentType.getLong(ctx, "amount"),
                                                        IntegerArgumentType.getInteger(ctx, "days"),
                                                        DoubleArgumentType.getDouble(ctx, "rate"))))))));
        root.then(Commands.literal("repayloan")
                .then(Commands.argument("name", StringArgumentType.word())
                        .then(Commands.argument("loanId", StringArgumentType.word())
                                .executes(ctx -> repayLoan(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "name"),
                                        StringArgumentType.getString(ctx, "loanId"), null))
                                .then(Commands.argument("amount", LongArgumentType.longArg(1))
                                        .executes(ctx -> repayLoan(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "name"),
                                                StringArgumentType.getString(ctx, "loanId"),
                                                LongArgumentType.getLong(ctx, "amount")))))));
        root.then(Commands.literal("companyloans")
                .then(Commands.argument("name", StringArgumentType.word())
                        .executes(ctx -> companyLoans(ctx.getSource(), StringArgumentType.getString(ctx, "name")))));
        root.then(Commands.literal("withdraw")
                .then(Commands.argument("name", StringArgumentType.word())
                        .then(Commands.argument("currency", StringArgumentType.word())
                                .then(Commands.argument("amount", LongArgumentType.longArg(1))
                                        .executes(ctx -> withdraw(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "name"),
                                                StringArgumentType.getString(ctx, "currency"),
                                        LongArgumentType.getLong(ctx, "amount")))))));
        root.then(Commands.literal("repayinstallment")
                .then(Commands.argument("name", StringArgumentType.word())
                        .then(Commands.argument("loanId", StringArgumentType.word())
                                .executes(ctx -> repayInstallment(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "name"),
                                        StringArgumentType.getString(ctx, "loanId"))))));
        root.then(Commands.literal("ledger")
                .then(Commands.argument("name", StringArgumentType.word())
                        .executes(ctx -> ledger(ctx.getSource(),
                                StringArgumentType.getString(ctx, "name")))));
        root.then(Commands.literal("hire")
                .then(Commands.argument("name", StringArgumentType.word())
                        .then(Commands.argument("role", StringArgumentType.word())
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 10000))
                                        .then(Commands.argument("dailyWage", LongArgumentType.longArg(1))
                                                .then(Commands.argument("skill", IntegerArgumentType.integer(0, 100))
                                                        .executes(ctx -> hire(ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "name"),
                                                                StringArgumentType.getString(ctx, "role"),
                                                                IntegerArgumentType.getInteger(ctx, "count"),
                                                                LongArgumentType.getLong(ctx, "dailyWage"),
                                                                IntegerArgumentType.getInteger(ctx, "skill")))))))));
        root.then(Commands.literal("fire")
                .then(Commands.argument("name", StringArgumentType.word())
                        .then(Commands.argument("contract", StringArgumentType.word())
                                .executes(ctx -> fire(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "name"),
                                        StringArgumentType.getString(ctx, "contract"))))));
        root.then(Commands.literal("machine")
                .then(Commands.literal("install")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .then(Commands.argument("type", StringArgumentType.word())
                                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 10000))
                                                .executes(ctx -> installMachine(ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "name"),
                                                        StringArgumentType.getString(ctx, "type"),
                                                        IntegerArgumentType.getInteger(ctx, "count"))))))));
        root.then(Commands.literal("machine")
                .then(Commands.literal("maintain")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .then(Commands.argument("type", StringArgumentType.word())
                                        .executes(ctx -> maintainMachine(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "name"),
                                                StringArgumentType.getString(ctx, "type")))))));
        root.then(Commands.literal("operations")
                .then(Commands.argument("name", StringArgumentType.word())
                        .executes(ctx -> operations(ctx.getSource(), StringArgumentType.getString(ctx, "name")))));
        root.then(Commands.literal("recipes")
                .then(Commands.argument("name", StringArgumentType.word())
                        .executes(ctx -> recipes(ctx.getSource(), StringArgumentType.getString(ctx, "name")))));
        root.then(Commands.literal("recipe")
                .then(Commands.argument("name", StringArgumentType.word())
                        .then(Commands.argument("recipe", StringArgumentType.word())
                                .executes(ctx -> selectRecipe(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "name"),
                                        StringArgumentType.getString(ctx, "recipe"))))));
        root.then(Commands.literal("acquire")
                .then(Commands.argument("seller", EntityArgument.player())
                        .then(Commands.argument("company", StringArgumentType.word())
                                .then(Commands.argument("price", LongArgumentType.longArg(1))
                                        .executes(ctx -> createOffer(ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "seller"),
                                                StringArgumentType.getString(ctx, "company"),
                                                LongArgumentType.getLong(ctx, "price")))))));
        root.then(Commands.literal("takeover")
                .then(Commands.literal("list").executes(ctx -> listOffers(ctx.getSource())))
                .then(Commands.literal("accept")
                        .then(Commands.argument("offer", StringArgumentType.word())
                                .executes(ctx -> acceptOffer(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "offer")))))
                .then(Commands.literal("reject")
                        .then(Commands.argument("offer", StringArgumentType.word())
                                .executes(ctx -> rejectOffer(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "offer")))))
                .then(Commands.literal("publicoffer")
                        .then(Commands.argument("stock", StringArgumentType.word())
                                .then(Commands.argument("shareholder", EntityArgument.player())
                                        .then(Commands.argument("price", LongArgumentType.longArg(1))
                                                .then(Commands.argument("quantity", IntegerArgumentType.integer(1))
                                                        .executes(ctx -> createPublicOffer(ctx.getSource(),
                                                                EntityArgument.getPlayer(ctx, "shareholder"),
                                                                StringArgumentType.getString(ctx, "stock"),
                                                                LongArgumentType.getLong(ctx, "price"),
                                                                IntegerArgumentType.getInteger(ctx, "quantity"))))))))
                .then(Commands.literal("publiclist").executes(ctx -> listPublicOffers(ctx.getSource())))
                .then(Commands.literal("publicaccept")
                        .then(Commands.argument("offer", StringArgumentType.word())
                                .executes(ctx -> acceptPublicOffer(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "offer")))))
                .then(Commands.literal("publicreject")
                        .then(Commands.argument("offer", StringArgumentType.word())
                                .executes(ctx -> rejectPublicOffer(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "offer"))))));
        root.then(Commands.literal("control")
                .then(Commands.argument("stock", StringArgumentType.word())
                        .executes(ctx -> showControl(ctx.getSource(),
                                StringArgumentType.getString(ctx, "stock")))));
        root.then(Commands.literal("merge")
                .then(Commands.argument("source", StringArgumentType.word())
                        .then(Commands.argument("target", StringArgumentType.word())
                                .executes(ctx -> merge(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "source"),
                                        StringArgumentType.getString(ctx, "target"))))));
        dispatcher.register(root);
    }

    private static int list(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Map<String, Company> companies = CompanyHelper.getCompanies(player);
        if (companies.isEmpty()) {
            source.sendFailure(Component.translatable("command.capitalismmod.no_companies"));
            return 0;
        }
        for (Company company : companies.values()) {
            player.sendSystemMessage(Component.translatable("command.capitalismmod.company_line",
                    company.registeredCapital(), company.name(),
                    Component.translatable(CompanyTypes.nameKey(company.type())),
                    company.treasuryOf("usd"), Component.translatable(Currencies.USD.nameKey())));
        }
        return companies.size();
    }

    private static int registerSite(CommandSourceStack source, String name) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Company company = CompanyHelper.getCompany(player, name);
        if (company == null || player.getServer() == null) {
            source.sendFailure(Component.literal("Company not found."));
            return 0;
        }
        ChunkPos chunk = new ChunkPos(player.blockPosition());
        String dimension = player.level().dimension().location().toString();
        if (!LandHelper.hasCommercialRight(player.getServer(), dimension, chunk.x, chunk.z, company.ownerUuid())) {
            source.sendFailure(Component.literal("Operating sites require owned or leased land that is not frozen or auctioned."));
            return 0;
        }
        CompanySiteSavedData.get(player.getServer()).set(new CompanySiteSavedData.Site(
                company.companyId(), dimension, chunk.x, chunk.z));
        OilFieldSavedData.Field field = OilFieldSavedData.get(player.getServer()).prospect(
                dimension, chunk.x, chunk.z);
        if (field == null) {
            source.sendSuccess(() -> Component.literal("Operating site registered at " + dimension
                    + " chunk " + chunk.x + ", " + chunk.z + "; no oil field detected."), false);
        } else {
            source.sendSuccess(() -> Component.literal("Operating site registered at " + dimension
                    + " chunk " + chunk.x + ", " + chunk.z + "; oil reserve remaining: "
                    + field.remainingReserve()), false);
        }
        return 1;
    }

    private static int contribute(CommandSourceStack source, String name, long amount) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!CompanyHelper.contributeCapital(player, name, amount)) {
            source.sendFailure(Component.literal("Capital contribution failed: company not found or funds are insufficient."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Contributed USD " + amount + " as paid-in capital."), false);
        return 1;
    }

    private static int status(CommandSourceStack source, String name) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Company company = CompanyHelper.getCompany(player, name);
        if (company == null) {
            source.sendFailure(Component.literal("Company not found."));
            return 0;
        }
        String state = CompanyLifecycleService.status(player.getServer(), company.companyId());
        var record = com.ailudick.capitalismmod.company.CompanyStatusSavedData.get(player.getServer()).record(company.companyId());
        source.sendSuccess(() -> Component.literal("Company status: " + state
                + (record == null || record.reason().isBlank() ? "" : " | " + record.reason())), false);
        return 1;
    }

    private static int suspend(CommandSourceStack source, String name) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Company company = CompanyHelper.getCompany(player, name);
        if (company == null || !CompanyLifecycleService.suspend(player, company, "suspended by owner")) {
            source.sendFailure(Component.literal("Company cannot be suspended."));
            return 0;
        }
        SupplyMarket.removeOffersForCompany(player.getServer(), player.getUUID(), company.name());
        source.sendSuccess(() -> Component.literal("Company suspended; production and new supply listings are paused."), false);
        return 1;
    }

    private static int resume(CommandSourceStack source, String name) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Company company = CompanyHelper.getCompany(player, name);
        if (company == null || !CompanyLifecycleService.resume(player, company)) {
            source.sendFailure(Component.literal("Company cannot be resumed."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Company resumed."), false);
        return 1;
    }

    private static int liquidate(CommandSourceStack source, String name) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Company company = CompanyHelper.getCompany(player, name);
        if (company == null || !CompanyLifecycleService.beginLiquidation(player, company)) {
            source.sendFailure(Component.literal("Company cannot enter liquidation (it may be listed or already closed)."));
            return 0;
        }
        SupplyMarket.removeOffersForCompany(player.getServer(), player.getUUID(), company.name());
        source.sendSuccess(() -> Component.literal("Liquidation opened; operations and new listings are paused."), false);
        return 1;
    }

    private static int settleLiquidation(CommandSourceStack source, String name) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Company company = CompanyHelper.getCompany(player, name);
        if (company == null) {
            source.sendFailure(Component.literal("Company not found."));
            return 0;
        }
        if (!CompanyLifecycleService.settleLiquidation(player, company)) {
            source.sendFailure(Component.literal("Liquidation remains incomplete; unresolved tax or company-loan liabilities may remain."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Liquidation completed; remaining equity was returned and the company was dissolved."), false);
        return 1;
    }

    private static int statement(CommandSourceStack source, String name) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Company company = CompanyHelper.getCompany(player, name);
        if (company == null) {
            source.sendFailure(Component.literal("Company not found."));
            return 0;
        }
        var statement = com.ailudick.capitalismmod.company.CompanyFinancialSnapshot.from(player.getServer(), company);
        source.sendSuccess(() -> Component.literal("Assets: USD " + statement.assets()
                + " (cash " + statement.cash() + ", inventory " + statement.inventory()
                + ", equipment " + statement.equipment() + ")"), false);
        source.sendSuccess(() -> Component.literal("Liabilities: USD " + statement.liabilities()
                + " (tax " + statement.taxLiabilities() + ", loans " + statement.loanLiabilities()
                + ", payroll " + statement.payrollLiabilities()
                + "), equity: USD " + statement.equity()), false);
        return 1;
    }

    private static int metrics(CommandSourceStack source, String name, long days) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Company company = CompanyHelper.getCompany(player, name);
        if (company == null) {
            source.sendFailure(Component.literal("Company not found."));
            return 0;
        }
        CompanyOperatingSnapshot metrics = CompanyOperatingSnapshot.from(player.getServer(), company, days);
        source.sendSuccess(() -> Component.literal("Operating metrics (last " + metrics.lookbackDays()
                + " days): revenue USD " + metrics.revenue() + ", expenses USD "
                + metrics.operatingExpenses() + ", cost of sales USD " + metrics.costOfSales()
                + ", gross profit USD " + metrics.grossProfit() + ", operating profit USD "
                + metrics.operatingProfit() + ", operating cash flow USD "
                + metrics.operatingCashFlow()), false);
        source.sendSuccess(() -> Component.literal("Statistical size profile: " + metrics.sizeProfile().label()
                + " (annualized turnover proxy USD " + metrics.annualizedRevenue() + "; report only, no upgrade effect)"), false);
        source.sendSuccess(() -> Component.literal("Workforce " + metrics.activeWorkers()
                + ", gross daily wages USD " + metrics.grossDailyWages()
                + ", employer contributions USD " + metrics.employerDailyContributions()
                + ", total labor cost USD " + metrics.dailyLaborCost() + ", machines "
                + metrics.machineUnits() + ", parallel capacity " + metrics.parallelCapacity()), false);
        source.sendSuccess(() -> Component.literal("Production: successful " + metrics.successfulBatches()
                + ", failed cycles " + metrics.failedCycles() + ", assets USD " + metrics.assets()
                + ", equity USD " + metrics.equity()), false);
        return 1;
    }

    private static int credit(CommandSourceStack source, String name) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Company company = CompanyHelper.getCompany(player, name);
        if (company == null) {
            source.sendFailure(Component.literal("Company not found."));
            return 0;
        }
        CompanyCreditSnapshot credit = CompanyCreditSnapshot.from(player.getServer(), company, 90L);
        source.sendSuccess(() -> Component.literal("Credit report (last " + credit.lookbackDays()
                + " days): operating cash flow USD " + credit.operatingCashFlow()
                + ", existing debt USD " + credit.existingDebt()), false);
        source.sendSuccess(() -> Component.literal("Debt limits: capital USD " + credit.capitalDebtLimit()
                + ", cash-flow USD " + credit.cashFlowDebtLimit()
                + ", remaining capacity USD " + credit.remainingDebtCapacity()), false);
        String coverage = Double.isInfinite(credit.coverageRatio()) ? "unlimited" : String.format("%.2f", credit.coverageRatio());
        source.sendSuccess(() -> Component.literal("Annual debt service USD "
                + Math.round(credit.annualDebtService()) + ", coverage ratio " + coverage
                + ", operating history " + credit.hasOperatingHistory()
                + ", overdue loan " + credit.hasOverdueLoan()), false);
        return 1;
    }

    private static int borrow(CommandSourceStack source, String name, long amount, int days, double rate)
            throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        String id = CompanyLoanHelper.borrow(player, name, amount, days, rate);
        if (id == null) {
            source.sendFailure(Component.literal("Company loan denied: capital limit, company funds, or terms are invalid."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Company loan approved: " + id.substring(0, 8)
                + " for USD " + amount + " at " + rate + "% annual interest."), false);
        return 1;
    }

    private static int repayLoan(CommandSourceStack source, String name, String loanId, Long amount)
            throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!CompanyLoanHelper.repay(player, name, loanId, amount)) {
            source.sendFailure(Component.literal("Company loan repayment failed: insufficient company cash or invalid payment."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Company loan repayment completed."), false);
        return 1;
    }

    private static int companyLoans(CommandSourceStack source, String name) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Company company = CompanyHelper.getCompany(player, name);
        if (company == null) {
            source.sendFailure(Component.literal("Company not found."));
            return 0;
        }
        int count = 0;
        for (CompanyLoan loan : CompanyLoanSavedData.get(player.getServer()).forCompany(company.companyId())) {
            source.sendSuccess(() -> Component.literal(loan.id().substring(0, Math.min(8, loan.id().length()))
                    + " principal USD " + loan.principal() + " interest due USD " + loan.interestDue()
                    + " installment USD " + loan.scheduledPayment() + " days " + loan.daysRemaining()), false);
            count++;
        }
        if (count == 0) source.sendSuccess(() -> Component.literal("No company loans."), false);
        return count;
    }

    private static int repayInstallment(CommandSourceStack source, String name, String loanId)
            throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!CompanyLoanHelper.repayScheduled(player, name, loanId)) {
            source.sendFailure(Component.literal("Scheduled company-loan payment failed: insufficient cash or invalid loan."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Scheduled company-loan payment completed."), false);
        return 1;
    }

    private static int dividend(CommandSourceStack source, String name, long amountPerShare)
            throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!CompanyHelper.declareDividend(player, name, amountPerShare)) {
            source.sendFailure(Component.literal("Dividend failed: the company must be listed and have enough cash."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Dividend declared: USD " + amountPerShare + " per share."), false);
        return 1;
    }

    private static int withdraw(CommandSourceStack source, String name, String currencyId, long amount) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!Currencies.exists(currencyId)) {
            source.sendFailure(Component.translatable("command.capitalismmod.unknown_currency"));
            return 0;
        }
        if (!CompanyHelper.withdraw(player, name, currencyId, amount)) {
            source.sendFailure(Component.translatable("command.capitalismmod.insufficient"));
            return 0;
        }
        Currency currency = Currencies.byId(currencyId);
        player.sendSystemMessage(Component.translatable("command.capitalismmod.company_withdrawn",
                name, amount, Component.translatable(currency.nameKey())));
        return 1;
    }

    private static int ledger(CommandSourceStack source, String name) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Company company = CompanyHelper.getCompany(player, name);
        if (company == null) {
            source.sendFailure(Component.translatable("command.capitalismmod.company_not_found", name));
            return 0;
        }
        var entries = CompanyLedgerSavedData.get(player.getServer()).entries(company.companyId());
        for (var entry : entries) {
            source.sendSuccess(() -> Component.literal(entry.type() + " | " + entry.amount()
                    + " " + entry.currencyId() + " | 余额 " + entry.balanceAfter()
                    + " | " + entry.description()), false);
        }
        if (entries.isEmpty()) {
            source.sendSuccess(() -> Component.literal("该企业暂无账务记录。"), false);
        }
        return entries.size();
    }

    private static int hire(CommandSourceStack source, String name, String role, int count,
                            long dailyWage, int skill) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!CompanyHelper.hire(player, name, role, count, dailyWage, skill)) {
            source.sendFailure(Component.literal("Unable to hire: invalid company or workforce values."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Hired " + count + " " + role + " worker(s)."), false);
        return 1;
    }

    private static int fire(CommandSourceStack source, String name, String contract) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!CompanyHelper.fire(player, name, contract)) {
            source.sendFailure(Component.literal("Worker contract not found."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Worker contract terminated."), false);
        return 1;
    }

    private static int installMachine(CommandSourceStack source, String name, String type, int count)
            throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!CompanyHelper.installMachine(player, name, type, count)) {
            source.sendFailure(Component.literal("Unable to install machine: invalid type or insufficient company funds."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Installed " + count + " " + type + "."), false);
        return 1;
    }

    private static int maintainMachine(CommandSourceStack source, String name, String type)
            throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!CompanyHelper.maintainMachine(player, name, type)) {
            source.sendFailure(Component.literal("Maintenance failed: machine is missing, already healthy, or funds are insufficient."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Machine maintenance completed: " + type), false);
        return 1;
    }

    private static int operations(CommandSourceStack source, String name) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Company company = CompanyHelper.getCompany(player, name);
        if (company == null) {
            source.sendFailure(Component.literal("Company not found."));
            return 0;
        }
        MinecraftServer server = player.getServer();
        var labor = CompanyLaborSavedData.get(server);
        source.sendSuccess(() -> Component.literal("Workers: " + labor.activeWorkers(company.companyId())
                + ", daily wages: USD " + labor.dailyWages(company.companyId())
                + ", average skill: " + labor.averageSkill(company.companyId())
                + ", parallel batches: " + CompanyHelper.parallelCapacity(server, company)), false);
        for (var contract : labor.contracts(company.companyId())) {
            source.sendSuccess(() -> Component.literal("Contract " + contract.id() + " | " + contract.role()
                    + " x" + contract.count() + " | wage " + contract.dailyWage()
                    + " | skill " + contract.skill()), false);
        }
        for (var entry : CompanyEquipmentSavedData.get(server).all(company.companyId()).values()) {
            source.sendSuccess(() -> Component.literal("Machine " + entry.machineType() + " x" + entry.count()
                    + " | condition " + entry.condition()), false);
        }
        return 1;
    }

    private static int recipes(CommandSourceStack source, String name) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Company company = CompanyHelper.getCompany(player, name);
        var spec = company == null ? null : Industries.byId(company.type());
        if (spec == null) {
            source.sendFailure(Component.literal("Company or industry recipes not found."));
            return 0;
        }
        for (var recipe : spec.recipes()) {
            source.sendSuccess(() -> Component.literal(recipe.id() + " | inputs " + recipe.inputs()
                    + " -> outputs " + recipe.outputs() + " | machine " + recipe.machineType()
                    + (recipe.id().equals(company.productionRecipe()) ? " [active]" : "")), false);
        }
        return spec.recipes().size();
    }

    private static int selectRecipe(CommandSourceStack source, String name, String recipe) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!CompanyHelper.selectRecipe(player, name, recipe)) {
            source.sendFailure(Component.literal("Recipe not found for this company."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Active production recipe: " + recipe), false);
        return 1;
    }

    private static int createOffer(CommandSourceStack source, ServerPlayer seller, String company, long price)
            throws CommandSyntaxException {
        ServerPlayer buyer = source.getPlayerOrException();
        if (seller.getUUID().equals(buyer.getUUID()) || CompanyHelper.getCompany(seller, company) == null
                || CompanyHelper.isListed(seller, company)) {
            source.sendFailure(Component.literal("Only an online, unlisted company can receive an acquisition offer."));
            return 0;
        }
        AcquisitionSavedData data = AcquisitionSavedData.get(source.getServer());
        AcquisitionSavedData.Offer offer = new AcquisitionSavedData.Offer(
                java.util.UUID.randomUUID().toString().substring(0, 8), buyer.getUUID(), seller.getUUID(), company,
                price, source.getServer().overworld().getGameTime());
        data.add(offer);
        buyer.sendSystemMessage(Component.literal("Acquisition offer sent: " + offer.id()));
        seller.sendSystemMessage(Component.literal("You received an acquisition offer " + offer.id()
                + " for " + company + " at USD " + price + ". Use /company takeover accept " + offer.id()));
        return 1;
    }

    private static int listOffers(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        int count = 0;
        for (AcquisitionSavedData.Offer offer : AcquisitionSavedData.get(source.getServer()).offers()) {
            if (offer.sellerUuid().equals(player.getUUID()) || offer.buyerUuid().equals(player.getUUID())) {
                source.sendSuccess(() -> Component.literal("Offer " + offer.id() + " | " + offer.companyName()
                        + " | USD " + offer.price() + " | "
                        + (offer.sellerUuid().equals(player.getUUID()) ? "incoming" : "outgoing")), false);
                count++;
            }
        }
        if (count == 0) {
            source.sendSuccess(() -> Component.literal("No acquisition offers."), false);
        }
        return count;
    }

    private static int acceptOffer(CommandSourceStack source, String id) throws CommandSyntaxException {
        ServerPlayer seller = source.getPlayerOrException();
        AcquisitionSavedData data = AcquisitionSavedData.get(source.getServer());
        AcquisitionSavedData.Offer offer = data.find(id);
        ServerPlayer buyer = offer == null ? null : source.getServer().getPlayerList().getPlayer(offer.buyerUuid());
        if (offer == null || !offer.sellerUuid().equals(seller.getUUID()) || buyer == null
                || !CompanyHelper.acquire(seller, buyer, offer)) {
            source.sendFailure(Component.literal("Offer invalid, buyer offline, company changed, or funds are insufficient."));
            return 0;
        }
        data.remove(id);
        seller.sendSystemMessage(Component.literal("Acquisition completed."));
        buyer.sendSystemMessage(Component.literal("You acquired " + offer.companyName() + "."));
        return 1;
    }

    private static int rejectOffer(CommandSourceStack source, String id) throws CommandSyntaxException {
        ServerPlayer seller = source.getPlayerOrException();
        AcquisitionSavedData data = AcquisitionSavedData.get(source.getServer());
        AcquisitionSavedData.Offer offer = data.find(id);
        if (offer == null || !offer.sellerUuid().equals(seller.getUUID())) {
            source.sendFailure(Component.literal("Offer not found or you are not the seller."));
            return 0;
        }
        data.remove(id);
        source.sendSuccess(() -> Component.literal("Acquisition offer rejected."), false);
        return 1;
    }

    private static int merge(CommandSourceStack source, String sourceName, String targetName)
            throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!CompanyHelper.merge(player, sourceName, targetName)) {
            source.sendFailure(Component.literal("Companies must be yours, unlisted, and use the same industry."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Company " + sourceName + " merged into " + targetName + "."), false);
        return 1;
    }

    private static int createPublicOffer(CommandSourceStack source, ServerPlayer seller, String stockId,
                                         long price, int quantity) throws CommandSyntaxException {
        ServerPlayer buyer = source.getPlayerOrException();
        EconomySavedData economy = EconomySavedData.get(source.getServer());
        if (seller.getUUID().equals(buyer.getUUID()) || price <= 0 || quantity <= 0 || !economy.isListed(stockId)
                || economy.holdings(stockId, seller.getUUID()) < quantity) {
            source.sendFailure(Component.literal("The stock is not listed or the shareholder lacks enough shares."));
            return 0;
        }
        PublicTakeoverSavedData.Offer offer = new PublicTakeoverSavedData.Offer(
                java.util.UUID.randomUUID().toString().substring(0, 8), buyer.getUUID(), seller.getUUID(), stockId,
                price, quantity, source.getServer().overworld().getGameTime());
        PublicTakeoverSavedData.get(source.getServer()).add(offer);
        seller.sendSystemMessage(Component.literal("Public takeover offer " + offer.id() + " received for "
                + quantity + " shares at USD " + price + " each."));
        buyer.sendSystemMessage(Component.literal("Public takeover offer sent: " + offer.id()));
        return 1;
    }

    private static int listPublicOffers(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        long now = source.getServer().overworld().getGameTime();
        int count = 0;
        for (PublicTakeoverSavedData.Offer offer : PublicTakeoverSavedData.get(source.getServer()).offers()) {
            if (offer.buyerUuid().equals(player.getUUID()) || offer.sellerUuid().equals(player.getUUID())) {
                long left = Math.max(0L, PublicTakeoverSavedData.OFFER_TTL - (now - offer.createdTick()));
                source.sendSuccess(() -> Component.literal("Public offer " + offer.id() + " | " + offer.stockId()
                        + " | " + offer.quantity() + " shares @ USD " + offer.pricePerShare()
                        + " | " + left + " ticks left"), false);
                count++;
            }
        }
        if (count == 0) source.sendSuccess(() -> Component.literal("No public takeover offers."), false);
        return count;
    }

    private static int acceptPublicOffer(CommandSourceStack source, String id) throws CommandSyntaxException {
        ServerPlayer seller = source.getPlayerOrException();
        PublicTakeoverSavedData data = PublicTakeoverSavedData.get(source.getServer());
        PublicTakeoverSavedData.Offer offer = data.find(id);
        ServerPlayer buyer = offer == null ? null : source.getServer().getPlayerList().getPlayer(offer.buyerUuid());
        long now = source.getServer().overworld().getGameTime();
        if (offer == null || buyer == null || now - offer.createdTick() > PublicTakeoverSavedData.OFFER_TTL
                || !offer.sellerUuid().equals(seller.getUUID())
                || !CompanyHelper.acceptPublicOffer(seller, buyer, offer)) {
            source.sendFailure(Component.literal("Public offer expired, invalid, buyer offline, or funds are insufficient."));
            return 0;
        }
        data.remove(id);
        seller.sendSystemMessage(Component.literal("Public share sale completed."));
        buyer.sendSystemMessage(Component.literal("Public share purchase completed."));
        return 1;
    }

    private static int rejectPublicOffer(CommandSourceStack source, String id) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        PublicTakeoverSavedData data = PublicTakeoverSavedData.get(source.getServer());
        PublicTakeoverSavedData.Offer offer = data.find(id);
        if (offer == null || (!offer.sellerUuid().equals(player.getUUID()) && !offer.buyerUuid().equals(player.getUUID()))) {
            source.sendFailure(Component.literal("Public offer not found."));
            return 0;
        }
        data.remove(id);
        source.sendSuccess(() -> Component.literal("Public offer cancelled."), false);
        return 1;
    }

    private static int showControl(CommandSourceStack source, String stockId) {
        EconomySavedData data = EconomySavedData.get(source.getServer());
        if (!data.isListed(stockId)) {
            source.sendFailure(Component.literal("Listed company stock not found."));
            return 0;
        }
        java.util.UUID controller = CompanyHelper.controller(source.getServer(), stockId);
        String message = controller == null ? "No shareholder currently controls " + stockId
                : "Controlling shareholder of " + stockId + ": " + controller;
        source.sendSuccess(() -> Component.literal(message), false);
        return 1;
    }
}
