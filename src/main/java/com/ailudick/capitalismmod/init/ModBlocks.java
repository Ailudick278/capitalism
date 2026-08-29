package com.ailudick.capitalismmod.init;

import com.ailudick.capitalismmod.CapitalismMod;
import com.ailudick.capitalismmod.block.AuctionHouseBlock;
import com.ailudick.capitalismmod.block.BankBlock;
import com.ailudick.capitalismmod.block.BondMarketBlock;
import com.ailudick.capitalismmod.block.BusinessBureau;
import com.ailudick.capitalismmod.block.CommodityExchangeBlock;
import com.ailudick.capitalismmod.block.FuturesExchangeBlock;
import com.ailudick.capitalismmod.block.CompanyBlock;
import com.ailudick.capitalismmod.block.ShopBlock;
import com.ailudick.capitalismmod.block.StockExchangeBlock;
import com.ailudick.capitalismmod.block.TaxBureau;
import com.ailudick.capitalismmod.block.SecuritiesCommission;
import com.ailudick.capitalismmod.block.WarehouseBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CapitalismMod.MODID);

    public static final DeferredBlock<Block> SHOP_BLOCK = BLOCKS.register("shop",
            () -> new ShopBlock(BlockBehaviour.Properties.of().strength(3.0f)));
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
    public static final DeferredBlock<Block> WAREHOUSE_BLOCK = BLOCKS.register("warehouse",
            () -> new WarehouseBlock(BlockBehaviour.Properties.of().strength(3.0f)));
    public static final DeferredBlock<Block> FUTURES_EXCHANGE_BLOCK = BLOCKS.register("futures_exchange",
            () -> new FuturesExchangeBlock(BlockBehaviour.Properties.of().strength(3.0f)));
    public static final DeferredBlock<Block> AUCTION_HOUSE_BLOCK = BLOCKS.register("auction_house",
            () -> new AuctionHouseBlock(BlockBehaviour.Properties.of().strength(3.0f)));
    public static final DeferredBlock<Block> BOND_MARKET_BLOCK = BLOCKS.register("bond_market",
            () -> new BondMarketBlock(BlockBehaviour.Properties.of().strength(3.0f)));
}
