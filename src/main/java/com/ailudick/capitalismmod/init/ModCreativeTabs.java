package com.ailudick.capitalismmod.init;

import com.ailudick.capitalismmod.CapitalismMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CapitalismMod.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CAPITALISM_TAB =
            CREATIVE_TABS.register("capitalism_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.capitalismmod"))
                    .icon(() -> ModItems.USD_1.get().getDefaultInstance())
                    .displayItems((params, output) -> {
                        output.accept(ModItems.USD_1C.get());
                        output.accept(ModItems.USD_5C.get());
                        output.accept(ModItems.USD_10C.get());
                        output.accept(ModItems.USD_25C.get());
                        output.accept(ModItems.USD_50C.get());
                        output.accept(ModItems.USD_1.get());
                        output.accept(ModItems.USD_5.get());
                        output.accept(ModItems.USD_10.get());
                        output.accept(ModItems.USD_20.get());
                        output.accept(ModItems.USD_50.get());
                        output.accept(ModItems.USD_100.get());
                        output.accept(ModItems.CNY_1F.get());
                        output.accept(ModItems.CNY_5F.get());
                        output.accept(ModItems.CNY_1J.get());
                        output.accept(ModItems.CNY_5J.get());
                        output.accept(ModItems.CNY_1.get());
                        output.accept(ModItems.CNY_5.get());
                        output.accept(ModItems.CNY_10.get());
                        output.accept(ModItems.CNY_20.get());
                        output.accept(ModItems.CNY_50.get());
                        output.accept(ModItems.CNY_100.get());
                        output.accept(ModItems.EUR_1C.get());
                        output.accept(ModItems.EUR_2C.get());
                        output.accept(ModItems.EUR_5C.get());
                        output.accept(ModItems.EUR_10C.get());
                        output.accept(ModItems.EUR_20C.get());
                        output.accept(ModItems.EUR_50C.get());
                        output.accept(ModItems.EUR_1.get());
                        output.accept(ModItems.EUR_2.get());
                        output.accept(ModItems.EUR_5.get());
                        output.accept(ModItems.EUR_10.get());
                        output.accept(ModItems.EUR_20.get());
                        output.accept(ModItems.EUR_50.get());
                        output.accept(ModItems.EUR_100.get());
                        output.accept(ModItems.RUB_1K.get());
                        output.accept(ModItems.RUB_5K.get());
                        output.accept(ModItems.RUB_10K.get());
                        output.accept(ModItems.RUB_50K.get());
                        output.accept(ModItems.RUB_1.get());
                        output.accept(ModItems.RUB_2.get());
                        output.accept(ModItems.RUB_5.get());
                        output.accept(ModItems.RUB_10.get());
                        output.accept(ModItems.RUB_50.get());
                        output.accept(ModItems.RUB_100.get());
                        output.accept(ModItems.RUB_200.get());
                        output.accept(ModItems.RUB_500.get());
                        output.accept(ModItems.RUB_1000.get());
                        output.accept(ModItems.DEBIT_CARD.get());
                        output.accept(ModItems.CREDIT_CARD.get());
                        output.accept(ModItems.SHOP.get());
                        output.accept(ModItems.BANK.get());
                        output.accept(ModItems.BUSINESS_BUREAU.get());
                        output.accept(ModItems.BUSINESS_LICENSE.get());
                        output.accept(ModItems.INVOICE.get());
                        output.accept(ModItems.DEBUG_STICK.get());
                        output.accept(ModItems.COMMODITY_EXCHANGE.get());
                        output.accept(ModItems.STOCK_EXCHANGE.get());
                        output.accept(ModItems.SECURITIES_COMMISSION.get());
                        output.accept(ModItems.TAX_BUREAU.get());
                        output.accept(ModItems.COMPANY.get());
                        output.accept(ModItems.WAREHOUSE.get());
                        output.accept(ModItems.FUTURES_EXCHANGE.get());
                        output.accept(ModItems.AUCTION_HOUSE.get());
                        output.accept(ModItems.BOND_MARKET.get());
                    })
                    .build());
}
