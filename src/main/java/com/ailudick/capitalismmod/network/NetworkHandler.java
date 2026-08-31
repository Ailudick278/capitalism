package com.ailudick.capitalismmod.network;

import com.ailudick.capitalismmod.network.payload.BankTransactionPayload;
import com.ailudick.capitalismmod.network.payload.BuyItemPayload;
import com.ailudick.capitalismmod.network.payload.CancelOrderPayload;
import com.ailudick.capitalismmod.network.payload.CreateCompanyPayload;
import com.ailudick.capitalismmod.network.payload.OpenConglomeratePayload;
import com.ailudick.capitalismmod.network.payload.RenameConglomeratePayload;
import com.ailudick.capitalismmod.network.payload.SyncConglomeratePayload;
import com.ailudick.capitalismmod.network.payload.ExchangePayload;
import com.ailudick.capitalismmod.network.payload.LoanPayload;
import com.ailudick.capitalismmod.network.payload.OpenAccountPayload;
import com.ailudick.capitalismmod.network.payload.PlaceOrderPayload;
import com.ailudick.capitalismmod.network.payload.ReplaceCardPayload;
import com.ailudick.capitalismmod.network.payload.TransferPayload;
import com.ailudick.capitalismmod.network.payload.OpenTermDepositPayload;
import com.ailudick.capitalismmod.network.payload.WithdrawTermDepositPayload;
import com.ailudick.capitalismmod.network.payload.PayTaxPayload;
import com.ailudick.capitalismmod.network.payload.RedeemInvoicesPayload;
import com.ailudick.capitalismmod.network.payload.WithdrawCompanyPayload;
import com.ailudick.capitalismmod.network.payload.UpgradeCompanyPayload;
import com.ailudick.capitalismmod.network.payload.SyncBankAccountsPayload;
import com.ailudick.capitalismmod.network.payload.SyncCommodityPayload;
import com.ailudick.capitalismmod.network.payload.SyncMarketOrdersPayload;
import com.ailudick.capitalismmod.network.payload.SyncShopDataPayload;
import com.ailudick.capitalismmod.network.payload.SyncStocksPayload;
import com.ailudick.capitalismmod.network.payload.PlaceStockOrderPayload;
import com.ailudick.capitalismmod.network.payload.CancelStockOrderPayload;
import com.ailudick.capitalismmod.network.payload.SyncStockOrdersPayload;
import com.ailudick.capitalismmod.network.payload.CloseFuturesPositionPayload;
import com.ailudick.capitalismmod.network.payload.DepositMarginPayload;
import com.ailudick.capitalismmod.network.payload.OpenFuturesPositionPayload;
import com.ailudick.capitalismmod.network.payload.SyncFuturesPayload;
import com.ailudick.capitalismmod.network.payload.WithdrawMarginPayload;
import com.ailudick.capitalismmod.network.payload.BidPayload;
import com.ailudick.capitalismmod.network.payload.BuyBondPayload;
import com.ailudick.capitalismmod.network.payload.ListAuctionPayload;
import com.ailudick.capitalismmod.network.payload.PlaceSupplyOrderPayload;
import com.ailudick.capitalismmod.network.payload.RedeemBondPayload;
import com.ailudick.capitalismmod.network.payload.SyncSupplyMarketPayload;
import com.ailudick.capitalismmod.network.payload.SyncAuctionsPayload;
import com.ailudick.capitalismmod.network.payload.SyncBondsPayload;
import com.ailudick.capitalismmod.network.payload.IpoCompanyPayload;
import com.ailudick.capitalismmod.network.payload.SyncSecuritiesPayload;
import com.ailudick.capitalismmod.network.payload.SyncWarehousePayload;
import com.ailudick.capitalismmod.network.payload.WarehouseDepositPayload;
import com.ailudick.capitalismmod.network.payload.WarehouseWithdrawPayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class NetworkHandler {
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        // Client -> Server.
        registrar.playToServer(BuyItemPayload.TYPE, BuyItemPayload.STREAM_CODEC, ServerPayloadHandler::handleBuyItem);
        registrar.playToServer(ExchangePayload.TYPE, ExchangePayload.STREAM_CODEC, ServerPayloadHandler::handleExchange);
        registrar.playToServer(OpenAccountPayload.TYPE, OpenAccountPayload.STREAM_CODEC, ServerPayloadHandler::handleOpenAccount);
        registrar.playToServer(BankTransactionPayload.TYPE, BankTransactionPayload.STREAM_CODEC, ServerPayloadHandler::handleBankTransaction);
        registrar.playToServer(ReplaceCardPayload.TYPE, ReplaceCardPayload.STREAM_CODEC, ServerPayloadHandler::handleReplaceCard);
        registrar.playToServer(LoanPayload.TYPE, LoanPayload.STREAM_CODEC, ServerPayloadHandler::handleLoan);
        registrar.playToServer(TransferPayload.TYPE, TransferPayload.STREAM_CODEC, ServerPayloadHandler::handleTransfer);
        registrar.playToServer(OpenTermDepositPayload.TYPE, OpenTermDepositPayload.STREAM_CODEC, ServerPayloadHandler::handleOpenTermDeposit);
        registrar.playToServer(WithdrawTermDepositPayload.TYPE, WithdrawTermDepositPayload.STREAM_CODEC, ServerPayloadHandler::handleWithdrawTermDeposit);
        registrar.playToServer(PayTaxPayload.TYPE, PayTaxPayload.STREAM_CODEC, ServerPayloadHandler::handlePayTax);
        registrar.playToServer(RedeemInvoicesPayload.TYPE, RedeemInvoicesPayload.STREAM_CODEC, ServerPayloadHandler::handleRedeemInvoices);
        registrar.playToServer(WithdrawCompanyPayload.TYPE, WithdrawCompanyPayload.STREAM_CODEC, ServerPayloadHandler::handleWithdrawCompany);
        registrar.playToServer(UpgradeCompanyPayload.TYPE, UpgradeCompanyPayload.STREAM_CODEC, ServerPayloadHandler::handleUpgradeCompany);
        registrar.playToServer(PlaceOrderPayload.TYPE, PlaceOrderPayload.STREAM_CODEC, ServerPayloadHandler::handlePlaceOrder);
        registrar.playToServer(CancelOrderPayload.TYPE, CancelOrderPayload.STREAM_CODEC, ServerPayloadHandler::handleCancelOrder);
        registrar.playToServer(PlaceStockOrderPayload.TYPE, PlaceStockOrderPayload.STREAM_CODEC, ServerPayloadHandler::handlePlaceStockOrder);
        registrar.playToServer(CancelStockOrderPayload.TYPE, CancelStockOrderPayload.STREAM_CODEC, ServerPayloadHandler::handleCancelStockOrder);
        registrar.playToServer(CreateCompanyPayload.TYPE, CreateCompanyPayload.STREAM_CODEC, ServerPayloadHandler::handleCreateCompany);
        registrar.playToServer(OpenConglomeratePayload.TYPE, OpenConglomeratePayload.STREAM_CODEC, ServerPayloadHandler::handleOpenConglomerate);
        registrar.playToServer(RenameConglomeratePayload.TYPE, RenameConglomeratePayload.STREAM_CODEC, ServerPayloadHandler::handleRenameConglomerate);
        registrar.playToServer(IpoCompanyPayload.TYPE, IpoCompanyPayload.STREAM_CODEC, ServerPayloadHandler::handleIpoCompany);
        registrar.playToServer(WarehouseDepositPayload.TYPE, WarehouseDepositPayload.STREAM_CODEC, ServerPayloadHandler::handleWarehouseDeposit);
        registrar.playToServer(WarehouseWithdrawPayload.TYPE, WarehouseWithdrawPayload.STREAM_CODEC, ServerPayloadHandler::handleWarehouseWithdraw);
        registrar.playToServer(OpenFuturesPositionPayload.TYPE, OpenFuturesPositionPayload.STREAM_CODEC, ServerPayloadHandler::handleOpenFuturesPosition);
        registrar.playToServer(CloseFuturesPositionPayload.TYPE, CloseFuturesPositionPayload.STREAM_CODEC, ServerPayloadHandler::handleCloseFuturesPosition);
        registrar.playToServer(DepositMarginPayload.TYPE, DepositMarginPayload.STREAM_CODEC, ServerPayloadHandler::handleDepositMargin);
        registrar.playToServer(WithdrawMarginPayload.TYPE, WithdrawMarginPayload.STREAM_CODEC, ServerPayloadHandler::handleWithdrawMargin);
        registrar.playToServer(ListAuctionPayload.TYPE, ListAuctionPayload.STREAM_CODEC, ServerPayloadHandler::handleListAuction);
        registrar.playToServer(BidPayload.TYPE, BidPayload.STREAM_CODEC, ServerPayloadHandler::handleBid);
        registrar.playToServer(BuyBondPayload.TYPE, BuyBondPayload.STREAM_CODEC, ServerPayloadHandler::handleBuyBond);
        registrar.playToServer(RedeemBondPayload.TYPE, RedeemBondPayload.STREAM_CODEC, ServerPayloadHandler::handleRedeemBond);
        registrar.playToServer(PlaceSupplyOrderPayload.TYPE, PlaceSupplyOrderPayload.STREAM_CODEC, ServerPayloadHandler::handlePlaceSupplyOrder);

        // Server -> Client.
        registrar.playToClient(SyncShopDataPayload.TYPE, SyncShopDataPayload.STREAM_CODEC, ClientPayloadHandler::handleSyncShopData);
        registrar.playToClient(SyncBankAccountsPayload.TYPE, SyncBankAccountsPayload.STREAM_CODEC, ClientPayloadHandler::handleSyncBankAccounts);
        registrar.playToClient(SyncMarketOrdersPayload.TYPE, SyncMarketOrdersPayload.STREAM_CODEC, ClientPayloadHandler::handleSyncMarketOrders);
        registrar.playToClient(SyncCommodityPayload.TYPE, SyncCommodityPayload.STREAM_CODEC, ClientPayloadHandler::handleSyncCommodity);
        registrar.playToClient(SyncStocksPayload.TYPE, SyncStocksPayload.STREAM_CODEC, ClientPayloadHandler::handleSyncStocks);
        registrar.playToClient(SyncConglomeratePayload.TYPE, SyncConglomeratePayload.STREAM_CODEC, ClientPayloadHandler::handleSyncConglomerate);
        registrar.playToClient(SyncSecuritiesPayload.TYPE, SyncSecuritiesPayload.STREAM_CODEC, ClientPayloadHandler::handleSyncSecurities);
        registrar.playToClient(SyncStockOrdersPayload.TYPE, SyncStockOrdersPayload.STREAM_CODEC, ClientPayloadHandler::handleSyncStockOrders);
        registrar.playToClient(SyncWarehousePayload.TYPE, SyncWarehousePayload.STREAM_CODEC, ClientPayloadHandler::handleSyncWarehouse);
        registrar.playToClient(SyncFuturesPayload.TYPE, SyncFuturesPayload.STREAM_CODEC, ClientPayloadHandler::handleSyncFutures);
        registrar.playToClient(SyncAuctionsPayload.TYPE, SyncAuctionsPayload.STREAM_CODEC, ClientPayloadHandler::handleSyncAuctions);
        registrar.playToClient(SyncBondsPayload.TYPE, SyncBondsPayload.STREAM_CODEC, ClientPayloadHandler::handleSyncBonds);
        registrar.playToClient(SyncSupplyMarketPayload.TYPE, SyncSupplyMarketPayload.STREAM_CODEC, ClientPayloadHandler::handleSyncSupplyMarket);
    }
}
