package com.ailudick.capitalismmod.init;

import com.ailudick.capitalismmod.CapitalismMod;
import com.ailudick.capitalismmod.block.AuctionHouseBlock;
import com.ailudick.capitalismmod.block.BankBlock;
import com.ailudick.capitalismmod.block.BondMarketBlock;
import com.ailudick.capitalismmod.block.ProcurementBlock;
import com.ailudick.capitalismmod.block.BusinessBureau;
import com.ailudick.capitalismmod.block.CommodityExchangeBlock;
import com.ailudick.capitalismmod.block.FuturesExchangeBlock;
import com.ailudick.capitalismmod.block.CompanyBlock;
import com.ailudick.capitalismmod.block.FactoryBlock;
import com.ailudick.capitalismmod.block.FactoryPortBlock;
import com.ailudick.capitalismmod.block.FactoryMachineBlock;
import com.ailudick.capitalismmod.block.StockExchangeBlock;
import com.ailudick.capitalismmod.block.TaxBureau;
import com.ailudick.capitalismmod.block.SecuritiesCommission;
import com.ailudick.capitalismmod.block.WarehouseBlock;
import com.ailudick.capitalismmod.block.MailboxBlock;
import com.ailudick.capitalismmod.block.IndividualBusinessBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CapitalismMod.MODID);
    public static final DeferredBlock<Block> GAS_CYLINDER_BLOCK = BLOCKS.register("gas_cylinder",
            () -> new com.ailudick.capitalismmod.block.GasCylinderBlock(BlockBehaviour.Properties.of().strength(3.0f)));

    public static final DeferredBlock<Block> BANK_BLOCK = BLOCKS.register("bank",
            () -> new BankBlock(BlockBehaviour.Properties.of().strength(3.0f)));
    public static final DeferredBlock<Block> BUSINESS_BUREAU_BLOCK = BLOCKS.register("business_bureau",
            () -> new BusinessBureau(BlockBehaviour.Properties.of().strength(3.0f)));
    public static final DeferredBlock<Block> COMMODITY_EXCHANGE_BLOCK = BLOCKS.register("commodity_exchange",
            () -> new CommodityExchangeBlock(BlockBehaviour.Properties.of().strength(3.0f)));
    public static final DeferredBlock<Block> STOCK_EXCHANGE_BLOCK = BLOCKS.register("stock_exchange",
            () -> new StockExchangeBlock(BlockBehaviour.Properties.of().strength(3.0f)));
    public static final DeferredBlock<Block> SECURITIES_COMMISSION_BLOCK = BLOCKS.register("securities_commission",
            () -> new SecuritiesCommission(BlockBehaviour.Properties.of().strength(3.0f)));
    public static final DeferredBlock<Block> TAX_BUREAU_BLOCK = BLOCKS.register("tax_bureau",
            () -> new TaxBureau(BlockBehaviour.Properties.of().strength(3.0f)));
    public static final DeferredBlock<Block> COMPANY_BLOCK = BLOCKS.register("company",
            () -> new CompanyBlock(BlockBehaviour.Properties.of().strength(3.0f)));
    public static final DeferredBlock<Block> FACTORY_BLOCK = BLOCKS.register("factory",
            () -> new FactoryBlock(BlockBehaviour.Properties.of().strength(4.0f)));
    public static final DeferredBlock<Block> FACTORY_INPUT_PORT_BLOCK = BLOCKS.register("factory_input_port",
            () -> new FactoryPortBlock(BlockBehaviour.Properties.of().strength(3.0f), true));
    public static final DeferredBlock<Block> FACTORY_OUTPUT_PORT_BLOCK = BLOCKS.register("factory_output_port",
            () -> new FactoryPortBlock(BlockBehaviour.Properties.of().strength(3.0f), false));
    public static final DeferredBlock<Block> FACTORY_MACHINE_BLOCK = BLOCKS.register("factory_machine",
            () -> new FactoryMachineBlock(BlockBehaviour.Properties.of().strength(4.0f)));
    public static final DeferredBlock<Block> WAREHOUSE_BLOCK = BLOCKS.register("warehouse",
            () -> new WarehouseBlock(BlockBehaviour.Properties.of().strength(3.0f)));
    public static final DeferredBlock<Block> ALUMINUM_ORE_BLOCK = BLOCKS.register("aluminum_ore",
            () -> new Block(BlockBehaviour.Properties.of().strength(3.0f).requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> DEEPSLATE_ALUMINUM_ORE_BLOCK = BLOCKS.register("deepslate_aluminum_ore",
            () -> new Block(BlockBehaviour.Properties.of().strength(4.5f).requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> SULFUR_ORE_BLOCK = BLOCKS.register("sulfur_ore",
            () -> new Block(BlockBehaviour.Properties.of().strength(3.0f).requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> DEEPSLATE_SULFUR_ORE_BLOCK = BLOCKS.register("deepslate_sulfur_ore",
            () -> new Block(BlockBehaviour.Properties.of().strength(4.5f).requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> BAUXITE_ORE_BLOCK = BLOCKS.register("bauxite_ore", () -> new Block(BlockBehaviour.Properties.of().strength(3.0f).requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> DEEPSLATE_BAUXITE_ORE_BLOCK = BLOCKS.register("deepslate_bauxite_ore", () -> new Block(BlockBehaviour.Properties.of().strength(4.5f).requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> LIMESTONE_ORE_BLOCK = BLOCKS.register("limestone_ore", () -> new Block(BlockBehaviour.Properties.of().strength(2.5f).requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> PHOSPHATE_ORE_BLOCK = BLOCKS.register("phosphate_ore", () -> new Block(BlockBehaviour.Properties.of().strength(3.0f).requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> POTASH_ORE_BLOCK = BLOCKS.register("potash_ore", () -> new Block(BlockBehaviour.Properties.of().strength(3.0f).requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> NICKEL_ORE_BLOCK = BLOCKS.register("nickel_ore", () -> new Block(BlockBehaviour.Properties.of().strength(3.5f).requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> DEEPSLATE_NICKEL_ORE_BLOCK = BLOCKS.register("deepslate_nickel_ore", () -> new Block(BlockBehaviour.Properties.of().strength(4.5f).requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> QUARTZ_SAND_ORE_BLOCK = BLOCKS.register("quartz_sand_ore", () -> new Block(BlockBehaviour.Properties.of().strength(2.5f).requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> ZINC_ORE_BLOCK = BLOCKS.register("zinc_ore", () -> new Block(BlockBehaviour.Properties.of().strength(3.0f).requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> DEEPSLATE_ZINC_ORE_BLOCK = BLOCKS.register("deepslate_zinc_ore", () -> new Block(BlockBehaviour.Properties.of().strength(4.5f).requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> TIN_ORE_BLOCK = BLOCKS.register("tin_ore", () -> new Block(BlockBehaviour.Properties.of().strength(3.0f).requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> DEEPSLATE_TIN_ORE_BLOCK = BLOCKS.register("deepslate_tin_ore", () -> new Block(BlockBehaviour.Properties.of().strength(4.5f).requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> LEAD_ORE_BLOCK = BLOCKS.register("lead_ore", () -> new Block(BlockBehaviour.Properties.of().strength(3.5f).requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> DEEPSLATE_LEAD_ORE_BLOCK = BLOCKS.register("deepslate_lead_ore", () -> new Block(BlockBehaviour.Properties.of().strength(4.5f).requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> GRAPHITE_ORE_BLOCK = BLOCKS.register("graphite_ore", () -> new Block(BlockBehaviour.Properties.of().strength(3.0f).requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> DEEPSLATE_GRAPHITE_ORE_BLOCK = BLOCKS.register("deepslate_graphite_ore", () -> new Block(BlockBehaviour.Properties.of().strength(4.5f).requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> COBALT_ORE_BLOCK = BLOCKS.register("cobalt_ore", () -> new Block(BlockBehaviour.Properties.of().strength(3.5f).requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> DEEPSLATE_COBALT_ORE_BLOCK = BLOCKS.register("deepslate_cobalt_ore", () -> new Block(BlockBehaviour.Properties.of().strength(4.5f).requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> MANGANESE_ORE_BLOCK = BLOCKS.register("manganese_ore", () -> new Block(BlockBehaviour.Properties.of().strength(3.5f).requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> DEEPSLATE_MANGANESE_ORE_BLOCK = BLOCKS.register("deepslate_manganese_ore", () -> new Block(BlockBehaviour.Properties.of().strength(4.5f).requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> CHROMIUM_ORE_BLOCK = BLOCKS.register("chromium_ore", () -> new Block(BlockBehaviour.Properties.of().strength(3.5f).requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> DEEPSLATE_CHROMIUM_ORE_BLOCK = BLOCKS.register("deepslate_chromium_ore", () -> new Block(BlockBehaviour.Properties.of().strength(4.5f).requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> TITANIUM_ORE_BLOCK = BLOCKS.register("titanium_ore", () -> new Block(BlockBehaviour.Properties.of().strength(3.5f).requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> DEEPSLATE_TITANIUM_ORE_BLOCK = BLOCKS.register("deepslate_titanium_ore", () -> new Block(BlockBehaviour.Properties.of().strength(4.5f).requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> TUNGSTEN_ORE_BLOCK = BLOCKS.register("tungsten_ore", () -> new Block(BlockBehaviour.Properties.of().strength(4.0f).requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> DEEPSLATE_TUNGSTEN_ORE_BLOCK = BLOCKS.register("deepslate_tungsten_ore", () -> new Block(BlockBehaviour.Properties.of().strength(5.0f).requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> MOLYBDENUM_ORE_BLOCK = BLOCKS.register("molybdenum_ore", () -> new Block(BlockBehaviour.Properties.of().strength(4.0f).requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> DEEPSLATE_MOLYBDENUM_ORE_BLOCK = BLOCKS.register("deepslate_molybdenum_ore", () -> new Block(BlockBehaviour.Properties.of().strength(5.0f).requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> FLUORITE_ORE_BLOCK = BLOCKS.register("fluorite_ore", () -> new Block(BlockBehaviour.Properties.of().strength(3.0f).requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> DEEPSLATE_FLUORITE_ORE_BLOCK = BLOCKS.register("deepslate_fluorite_ore", () -> new Block(BlockBehaviour.Properties.of().strength(4.5f).requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> RARE_EARTH_ORE_BLOCK = BLOCKS.register("rare_earth_ore", () -> new Block(BlockBehaviour.Properties.of().strength(4.0f).requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> DEEPSLATE_RARE_EARTH_ORE_BLOCK = BLOCKS.register("deepslate_rare_earth_ore", () -> new Block(BlockBehaviour.Properties.of().strength(5.0f).requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> URANIUM_ORE_BLOCK = BLOCKS.register("uranium_ore", () -> new Block(BlockBehaviour.Properties.of().strength(4.0f).requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> DEEPSLATE_URANIUM_ORE_BLOCK = BLOCKS.register("deepslate_uranium_ore", () -> new Block(BlockBehaviour.Properties.of().strength(5.0f).requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> FUTURES_EXCHANGE_BLOCK = BLOCKS.register("futures_exchange",
            () -> new FuturesExchangeBlock(BlockBehaviour.Properties.of().strength(3.0f)));
    public static final DeferredBlock<Block> AUCTION_HOUSE_BLOCK = BLOCKS.register("auction_house",
            () -> new AuctionHouseBlock(BlockBehaviour.Properties.of().strength(3.0f)));
    public static final DeferredBlock<Block> BOND_MARKET_BLOCK = BLOCKS.register("bond_market",
            () -> new BondMarketBlock(BlockBehaviour.Properties.of().strength(3.0f)));
    public static final DeferredBlock<Block> PROCUREMENT_BLOCK = BLOCKS.register("procurement",
            () -> new ProcurementBlock(BlockBehaviour.Properties.of().strength(3.0f)));
    public static final DeferredBlock<Block> LOGISTICS_CENTER_BLOCK = BLOCKS.register("logistics_center",
            () -> new Block(BlockBehaviour.Properties.of().strength(3.0f)));
    public static final DeferredBlock<Block> TRANSFER_STATION_BLOCK = BLOCKS.register("transfer_station",
            () -> new Block(BlockBehaviour.Properties.of().strength(3.0f)));
    public static final DeferredBlock<Block> PORT_BLOCK = BLOCKS.register("port",
            () -> new Block(BlockBehaviour.Properties.of().strength(3.0f)));
    public static final DeferredBlock<Block> MAILBOX_BLOCK = BLOCKS.register("mailbox",
            () -> new MailboxBlock(BlockBehaviour.Properties.of().strength(2.5f)));
    public static final DeferredBlock<Block> INDIVIDUAL_BUSINESS_BLOCK = BLOCKS.register("individual_business",
            () -> new IndividualBusinessBlock(BlockBehaviour.Properties.of().strength(3.0f)));
}
