package com.ailudick.capitalismmod.init;

import com.ailudick.capitalismmod.CapitalismMod;
import com.ailudick.capitalismmod.menu.AuctionHouseMenu;
import com.ailudick.capitalismmod.menu.BankMenu;
import com.ailudick.capitalismmod.menu.BondMarketMenu;
import com.ailudick.capitalismmod.menu.BusinessLicenseMenu;
import com.ailudick.capitalismmod.menu.CommodityExchangeMenu;
import com.ailudick.capitalismmod.menu.CompanyMenu;
import com.ailudick.capitalismmod.menu.ConglomerateMenu;
import com.ailudick.capitalismmod.menu.FuturesExchangeMenu;
import com.ailudick.capitalismmod.menu.SecuritiesCommissionMenu;
import com.ailudick.capitalismmod.menu.ShopMenu;
import com.ailudick.capitalismmod.menu.StockExchangeMenu;
import com.ailudick.capitalismmod.menu.TaxBureauMenu;
import com.ailudick.capitalismmod.menu.WarehouseMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, CapitalismMod.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<ShopMenu>> SHOP_MENU =
            MENUS.register("shop_menu", () -> new MenuType<>(ShopMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<BankMenu>> BANK_MENU =
            MENUS.register("bank_menu", () -> new MenuType<>(BankMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<BusinessLicenseMenu>> BUSINESS_LICENSE_MENU =
            MENUS.register("business_license_menu", () -> new MenuType<>(BusinessLicenseMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<ConglomerateMenu>> CONGLOMERATE_MENU =
            MENUS.register("conglomerate_menu", () -> new MenuType<>(ConglomerateMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<CommodityExchangeMenu>> COMMODITY_EXCHANGE_MENU =
            MENUS.register("commodity_exchange_menu", () -> new MenuType<>(CommodityExchangeMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<StockExchangeMenu>> STOCK_EXCHANGE_MENU =
            MENUS.register("stock_exchange_menu", () -> new MenuType<>(StockExchangeMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<SecuritiesCommissionMenu>> SECURITIES_COMMISSION_MENU =
            MENUS.register("securities_commission_menu", () -> new MenuType<>(SecuritiesCommissionMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<TaxBureauMenu>> TAX_BUREAU_MENU =
            MENUS.register("tax_bureau_menu", () -> new MenuType<>(TaxBureauMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<CompanyMenu>> COMPANY_MENU =
            MENUS.register("company_menu", () -> new MenuType<>(CompanyMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<WarehouseMenu>> WAREHOUSE_MENU =
            MENUS.register("warehouse_menu", () -> new MenuType<>(WarehouseMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<FuturesExchangeMenu>> FUTURES_EXCHANGE_MENU =
            MENUS.register("futures_exchange_menu", () -> new MenuType<>(FuturesExchangeMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<AuctionHouseMenu>> AUCTION_HOUSE_MENU =
            MENUS.register("auction_house_menu", () -> new MenuType<>(AuctionHouseMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<BondMarketMenu>> BOND_MARKET_MENU =
            MENUS.register("bond_market_menu", () -> new MenuType<>(BondMarketMenu::new, FeatureFlags.DEFAULT_FLAGS));
}
