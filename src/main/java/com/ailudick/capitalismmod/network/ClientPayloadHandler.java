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
import com.ailudick.capitalismmod.screen.WarehouseScreen;
import com.ailudick.capitalismmod.menu.LandMenu;
import com.ailudick.capitalismmod.menu.WorldMapMenu;
import net.minecraft.network.chat.Component;
import com.ailudick.capitalismmod.network.payload.SyncWorldMapPayload;
import com.ailudick.capitalismmod.network.payload.SyncWorldMapTilesPayload;
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
import com.ailudick.capitalismmod.network.payload.SyncTaxBillsPayload;
import com.ailudick.capitalismmod.network.payload.SyncTaxCorrectionRequestsPayload;
import com.ailudick.capitalismmod.network.payload.SyncFuturesPayload;
import com.ailudick.capitalismmod.network.payload.SyncMarketOrdersPayload;
import com.ailudick.capitalismmod.network.payload.SyncSecuritiesPayload;
import com.ailudick.capitalismmod.network.payload.SyncStockOrdersPayload;
import com.ailudick.capitalismmod.network.payload.SyncStocksPayload;
import com.ailudick.capitalismmod.network.payload.SyncWarehousePayload;
import com.ailudick.capitalismmod.network.payload.SyncLandPayload;
import com.ailudick.capitalismmod.network.payload.SyncOwnedLandsPayload;
import com.ailudick.capitalismmod.network.payload.SyncLandLogsPayload;
import com.ailudick.capitalismmod.network.payload.SyncLandOverlayPayload;
import com.ailudick.capitalismmod.network.payload.SyncLandPermissionsPayload;
import com.ailudick.capitalismmod.network.payload.SyncLandSalePayload;
import com.ailudick.capitalismmod.network.payload.SyncLandOwnershipPayload;
import com.ailudick.capitalismmod.network.payload.SyncLandTrustsPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashSet;

/**
 * Client-side handlers for server -> client payloads.
 * Kept free of client-only imports so it can be referenced from common code.
 */
public class ClientPayloadHandler {

    public static void handleSyncLand(SyncLandPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof LandMenu menu) {
                menu.setData(payload);
            }
        });
    }

    public static void handleSyncOwnedLands(SyncOwnedLandsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof LandMenu menu) menu.setOwnedLands(payload.lands());
        });
    }

    public static void handleSyncLandLogs(SyncLandLogsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof LandMenu menu) menu.setLandLogs(payload.logs());
        });
    }

    public static void handleSyncWorldMap(SyncWorldMapPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof WorldMapMenu menu) {
                menu.dimension = payload.dimension();
                menu.chunkX = payload.chunkX();
                menu.chunkZ = payload.chunkZ();
            }
        });
    }

    public static void handleSyncWorldMapTiles(SyncWorldMapTilesPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof WorldMapMenu menu) {
                menu.setTiles(payload);
            } else if (context.player().containerMenu instanceof LandMenu menu) {
                menu.setTiles(payload);
            }
        });
    }

    public static void handleSyncLandOverlay(SyncLandOverlayPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof WorldMapMenu menu) menu.setLandOverlay(payload);
        });
    }

    public static void handleSyncLandPermissions(SyncLandPermissionsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof LandMenu menu
                    && menu.id.equals(payload.claimId())) {
                menu.memberBuild = payload.memberBuild();
                menu.memberInteract = payload.memberInteract();
                menu.memberContainer = payload.container();
                menu.memberRedstone = payload.redstone();
            }
        });
    }

    public static void handleSyncLandSale(SyncLandSalePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof LandMenu menu) {
                String oldBidder = menu.auctionBidder;
                long oldBid = menu.auctionHighestBid;
                menu.saleActive = payload.active(); menu.saleTarget = payload.target();
                menu.salePrice = payload.price(); menu.saleExpiresAt = payload.expiresAt();
                menu.auctionActive = payload.auctionActive(); menu.auctionStartPrice = payload.auctionStartPrice();
                menu.auctionHighestBid = payload.auctionHighestBid(); menu.auctionBidder = payload.auctionBidder();
                menu.auctionEndsAt = payload.auctionEndsAt();
                if (context.player().getGameProfile().getName().equals(oldBidder)
                        && !context.player().getGameProfile().getName().equals(payload.auctionBidder())
                        && payload.auctionHighestBid() > oldBid) {
                    context.player().displayClientMessage(
                            Component.literal("你的土地竞价已被超过，当前最高价：" + payload.auctionHighestBid()), true);
                }
            }
        });
    }

    public static void handleSyncLandOwnership(SyncLandOwnershipPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof LandMenu menu) menu.setOwnershipHistory(payload.owners());
        });
    }

    public static void handleSyncLandTrusts(SyncLandTrustsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof LandMenu menu) {
                menu.setTrustedPlayers(payload.players());
            }
        });
    }

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
                menu.setOwnerKey(payload.ownerKey());
                menu.setOwners(payload.owners());
                if (net.minecraft.client.Minecraft.getInstance().screen instanceof WarehouseScreen screen) {
                    screen.rebuildOwnerButtons();
                }
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
                menu.setCompanyNames(payload.companyNames());
            }
        });
    }

    public static void handleSyncConglomerate(SyncConglomeratePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof ConglomerateMenu menu) {
                menu.setData(payload.name(), payload.companies());
            } else if (context.player().containerMenu instanceof TaxBureauMenu menu) {
                menu.setCompanies(payload.companies());
            } else if (context.player().containerMenu instanceof CompanyMenu menu) {
                menu.setCompanies(payload.companies());
            }
        });
    }

    public static void handleSyncTaxBills(SyncTaxBillsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof TaxBureauMenu menu) {
                menu.setTaxBills(payload.bills());
                  menu.setTaxPayments(payload.payments());
                  menu.setCreditBalance(payload.creditBalance());
                  menu.setRefunds(payload.refunds());
                  menu.setRefundAudit(payload.refundAudit());
                  menu.setRefundNotifications(payload.notifications());
                  menu.setAnnualReport(payload.annualReport());
                  menu.setIndividualPeriods(payload.individualPeriods());
                  menu.setIndividualExpenses(payload.individualExpenses());
                  menu.setIndividualIncomes(payload.individualIncomes());
                  menu.setCorrectionAudits(payload.correctionAudits());
            }
        });
    }

    public static void handleSyncTaxCorrectionRequests(SyncTaxCorrectionRequestsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof TaxBureauMenu menu) menu.setCorrectionRequests(payload.requests());
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
