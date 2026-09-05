package com.ailudick.capitalismmod.network;

import com.ailudick.capitalismmod.Config;
import com.ailudick.capitalismmod.calendar.PerpetualCalendar;
import com.ailudick.capitalismmod.business.IndividualBusinessHelper;
import com.ailudick.capitalismmod.bank.BankAccount;
import com.ailudick.capitalismmod.bank.BankAccountHelper;
import com.ailudick.capitalismmod.currency.Currencies;
import com.ailudick.capitalismmod.currency.Currency;
import com.ailudick.capitalismmod.currency.ExchangeRates;
import com.ailudick.capitalismmod.currency.Money;
import com.ailudick.capitalismmod.init.ModItems;
import com.ailudick.capitalismmod.init.ModAttachments;
import com.ailudick.capitalismmod.item.BankCard;
import com.ailudick.capitalismmod.network.payload.BankTransactionPayload;
import com.ailudick.capitalismmod.network.payload.ExchangePayload;
import com.ailudick.capitalismmod.network.payload.LoanPayload;
import com.ailudick.capitalismmod.network.payload.OpenAccountPayload;
import com.ailudick.capitalismmod.network.payload.ReplaceCardPayload;
import com.ailudick.capitalismmod.network.payload.TransferPayload;
import com.ailudick.capitalismmod.network.payload.OpenTermDepositPayload;
import com.ailudick.capitalismmod.network.payload.WithdrawTermDepositPayload;
import com.ailudick.capitalismmod.network.payload.PayTaxPayload;
import com.ailudick.capitalismmod.network.payload.PayUnifiedTaxPayload;
import com.ailudick.capitalismmod.network.payload.DeclareTaxPayload;
import com.ailudick.capitalismmod.network.payload.RequestTaxRefundPayload;
import com.ailudick.capitalismmod.network.payload.ReviewTaxRefundPayload;
import com.ailudick.capitalismmod.network.payload.ManageTaxRefundNotificationsPayload;
import com.ailudick.capitalismmod.network.payload.SyncTaxBillsPayload;
import com.ailudick.capitalismmod.network.payload.SyncTaxCorrectionRequestsPayload;
import com.ailudick.capitalismmod.network.payload.ReviewTaxCorrectionRequestPayload;
import com.ailudick.capitalismmod.network.payload.WithdrawCompanyPayload;
import com.ailudick.capitalismmod.network.payload.UpgradeCompanyPayload;
import com.ailudick.capitalismmod.network.payload.SyncBankAccountsPayload;
import com.ailudick.capitalismmod.network.payload.SyncPersonalAssetsPayload;
import com.ailudick.capitalismmod.network.payload.OperationResultPayload;
import com.ailudick.capitalismmod.economy.PersonalAssets;
import com.ailudick.capitalismmod.company.Conglomerate;
import com.ailudick.capitalismmod.menu.ConglomerateMenu;
import com.ailudick.capitalismmod.network.payload.OpenConglomeratePayload;
import com.ailudick.capitalismmod.network.payload.OpenLandPayload;
import com.ailudick.capitalismmod.network.payload.ClaimLandPayload;
import com.ailudick.capitalismmod.network.payload.ReleaseLandPayload;
import com.ailudick.capitalismmod.network.payload.SetLandPurposePayload;
import com.ailudick.capitalismmod.network.payload.ManageLandTrustPayload;
import com.ailudick.capitalismmod.network.payload.SetLandPermissionsPayload;
import com.ailudick.capitalismmod.network.payload.SyncLandPermissionsPayload;
import com.ailudick.capitalismmod.network.payload.ClearLandLogsPayload;
import com.ailudick.capitalismmod.network.payload.SyncLandSalePayload;
import com.ailudick.capitalismmod.network.payload.SyncLandTrustsPayload;
import com.ailudick.capitalismmod.network.payload.LeaseLandPayload;
import com.ailudick.capitalismmod.network.payload.UnleaseLandPayload;
import com.ailudick.capitalismmod.network.payload.RequestLandDetailsPayload;
import com.ailudick.capitalismmod.network.payload.OpenLandAtChunkPayload;
import com.ailudick.capitalismmod.network.payload.OpenWorldMapPayload;
import com.ailudick.capitalismmod.network.payload.SyncWorldMapPayload;
import com.ailudick.capitalismmod.network.payload.RequestWorldMapTilesPayload;
import com.ailudick.capitalismmod.network.payload.SyncWorldMapTilesPayload;
import com.ailudick.capitalismmod.worldmap.WorldMapTileSavedData;
import com.ailudick.capitalismmod.network.payload.SyncLandPayload;
import com.ailudick.capitalismmod.network.payload.SyncOwnedLandsPayload;
import com.ailudick.capitalismmod.network.payload.SyncLandLogsPayload;
import com.ailudick.capitalismmod.network.payload.SyncLandOverlayPayload;
import com.ailudick.capitalismmod.network.payload.RenameConglomeratePayload;
import com.ailudick.capitalismmod.network.payload.SyncConglomeratePayload;
import com.ailudick.capitalismmod.wallet.EconomyHelper;
import com.ailudick.capitalismmod.tax.TaxBill;
import com.ailudick.capitalismmod.tax.TaxLedgerSavedData;
import com.ailudick.capitalismmod.tax.TaxService;
import com.ailudick.capitalismmod.tax.TaxSubject;
import com.ailudick.capitalismmod.tax.TaxType;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.ailudick.capitalismmod.company.Company;
import com.ailudick.capitalismmod.company.CompanyHelper;
import com.ailudick.capitalismmod.land.LandClaim;
import com.ailudick.capitalismmod.land.LandPermissionSavedData;
import com.ailudick.capitalismmod.land.LandSavedData;
import com.ailudick.capitalismmod.land.LandOperationLogSavedData;
import com.ailudick.capitalismmod.land.LandHelper;
import com.ailudick.capitalismmod.event.TradeCompletedEvent;
import com.ailudick.capitalismmod.item.BusinessLicense;
import com.ailudick.capitalismmod.network.payload.CreateCompanyPayload;
import com.ailudick.capitalismmod.auction.AuctionMarket;
import com.ailudick.capitalismmod.auction.AuctionSavedData;
import com.ailudick.capitalismmod.bond.BondHolding;
import com.ailudick.capitalismmod.bond.BondMarket;
import com.ailudick.capitalismmod.bond.BondSavedData;
import com.ailudick.capitalismmod.futures.FuturesMarket;
import com.ailudick.capitalismmod.futures.Position;
import com.ailudick.capitalismmod.supply.PurchaseOrder;
import com.ailudick.capitalismmod.supply.SupplyMarket;
import com.ailudick.capitalismmod.supply.SupplyMarketSavedData;
import com.ailudick.capitalismmod.market.Commodities;
import com.ailudick.capitalismmod.market.CommodityMarket;
import com.ailudick.capitalismmod.market.WarehouseSavedData;
import com.ailudick.capitalismmod.market.WarehouseAccess;
import com.ailudick.capitalismmod.market.InventoryOwner;
import com.ailudick.capitalismmod.network.payload.CancelOrderPayload;
import com.ailudick.capitalismmod.stock.StockMarket;
import com.ailudick.capitalismmod.network.payload.PlaceOrderPayload;
import com.ailudick.capitalismmod.network.payload.SyncCommodityPayload;
import com.ailudick.capitalismmod.network.payload.SyncMarketOrdersPayload;
import com.ailudick.capitalismmod.network.payload.SyncStocksPayload;
import com.ailudick.capitalismmod.network.payload.PlaceStockOrderPayload;
import com.ailudick.capitalismmod.network.payload.CancelStockOrderPayload;
import com.ailudick.capitalismmod.network.payload.SyncStockOrdersPayload;
import com.ailudick.capitalismmod.network.payload.BidPayload;
import com.ailudick.capitalismmod.network.payload.BuyBondPayload;
import com.ailudick.capitalismmod.network.payload.CloseFuturesPositionPayload;
import com.ailudick.capitalismmod.network.payload.ListAuctionPayload;
import com.ailudick.capitalismmod.network.payload.PlaceSupplyOrderPayload;
import com.ailudick.capitalismmod.network.payload.RedeemBondPayload;
import com.ailudick.capitalismmod.network.payload.SyncSupplyMarketPayload;
import com.ailudick.capitalismmod.network.payload.SyncAuctionsPayload;
import com.ailudick.capitalismmod.network.payload.SyncBondsPayload;
import com.ailudick.capitalismmod.network.payload.DepositMarginPayload;
import com.ailudick.capitalismmod.network.payload.OpenFuturesPositionPayload;
import com.ailudick.capitalismmod.network.payload.SyncFuturesPayload;
import com.ailudick.capitalismmod.network.payload.WithdrawMarginPayload;
import com.ailudick.capitalismmod.network.payload.IpoCompanyPayload;
import com.ailudick.capitalismmod.network.payload.SyncSecuritiesPayload;
import com.ailudick.capitalismmod.network.payload.SyncWarehousePayload;
import com.ailudick.capitalismmod.network.payload.WarehouseDepositPayload;
import com.ailudick.capitalismmod.network.payload.WarehouseWithdrawPayload;
import com.ailudick.capitalismmod.network.payload.SelectWarehouseOwnerPayload;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerPayloadHandler {

    public static void handleExchange(ExchangePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!Currencies.exists(payload.from()) || !Currencies.exists(payload.to())) {
                return;
            }

            Currency from = Currencies.byId(payload.from());
            Currency to = Currencies.byId(payload.to());
            long amountMinor = Money.toMinor(payload.amount());
            if (from.equals(to) || amountMinor <= 0) {
                return;
            }

            boolean buyingForeign = from.equals(baseCurrency());
            long sourceAmount = buyingForeign ? ExchangeRates.convert(amountMinor, to, from) : amountMinor;
            long converted = buyingForeign ? amountMinor : ExchangeRates.convert(amountMinor, from, to);
            if (sourceAmount <= 0 || converted <= 0 || !BankAccountHelper.exchange(player, payload.accountId(), from, to, sourceAmount, converted)) {
                operationResult(player, false, Component.translatable("command.capitalismmod.amount_too_small"));
                return;
            }
            syncBankState(player);
            operationResult(player, true, Component.translatable("gui.capitalismmod.exchange"));
        });
    }

    public static void handleOpenAccount(OpenAccountPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!BankAccountHelper.canOpenAccount(player, payload.credit())) {
                operationResult(player, false, Component.translatable(payload.credit()
                        ? "message.capitalismmod.credit_account_limit" : "message.capitalismmod.debit_account_limit"));
                return;
            }
            BankAccount account = BankAccountHelper.openAccount(player, payload.credit());
            giveCard(player, account.id(), payload.credit());
            syncBankState(player);
            operationResult(player, true, Component.translatable("gui.capitalismmod.open_account"));
        });
    }

    public static void handleBankTransaction(BankTransactionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!Currencies.exists(payload.currencyId())) {
                return;
            }
            Currency currency = Currencies.byId(payload.currencyId());

            boolean success = BankAccountHelper.transfer(player, payload.accountId(), currency, Money.toMinor(payload.amount()), payload.deposit());
            if (!success) {
                operationResult(player, false, Component.translatable("command.capitalismmod.insufficient"));
            }
            syncBankState(player);
            if (success) operationResult(player, true, Component.translatable(payload.deposit() ? "gui.capitalismmod.deposit" : "gui.capitalismmod.withdraw"));
        });
    }

    public static void handleReplaceCard(ReplaceCardPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            BankAccount account = BankAccountHelper.getAccount(player, payload.accountId());
            if (account == null) {
                operationResult(player, false, Component.translatable("gui.capitalismmod.no_account"));
                return;
            }
            if (hasMatchingCard(player, payload.accountId(), account.credit())) {
                operationResult(player, false, Component.literal("物品栏中已有对应银行卡，无需补办"));
                return;
            }
            long now = player.getServer().overworld().getGameTime();
            long lastReplacement = player.getData(ModAttachments.LAST_CARD_REPLACEMENT_TICK);
            if (lastReplacement != Long.MIN_VALUE && now - lastReplacement < 24000L) {
                operationResult(player, false, Component.literal("请等待一个 Minecraft 日后再补办银行卡。"));
                return;
            }
            giveCard(player, payload.accountId(), account.credit());
            player.setData(ModAttachments.LAST_CARD_REPLACEMENT_TICK, now);
            operationResult(player, true, Component.translatable("gui.capitalismmod.report_loss"));
        });
    }

    private static boolean hasMatchingCard(ServerPlayer player, String accountId, boolean credit) {
        Item expected = credit ? ModItems.CREDIT_CARD.get() : ModItems.DEBIT_CARD.get();
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() == expected && BankCard.isBound(stack)
                    && accountId.equals(BankCard.getAccountId(stack))) {
                return true;
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.getItem() == expected && BankCard.isBound(stack)
                    && accountId.equals(BankCard.getAccountId(stack))) {
                return true;
            }
        }
        return false;
    }

    public static void handleLoan(LoanPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!Currencies.exists(payload.currencyId())) {
                return;
            }
            Currency currency = Currencies.byId(payload.currencyId());

            boolean success = payload.repay()
                    ? BankAccountHelper.repay(player, payload.accountId(), currency, Money.toMinor(payload.amount()))
                    : BankAccountHelper.loan(player, payload.accountId(), currency, Money.toMinor(payload.amount()));
            if (!success) {
                operationResult(player, false, Component.translatable("command.capitalismmod.insufficient"));
            }
            syncBankState(player);
            if (success) operationResult(player, true, Component.translatable(payload.repay() ? "gui.capitalismmod.repay" : "gui.capitalismmod.loan"));
        });
    }

    public static void handleTransfer(TransferPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!Currencies.exists(payload.currencyId())) {
                return;
            }
            boolean success = BankAccountHelper.transferBetween(player, payload.fromAccountId(), payload.targetAccountId(), payload.currencyId(), Money.toMinor(payload.amount()));
            if (!success) {
                operationResult(player, false, Component.translatable("message.capitalismmod.transfer_failed"));
            }
            syncBankState(player);
            if (success) operationResult(player, true, Component.translatable("gui.capitalismmod.transfer"));
        });
    }

    public static void handleOpenTermDeposit(OpenTermDepositPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            boolean success = BankAccountHelper.openTermDeposit(player, payload.accountId(), payload.currencyId(), Money.toMinor(payload.amount()), payload.termDays());
            if (!success) {
                operationResult(player, false, Component.translatable("command.capitalismmod.insufficient"));
            }
            syncBankState(player);
            if (success) operationResult(player, true, Component.translatable("gui.capitalismmod.open_term"));
        });
    }

    public static void handleWithdrawTermDeposit(WithdrawTermDepositPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            BankAccountHelper.withdrawTermDeposit(player, payload.accountId(), payload.index());
            syncBankState(player);
        });
    }

    public static void handlePayTax(PayTaxPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!CompanyHelper.payTax(player, payload.companyName())) {
                player.displayClientMessage(Component.translatable("command.capitalismmod.insufficient"), true);
                return;
            }
            Conglomerate conglomerate = CompanyHelper.getConglomerate(player);
            PacketDistributor.sendToPlayer(player, new SyncConglomeratePayload(conglomerate.name(), CompanyHelper.getCompanies(player)));
        });
    }

    public static void handlePayUnifiedTax(PayUnifiedTaxPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            TaxLedgerSavedData ledger = TaxLedgerSavedData.get(player.getServer());
            TaxBill bill = ledger.get(payload.billId());
            if (bill == null || !bill.subject().taxpayerUuid().equals(player.getUUID())
                    || !TaxService.pay(player, bill.id(), bill.outstanding())) {
                player.displayClientMessage(Component.literal("税单不存在、无权缴税或余额不足"), true);
                sendTaxBills(player);
                return;
            }
            player.displayClientMessage(Component.literal("税款已缴清：" + bill.subject().type().displayName()), true);
            syncLegacyTaxMirror(player, bill);
            sendTaxBills(player);
        });
    }

    public static void handleDeclareTax(DeclareTaxPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!TaxService.declare(player, payload.billId())) {
                player.displayClientMessage(Component.literal("Tax bill cannot be declared"), true);
            }
            sendTaxBills(player);
        });
    }

    public static void handleReviewTaxCorrectionRequest(ReviewTaxCorrectionRequestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || !player.hasPermissions(2)) return;
            com.ailudick.capitalismmod.command.TaxCorrectionCommand.reviewRequest(
                    player.createCommandSourceStack(), payload.id(), payload.approve(), payload.reason());
            sendTaxBills(player);
        });
    }

    public static void handleRequestTaxRefund(RequestTaxRefundPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            var request = com.ailudick.capitalismmod.tax.TaxRefundService.request(player, payload.currencyId(), payload.amount());
              String message = request == null ? "Refund request could not be recorded"
                      : ("APPROVED".equals(request.status()) ? "Refund approved automatically: " + request.reason()
                      : "REJECTED".equals(request.status()) ? "Refund rejected: " + request.reason()
                      : "Refund request submitted for manual review: " + request.reason());
              player.displayClientMessage(Component.literal(message), true);
            sendTaxBills(player);
        });
    }

    private static void syncLegacyTaxMirror(ServerPlayer player, TaxBill bill) {
        long remaining = TaxService.outstanding(player.getServer(), bill.subject());
        switch (bill.subject().type()) {
            case CORPORATE_INCOME -> com.ailudick.capitalismmod.company.CompanyHelper
                    .syncTaxMirror(player, bill.subject().subjectId(), remaining);
            case INDIVIDUAL_BUSINESS_INCOME -> IndividualBusinessHelper.syncTaxMirror(player, remaining);
            case LAND -> {
                var claim = com.ailudick.capitalismmod.land.LandSavedData.get(player.getServer())
                        .get(bill.subject().subjectId());
                if (claim != null) {
                    com.ailudick.capitalismmod.land.LandSavedData.get(player.getServer()).put(
                            claim.withTaxSchedule(remaining, claim.taxDueAt(), claim.taxGraceUntil()));
      }
    }

            default -> { }
        }
    }

    public static void handleReviewTaxRefund(ReviewTaxRefundPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || !player.hasPermissions(2)) return;
            boolean result = payload.approve()
                    ? com.ailudick.capitalismmod.tax.TaxRefundService.approve(player.getServer(), payload.requestId(), player.getGameProfile().getName())
                    : com.ailudick.capitalismmod.tax.TaxRefundService.reject(player.getServer(), payload.requestId(), player.getGameProfile().getName(),
                    payload.reason().isBlank() ? "Rejected by administrator" : payload.reason());
            player.displayClientMessage(Component.literal(result ? "Tax refund review completed" : "Tax refund review failed"), true);
            sendTaxBills(player);
        });
    }

    public static void handleManageTaxRefundNotifications(ManageTaxRefundNotificationsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            var data = com.ailudick.capitalismmod.tax.TaxRefundNotificationSavedData.get(player.getServer());
            if ("read".equals(payload.action())) data.markRead(player.getUUID());
            else if ("clear_read".equals(payload.action())) data.clearReadFor(player.getUUID());
            else if ("read_one".equals(payload.action())) data.markReadOne(player.getUUID(), payload.requestId());
            else if ("delete_one".equals(payload.action())) data.deleteOne(player.getUUID(), payload.requestId());
            sendTaxBills(player);
        });
    }

    public static void sendTaxBills(ServerPlayer player) {
        long now = player.getServer().overworld().getGameTime();
        var ledger = TaxLedgerSavedData.get(player.getServer());
        for (var company : com.ailudick.capitalismmod.company.CompanyHelper.getCompanies(player).values()) {
            if (company.taxOwed() > 0L) {
                TaxService.ensureOutstanding(player.getServer(),
                        new TaxSubject(TaxType.CORPORATE_INCOME, company.companyId(), company.ownerUuid()),
                        Config.defaultCurrencyId(), Money.toMinor(company.taxOwed()), now, 0L, 0L);
            }
        }
        var business = IndividualBusinessHelper.get(player);
        if (business != null && business.taxOwed() > 0L) {
            TaxService.ensureOutstanding(player.getServer(),
                    new TaxSubject(TaxType.INDIVIDUAL_BUSINESS_INCOME, business.businessId(), business.ownerUuid()),
                    Config.defaultCurrencyId(), Money.toMinor(business.taxOwed()), now, 0L, 0L);
        }
        for (var claim : com.ailudick.capitalismmod.land.LandSavedData.get(player.getServer()).claims().values()) {
            if (claim.ownerUuid().equals(player.getUUID()) && claim.taxOwed() > 0L) {
                TaxService.ensureOutstanding(player.getServer(),
                        new TaxSubject(TaxType.LAND, claim.id(), claim.ownerUuid()), Config.defaultCurrencyId(), Money.toMinorSaturated(claim.taxOwed()),
                        now, claim.taxDueAt(), claim.taxGraceUntil());
            }
        }
        for (var bill : ledger.outstandingFor(player.getUUID())) {
            TaxBill updated = TaxService.updateLateFee(player.getServer(), bill, now);
            TaxService.processEnforcement(player.getServer(), updated, now);
        }
        var visibleBills = ledger.allFor(player.getUUID());
        PacketDistributor.sendToPlayer(player, new SyncTaxBillsPayload(visibleBills,
                ledger.paymentsFor(player.getUUID()),
                com.ailudick.capitalismmod.tax.TaxCreditSavedData.get(player.getServer()).totalFor(player.getUUID()),
                com.ailudick.capitalismmod.tax.TaxRefundSavedData.get(player.getServer()).all().stream()
                        .filter(refund -> player.hasPermissions(2) || refund.taxpayerUuid().equals(player.getUUID())).toList(),
                com.ailudick.capitalismmod.tax.TaxRefundAuditSavedData.get(player.getServer()).all().stream()
                        .filter(event -> player.hasPermissions(2) || event.taxpayerUuid().equals(player.getUUID())).toList(),
                com.ailudick.capitalismmod.tax.TaxRefundNotificationSavedData.get(player.getServer()).forPlayer(player.getUUID()),
                com.ailudick.capitalismmod.tax.TaxAnnualReport.calculate(player.getServer(), player.getUUID(), now),
                com.ailudick.capitalismmod.tax.IndividualTaxPeriodSavedData.get(player.getServer())
                        .forBusiness(business == null ? "" : business.businessId()),
                com.ailudick.capitalismmod.tax.TaxExpenseLedgerSavedData.get(player.getServer())
                        .forTaxpayer(player.getUUID()).stream()
                        .filter(expense -> business != null && expense.subjectId().equals(business.businessId()))
                        .toList(),
                com.ailudick.capitalismmod.tax.TaxIncomeVoucherLedgerSavedData.get(player.getServer())
                        .forTaxpayer(player.getUUID()).stream()
                        .filter(income -> business != null && income.subjectId().equals(business.businessId()))
                        .toList(),
                com.ailudick.capitalismmod.tax.TaxCorrectionAuditSavedData.get(player.getServer()).all()));
        var correctionRequests = com.ailudick.capitalismmod.tax.TaxCorrectionRequestSavedData.get(player.getServer()).all().stream()
                .filter(request -> player.hasPermissions(2) || request.applicant().equals(player.getUUID())).toList();
        PacketDistributor.sendToPlayer(player, new SyncTaxCorrectionRequestsPayload(correctionRequests));
                
        long overdueDeclarations = ledger.outstandingFor(player.getUUID()).stream()
                .filter(bill -> bill.status(now) == TaxBill.Status.DECLARATION_OVERDUE)
                .count();
        if (overdueDeclarations > 0L) {
            player.displayClientMessage(Component.literal("You have " + overdueDeclarations
                    + " overdue tax declaration(s). Please declare them."), true);
        }
    }

    public static void handleWithdrawCompany(WithdrawCompanyPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!CompanyHelper.withdrawAll(player, payload.companyName())) {
                player.displayClientMessage(Component.translatable("command.capitalismmod.insufficient"), true);
                return;
            }
            Conglomerate conglomerate = CompanyHelper.getConglomerate(player);
            PacketDistributor.sendToPlayer(player, new SyncConglomeratePayload(conglomerate.name(), CompanyHelper.getCompanies(player)));
        });
    }

    public static void handleUpgradeCompany(UpgradeCompanyPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!CompanyHelper.upgrade(player, payload.companyName())) {
                player.displayClientMessage(Component.translatable("command.capitalismmod.insufficient"), true);
                return;
            }
            Conglomerate conglomerate = CompanyHelper.getConglomerate(player);
            PacketDistributor.sendToPlayer(player, new SyncConglomeratePayload(conglomerate.name(), CompanyHelper.getCompanies(player)));
        });
    }

    private static void giveCard(ServerPlayer player, String accountId, boolean credit) {
        ItemStack card = new ItemStack(credit ? ModItems.CREDIT_CARD.get() : ModItems.DEBIT_CARD.get());
        BankCard.bind(card, accountId);
        if (!player.getInventory().add(card)) {
            player.drop(card, false);
        }
    }

    public static void handlePlaceOrder(PlaceOrderPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            boolean success = payload.sell()
                    ? CommodityMarket.placeSell(player, payload.commodityIndex(), payload.quantity(), payload.pricePerUnit())
                    : CommodityMarket.placeBuy(player, payload.commodityIndex(), payload.quantity(), payload.pricePerUnit());
            if (!success) {
                player.displayClientMessage(Component.translatable("command.capitalismmod.insufficient"), true);
            }
            syncCommodity(player);
            syncMarket(player);
        });
    }

    public static void handleCancelOrder(CancelOrderPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            CommodityMarket.cancelOrder(player, payload.orderId());
            syncCommodity(player);
            syncMarket(player);
        });
    }

    private static void syncCommodity(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new SyncCommodityPayload(
                new HashMap<>(CommodityMarket.getPrices(player.getServer())),
                new HashMap<>(CommodityMarket.getHistory(player.getServer()))));
    }

    private static void syncMarket(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new SyncMarketOrdersPayload(new ArrayList<>(CommodityMarket.getOrders(player.getServer()))));
    }

    public static void handleWarehouseDeposit(WarehouseDepositPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!Commodities.isValid(payload.commodityIndex())) {
                return;
            }
            Item item = Commodities.get(payload.commodityIndex()).getItem();
            InventoryOwner owner = payload.ownerKey().isBlank() ? WarehouseAccess.personal(player)
                    : WarehouseAccess.resolve(player, payload.ownerKey());
            if (owner == null) return;
            WarehouseSavedData.get(player.getServer()).deposit(player, owner, item, payload.count());
            syncWarehouse(player, owner);
        });
    }

    public static void handleWarehouseWithdraw(WarehouseWithdrawPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!Commodities.isValid(payload.commodityIndex())) {
                return;
            }
            Item item = Commodities.get(payload.commodityIndex()).getItem();
            InventoryOwner owner = payload.ownerKey().isBlank() ? WarehouseAccess.personal(player)
                    : WarehouseAccess.resolve(player, payload.ownerKey());
            if (owner == null) return;
            WarehouseSavedData.get(player.getServer()).withdraw(player, owner, item, payload.count());
            syncWarehouse(player, owner);
        });
    }

    private static void syncWarehouse(ServerPlayer player) {
        syncWarehouse(player, WarehouseAccess.personal(player));
    }

    private static void syncWarehouse(ServerPlayer player, InventoryOwner owner) {
        PacketDistributor.sendToPlayer(player, new SyncWarehousePayload(
                new HashMap<>(WarehouseSavedData.get(player.getServer()).storage(owner)), owner.storageKey(),
                WarehouseAccess.accessibleOwners(player)));
    }

    public static void handleSelectWarehouseOwner(SelectWarehouseOwnerPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                InventoryOwner owner = WarehouseAccess.resolve(player, payload.ownerKey());
                if (owner != null) syncWarehouse(player, owner);
            }
        });
    }

    public static void handleOpenFuturesPosition(OpenFuturesPositionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            boolean success = FuturesMarket.openPosition(player, payload.commodityIndex(), payload.quantity(), payload.longSide());
            if (!success) {
                player.displayClientMessage(Component.translatable("command.capitalismmod.insufficient"), true);
            }
            syncFutures(player);
        });
    }

    public static void handleCloseFuturesPosition(CloseFuturesPositionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            FuturesMarket.closePosition(player, payload.positionId());
            syncFutures(player);
        });
    }

    public static void handleDepositMargin(DepositMarginPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!FuturesMarket.depositMargin(player, payload.amount())) {
                player.displayClientMessage(Component.translatable("command.capitalismmod.insufficient"), true);
            }
            syncFutures(player);
        });
    }

    public static void handleWithdrawMargin(WithdrawMarginPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!FuturesMarket.withdrawMargin(player, payload.amount())) {
                player.displayClientMessage(Component.translatable("command.capitalismmod.insufficient"), true);
            }
            syncFutures(player);
        });
    }

    private static void syncFutures(ServerPlayer player) {
        List<Position> mine = new ArrayList<>();
        for (Position position : FuturesMarket.getPositions(player.getServer())) {
            if (position.playerId().equals(player.getUUID())) {
                mine.add(position);
            }
        }
        PacketDistributor.sendToPlayer(player, new SyncFuturesPayload(
                new HashMap<>(FuturesMarket.getPrices(player.getServer())),
                mine,
                FuturesMarket.marginBalance(player.getServer(), player.getUUID()),
                new HashMap<>(FuturesMarket.getDaysToExpiry(player.getServer()))));
    }

    public static void handleListAuction(ListAuctionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            boolean success = AuctionMarket.listAuction(player, payload.commodityIndex(), payload.quantity(),
                    payload.startingPrice(), payload.durationSeconds());
            if (!success) {
                player.displayClientMessage(Component.translatable("command.capitalismmod.insufficient"), true);
            }
            syncAuctions(player);
        });
    }

    public static void handleBid(BidPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            boolean success = AuctionMarket.bid(player, payload.auctionId(), payload.amount());
            if (!success) {
                player.displayClientMessage(Component.translatable("command.capitalismmod.insufficient"), true);
            }
            syncAuctions(player);
        });
    }

    public static void handleBuyBond(BuyBondPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!BondMarket.buyBond(player, payload.count())) {
                player.displayClientMessage(Component.translatable("command.capitalismmod.insufficient"), true);
            }
            syncBonds(player);
        });
    }

    public static void handleRedeemBond(RedeemBondPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            BondMarket.redeemBond(player, payload.holdingId());
            syncBonds(player);
        });
    }

    private static void syncAuctions(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new SyncAuctionsPayload(
                new ArrayList<>(AuctionSavedData.get(player.getServer()).auctions())));
    }

    private static void syncBonds(ServerPlayer player) {
        List<BondHolding> mine = new ArrayList<>();
        for (BondHolding holding : BondSavedData.get(player.getServer()).holdings()) {
            if (holding.holder().equals(player.getUUID())) {
                mine.add(holding);
            }
        }
        PacketDistributor.sendToPlayer(player, new SyncBondsPayload(mine));
    }

    public static void handlePlaceSupplyOrder(PlaceSupplyOrderPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!SupplyMarket.placeOrder(player, payload.offerId(), payload.quantity(), payload.companyName())) {
                player.displayClientMessage(Component.translatable("command.capitalismmod.insufficient"), true);
            }
            syncSupplyMarket(player);
        });
    }

    private static void syncSupplyMarket(ServerPlayer player) {
        List<PurchaseOrder> mine = new ArrayList<>();
        for (PurchaseOrder order : SupplyMarketSavedData.get(player.getServer()).orders()) {
            if (order.buyerUuid().equals(player.getUUID())) {
                mine.add(order);
            }
        }
        PacketDistributor.sendToPlayer(player, new SyncSupplyMarketPayload(
                new ArrayList<>(SupplyMarketSavedData.get(player.getServer()).offers()), mine,
                new ArrayList<>(CompanyHelper.getCompanies(player).keySet())));
    }

    public static void handlePlaceStockOrder(PlaceStockOrderPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            boolean success = StockMarket.placeOrder(player, payload.stockId(), payload.quantity(), payload.pricePerUnit(), payload.sell());
            if (!success) {
                player.displayClientMessage(Component.translatable("command.capitalismmod.insufficient"), true);
            }
            syncStocks(player);
            syncStockOrders(player);
        });
    }

    public static void handleCancelStockOrder(CancelStockOrderPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            StockMarket.cancelOrder(player, payload.orderId());
            syncStocks(player);
            syncStockOrders(player);
        });
    }

    private static void syncStocks(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new SyncStocksPayload(
                new HashMap<>(StockMarket.getPrices(player.getServer())),
                new HashMap<>(StockMarket.getPortfolio(player.getServer(), player)),
                new HashMap<>(StockMarket.getHistory(player.getServer())),
                new HashMap<>(StockMarket.getCompanyStocks(player.getServer()))));
    }

    private static void syncStockOrders(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new SyncStockOrdersPayload(new ArrayList<>(StockMarket.getOrders(player.getServer()))));
    }

    public static void handleCreateCompany(CreateCompanyPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            String name = payload.name().trim();
            if (!CompanyHelper.create(player, name, payload.companyType())) {
                player.displayClientMessage(Component.translatable("command.capitalismmod.company_exists"), true);
                return;
            }
            issueLicense(player, name, payload.companyType());
            player.displayClientMessage(Component.translatable("message.capitalismmod.company_created", name), true);
            player.closeContainer();
        });
    }

    /** Issues a bound business license (营业执照) to the player after registering at the bureau. */
    private static void issueLicense(ServerPlayer player, String name, String type) {
        ItemStack license = new ItemStack(ModItems.BUSINESS_LICENSE.get());
        BusinessLicense.bind(license, name, type);
        if (!player.getInventory().add(license)) {
            player.drop(license, false);
        }
    }

    public static void handleOpenConglomerate(OpenConglomeratePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            Conglomerate conglomerate = CompanyHelper.getConglomerate(player);
            player.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new ConglomerateMenu(id, inv),
                    Component.translatable("container.capitalismmod.conglomerate")));
            PacketDistributor.sendToPlayer(player, new SyncConglomeratePayload(conglomerate.name(), CompanyHelper.getCompanies(player)));
        });
    }

    public static void handleOpenLand(OpenLandPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            LandClaim claim = LandHelper.at(player, player.blockPosition());
            if (!(player.containerMenu instanceof com.ailudick.capitalismmod.menu.LandMenu)) {
                player.openMenu(new SimpleMenuProvider((id, inv, p) -> new com.ailudick.capitalismmod.menu.LandMenu(id, inv),
                    Component.literal("土地系统")));
            }
            List<SyncLandPayload.MapCell> mapCells = new ArrayList<>();
            var claims = com.ailudick.capitalismmod.land.LandSavedData.get(player.getServer()).claims();
            int centerX = player.chunkPosition().x;
            int centerZ = player.chunkPosition().z;
            String dimension = player.level().dimension().location().toString();
            for (int z = centerZ - 4; z <= centerZ + 4; z++) {
                for (int x = centerX - 4; x <= centerX + 4; x++) {
                    LandClaim nearby = claims.get(dimension + ":" + x + ":" + z);
                    mapCells.add(new SyncLandPayload.MapCell(x, z, nearby != null,
                            nearby != null && nearby.ownerUuid().equals(player.getUUID()), nearby != null
                            && com.ailudick.capitalismmod.land.LandAuctionSavedData.get(player.getServer()).get(nearby.id()) != null));
                }
            }
            if (claim == null) {
                PacketDistributor.sendToPlayer(player, new SyncLandPayload(false, "", player.level().dimension().location().toString(),
                        player.chunkPosition().x, player.chunkPosition().z, "", "", "", "", 0L, 0L, false, false, "", 0L, 0L, 0L, 0L, 0L, 0L, mapCells));
            } else {
                PacketDistributor.sendToPlayer(player, new SyncLandPayload(true, claim.id(), claim.dimension(), centerX, centerZ,
                        claim.ownerUuid().toString(), claim.purpose(), claim.linkedBusinessId(), claim.resourceType(),
                        claim.resourceAmount(), claim.taxOwed(), claim.trusts(player.getUUID()), claim.leaseeUuid() != null,
                        claim.leaseeUuid() == null ? "" : claim.leaseeUuid().toString(),
                        claim.leaseUntil(), claim.leaseRent(), claim.leaseDebt(), claim.leaseGraceUntil(), claim.taxDueAt(), claim.taxGraceUntil(), mapCells));
            }
            sendOwnedLands(player);
            sendLandLogs(player);
        });
    }

    public static void handleRequestLandDetails(RequestLandDetailsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            String currentDimension = player.level().dimension().location().toString();
            if (!currentDimension.equals(payload.dimension())) return;
            sendLandData(player, payload.chunkX(), payload.chunkZ());
        });
    }

    public static void handleClaimLand(ClaimLandPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            String currentDimension = player.level().dimension().location().toString();
            if (!currentDimension.equals(payload.dimension())) return;
            boolean claimed = com.ailudick.capitalismmod.land.LandHelper.claim(player, payload.chunkX(), payload.chunkZ());
            if (claimed) {
                sendLandData(player, payload.chunkX(), payload.chunkZ());
                operationResult(player, true, Component.literal("区块已成功认领"));
            } else {
                sendLandData(player, payload.chunkX(), payload.chunkZ());
                var claims = com.ailudick.capitalismmod.land.LandSavedData.get(player.getServer()).claims();
                String id = currentDimension + ":" + payload.chunkX() + ":" + payload.chunkZ();
                boolean occupied = claims.containsKey(id);
                long owned = claims.values().stream()
                        .filter(claim -> claim.ownerUuid().equals(player.getUUID())
                                && claim.dimension().equals(currentDimension)).count();
                boolean adjacent = owned == 0 || com.ailudick.capitalismmod.land.LandHelper.hasAdjacentClaim(
                        player, payload.chunkX(), payload.chunkZ());
                String message = occupied ? "区块已被其他土地占用"
                        : owned >= Config.MAX_LAND_CLAIMS.get() ? "已达到土地数量上限：" + Config.MAX_LAND_CLAIMS.get()
                        : Config.REQUIRE_ADJACENT_LAND_CLAIMS.get() && !adjacent ? "新区块必须与现有土地相邻"
                        : "土地认领失败";
                operationResult(player, false, Component.literal(message));
            }
        });
    }

    public static void handleReleaseLand(ReleaseLandPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            String currentDimension = player.level().dimension().location().toString();
            if (!currentDimension.equals(payload.dimension())) return;
            boolean released = com.ailudick.capitalismmod.land.LandHelper.release(player, payload.chunkX(), payload.chunkZ());
            sendLandData(player, payload.chunkX(), payload.chunkZ());
            operationResult(player, released, Component.literal(released ? "土地已放弃" : "你不是该土地的所有者"));
        });
    }

    public static void handleSetLandPurpose(SetLandPurposePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            String currentDimension = player.level().dimension().location().toString();
            if (!currentDimension.equals(payload.dimension())) return;
            boolean changed = LandHelper.setPurpose(player, payload.chunkX(), payload.chunkZ(), payload.purpose());
            sendLandData(player, payload.chunkX(), payload.chunkZ());
            operationResult(player, changed, Component.literal(changed ? "土地用途已修改" : "土地用途修改失败"));
        });
    }

    public static void handleManageLandTrust(ManageLandTrustPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            String currentDimension = player.level().dimension().location().toString();
            if (!currentDimension.equals(payload.dimension())) return;
            ServerPlayer target = player.getServer().getPlayerList().getPlayerByName(payload.targetName());
            java.util.UUID targetUuid = target == null ? parseUuid(payload.targetName()) : target.getUUID();
            if (targetUuid == null) {
                operationResult(player, false, Component.literal("目标玩家必须在线"));
                return;
            }
            boolean changed = LandHelper.manageTrust(player, payload.chunkX(), payload.chunkZ(),
                    targetUuid, payload.add());
            sendLandData(player, payload.chunkX(), payload.chunkZ());
            operationResult(player, changed,
                    Component.literal(changed ? (payload.add() ? "已添加信任玩家" : "已移除信任玩家")
                            : "你不是该土地的所有者或目标玩家无效"));
        });
    }

    public static void handleSetLandPermissions(SetLandPermissionsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!player.level().dimension().location().toString().equals(payload.dimension())) return;
            LandClaim claim = LandSavedData.get(player.getServer()).get(
                    payload.dimension() + ":" + payload.chunkX() + ":" + payload.chunkZ());
            boolean allowed = claim != null && claim.ownerUuid().equals(player.getUUID());
            if (allowed) {
                var permissions = LandPermissionSavedData.get(player.getServer());
                permissions.set(claim.id(), payload.memberBuild(), payload.memberInteract(),
                        payload.container(), payload.redstone());
            }
            operationResult(player, allowed, Component.literal(allowed ? "成员权限已更新" : "只有土地所有者可以修改权限"));
        });
    }

    public static void handleClearLandLogs(ClearLandLogsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            var claims = com.ailudick.capitalismmod.land.LandSavedData.get(player.getServer()).claims();
            LandOperationLogSavedData.get(player.getServer()).clearFor(player.getUUID(), claims);
            sendLandLogs(player);
            operationResult(player, true, Component.literal("土地日志已清理"));
        });
    }

    public static void handleLeaseLand(LeaseLandPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            String currentDimension = player.level().dimension().location().toString();
            if (!currentDimension.equals(payload.dimension())) return;
            ServerPlayer target = player.getServer().getPlayerList().getPlayerByName(payload.targetName());
            if (target == null) {
                operationResult(player, false, Component.literal("承租玩家必须在线"));
                return;
            }
            boolean leased = LandHelper.lease(player, payload.chunkX(), payload.chunkZ(), target.getUUID(),
                    payload.days(), payload.rent());
            sendLandData(player, payload.chunkX(), payload.chunkZ());
            operationResult(player, leased, Component.literal(leased ? "土地已出租" : "出租失败：参数无效或你不是土地所有者"));
        });
    }

    public static void handleUnleaseLand(UnleaseLandPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            String currentDimension = player.level().dimension().location().toString();
            if (!currentDimension.equals(payload.dimension())) return;
            boolean unleased = LandHelper.unlease(player, payload.chunkX(), payload.chunkZ());
            sendLandData(player, payload.chunkX(), payload.chunkZ());
            operationResult(player, unleased, Component.literal(unleased ? "租赁已解除" : "解除出租失败"));
        });
    }

    private static java.util.UUID parseUuid(String value) {
        try {
            return java.util.UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static void handleOpenLandAtChunk(OpenLandAtChunkPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!player.level().dimension().location().toString().equals(payload.dimension())) return;
            int chunkX = payload.chunkX();
            int chunkZ = payload.chunkZ();
            player.openMenu(new SimpleMenuProvider((id, inv, p) -> new com.ailudick.capitalismmod.menu.LandMenu(id, inv),
                    Component.literal("土地系统")));
            sendLandData(player, chunkX, chunkZ);
        });
    }

    public static void sendLandData(ServerPlayer player, int centerX, int centerZ) {
        var claims = com.ailudick.capitalismmod.land.LandSavedData.get(player.getServer()).claims();
        String dimension = player.level().dimension().location().toString();
        LandClaim claim = claims.get(dimension + ":" + centerX + ":" + centerZ);
        if (claim != null && claim.leaseeUuid() != null
                && player.level().getGameTime() >= claim.leaseUntil()) {
            claim = claim.clearLease();
            com.ailudick.capitalismmod.land.LandSavedData.get(player.getServer()).put(claim);
        }
        sendLandTrusts(player, claim, centerX, centerZ);
        List<SyncLandPayload.MapCell> mapCells = new ArrayList<>();
        for (int z = centerZ - 4; z <= centerZ + 4; z++) {
            for (int x = centerX - 4; x <= centerX + 4; x++) {
                LandClaim nearby = claims.get(dimension + ":" + x + ":" + z);
                mapCells.add(new SyncLandPayload.MapCell(x, z, nearby != null,
                        nearby != null && nearby.ownerUuid().equals(player.getUUID()), nearby != null
                        && com.ailudick.capitalismmod.land.LandAuctionSavedData.get(player.getServer()).get(nearby.id()) != null));
            }
        }
        if (claim == null) {
            PacketDistributor.sendToPlayer(player, new SyncLandPayload(false, "", dimension, centerX, centerZ,
                    "", "", "", "", 0L, 0L, false, false, "", 0L, 0L, 0L, 0L, 0L, 0L, mapCells));
        } else {
            PacketDistributor.sendToPlayer(player, new SyncLandPayload(true, claim.id(), dimension, centerX, centerZ,
                    claim.ownerUuid().toString(), claim.purpose(), claim.linkedBusinessId(), claim.resourceType(),
                    claim.resourceAmount(), claim.taxOwed(), claim.trusts(player.getUUID()), claim.leaseeUuid() != null,
                    claim.leaseeUuid() == null ? "" : claim.leaseeUuid().toString(),
                    claim.leaseUntil(), claim.leaseRent(), claim.leaseDebt(), claim.leaseGraceUntil(), claim.taxDueAt(), claim.taxGraceUntil(), mapCells));
        }
        String claimId = claim == null ? "" : claim.id();
        var permissionData = LandPermissionSavedData.get(player.getServer());
        PacketDistributor.sendToPlayer(player, new SyncLandPermissionsPayload(claimId,
                claim != null && permissionData.canBuild(claimId),
                claim != null && permissionData.canInteract(claimId),
                claim != null && permissionData.canContainer(claimId),
                claim != null && permissionData.canRedstone(claimId)));
        var sale = claim == null ? null : com.ailudick.capitalismmod.land.LandTransferSavedData.get(player.getServer())
                .findForLand(claim.dimension(), claim.chunkX(), claim.chunkZ());
        String saleTarget = "";
        if (sale != null) {
            ServerPlayer target = player.getServer().getPlayerList().getPlayer(sale.to());
            saleTarget = target == null ? sale.to().toString() : target.getGameProfile().getName();
        }
        var auction = claim == null ? null : com.ailudick.capitalismmod.land.LandAuctionSavedData.get(player.getServer()).get(claim.id());
        String auctionBidder = "";
        if (auction != null && auction.highestBidder() != null) {
            ServerPlayer bidder = player.getServer().getPlayerList().getPlayer(auction.highestBidder());
            auctionBidder = bidder == null ? auction.highestBidder().toString() : bidder.getGameProfile().getName();
        }
        PacketDistributor.sendToPlayer(player, new SyncLandSalePayload(sale != null, saleTarget,
                sale == null ? 0L : sale.price(), sale == null ? 0L : sale.expiresAt(), auction != null,
                auction == null ? 0L : auction.startPrice(), auction == null ? 0L : auction.highestBid(),
                auctionBidder, auction == null ? 0L : auction.endsAt()));
        sendOwnedLands(player);
        sendLandLogs(player);
        sendOwnershipHistory(player, claim);
    }

    private static void sendOwnershipHistory(ServerPlayer player, LandClaim claim) {
        List<String> owners = new ArrayList<>();
        if (claim != null) {
            var history = com.ailudick.capitalismmod.land.LandOwnershipSavedData.get(player.getServer()).history(claim.id());
            for (var event : history) {
                ServerPlayer owner = player.getServer().getPlayerList().getPlayer(event.owner());
                String name = owner == null ? event.owner().toString().substring(0, 8) : owner.getGameProfile().getName();
                owners.add(name + "（" + formatGameTime(event.time()) + "，" + event.reason() + "）");
            }
        }
        PacketDistributor.sendToPlayer(player, new com.ailudick.capitalismmod.network.payload.SyncLandOwnershipPayload(List.copyOf(owners)));
    }

    private static String formatGameTime(long ticks) {
        return PerpetualCalendar.formatMinecraftTicks(ticks);
    }

    private static void sendLandLogs(ServerPlayer player) {
        var saved = com.ailudick.capitalismmod.land.LandOperationLogSavedData.get(player.getServer());
        var claims = com.ailudick.capitalismmod.land.LandSavedData.get(player.getServer()).claims();
        List<String> logs = saved.recentFor(player.getUUID(), claims, 32,
                com.ailudick.capitalismmod.land.LandOwnershipSavedData.get(player.getServer())).stream()
                .map(entry -> {
                    ServerPlayer actor = player.getServer().getPlayerList().getPlayer(entry.actor());
                    String name = actor == null ? entry.actor().toString().substring(0, 8) : actor.getGameProfile().getName();
                    return "[" + name + "] [" + entry.chunkX() + "," + entry.chunkZ() + "] " + entry.action();
                })
                .toList();
        PacketDistributor.sendToPlayer(player, new SyncLandLogsPayload(logs));
    }

    private static void sendOwnedLands(ServerPlayer player) {
        List<SyncOwnedLandsPayload.LandEntry> lands = new ArrayList<>();
        for (LandClaim claim : com.ailudick.capitalismmod.land.LandSavedData.get(player.getServer()).claims().values()) {
            if (claim.ownerUuid().equals(player.getUUID())) {
                lands.add(new SyncOwnedLandsPayload.LandEntry(claim.dimension(), claim.chunkX(), claim.chunkZ(),
                        claim.purpose(), claim.leaseeUuid() != null, claim.leaseDebt()));
            }
        }
        lands.sort(java.util.Comparator.comparing(SyncOwnedLandsPayload.LandEntry::dimension)
                .thenComparingInt(SyncOwnedLandsPayload.LandEntry::chunkX)
                .thenComparingInt(SyncOwnedLandsPayload.LandEntry::chunkZ));
        PacketDistributor.sendToPlayer(player, new SyncOwnedLandsPayload(List.copyOf(lands)));
    }

    private static void sendLandTrusts(ServerPlayer player, LandClaim claim, int chunkX, int chunkZ) {
        List<String> names = new ArrayList<>();
        if (claim != null) {
            for (java.util.UUID uuid : claim.trustedPlayers()) {
                ServerPlayer online = player.getServer().getPlayerList().getPlayer(uuid);
                names.add(online != null ? online.getGameProfile().getName() : uuid.toString());
            }
        }
        PacketDistributor.sendToPlayer(player, new SyncLandTrustsPayload(chunkX, chunkZ, names));
    }

    public static void handleOpenWorldMap(OpenWorldMapPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            player.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new com.ailudick.capitalismmod.menu.WorldMapMenu(id, inv),
                    Component.literal("世界地图")));
            PacketDistributor.sendToPlayer(player, new SyncWorldMapPayload(
                    player.level().dimension().location().toString(),
                    player.chunkPosition().x, player.chunkPosition().z));
            refreshLegacyWorldMapTiles(player);
            sendWorldMapTiles(player, player.chunkPosition().x, player.chunkPosition().z,
                    Config.WORLD_MAP_DISCOVERY_RADIUS.get(), true);
            sendAllWorldMapTiles(player);
            sendLandOverlay(player, player.chunkPosition().x, player.chunkPosition().z,
                    Config.WORLD_MAP_DISCOVERY_RADIUS.get());
        });
    }

    public static void handleRequestWorldMapTiles(RequestWorldMapTilesPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            sendWorldMapTiles(player, payload.centerChunkX(), payload.centerChunkZ(), payload.radius(), payload.discover());
            sendLandOverlay(player, payload.centerChunkX(), payload.centerChunkZ(), payload.radius());
        });
    }

    private static void sendLandOverlay(ServerPlayer player, int centerChunkX, int centerChunkZ, int requestedRadius) {
        int radius = Math.max(1, Math.min(Config.WORLD_MAP_DISCOVERY_RADIUS.get(), requestedRadius));
        String dimension = player.serverLevel().dimension().location().toString();
        var claims = com.ailudick.capitalismmod.land.LandSavedData.get(player.getServer()).claims();
        List<SyncLandOverlayPayload.Cell> cells = new ArrayList<>((radius * 2 + 1) * (radius * 2 + 1));
        for (int chunkZ = centerChunkZ - radius; chunkZ <= centerChunkZ + radius; chunkZ++) {
            for (int chunkX = centerChunkX - radius; chunkX <= centerChunkX + radius; chunkX++) {
                LandClaim claim = claims.get(dimension + ":" + chunkX + ":" + chunkZ);
                cells.add(new SyncLandOverlayPayload.Cell(chunkX, chunkZ, claim != null,
                        claim != null && claim.ownerUuid().equals(player.getUUID()), claim != null
                        && com.ailudick.capitalismmod.land.LandAuctionSavedData.get(player.getServer()).get(claim.id()) != null));
            }
        }
        PacketDistributor.sendToPlayer(player, new SyncLandOverlayPayload(dimension, List.copyOf(cells)));
    }

    private static void sendWorldMapTiles(ServerPlayer player, int requestedCenterX, int requestedCenterZ,
                                          int requestedRadius, boolean discover) {
        int radius = Math.max(1, Math.min(Config.WORLD_MAP_DISCOVERY_RADIUS.get(), requestedRadius));
        ServerLevel level = player.serverLevel();
        WorldMapTileSavedData cache = WorldMapTileSavedData.get(player.getServer());
        int centerChunkX = discover ? player.chunkPosition().x : requestedCenterX;
        int centerChunkZ = discover ? player.chunkPosition().z : requestedCenterZ;
        List<SyncWorldMapTilesPayload.Tile> tiles = new ArrayList<>();
        for (int chunkZ = centerChunkZ - radius; chunkZ <= centerChunkZ + radius; chunkZ++) {
            for (int chunkX = centerChunkX - radius; chunkX <= centerChunkX + radius; chunkX++) {
                String cacheKey = level.dimension().location() + ":" + chunkX + ":" + chunkZ;
                int[] colors = cache.get(cacheKey);
                if (colors == null && discover) {
                    level.getChunk(chunkX, chunkZ);
                    colors = renderWorldMapTile(level, chunkX, chunkZ);
                    cache.put(cacheKey, colors);
                }
                if (colors != null) {
                    tiles.add(new SyncWorldMapTilesPayload.Tile(chunkX, chunkZ, colors));
                }
            }
        }
        sendWorldMapTileBatches(player, tiles);
    }

    private static void sendWorldMapTileBatches(ServerPlayer player, List<SyncWorldMapTilesPayload.Tile> tiles) {
        if (tiles.isEmpty()) {
            PacketDistributor.sendToPlayer(player, new SyncWorldMapTilesPayload(List.of()));
            return;
        }
        for (int from = 0; from < tiles.size(); from += 81) {
            int to = Math.min(from + 81, tiles.size());
            PacketDistributor.sendToPlayer(player,
                    new SyncWorldMapTilesPayload(List.copyOf(tiles.subList(from, to))));
        }
    }

    private static void refreshLegacyWorldMapTiles(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        String dimension = level.dimension().location().toString();
        WorldMapTileSavedData cache = WorldMapTileSavedData.get(player.getServer());
        if (cache.isDimensionShaded(dimension)) return;
        for (WorldMapTileSavedData.StoredTile tile : cache.getDimensionTiles(dimension)) {
            level.getChunk(tile.chunkX(), tile.chunkZ());
            cache.put(dimension + ":" + tile.chunkX() + ":" + tile.chunkZ(),
                    renderWorldMapTile(level, tile.chunkX(), tile.chunkZ()));
        }
        cache.markDimensionShaded(dimension);
    }

    private static int[] renderWorldMapTile(ServerLevel level, int chunkX, int chunkZ) {
        int[] colors = new int[256];
        for (int localZ = 0; localZ < 16; localZ++) {
            for (int localX = 0; localX < 16; localX++) {
                int worldX = chunkX * 16 + localX;
                int worldZ = chunkZ * 16 + localZ;
                int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, worldX, worldZ);
                BlockPos pos = new BlockPos(worldX, Math.max(level.getMinBuildHeight(), surfaceY - 1), worldZ);
                var state = level.getBlockState(pos);
                MapColor mapColor = state.getMapColor(level, pos);
                int baseColor = state.is(BlockTags.LEAVES) ? 0xFF4C8F45
                        : state.is(BlockTags.LOGS) ? 0xFF8B5A2B
                        : mapColor == MapColor.NONE ? 0xFF56616A : 0xFF000000 | mapColor.col;

                int west = level.getHeight(Heightmap.Types.MOTION_BLOCKING, worldX - 1, worldZ);
                int east = level.getHeight(Heightmap.Types.MOTION_BLOCKING, worldX + 1, worldZ);
                int north = level.getHeight(Heightmap.Types.MOTION_BLOCKING, worldX, worldZ - 1);
                int south = level.getHeight(Heightmap.Types.MOTION_BLOCKING, worldX, worldZ + 1);
                double dx = (east - west) * 0.5;
                double dz = (south - north) * 0.5;
                double nx = -dx;
                double ny = 2.8;
                double nz = -dz;
                double length = Math.sqrt(nx * nx + ny * ny + nz * nz);
                double light = (nx * -0.55 + ny * 0.85 + nz * -0.55) / length;
                // Keep even a one-block step visible: the directional term is deliberately
                // stronger than vanilla's subtle map shading, while flat terrain stays neutral.
                double brightness = 0.54 + light * 0.63;
                int higherNeighbor = Math.max(Math.max(north, south), Math.max(west, east));
                int lowerNeighbor = Math.min(Math.min(north, south), Math.min(west, east));
                int rise = Math.max(0, surfaceY - lowerNeighbor);
                int drop = Math.max(0, higherNeighbor - surfaceY);
                // Add a crisp relief edge on terraces and one-block steps. This is an
                // intentional cartographic exaggeration so individual block elevations read.
                brightness += Math.min(0.18, rise * 0.10);
                brightness -= Math.min(0.18, drop * 0.10);
                brightness = Math.max(0.42, Math.min(1.46, brightness));
                colors[localZ * 16 + localX] = multiplyColor(baseColor, brightness);
            }
        }
        return colors;
    }

    private static int multiplyColor(int color, double brightness) {
        int red = Math.max(0, Math.min(255, (int) (((color >> 16) & 0xFF) * brightness)));
        int green = Math.max(0, Math.min(255, (int) (((color >> 8) & 0xFF) * brightness)));
        int blue = Math.max(0, Math.min(255, (int) ((color & 0xFF) * brightness)));
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }

    private static void sendAllWorldMapTiles(ServerPlayer player) {
        WorldMapTileSavedData cache = WorldMapTileSavedData.get(player.getServer());
        List<SyncWorldMapTilesPayload.Tile> batch = new ArrayList<>(81);
        String dimension = player.serverLevel().dimension().location().toString();
        for (WorldMapTileSavedData.StoredTile tile : cache.getDimensionTiles(dimension)) {
            batch.add(new SyncWorldMapTilesPayload.Tile(tile.chunkX(), tile.chunkZ(), tile.colors()));
            if (batch.size() == 81) {
                PacketDistributor.sendToPlayer(player, new SyncWorldMapTilesPayload(List.copyOf(batch)));
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            PacketDistributor.sendToPlayer(player, new SyncWorldMapTilesPayload(List.copyOf(batch)));
        }
    }

    public static void handleRenameConglomerate(RenameConglomeratePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (CompanyHelper.rename(player, payload.newName())) {
                Conglomerate conglomerate = CompanyHelper.getConglomerate(player);
                PacketDistributor.sendToPlayer(player, new SyncConglomeratePayload(conglomerate.name(), CompanyHelper.getCompanies(player)));
            }
        });
    }

    public static void handleIpoCompany(IpoCompanyPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            String name = payload.companyName();
            Company company = CompanyHelper.getCompany(player, name);
            if (company == null) {
                return;
            }
            if (!CompanyHelper.ipo(player, name)) {
                player.displayClientMessage(Component.translatable("command.capitalismmod.company_already_listed", name), true);
                return;
            }
            player.displayClientMessage(Component.translatable("command.capitalismmod.company_ipo", name, 1000L * company.level()), true);
            syncSecurities(player);
        });
    }

    private static void syncSecurities(ServerPlayer player) {
        Map<String, Company> companies = CompanyHelper.getCompanies(player);
        List<String> listed = new ArrayList<>();
        for (String name : companies.keySet()) {
            if (CompanyHelper.isListed(player, name)) {
                listed.add(name);
            }
        }
        PacketDistributor.sendToPlayer(player, new SyncSecuritiesPayload(new HashMap<>(companies), listed));
    }

    private static void syncBankState(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new SyncBankAccountsPayload(BankAccountHelper.getAccounts(player)));
        PacketDistributor.sendToPlayer(player, new SyncPersonalAssetsPayload(
                PersonalAssets.estimate(player.getServer(), player)));
    }

    private static void operationResult(ServerPlayer player, boolean success, Component message) {
        PacketDistributor.sendToPlayer(player, new OperationResultPayload(success, message.getString()));
    }

    private static Currency baseCurrency() {
        String id = Config.CROSS_BORDER_BASE_CURRENCY.get();
        return Currencies.exists(id) ? Currencies.byId(id) : Currencies.CNY;
    }
}
