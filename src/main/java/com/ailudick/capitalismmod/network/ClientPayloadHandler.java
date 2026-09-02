package com.ailudick.capitalismmod.network;

import com.ailudick.capitalismmod.menu.AuctionHouseMenu;
import com.ailudick.capitalismmod.menu.BankMenu;
import com.ailudick.capitalismmod.menu.BondMarketMenu;
import com.ailudick.capitalismmod.menu.ProcurementMenu;
import com.ailudick.capitalismmod.menu.CommodityExchangeMenu;
import com.ailudick.capitalismmod.menu.CompanyMenu;
import com.ailudick.capitalismmod.menu.FuturesExchangeMenu;
import com.ailudick.capitalismmod.menu.ConglomerateMenu;
import com.ailudick.capitalismmod.menu.SecuritiesCommissionMenu;
import com.ailudick.capitalismmod.menu.StockExchangeMenu;
import com.ailudick.capitalismmod.menu.TaxBureauMenu;
import com.ailudick.capitalismmod.menu.WarehouseMenu;
import com.ailudick.capitalismmod.network.payload.SyncAuctionsPayload;
import com.ailudick.capitalismmod.network.payload.SyncBankAccountsPayload;
import com.ailudick.capitalismmod.network.payload.SyncPersonalAssetsPayload;
import com.ailudick.capitalismmod.network.payload.SyncExchangeRatesPayload;
import com.ailudick.capitalismmod.currency.ExchangeRateProvider;
import com.ailudick.capitalismmod.network.payload.OperationResultPayload;
import com.ailudick.capitalismmod.client.OperationResultHandler;
import com.ailudick.capitalismmod.network.payload.SyncBondsPayload;
import com.ailudick.capitalismmod.network.payload.SyncSupplyMarketPayload;
import com.ailudick.capitalismmod.network.payload.SyncCommodityPayload;
import com.ailudick.capitalismmod.network.payload.SyncConglomeratePayload;
import com.ailudick.capitalismmod.network.payload.SyncFuturesPayload;
import com.ailudick.capitalismmod.network.payload.SyncMarketOrdersPayload;
import com.ailudick.capitalismmod.network.payload.SyncSecuritiesPayload;
import com.ailudick.capitalismmod.network.payload.SyncStockOrdersPayload;
import com.ailudick.capitalismmod.network.payload.SyncStocksPayload;
import com.ailudick.capitalismmod.network.payload.SyncWarehousePayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashSet;

/**
 * Client-side handlers for server -> client payloads.
 * Kept free of client-only imports so it can be referenced from common code.
 */
public class ClientPayloadHandler {

    public static void handleSyncBankAccounts(SyncBankAccountsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof BankMenu menu) {
                menu.setAccounts(payload.accounts());
            }
        });
    }

    public static void handleSyncPersonalAssets(SyncPersonalAssetsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof BankMenu menu) {
                menu.setPersonalAssets(payload.assets());
            }
        });
    }

    public static void handleSyncExchangeRates(SyncExchangeRatesPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ExchangeRateProvider.applySnapshot(payload.anchors(), payload.updatedAt(), payload.live()));
    }

    public static void handleOperationResult(OperationResultPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> OperationResultHandler.show(payload.success(), payload.message()));
    }

    public static void handleSyncMarketOrders(SyncMarketOrdersPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof CommodityExchangeMenu menu) {
                menu.setOrders(payload.orders());
            }
        });
    }

    public static void handleSyncCommodity(SyncCommodityPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof CommodityExchangeMenu menu) {
                menu.setPrices(payload.prices());
                menu.setHistory(payload.history());
            }
        });
    }

    public static void handleSyncStocks(SyncStocksPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof StockExchangeMenu menu) {
                menu.setPrices(payload.prices());
                menu.setPortfolio(payload.portfolio());
                menu.setHistory(payload.history());
                menu.setCompanies(payload.companies());
            }
        });
    }

    public static void handleSyncStockOrders(SyncStockOrdersPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof StockExchangeMenu menu) {
                menu.setOrders(payload.orders());
            }
        });
    }

    public static void handleSyncWarehouse(SyncWarehousePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof WarehouseMenu menu) {
                menu.setStorage(payload.storage());
            }
        });
    }

    public static void handleSyncFutures(SyncFuturesPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof FuturesExchangeMenu menu) {
                menu.setPrices(payload.prices());
                menu.setPositions(payload.positions());
                menu.setMarginBalance(payload.marginBalance());
                menu.setDaysToExpiry(payload.daysToExpiry());
            }
        });
    }

    public static void handleSyncAuctions(SyncAuctionsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof AuctionHouseMenu menu) {
                menu.setAuctions(payload.auctions());
            }
        });
    }

    public static void handleSyncBonds(SyncBondsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof BondMarketMenu menu) {
                menu.setHoldings(payload.holdings());
            }
        });
    }

    public static void handleSyncSupplyMarket(SyncSupplyMarketPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof ProcurementMenu menu) {
                menu.setOffers(payload.offers());
                menu.setOrders(payload.orders());
            }
        });
    }

    public static void handleSyncConglomerate(SyncConglomeratePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof ConglomerateMenu menu) {
                menu.setConglomerate(new com.ailudick.capitalismmod.company.Conglomerate(payload.name(), payload.companies()));
            } else if (context.player().containerMenu instanceof TaxBureauMenu menu) {
                menu.setCompanies(payload.companies());
            } else if (context.player().containerMenu instanceof CompanyMenu menu) {
                menu.setCompanies(payload.companies());
            }
        });
    }

    public static void handleSyncSecurities(SyncSecuritiesPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof SecuritiesCommissionMenu menu) {
                menu.setCompanies(payload.companies());
                menu.setListed(new HashSet<>(payload.listed()));
            }
        });
    }
}
