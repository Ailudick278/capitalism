package com.ailudick.capitalismmod.network;

import com.ailudick.capitalismmod.Config;
import com.ailudick.capitalismmod.bank.BankAccount;
import com.ailudick.capitalismmod.bank.BankAccountHelper;
import com.ailudick.capitalismmod.currency.Currencies;
import com.ailudick.capitalismmod.currency.Currency;
import com.ailudick.capitalismmod.currency.ExchangeRates;
import com.ailudick.capitalismmod.currency.Money;
import com.ailudick.capitalismmod.init.ModItems;
import com.ailudick.capitalismmod.init.ModDataComponents;
import com.ailudick.capitalismmod.item.BankCard;
import com.ailudick.capitalismmod.item.Invoice;
import com.ailudick.capitalismmod.menu.ShopMenu;
import com.ailudick.capitalismmod.network.payload.BankTransactionPayload;
import com.ailudick.capitalismmod.network.payload.BuyItemPayload;
import com.ailudick.capitalismmod.network.payload.ExchangePayload;
import com.ailudick.capitalismmod.network.payload.LoanPayload;
import com.ailudick.capitalismmod.network.payload.OpenAccountPayload;
import com.ailudick.capitalismmod.network.payload.ReplaceCardPayload;
import com.ailudick.capitalismmod.network.payload.TransferPayload;
import com.ailudick.capitalismmod.network.payload.OpenTermDepositPayload;
import com.ailudick.capitalismmod.network.payload.WithdrawTermDepositPayload;
import com.ailudick.capitalismmod.network.payload.PayTaxPayload;
import com.ailudick.capitalismmod.network.payload.RedeemInvoicesPayload;
import com.ailudick.capitalismmod.network.payload.WithdrawCompanyPayload;
import com.ailudick.capitalismmod.network.payload.UpgradeCompanyPayload;
import com.ailudick.capitalismmod.network.payload.SyncBankAccountsPayload;
import com.ailudick.capitalismmod.shop.ShopOffer;
import com.ailudick.capitalismmod.company.Conglomerate;
import com.ailudick.capitalismmod.menu.ConglomerateMenu;
import com.ailudick.capitalismmod.network.payload.OpenConglomeratePayload;
import com.ailudick.capitalismmod.network.payload.RenameConglomeratePayload;
import com.ailudick.capitalismmod.network.payload.SyncConglomeratePayload;
import com.ailudick.capitalismmod.wallet.EconomyHelper;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.ailudick.capitalismmod.company.Company;
import com.ailudick.capitalismmod.company.CompanyHelper;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerPayloadHandler {

    public static void handleBuyItem(BuyItemPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!(player.containerMenu instanceof ShopMenu menu)) {
                return;
            }

            List<ShopOffer> offers = menu.getOffers();
            int index = payload.offerIndex();
            if (index < 0 || index >= offers.size()) {
                return;
            }

            ShopOffer offer = offers.get(index);
            Currency currency = Currencies.byId(offer.currencyId());
            if (!EconomyHelper.tryPay(player, currency, Money.toMinor(offer.price()))) {
                player.displayClientMessage(Component.translatable("command.capitalismmod.insufficient"), true);
                return;
            }

            ItemStack bought = offer.item().copy();
            if (!player.getInventory().add(bought)) {
                player.drop(bought, false);
            }

            ItemStack invoice = new ItemStack(ModItems.INVOICE.get());
            invoice.set(ModDataComponents.INVOICE_AMOUNT.get(), Money.toMinor(offer.price()));
            if (!player.getInventory().add(invoice)) {
                player.drop(invoice, false);
            }

            NeoForge.EVENT_BUS.post(new TradeCompletedEvent(player, null, offer.item(), offer.item().getCount(), offer.currencyId(), offer.price(), "shop", 0));
        });
    }

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

            long converted = ExchangeRates.convert(amountMinor, from, to);
            if (converted <= 0) {
                player.displayClientMessage(Component.translatable("command.capitalismmod.amount_too_small"), true);
                return;
            }
            if (!EconomyHelper.tryPay(player, from, amountMinor)) {
                player.displayClientMessage(Component.translatable("command.capitalismmod.insufficient"), true);
                return;
            }
            EconomyHelper.giveMoney(player, to, converted);
        });
    }

    public static void handleOpenAccount(OpenAccountPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!consumeCard(player, payload.credit())) {
                player.displayClientMessage(Component.translatable(payload.credit()
                        ? "message.capitalismmod.no_credit_card" : "message.capitalismmod.no_debit_card"), true);
                return;
            }
            BankAccount account = BankAccountHelper.openAccount(player, payload.credit());
            giveCard(player, account.id(), payload.credit());
            PacketDistributor.sendToPlayer(player, new SyncBankAccountsPayload(BankAccountHelper.getAccounts(player)));
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
                player.displayClientMessage(Component.translatable("command.capitalismmod.insufficient"), true);
            }
            PacketDistributor.sendToPlayer(player, new SyncBankAccountsPayload(BankAccountHelper.getAccounts(player)));
        });
    }

    public static void handleReplaceCard(ReplaceCardPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            BankAccount account = BankAccountHelper.getAccount(player, payload.accountId());
            if (account == null) {
                return;
            }
            giveCard(player, payload.accountId(), account.credit());
        });
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
                player.displayClientMessage(Component.translatable("command.capitalismmod.insufficient"), true);
            }
            PacketDistributor.sendToPlayer(player, new SyncBankAccountsPayload(BankAccountHelper.getAccounts(player)));
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
                player.displayClientMessage(Component.translatable("message.capitalismmod.transfer_failed"), true);
            }
            PacketDistributor.sendToPlayer(player, new SyncBankAccountsPayload(BankAccountHelper.getAccounts(player)));
        });
    }

    public static void handleOpenTermDeposit(OpenTermDepositPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            boolean success = BankAccountHelper.openTermDeposit(player, payload.accountId(), payload.currencyId(), Money.toMinor(payload.amount()), payload.termDays());
            if (!success) {
                player.displayClientMessage(Component.translatable("command.capitalismmod.insufficient"), true);
            }
            PacketDistributor.sendToPlayer(player, new SyncBankAccountsPayload(BankAccountHelper.getAccounts(player)));
        });
    }

    public static void handleWithdrawTermDeposit(WithdrawTermDepositPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            BankAccountHelper.withdrawTermDeposit(player, payload.accountId(), payload.index());
            PacketDistributor.sendToPlayer(player, new SyncBankAccountsPayload(BankAccountHelper.getAccounts(player)));
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
            PacketDistributor.sendToPlayer(player, new SyncConglomeratePayload(conglomerate.name(), conglomerate.companies()));
        });
    }

    public static void handleRedeemInvoices(RedeemInvoicesPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            long total = 0;
            for (ItemStack stack : player.getInventory().items) {
                if (stack.getItem() instanceof Invoice && stack.has(ModDataComponents.INVOICE_AMOUNT.get())) {
                    total += stack.get(ModDataComponents.INVOICE_AMOUNT.get()) * stack.getCount();
                    stack.setCount(0);
                }
            }
            if (total <= 0) {
                return;
            }
            long refund = (long) (total * Config.INVOICE_REFUND_RATE.get());
            if (refund > 0) {
                EconomyHelper.giveMoney(player, Currencies.USD, refund);
            }
        });
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
            PacketDistributor.sendToPlayer(player, new SyncConglomeratePayload(conglomerate.name(), conglomerate.companies()));
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
            PacketDistributor.sendToPlayer(player, new SyncConglomeratePayload(conglomerate.name(), conglomerate.companies()));
        });
    }

    private static boolean consumeCard(ServerPlayer player, boolean credit) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof BankCard card && card.isCredit() == credit && !BankCard.isBound(stack)) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
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
            WarehouseSavedData.get(player.getServer()).deposit(player, item, payload.count());
            syncWarehouse(player);
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
            WarehouseSavedData.get(player.getServer()).withdraw(player, item, payload.count());
            syncWarehouse(player);
        });
    }

    private static void syncWarehouse(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new SyncWarehousePayload(
                new HashMap<>(WarehouseSavedData.get(player.getServer()).storage(player.getUUID()))));
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
            if (!SupplyMarket.placeOrder(player, payload.offerId(), payload.quantity())) {
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
                new ArrayList<>(SupplyMarketSavedData.get(player.getServer()).offers()), mine));
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
            PacketDistributor.sendToPlayer(player, new SyncConglomeratePayload(conglomerate.name(), conglomerate.companies()));
        });
    }

    public static void handleRenameConglomerate(RenameConglomeratePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (CompanyHelper.rename(player, payload.newName())) {
                Conglomerate conglomerate = CompanyHelper.getConglomerate(player);
                PacketDistributor.sendToPlayer(player, new SyncConglomeratePayload(conglomerate.name(), conglomerate.companies()));
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
}
