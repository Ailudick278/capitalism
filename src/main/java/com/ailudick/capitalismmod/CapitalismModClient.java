package com.ailudick.capitalismmod;

import com.ailudick.capitalismmod.client.ConglomerateKeyMapping;
import com.ailudick.capitalismmod.init.ModMenuTypes;
import com.ailudick.capitalismmod.screen.AuctionHouseScreen;
import com.ailudick.capitalismmod.screen.BankScreen;
import com.ailudick.capitalismmod.screen.BondMarketScreen;
import com.ailudick.capitalismmod.screen.BusinessLicenseScreen;
import com.ailudick.capitalismmod.screen.CommodityExchangeScreen;
import com.ailudick.capitalismmod.screen.CompanyScreen;
import com.ailudick.capitalismmod.screen.FuturesExchangeScreen;
import com.ailudick.capitalismmod.screen.ConglomerateScreen;
import com.ailudick.capitalismmod.screen.SecuritiesCommissionScreen;
import com.ailudick.capitalismmod.screen.ShopScreen;
import com.ailudick.capitalismmod.screen.StockExchangeScreen;
import com.ailudick.capitalismmod.screen.TaxBureauScreen;
import com.ailudick.capitalismmod.screen.WarehouseScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = CapitalismMod.MODID, dist = Dist.CLIENT)
public class CapitalismModClient {
    public CapitalismModClient(IEventBus modEventBus, ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modEventBus.addListener(CapitalismModClient::registerMenuScreens);
        modEventBus.addListener(ConglomerateKeyMapping::register);
    }

    static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.SHOP_MENU.get(), ShopScreen::new);
        event.register(ModMenuTypes.BANK_MENU.get(), BankScreen::new);
        event.register(ModMenuTypes.BUSINESS_LICENSE_MENU.get(), BusinessLicenseScreen::new);
        event.register(ModMenuTypes.CONGLOMERATE_MENU.get(), ConglomerateScreen::new);
        event.register(ModMenuTypes.COMMODITY_EXCHANGE_MENU.get(), CommodityExchangeScreen::new);
        event.register(ModMenuTypes.STOCK_EXCHANGE_MENU.get(), StockExchangeScreen::new);
        event.register(ModMenuTypes.SECURITIES_COMMISSION_MENU.get(), SecuritiesCommissionScreen::new);
        event.register(ModMenuTypes.TAX_BUREAU_MENU.get(), TaxBureauScreen::new);
        event.register(ModMenuTypes.COMPANY_MENU.get(), CompanyScreen::new);
        event.register(ModMenuTypes.WAREHOUSE_MENU.get(), WarehouseScreen::new);
        event.register(ModMenuTypes.FUTURES_EXCHANGE_MENU.get(), FuturesExchangeScreen::new);
        event.register(ModMenuTypes.AUCTION_HOUSE_MENU.get(), AuctionHouseScreen::new);
        event.register(ModMenuTypes.BOND_MARKET_MENU.get(), BondMarketScreen::new);
    }
}
