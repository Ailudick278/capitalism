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
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CapitalismMod.MODID);
    public static final DeferredItem<BlockItem> GAS_CYLINDER = ITEMS.register("gas_cylinder",
            () -> new BlockItem(ModBlocks.GAS_CYLINDER_BLOCK.get(), new Item.Properties()));

    // Denomination items. Values are in minor units (1 major unit = 100 minor units):
    // US Dollar uses cents, Chinese Yuan uses fen, Euro uses cents, Ruble uses kopecks.
    // US Dollar
    public static final DeferredItem<CurrencyItem> USD_1C = ITEMS.register("usd_1c",
            () -> new CurrencyItem(Currencies.USD, 1, CurrencyItem.Form.COIN, "停止普通生产", new Item.Properties().stacksTo(64)));
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
    public static final DeferredItem<CurrencyItem> USD_2 = ITEMS.register("usd_2",
            () -> new CurrencyItem(Currencies.USD, 200, new Item.Properties().stacksTo(64)));
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
            () -> new CurrencyItem(Currencies.CNY, 1, CurrencyItem.Form.COIN, "历史/少见", new Item.Properties().stacksTo(64)));
    public static final DeferredItem<CurrencyItem> CNY_5F = ITEMS.register("cny_5f",
            () -> new CurrencyItem(Currencies.CNY, 5, CurrencyItem.Form.COIN, "历史/少见", new Item.Properties().stacksTo(64)));
    public static final DeferredItem<CurrencyItem> CNY_1J = ITEMS.register("cny_1j",
            () -> new CurrencyItem(Currencies.CNY, 10, new Item.Properties().stacksTo(64)));
    public static final DeferredItem<CurrencyItem> CNY_5J = ITEMS.register("cny_5j",
            () -> new CurrencyItem(Currencies.CNY, 50, new Item.Properties().stacksTo(64)));
    public static final DeferredItem<CurrencyItem> CNY_1 = ITEMS.register("cny_1",
            () -> new CurrencyItem(Currencies.CNY, 100, new Item.Properties().stacksTo(64)));
    public static final DeferredItem<CurrencyItem> CNY_1_COIN = ITEMS.register("cny_1_coin",
            () -> new CurrencyItem(Currencies.CNY, 100, CurrencyItem.Form.COIN, "current", new Item.Properties().stacksTo(64)));
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
            () -> new CurrencyItem(Currencies.EUR, 100, CurrencyItem.Form.COIN, "current", new Item.Properties().stacksTo(64)));
    public static final DeferredItem<CurrencyItem> EUR_2 = ITEMS.register("eur_2",
            () -> new CurrencyItem(Currencies.EUR, 200, CurrencyItem.Form.COIN, "current", new Item.Properties().stacksTo(64)));
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
    public static final DeferredItem<CurrencyItem> EUR_200 = ITEMS.register("eur_200",
            () -> new CurrencyItem(Currencies.EUR, 20000, new Item.Properties().stacksTo(64)));
    public static final DeferredItem<CurrencyItem> EUR_500 = ITEMS.register("eur_500",
            () -> new CurrencyItem(Currencies.EUR, 50000, CurrencyItem.Form.BANKNOTE, "停止生产", new Item.Properties().stacksTo(64)));

    // Russian Ruble
    public static final DeferredItem<CurrencyItem> RUB_1K = ITEMS.register("rub_1k",
            () -> new CurrencyItem(Currencies.RUB, 1, CurrencyItem.Form.COIN, "current", new Item.Properties().stacksTo(64)));
    public static final DeferredItem<CurrencyItem> RUB_5K = ITEMS.register("rub_5k",
            () -> new CurrencyItem(Currencies.RUB, 5, CurrencyItem.Form.COIN, "current", new Item.Properties().stacksTo(64)));
    public static final DeferredItem<CurrencyItem> RUB_10K = ITEMS.register("rub_10k",
            () -> new CurrencyItem(Currencies.RUB, 10, CurrencyItem.Form.COIN, "current", new Item.Properties().stacksTo(64)));
    public static final DeferredItem<CurrencyItem> RUB_50K = ITEMS.register("rub_50k",
            () -> new CurrencyItem(Currencies.RUB, 50, CurrencyItem.Form.COIN, "current", new Item.Properties().stacksTo(64)));
    public static final DeferredItem<CurrencyItem> RUB_1 = ITEMS.register("rub_1",
            () -> new CurrencyItem(Currencies.RUB, 100, CurrencyItem.Form.COIN, "current", new Item.Properties().stacksTo(64)));
    public static final DeferredItem<CurrencyItem> RUB_2 = ITEMS.register("rub_2",
            () -> new CurrencyItem(Currencies.RUB, 200, CurrencyItem.Form.COIN, "current", new Item.Properties().stacksTo(64)));
    public static final DeferredItem<CurrencyItem> RUB_5 = ITEMS.register("rub_5",
            () -> new CurrencyItem(Currencies.RUB, 500, CurrencyItem.Form.COIN, "current", new Item.Properties().stacksTo(64)));
    public static final DeferredItem<CurrencyItem> RUB_10 = ITEMS.register("rub_10",
            () -> new CurrencyItem(Currencies.RUB, 1000, CurrencyItem.Form.COIN, "current", new Item.Properties().stacksTo(64)));
    public static final DeferredItem<CurrencyItem> RUB_5_NOTE = ITEMS.register("rub_5_note",
            () -> new CurrencyItem(Currencies.RUB, 500, CurrencyItem.Form.BANKNOTE, "少见", new Item.Properties().stacksTo(64)));
    public static final DeferredItem<CurrencyItem> RUB_10_NOTE = ITEMS.register("rub_10_note",
            () -> new CurrencyItem(Currencies.RUB, 1000, CurrencyItem.Form.BANKNOTE, "少见", new Item.Properties().stacksTo(64)));
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
    public static final DeferredItem<CurrencyItem> RUB_2000 = ITEMS.register("rub_2000",
            () -> new CurrencyItem(Currencies.RUB, 200000, new Item.Properties().stacksTo(64)));
    public static final DeferredItem<CurrencyItem> RUB_5000 = ITEMS.register("rub_5000",
            () -> new CurrencyItem(Currencies.RUB, 500000, new Item.Properties().stacksTo(64)));

    // Bank cards.
    public static final DeferredItem<BankCard> DEBIT_CARD = ITEMS.register("debit_card",
            () -> new BankCard(false, new Item.Properties().stacksTo(16)));
    public static final DeferredItem<BankCard> CREDIT_CARD = ITEMS.register("credit_card",
            () -> new BankCard(true, new Item.Properties().stacksTo(16)));

    // Block items.
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
    public static final DeferredItem<BlockItem> FACTORY = ITEMS.register("factory",
            () -> new BlockItem(ModBlocks.FACTORY_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> FACTORY_INPUT_PORT = ITEMS.register("factory_input_port",
            () -> new BlockItem(ModBlocks.FACTORY_INPUT_PORT_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> FACTORY_OUTPUT_PORT = ITEMS.register("factory_output_port",
            () -> new BlockItem(ModBlocks.FACTORY_OUTPUT_PORT_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> FACTORY_MACHINE = ITEMS.register("factory_machine",
            () -> new BlockItem(ModBlocks.FACTORY_MACHINE_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> WAREHOUSE = ITEMS.register("warehouse",
            () -> new BlockItem(ModBlocks.WAREHOUSE_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> FUTURES_EXCHANGE = ITEMS.register("futures_exchange",
            () -> new BlockItem(ModBlocks.FUTURES_EXCHANGE_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> AUCTION_HOUSE = ITEMS.register("auction_house",
            () -> new BlockItem(ModBlocks.AUCTION_HOUSE_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> BOND_MARKET = ITEMS.register("bond_market",
            () -> new BlockItem(ModBlocks.BOND_MARKET_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> PROCUREMENT = ITEMS.register("procurement",
            () -> new BlockItem(ModBlocks.PROCUREMENT_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> LOGISTICS_CENTER = ITEMS.register("logistics_center",
            () -> new BlockItem(ModBlocks.LOGISTICS_CENTER_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> TRANSFER_STATION = ITEMS.register("transfer_station",
            () -> new BlockItem(ModBlocks.TRANSFER_STATION_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> PORT = ITEMS.register("port",
            () -> new BlockItem(ModBlocks.PORT_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> MAILBOX = ITEMS.register("mailbox",
            () -> new BlockItem(ModBlocks.MAILBOX_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> INDIVIDUAL_BUSINESS = ITEMS.register("individual_business",
            () -> new BlockItem(ModBlocks.INDIVIDUAL_BUSINESS_BLOCK.get(), new Item.Properties()));

    // Business license.
    public static final DeferredItem<BusinessLicense> BUSINESS_LICENSE = ITEMS.register("business_license",
            () -> new BusinessLicense(new Item.Properties().stacksTo(1)));

    // Debug stick (uses vanilla stick texture via model parent).
    public static final DeferredItem<DebugStick> DEBUG_STICK = ITEMS.register("debug_stick",
            () -> new DebugStick(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<SpawnEggItem> PLACEHOLDER_NPC_SPAWN_EGG = ITEMS.register("placeholder_npc_spawn_egg",
            () -> new SpawnEggItem(ModEntities.PLACEHOLDER_NPC.get(), 0x1D3557, 0xE9C46A, new Item.Properties()));

    // Invoice (发票), issued on shop purchases and reserved for future tax workflows.
    public static final DeferredItem<Invoice> INVOICE = ITEMS.register("invoice",
            () -> new Invoice(new Item.Properties().stacksTo(64)));

    // Industrial chain materials and consumer goods.
    public static final DeferredItem<Item> STEEL_SHEET = ITEMS.register("steel_sheet",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> COPPER_WIRE = ITEMS.register("copper_wire",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> GLASS_LENS = ITEMS.register("glass_lens",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> ELECTRIC_LAMP = ITEMS.register("electric_lamp",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> FLOUR = ITEMS.register("flour",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> METAL_CAN = ITEMS.register("metal_can",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> CANNED_FOOD = ITEMS.register("canned_food",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> MACHINE_FRAME = ITEMS.register("machine_frame",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> ELECTRIC_MOTOR = ITEMS.register("electric_motor",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> GREEN_LUMBER = ITEMS.register("green_lumber",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> DRIED_LUMBER = ITEMS.register("dried_lumber",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> WOODEN_CRATE = ITEMS.register("wooden_crate",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> FURNITURE = ITEMS.register("furniture",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> CRUDE_OIL = ITEMS.register("crude_oil",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> NAPHTHA = ITEMS.register("naphtha",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> ETHYLENE = ITEMS.register("ethylene",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> PROPYLENE = ITEMS.register("propylene",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> FUEL_OIL = ITEMS.register("fuel_oil",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> DIESEL = ITEMS.register("diesel",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> GASOLINE = ITEMS.register("gasoline",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> LPG = ITEMS.register("lpg",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> BASE_OIL = ITEMS.register("base_oil",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> LUBRICANT = ITEMS.register("lubricant",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> ASPHALT = ITEMS.register("asphalt",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> PLASTIC_PELLETS = ITEMS.register("plastic_pellets",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> POLYETHYLENE_PELLETS = ITEMS.register("polyethylene_pellets",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> POLYPROPYLENE_PELLETS = ITEMS.register("polypropylene_pellets",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> SYNTHETIC_RUBBER = ITEMS.register("synthetic_rubber",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> ETHYLENE_GLYCOL = ITEMS.register("ethylene_glycol",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> EPOXY_RESIN = ITEMS.register("epoxy_resin",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> BENZENE = ITEMS.register("benzene",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> STYRENE_MONOMER = ITEMS.register("styrene_monomer",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> ABS_RESIN = ITEMS.register("abs_resin",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> P_XYLENE = ITEMS.register("p_xylene",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> TEREPHTHALIC_ACID = ITEMS.register("terephthalic_acid",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> PET_RESIN = ITEMS.register("pet_resin",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> PET_BOTTLE = ITEMS.register("pet_bottle",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> RECYCLED_PET_FLAKES = ITEMS.register("recycled_pet_flakes",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> FAN_BLADES = ITEMS.register("fan_blades",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> FAN_CONTROL = ITEMS.register("fan_control",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> POLYSILICON = ITEMS.register("polysilicon",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> SILICON_WAFER = ITEMS.register("silicon_wafer",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> SILICON_DIE = ITEMS.register("silicon_die",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> TESTED_DIE = ITEMS.register("tested_die",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> LEAD_FRAME = ITEMS.register("lead_frame",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> MOLD_COMPOUND = ITEMS.register("mold_compound",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> PACKAGED_CHIP = ITEMS.register("packaged_chip",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> CIRCUIT_BOARD = ITEMS.register("circuit_board",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> PCB_SUBSTRATE = ITEMS.register("pcb_substrate",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> DRILLED_PCB_PANEL = ITEMS.register("drilled_pcb_panel",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> ETCHED_PCB = ITEMS.register("etched_pcb",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> SOLDER_MASKED_PCB = ITEMS.register("solder_masked_pcb",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> COPPER_FOIL = ITEMS.register("copper_foil",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> SOLDER = ITEMS.register("solder",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> SMD_COMPONENTS = ITEMS.register("smd_components",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> PASSIVE_COMPONENTS = ITEMS.register("passive_components",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> POWER_MANAGEMENT_IC = ITEMS.register("power_management_ic",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> DISPLAY_DRIVER = ITEMS.register("display_driver",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> DISPLAY_PANEL = ITEMS.register("display_panel",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> DISPLAY_GLASS_SUBSTRATE = ITEMS.register("display_glass_substrate",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> BACKLIGHT_MODULE = ITEMS.register("backlight_module",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> PHONE_CASING = ITEMS.register("phone_casing",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> CAMERA_MODULE = ITEMS.register("camera_module",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> SPEAKER_MODULE = ITEMS.register("speaker_module",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> MICROPHONE_MODULE = ITEMS.register("microphone_module",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> CHARGING_PORT = ITEMS.register("charging_port",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> KEYBOARD_MODULE = ITEMS.register("keyboard_module",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> STORAGE_MODULE = ITEMS.register("storage_module",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> WIRELESS_MODULE = ITEMS.register("wireless_module",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> BATTERY_CELL = ITEMS.register("battery_cell",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> LITHIUM_MINERAL = ITEMS.register("lithium_mineral",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> LITHIUM_CARBONATE = ITEMS.register("lithium_carbonate",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> GRAPHITE_ANODE = ITEMS.register("graphite_anode",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> CATHODE_ACTIVE_MATERIAL = ITEMS.register("cathode_active_material",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> BATTERY_SEPARATOR = ITEMS.register("battery_separator",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> BATTERY_ELECTROLYTE = ITEMS.register("battery_electrolyte",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> BATTERY_PACK = ITEMS.register("battery_pack",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> BLACK_MASS = ITEMS.register("black_mass",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> BATTERY = ITEMS.register("battery",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> SMARTPHONE = ITEMS.register("smartphone",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> ELECTRIC_FAN = ITEMS.register("electric_fan",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> POWER_ADAPTER = ITEMS.register("power_adapter",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> TELEVISION = ITEMS.register("television",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> LAPTOP = ITEMS.register("laptop",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> WIRELESS_ROUTER = ITEMS.register("wireless_router",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> POWER_BANK = ITEMS.register("power_bank",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> SMART_SPEAKER = ITEMS.register("smart_speaker",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> REFURBISHED_SMARTPHONE = ITEMS.register("refurbished_smartphone",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> REFURBISHED_TELEVISION = ITEMS.register("refurbished_television",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> REFURBISHED_LAPTOP = ITEMS.register("refurbished_laptop",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> REFURBISHED_WIRELESS_ROUTER = ITEMS.register("refurbished_wireless_router",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> PLASTIC_CONTAINER = ITEMS.register("plastic_container",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> PACKAGING_FILM = ITEMS.register("packaging_film",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> REFRACTORY_BRICK = ITEMS.register("refractory_brick",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> INDUSTRIAL_COIL = ITEMS.register("industrial_coil",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> PRESSURE_PUMP = ITEMS.register("pressure_pump",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> CONTROL_PANEL = ITEMS.register("control_panel",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> MACHINE_CASING = ITEMS.register("machine_casing",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<BlockItem> ALUMINUM_ORE = ITEMS.register("aluminum_ore",
            () -> new BlockItem(ModBlocks.ALUMINUM_ORE_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> DEEPSLATE_ALUMINUM_ORE = ITEMS.register("deepslate_aluminum_ore",
            () -> new BlockItem(ModBlocks.DEEPSLATE_ALUMINUM_ORE_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> SULFUR_ORE = ITEMS.register("sulfur_ore",
            () -> new BlockItem(ModBlocks.SULFUR_ORE_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> DEEPSLATE_SULFUR_ORE = ITEMS.register("deepslate_sulfur_ore",
            () -> new BlockItem(ModBlocks.DEEPSLATE_SULFUR_ORE_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<Item> RAW_ALUMINUM = ITEMS.register("raw_aluminum",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> ALUMINUM_INGOT = ITEMS.register("aluminum_ingot",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> SULFUR = ITEMS.register("sulfur",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<BlockItem> BAUXITE_ORE = ITEMS.register("bauxite_ore", () -> new BlockItem(ModBlocks.BAUXITE_ORE_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> DEEPSLATE_BAUXITE_ORE = ITEMS.register("deepslate_bauxite_ore", () -> new BlockItem(ModBlocks.DEEPSLATE_BAUXITE_ORE_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> LIMESTONE_ORE = ITEMS.register("limestone_ore", () -> new BlockItem(ModBlocks.LIMESTONE_ORE_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> PHOSPHATE_ORE = ITEMS.register("phosphate_ore", () -> new BlockItem(ModBlocks.PHOSPHATE_ORE_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> POTASH_ORE = ITEMS.register("potash_ore", () -> new BlockItem(ModBlocks.POTASH_ORE_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> NICKEL_ORE = ITEMS.register("nickel_ore", () -> new BlockItem(ModBlocks.NICKEL_ORE_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> DEEPSLATE_NICKEL_ORE = ITEMS.register("deepslate_nickel_ore", () -> new BlockItem(ModBlocks.DEEPSLATE_NICKEL_ORE_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> QUARTZ_SAND_ORE = ITEMS.register("quartz_sand_ore", () -> new BlockItem(ModBlocks.QUARTZ_SAND_ORE_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<Item> RAW_BAUXITE = ITEMS.register("raw_bauxite", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> LIMESTONE = ITEMS.register("limestone", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> PHOSPHATE = ITEMS.register("phosphate", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> POTASH = ITEMS.register("potash", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> RAW_NICKEL = ITEMS.register("raw_nickel", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> NICKEL_INGOT = ITEMS.register("nickel_ingot", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> QUARTZ_SAND = ITEMS.register("quartz_sand", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<BlockItem> ZINC_ORE = ITEMS.register("zinc_ore", () -> new BlockItem(ModBlocks.ZINC_ORE_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> DEEPSLATE_ZINC_ORE = ITEMS.register("deepslate_zinc_ore", () -> new BlockItem(ModBlocks.DEEPSLATE_ZINC_ORE_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> TIN_ORE = ITEMS.register("tin_ore", () -> new BlockItem(ModBlocks.TIN_ORE_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> DEEPSLATE_TIN_ORE = ITEMS.register("deepslate_tin_ore", () -> new BlockItem(ModBlocks.DEEPSLATE_TIN_ORE_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> LEAD_ORE = ITEMS.register("lead_ore", () -> new BlockItem(ModBlocks.LEAD_ORE_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> DEEPSLATE_LEAD_ORE = ITEMS.register("deepslate_lead_ore", () -> new BlockItem(ModBlocks.DEEPSLATE_LEAD_ORE_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> GRAPHITE_ORE = ITEMS.register("graphite_ore", () -> new BlockItem(ModBlocks.GRAPHITE_ORE_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> DEEPSLATE_GRAPHITE_ORE = ITEMS.register("deepslate_graphite_ore", () -> new BlockItem(ModBlocks.DEEPSLATE_GRAPHITE_ORE_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> COBALT_ORE = ITEMS.register("cobalt_ore", () -> new BlockItem(ModBlocks.COBALT_ORE_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> DEEPSLATE_COBALT_ORE = ITEMS.register("deepslate_cobalt_ore", () -> new BlockItem(ModBlocks.DEEPSLATE_COBALT_ORE_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> MANGANESE_ORE = ITEMS.register("manganese_ore", () -> new BlockItem(ModBlocks.MANGANESE_ORE_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> DEEPSLATE_MANGANESE_ORE = ITEMS.register("deepslate_manganese_ore", () -> new BlockItem(ModBlocks.DEEPSLATE_MANGANESE_ORE_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> CHROMIUM_ORE = ITEMS.register("chromium_ore", () -> new BlockItem(ModBlocks.CHROMIUM_ORE_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> DEEPSLATE_CHROMIUM_ORE = ITEMS.register("deepslate_chromium_ore", () -> new BlockItem(ModBlocks.DEEPSLATE_CHROMIUM_ORE_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> TITANIUM_ORE = ITEMS.register("titanium_ore", () -> new BlockItem(ModBlocks.TITANIUM_ORE_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> DEEPSLATE_TITANIUM_ORE = ITEMS.register("deepslate_titanium_ore", () -> new BlockItem(ModBlocks.DEEPSLATE_TITANIUM_ORE_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> TUNGSTEN_ORE = ITEMS.register("tungsten_ore", () -> new BlockItem(ModBlocks.TUNGSTEN_ORE_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> DEEPSLATE_TUNGSTEN_ORE = ITEMS.register("deepslate_tungsten_ore", () -> new BlockItem(ModBlocks.DEEPSLATE_TUNGSTEN_ORE_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> MOLYBDENUM_ORE = ITEMS.register("molybdenum_ore", () -> new BlockItem(ModBlocks.MOLYBDENUM_ORE_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> DEEPSLATE_MOLYBDENUM_ORE = ITEMS.register("deepslate_molybdenum_ore", () -> new BlockItem(ModBlocks.DEEPSLATE_MOLYBDENUM_ORE_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> FLUORITE_ORE = ITEMS.register("fluorite_ore", () -> new BlockItem(ModBlocks.FLUORITE_ORE_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> DEEPSLATE_FLUORITE_ORE = ITEMS.register("deepslate_fluorite_ore", () -> new BlockItem(ModBlocks.DEEPSLATE_FLUORITE_ORE_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> RARE_EARTH_ORE = ITEMS.register("rare_earth_ore", () -> new BlockItem(ModBlocks.RARE_EARTH_ORE_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> DEEPSLATE_RARE_EARTH_ORE = ITEMS.register("deepslate_rare_earth_ore", () -> new BlockItem(ModBlocks.DEEPSLATE_RARE_EARTH_ORE_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<Item> RAW_COBALT = ITEMS.register("raw_cobalt", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> COBALT_INGOT = ITEMS.register("cobalt_ingot", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> RAW_MANGANESE = ITEMS.register("raw_manganese", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> MANGANESE_INGOT = ITEMS.register("manganese_ingot", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> RAW_CHROMIUM = ITEMS.register("raw_chromium", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> CHROMIUM_INGOT = ITEMS.register("chromium_ingot", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> RAW_TITANIUM = ITEMS.register("raw_titanium", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> TITANIUM_INGOT = ITEMS.register("titanium_ingot", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> RAW_TUNGSTEN = ITEMS.register("raw_tungsten", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> TUNGSTEN_INGOT = ITEMS.register("tungsten_ingot", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> RAW_MOLYBDENUM = ITEMS.register("raw_molybdenum", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> MOLYBDENUM_INGOT = ITEMS.register("molybdenum_ingot", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> FLUORITE = ITEMS.register("fluorite", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> RARE_EARTH_CONCENTRATE = ITEMS.register("rare_earth_concentrate", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<BlockItem> URANIUM_ORE = ITEMS.register("uranium_ore", () -> new BlockItem(ModBlocks.URANIUM_ORE_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> DEEPSLATE_URANIUM_ORE = ITEMS.register("deepslate_uranium_ore", () -> new BlockItem(ModBlocks.DEEPSLATE_URANIUM_ORE_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<Item> MIXED_RARE_EARTH_CARBONATE = ITEMS.register("mixed_rare_earth_carbonate", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> NEODYMIUM_OXIDE = ITEMS.register("neodymium_oxide", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> LANTHANUM_OXIDE = ITEMS.register("lanthanum_oxide", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> CERIUM_OXIDE = ITEMS.register("cerium_oxide", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> DYSPROSIUM_OXIDE = ITEMS.register("dysprosium_oxide", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> RARE_EARTH_MAGNET = ITEMS.register("rare_earth_magnet", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> RAW_URANIUM = ITEMS.register("raw_uranium", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> YELLOWCAKE = ITEMS.register("yellowcake", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> URANIUM_HEXAFLUORIDE = ITEMS.register("uranium_hexafluoride", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> NUCLEAR_FUEL = ITEMS.register("nuclear_fuel", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> RADIOACTIVE_TAILINGS = ITEMS.register("radioactive_tailings", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> RADIATION_SHIELDING = ITEMS.register("radiation_shielding", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> RAW_ZINC = ITEMS.register("raw_zinc", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> ZINC_INGOT = ITEMS.register("zinc_ingot", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> RAW_TIN = ITEMS.register("raw_tin", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> TIN_INGOT = ITEMS.register("tin_ingot", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> RAW_LEAD = ITEMS.register("raw_lead", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> LEAD_INGOT = ITEMS.register("lead_ingot", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> GRAPHITE = ITEMS.register("graphite", () -> new Item(new Item.Properties().stacksTo(64)));
    // Real-world industrial chain intermediates.
    public static final DeferredItem<Item> ALUMINA = ITEMS.register("alumina", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> COPPER_CONCENTRATE = ITEMS.register("copper_concentrate", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> COPPER_CATHODE = ITEMS.register("copper_cathode", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> NICKEL_SULFATE = ITEMS.register("nickel_sulfate", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> SILICON_INGOT = ITEMS.register("silicon_ingot", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> PHOTORESIST = ITEMS.register("photoresist", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> TESTED_WAFER = ITEMS.register("tested_wafer", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> FINAL_CHIP_TEST = ITEMS.register("final_chip_test", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> COPPER_CLAD_LAMINATE = ITEMS.register("copper_clad_laminate", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> ASSEMBLED_PCB = ITEMS.register("assembled_pcb", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> CATHODE_PRECURSOR = ITEMS.register("cathode_precursor", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> LFP_CATHODE = ITEMS.register("lfp_cathode", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> NMC_CATHODE = ITEMS.register("nmc_cathode", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> SYNGAS = ITEMS.register("syngas", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> REFORMATE = ITEMS.register("reformate", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> C4_FRACTION = ITEMS.register("c4_fraction", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> ETHYLBENZENE = ITEMS.register("ethylbenzene", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> ACRYLONITRILE = ITEMS.register("acrylonitrile", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> BISPHENOL_A = ITEMS.register("bisphenol_a", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> EPICHLOROHYDRIN = ITEMS.register("epichlorohydrin", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> REFINERY_GAS = ITEMS.register("refinery_gas", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> KEROSENE = ITEMS.register("kerosene", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> GAS_OIL = ITEMS.register("gas_oil", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> VACUUM_RESID = ITEMS.register("vacuum_resid", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> HYDROGEN = ITEMS.register("hydrogen", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> SULFURIC_ACID = ITEMS.register("sulfuric_acid", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> ETHYLENE_OXIDE = ITEMS.register("ethylene_oxide", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> PROPYLENE_OXIDE = ITEMS.register("propylene_oxide", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> POLYETHYLENE_FILM = ITEMS.register("polyethylene_film", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> POLYPROPYLENE_FIBER = ITEMS.register("polypropylene_fiber", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> SALT = ITEMS.register("salt", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> CHLORINE = ITEMS.register("chlorine", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> CAUSTIC_SODA = ITEMS.register("caustic_soda", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> VINYL_CHLORIDE = ITEMS.register("vinyl_chloride", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> PVC_RESIN = ITEMS.register("pvc_resin", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> PVC_PIPE = ITEMS.register("pvc_pipe", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> BUTADIENE = ITEMS.register("butadiene", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> PET_FIBER = ITEMS.register("pet_fiber", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> RECLAIMED_PLASTIC = ITEMS.register("reclaimed_plastic", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> POLYETHER_POLYOL = ITEMS.register("polyether_polyol", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> POLYURETHANE_FOAM = ITEMS.register("polyurethane_foam", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> CUMENE = ITEMS.register("cumene", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> PHENOL = ITEMS.register("phenol", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> ACETONE = ITEMS.register("acetone", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> PHENOLIC_RESIN = ITEMS.register("phenolic_resin", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> METHANOL = ITEMS.register("methanol", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> FORMALDEHYDE = ITEMS.register("formaldehyde", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> ETHYLENE_DICHLORIDE = ITEMS.register("ethylene_dichloride", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> NITROGEN = ITEMS.register("nitrogen", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> OXYGEN = ITEMS.register("oxygen", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> AMMONIA = ITEMS.register("ammonia", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> UREA = ITEMS.register("urea", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> AMMONIUM_NITRATE = ITEMS.register("ammonium_nitrate", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> PHOSPHATE_FERTILIZER = ITEMS.register("phosphate_fertilizer", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> HYDROCHLORIC_ACID = ITEMS.register("hydrochloric_acid", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> NITRIC_ACID = ITEMS.register("nitric_acid", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> SODIUM_HYPOCHLORITE = ITEMS.register("sodium_hypochlorite", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> HYDROGEN_PEROXIDE = ITEMS.register("hydrogen_peroxide", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> SODA_ASH = ITEMS.register("soda_ash", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> ACTIVATED_CARBON = ITEMS.register("activated_carbon", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> ALUM = ITEMS.register("alum", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> INDUSTRIAL_WATER = ITEMS.register("industrial_water", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> INDUSTRIAL_COATING = ITEMS.register("industrial_coating", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> INDUSTRIAL_ADHESIVE = ITEMS.register("industrial_adhesive", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> INDUSTRIAL_DYE = ITEMS.register("industrial_dye", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> ETHANOL = ITEMS.register("ethanol", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> LACTIC_ACID = ITEMS.register("lactic_acid", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> PLA_PELLETS = ITEMS.register("pla_pellets", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> ACTIVE_PHARMACEUTICAL = ITEMS.register("active_pharmaceutical", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> ANTIBIOTIC_TABLET = ITEMS.register("antibiotic_tablet", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> DISINFECTANT = ITEMS.register("disinfectant", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> BRINE = ITEMS.register("brine", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> SODIUM_BICARBONATE = ITEMS.register("sodium_bicarbonate", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> CALCIUM_CHLORIDE = ITEMS.register("calcium_chloride", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> PHOSPHORIC_ACID = ITEMS.register("phosphoric_acid", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> AMMONIUM_SULFATE = ITEMS.register("ammonium_sulfate", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> DAP_FERTILIZER = ITEMS.register("dap_fertilizer", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> BLEACHING_AGENT = ITEMS.register("bleaching_agent", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> DYED_FIBER = ITEMS.register("dyed_fiber", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> FINISHED_TEXTILE = ITEMS.register("finished_textile", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> GLUCOSE = ITEMS.register("glucose", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> CITRIC_ACID = ITEMS.register("citric_acid", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> BIODEGRADABLE_PACKAGING = ITEMS.register("biodegradable_packaging", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> SALICYLIC_ACID = ITEMS.register("salicylic_acid", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> ASPIRIN_INTERMEDIATE = ITEMS.register("aspirin_intermediate", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> PAINKILLER_TABLET = ITEMS.register("painkiller_tablet", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> WASTEWATER_SLUDGE = ITEMS.register("wastewater_sludge", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> COMPRESSED_AIR = ITEMS.register("compressed_air", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> ARGON = ITEMS.register("argon", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> ACETIC_ACID = ITEMS.register("acetic_acid", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> PHARMACEUTICAL_EXCIPIENT = ITEMS.register("pharmaceutical_excipient", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> STERILE_SOLUTION = ITEMS.register("sterile_solution", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> COATED_TABLET = ITEMS.register("coated_tablet", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> CULTURE_MEDIUM = ITEMS.register("culture_medium", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> YEAST_CULTURE = ITEMS.register("yeast_culture", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> FERMENTATION_BROTH = ITEMS.register("fermentation_broth", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> CARBON_DIOXIDE = ITEMS.register("carbon_dioxide", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> ETHYL_ACETATE = ITEMS.register("ethyl_acetate", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> SOLVENT_BLEND = ITEMS.register("solvent_blend", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> PESTICIDE_ACTIVE = ITEMS.register("pesticide_active", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> HERBICIDE = ITEMS.register("herbicide", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> FUNGICIDE = ITEMS.register("fungicide", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> CARBON_BLACK = ITEMS.register("carbon_black", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> TIRE = ITEMS.register("tire", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> INDUSTRIAL_SEALANT = ITEMS.register("industrial_sealant", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> HYDROFLUORIC_ACID = ITEMS.register("hydrofluoric_acid", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> ELECTRONIC_SOLVENT = ITEMS.register("electronic_solvent", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> ELECTRONIC_ETCHANT = ITEMS.register("electronic_etchant", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> PAPER_COATING = ITEMS.register("paper_coating", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> PRINTING_INK = ITEMS.register("printing_ink", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> COATED_PAPER = ITEMS.register("coated_paper", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> PRINTED_PAPER = ITEMS.register("printed_paper", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> SURFACTANT = ITEMS.register("surfactant", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> DETERGENT = ITEMS.register("detergent", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> CLEANING_AGENT = ITEMS.register("cleaning_agent", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> MEDICAL_OXYGEN = ITEMS.register("medical_oxygen", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> WELDING_GAS = ITEMS.register("welding_gas", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> FOOD_GRADE_CARBON_DIOXIDE = ITEMS.register("food_grade_carbon_dioxide", () -> new Item(new Item.Properties().stacksTo(64)));
}
