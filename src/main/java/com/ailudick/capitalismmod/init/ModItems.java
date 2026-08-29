package com.ailudick.capitalismmod.init;

import com.ailudick.capitalismmod.CapitalismMod;
import com.ailudick.capitalismmod.currency.Currencies;
import com.ailudick.capitalismmod.currency.CurrencyItem;
import com.ailudick.capitalismmod.item.BankCard;
import com.ailudick.capitalismmod.item.BusinessLicense;
import com.ailudick.capitalismmod.item.DebugStick;
import com.ailudick.capitalismmod.item.Invoice;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CapitalismMod.MODID);

    // Denomination items. Values are in minor units (1 major unit = 100 minor units):
    // US Dollar uses cents, Chinese Yuan uses fen, Euro uses cents, Ruble uses kopecks.
    // US Dollar
    public static final DeferredItem<CurrencyItem> USD_1C = ITEMS.register("usd_1c",
            () -> new CurrencyItem(Currencies.USD, 1, new Item.Properties().stacksTo(64)));
    public static final DeferredItem<CurrencyItem> USD_5C = ITEMS.register("usd_5c",
            () -> new CurrencyItem(Currencies.USD, 5, new Item.Properties().stacksTo(64)));
    public static final DeferredItem<CurrencyItem> USD_10C = ITEMS.register("usd_10c",
            () -> new CurrencyItem(Currencies.USD, 10, new Item.Properties().stacksTo(64)));
    public static final DeferredItem<CurrencyItem> USD_25C = ITEMS.register("usd_25c",
            () -> new CurrencyItem(Currencies.USD, 25, new Item.Properties().stacksTo(64)));
    public static final DeferredItem<CurrencyItem> USD_50C = ITEMS.register("usd_50c",
            () -> new CurrencyItem(Currencies.USD, 50, new Item.Properties().stacksTo(64)));
    public static final DeferredItem<CurrencyItem> USD_1 = ITEMS.register("usd_1",
            () -> new CurrencyItem(Currencies.USD, 100, new Item.Properties().stacksTo(64)));
    public static final DeferredItem<CurrencyItem> USD_5 = ITEMS.register("usd_5",
            () -> new CurrencyItem(Currencies.USD, 500, new Item.Properties().stacksTo(64)));
    public static final DeferredItem<CurrencyItem> USD_10 = ITEMS.register("usd_10",
            () -> new CurrencyItem(Currencies.USD, 1000, new Item.Properties().stacksTo(64)));
    public static final DeferredItem<CurrencyItem> USD_20 = ITEMS.register("usd_20",
            () -> new CurrencyItem(Currencies.USD, 2000, new Item.Properties().stacksTo(64)));
    public static final DeferredItem<CurrencyItem> USD_50 = ITEMS.register("usd_50",
            () -> new CurrencyItem(Currencies.USD, 5000, new Item.Properties().stacksTo(64)));
    public static final DeferredItem<CurrencyItem> USD_100 = ITEMS.register("usd_100",
            () -> new CurrencyItem(Currencies.USD, 10000, new Item.Properties().stacksTo(64)));

    // Chinese Yuan
    public static final DeferredItem<CurrencyItem> CNY_1F = ITEMS.register("cny_1f",
            () -> new CurrencyItem(Currencies.CNY, 1, new Item.Properties().stacksTo(64)));
    public static final DeferredItem<CurrencyItem> CNY_5F = ITEMS.register("cny_5f",
            () -> new CurrencyItem(Currencies.CNY, 5, new Item.Properties().stacksTo(64)));
    public static final DeferredItem<CurrencyItem> CNY_1J = ITEMS.register("cny_1j",
            () -> new CurrencyItem(Currencies.CNY, 10, new Item.Properties().stacksTo(64)));
    public static final DeferredItem<CurrencyItem> CNY_5J = ITEMS.register("cny_5j",
            () -> new CurrencyItem(Currencies.CNY, 50, new Item.Properties().stacksTo(64)));
    public static final DeferredItem<CurrencyItem> CNY_1 = ITEMS.register("cny_1",
            () -> new CurrencyItem(Currencies.CNY, 100, new Item.Properties().stacksTo(64)));
    public static final DeferredItem<CurrencyItem> CNY_5 = ITEMS.register("cny_5",
            () -> new CurrencyItem(Currencies.CNY, 500, new Item.Properties().stacksTo(64)));
    public static final DeferredItem<CurrencyItem> CNY_10 = ITEMS.register("cny_10",
            () -> new CurrencyItem(Currencies.CNY, 1000, new Item.Properties().stacksTo(64)));
    public static final DeferredItem<CurrencyItem> CNY_20 = ITEMS.register("cny_20",
            () -> new CurrencyItem(Currencies.CNY, 2000, new Item.Properties().stacksTo(64)));
    public static final DeferredItem<CurrencyItem> CNY_50 = ITEMS.register("cny_50",
            () -> new CurrencyItem(Currencies.CNY, 5000, new Item.Properties().stacksTo(64)));
    public static final DeferredItem<CurrencyItem> CNY_100 = ITEMS.register("cny_100",
            () -> new CurrencyItem(Currencies.CNY, 10000, new Item.Properties().stacksTo(64)));

    // Euro
    public static final DeferredItem<CurrencyItem> EUR_1C = ITEMS.register("eur_1c",
            () -> new CurrencyItem(Currencies.EUR, 1, new Item.Properties().stacksTo(64)));
    public static final DeferredItem<CurrencyItem> EUR_2C = ITEMS.register("eur_2c",
            () -> new CurrencyItem(Currencies.EUR, 2, new Item.Properties().stacksTo(64)));
    public static final DeferredItem<CurrencyItem> EUR_5C = ITEMS.register("eur_5c",
            () -> new CurrencyItem(Currencies.EUR, 5, new Item.Properties().stacksTo(64)));
    public static final DeferredItem<CurrencyItem> EUR_10C = ITEMS.register("eur_10c",
            () -> new CurrencyItem(Currencies.EUR, 10, new Item.Properties().stacksTo(64)));
    public static final DeferredItem<CurrencyItem> EUR_20C = ITEMS.register("eur_20c",
            () -> new CurrencyItem(Currencies.EUR, 20, new Item.Properties().stacksTo(64)));
    public static final DeferredItem<CurrencyItem> EUR_50C = ITEMS.register("eur_50c",
            () -> new CurrencyItem(Currencies.EUR, 50, new Item.Properties().stacksTo(64)));
    public static final DeferredItem<CurrencyItem> EUR_1 = ITEMS.register("eur_1",
            () -> new CurrencyItem(Currencies.EUR, 100, new Item.Properties().stacksTo(64)));
    public static final DeferredItem<CurrencyItem> EUR_2 = ITEMS.register("eur_2",
            () -> new CurrencyItem(Currencies.EUR, 200, new Item.Properties().stacksTo(64)));
    public static final DeferredItem<CurrencyItem> EUR_5 = ITEMS.register("eur_5",
            () -> new CurrencyItem(Currencies.EUR, 500, new Item.Properties().stacksTo(64)));
    public static final DeferredItem<CurrencyItem> EUR_10 = ITEMS.register("eur_10",
            () -> new CurrencyItem(Currencies.EUR, 1000, new Item.Properties().stacksTo(64)));
    public static final DeferredItem<CurrencyItem> EUR_20 = ITEMS.register("eur_20",
            () -> new CurrencyItem(Currencies.EUR, 2000, new Item.Properties().stacksTo(64)));
    public static final DeferredItem<CurrencyItem> EUR_50 = ITEMS.register("eur_50",
            () -> new CurrencyItem(Currencies.EUR, 5000, new Item.Properties().stacksTo(64)));
    public static final DeferredItem<CurrencyItem> EUR_100 = ITEMS.register("eur_100",
            () -> new CurrencyItem(Currencies.EUR, 10000, new Item.Properties().stacksTo(64)));

    // Russian Ruble
    public static final DeferredItem<CurrencyItem> RUB_1K = ITEMS.register("rub_1k",
            () -> new CurrencyItem(Currencies.RUB, 1, new Item.Properties().stacksTo(64)));
    public static final DeferredItem<CurrencyItem> RUB_5K = ITEMS.register("rub_5k",
            () -> new CurrencyItem(Currencies.RUB, 5, new Item.Properties().stacksTo(64)));
    public static final DeferredItem<CurrencyItem> RUB_10K = ITEMS.register("rub_10k",
            () -> new CurrencyItem(Currencies.RUB, 10, new Item.Properties().stacksTo(64)));
    public static final DeferredItem<CurrencyItem> RUB_50K = ITEMS.register("rub_50k",
            () -> new CurrencyItem(Currencies.RUB, 50, new Item.Properties().stacksTo(64)));
    public static final DeferredItem<CurrencyItem> RUB_1 = ITEMS.register("rub_1",
            () -> new CurrencyItem(Currencies.RUB, 100, new Item.Properties().stacksTo(64)));
    public static final DeferredItem<CurrencyItem> RUB_2 = ITEMS.register("rub_2",
            () -> new CurrencyItem(Currencies.RUB, 200, new Item.Properties().stacksTo(64)));
    public static final DeferredItem<CurrencyItem> RUB_5 = ITEMS.register("rub_5",
            () -> new CurrencyItem(Currencies.RUB, 500, new Item.Properties().stacksTo(64)));
    public static final DeferredItem<CurrencyItem> RUB_10 = ITEMS.register("rub_10",
            () -> new CurrencyItem(Currencies.RUB, 1000, new Item.Properties().stacksTo(64)));
    public static final DeferredItem<CurrencyItem> RUB_50 = ITEMS.register("rub_50",
            () -> new CurrencyItem(Currencies.RUB, 5000, new Item.Properties().stacksTo(64)));
    public static final DeferredItem<CurrencyItem> RUB_100 = ITEMS.register("rub_100",
            () -> new CurrencyItem(Currencies.RUB, 10000, new Item.Properties().stacksTo(64)));
    public static final DeferredItem<CurrencyItem> RUB_200 = ITEMS.register("rub_200",
            () -> new CurrencyItem(Currencies.RUB, 20000, new Item.Properties().stacksTo(64)));
    public static final DeferredItem<CurrencyItem> RUB_500 = ITEMS.register("rub_500",
            () -> new CurrencyItem(Currencies.RUB, 50000, new Item.Properties().stacksTo(64)));
    public static final DeferredItem<CurrencyItem> RUB_1000 = ITEMS.register("rub_1000",
            () -> new CurrencyItem(Currencies.RUB, 100000, new Item.Properties().stacksTo(64)));

    // Bank cards.
    public static final DeferredItem<BankCard> DEBIT_CARD = ITEMS.register("debit_card",
            () -> new BankCard(false, new Item.Properties().stacksTo(16)));
    public static final DeferredItem<BankCard> CREDIT_CARD = ITEMS.register("credit_card",
            () -> new BankCard(true, new Item.Properties().stacksTo(16)));

    // Block items.
    public static final DeferredItem<BlockItem> SHOP = ITEMS.register("shop",
            () -> new BlockItem(ModBlocks.SHOP_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> BANK = ITEMS.register("bank",
            () -> new BlockItem(ModBlocks.BANK_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> BUSINESS_BUREAU = ITEMS.register("business_bureau",
            () -> new BlockItem(ModBlocks.BUSINESS_BUREAU_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> COMMODITY_EXCHANGE = ITEMS.register("commodity_exchange",
            () -> new BlockItem(ModBlocks.COMMODITY_EXCHANGE_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> STOCK_EXCHANGE = ITEMS.register("stock_exchange",
            () -> new BlockItem(ModBlocks.STOCK_EXCHANGE_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> SECURITIES_COMMISSION = ITEMS.register("securities_commission",
            () -> new BlockItem(ModBlocks.SECURITIES_COMMISSION_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> TAX_BUREAU = ITEMS.register("tax_bureau",
            () -> new BlockItem(ModBlocks.TAX_BUREAU_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> COMPANY = ITEMS.register("company",
            () -> new BlockItem(ModBlocks.COMPANY_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> WAREHOUSE = ITEMS.register("warehouse",
            () -> new BlockItem(ModBlocks.WAREHOUSE_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> FUTURES_EXCHANGE = ITEMS.register("futures_exchange",
            () -> new BlockItem(ModBlocks.FUTURES_EXCHANGE_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> AUCTION_HOUSE = ITEMS.register("auction_house",
            () -> new BlockItem(ModBlocks.AUCTION_HOUSE_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> BOND_MARKET = ITEMS.register("bond_market",
            () -> new BlockItem(ModBlocks.BOND_MARKET_BLOCK.get(), new Item.Properties()));

    // Business license.
    public static final DeferredItem<BusinessLicense> BUSINESS_LICENSE = ITEMS.register("business_license",
            () -> new BusinessLicense(new Item.Properties().stacksTo(1)));

    // Debug stick (uses vanilla stick texture via model parent).
    public static final DeferredItem<DebugStick> DEBUG_STICK = ITEMS.register("debug_stick",
            () -> new DebugStick(new Item.Properties().stacksTo(1)));

    // Invoice (发票), issued on shop purchases and reimbursable at the tax bureau.
    public static final DeferredItem<Invoice> INVOICE = ITEMS.register("invoice",
            () -> new Invoice(new Item.Properties().stacksTo(64)));
}
