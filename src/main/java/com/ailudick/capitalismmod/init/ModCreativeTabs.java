package com.ailudick.capitalismmod.init;

import com.ailudick.capitalismmod.CapitalismMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.ItemLike;
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
                        output.accept(ModItems.USD_2.get());
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
                        output.accept(ModItems.CNY_1_COIN.get());
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
                        output.accept(ModItems.EUR_200.get());
                        output.accept(ModItems.EUR_500.get());
                        output.accept(ModItems.RUB_1K.get());
                        output.accept(ModItems.RUB_5K.get());
                        output.accept(ModItems.RUB_10K.get());
                        output.accept(ModItems.RUB_50K.get());
                        output.accept(ModItems.RUB_1.get());
                        output.accept(ModItems.RUB_2.get());
                        output.accept(ModItems.RUB_5.get());
                        output.accept(ModItems.RUB_10.get());
                        output.accept(ModItems.RUB_5_NOTE.get());
                        output.accept(ModItems.RUB_10_NOTE.get());
                        output.accept(ModItems.RUB_50.get());
                        output.accept(ModItems.RUB_100.get());
                        output.accept(ModItems.RUB_200.get());
                        output.accept(ModItems.RUB_500.get());
                        output.accept(ModItems.RUB_1000.get());
                        output.accept(ModItems.RUB_2000.get());
                        output.accept(ModItems.RUB_5000.get());
                        output.accept(ModItems.DEBIT_CARD.get());
                        output.accept(ModItems.CREDIT_CARD.get());
                        output.accept(ModItems.BANK.get());
                        output.accept(ModItems.BUSINESS_BUREAU.get());
                        output.accept(ModItems.BUSINESS_LICENSE.get());
                        output.accept(ModItems.INVOICE.get());
                        output.accept(ModItems.DEBUG_STICK.get());
                        output.accept(ModItems.PLACEHOLDER_NPC_SPAWN_EGG.get());
                        output.accept(ModItems.COMMODITY_EXCHANGE.get());
                        output.accept(ModItems.STOCK_EXCHANGE.get());
                        output.accept(ModItems.SECURITIES_COMMISSION.get());
                        output.accept(ModItems.TAX_BUREAU.get());
                        output.accept(ModItems.COMPANY.get());
                        output.accept(ModItems.FACTORY.get());
                        output.accept(ModItems.GAS_CYLINDER.get());
                        output.accept(ModItems.FACTORY_INPUT_PORT.get());
                        output.accept(ModItems.FACTORY_OUTPUT_PORT.get());
                        output.accept(ModItems.FACTORY_MACHINE.get());
                        output.accept(ModItems.WAREHOUSE.get());
                        output.accept(ModItems.FUTURES_EXCHANGE.get());
                        output.accept(ModItems.AUCTION_HOUSE.get());
                        output.accept(ModItems.BOND_MARKET.get());
                        output.accept(ModItems.PROCUREMENT.get());
                        output.accept(ModItems.LOGISTICS_CENTER.get());
                        output.accept(ModItems.TRANSFER_STATION.get());
                        output.accept(ModItems.PORT.get());
                        output.accept(ModItems.MAILBOX.get());
                        output.accept(ModItems.INDIVIDUAL_BUSINESS.get());
                        output.accept(ModItems.STEEL_SHEET.get());
                        output.accept(ModItems.COPPER_WIRE.get());
                        output.accept(ModItems.GLASS_LENS.get());
                        output.accept(ModItems.ELECTRIC_LAMP.get());
                        output.accept(ModItems.FLOUR.get());
                        output.accept(ModItems.REFRACTORY_BRICK.get());
                        output.accept(ModItems.INDUSTRIAL_COIL.get());
                        output.accept(ModItems.PRESSURE_PUMP.get());
                        output.accept(ModItems.CONTROL_PANEL.get());
                        output.accept(ModItems.MACHINE_CASING.get());
                        output.accept(ModItems.ALUMINUM_ORE.get());
                        output.accept(ModItems.DEEPSLATE_ALUMINUM_ORE.get());
                        output.accept(ModItems.SULFUR_ORE.get());
                        output.accept(ModItems.DEEPSLATE_SULFUR_ORE.get());
                        output.accept(ModItems.RAW_ALUMINUM.get());
                        output.accept(ModItems.ALUMINUM_INGOT.get());
                        output.accept(ModItems.SULFUR.get());
                        output.accept(ModItems.BAUXITE_ORE.get()); output.accept(ModItems.DEEPSLATE_BAUXITE_ORE.get());
                        output.accept(ModItems.LIMESTONE_ORE.get()); output.accept(ModItems.PHOSPHATE_ORE.get());
                        output.accept(ModItems.POTASH_ORE.get()); output.accept(ModItems.NICKEL_ORE.get());
                        output.accept(ModItems.DEEPSLATE_NICKEL_ORE.get()); output.accept(ModItems.QUARTZ_SAND_ORE.get());
                        output.accept(ModItems.RAW_BAUXITE.get()); output.accept(ModItems.LIMESTONE.get());
                        output.accept(ModItems.PHOSPHATE.get()); output.accept(ModItems.POTASH.get());
                        output.accept(ModItems.RAW_NICKEL.get()); output.accept(ModItems.NICKEL_INGOT.get());
                        output.accept(ModItems.QUARTZ_SAND.get());
                        output.accept(ModItems.ZINC_ORE.get()); output.accept(ModItems.DEEPSLATE_ZINC_ORE.get());
                        output.accept(ModItems.TIN_ORE.get()); output.accept(ModItems.DEEPSLATE_TIN_ORE.get());
                        output.accept(ModItems.LEAD_ORE.get()); output.accept(ModItems.DEEPSLATE_LEAD_ORE.get());
                        output.accept(ModItems.GRAPHITE_ORE.get()); output.accept(ModItems.DEEPSLATE_GRAPHITE_ORE.get());
                        output.accept(ModItems.RAW_ZINC.get()); output.accept(ModItems.ZINC_INGOT.get());
                        output.accept(ModItems.RAW_TIN.get()); output.accept(ModItems.TIN_INGOT.get());
                        output.accept(ModItems.RAW_LEAD.get()); output.accept(ModItems.LEAD_INGOT.get());
                        output.accept(ModItems.GRAPHITE.get());
                        output.accept(ModItems.ALUMINA.get());
                        output.accept(ModItems.COPPER_CONCENTRATE.get());
                        output.accept(ModItems.COPPER_CATHODE.get());
                        output.accept(ModItems.NICKEL_SULFATE.get());
                        output.accept(ModItems.SILICON_INGOT.get());
                        output.accept(ModItems.PHOTORESIST.get());
                        output.accept(ModItems.TESTED_WAFER.get());
                        output.accept(ModItems.FINAL_CHIP_TEST.get());
                        output.accept(ModItems.COPPER_CLAD_LAMINATE.get());
                        output.accept(ModItems.ASSEMBLED_PCB.get());
                        output.accept(ModItems.CATHODE_PRECURSOR.get());
                        output.accept(ModItems.LFP_CATHODE.get());
                        output.accept(ModItems.NMC_CATHODE.get());
                        output.accept(ModItems.METAL_CAN.get());
                        output.accept(ModItems.CANNED_FOOD.get());
                        output.accept(ModItems.GREEN_LUMBER.get());
                        output.accept(ModItems.DRIED_LUMBER.get());
                        output.accept(ModItems.WOODEN_CRATE.get());
                        output.accept(ModItems.FURNITURE.get());
                        output.accept(ModItems.CRUDE_OIL.get());
                        output.accept(ModItems.NAPHTHA.get());
                        output.accept(ModItems.FUEL_OIL.get());
                        output.accept(ModItems.BASE_OIL.get());
                        output.accept(ModItems.PLASTIC_PELLETS.get());
                        output.accept(ModItems.POLYETHYLENE_PELLETS.get());
                        output.accept(ModItems.POLYPROPYLENE_PELLETS.get());
                        output.accept(ModItems.SYNTHETIC_RUBBER.get());
                        output.accept(ModItems.ETHYLENE_GLYCOL.get());
                        output.accept(ModItems.EPOXY_RESIN.get());
                        output.accept(ModItems.BENZENE.get());
                        output.accept(ModItems.STYRENE_MONOMER.get());
                        output.accept(ModItems.ABS_RESIN.get());
                        output.accept(ModItems.P_XYLENE.get());
                        output.accept(ModItems.TEREPHTHALIC_ACID.get());
                        output.accept(ModItems.PET_RESIN.get());
                        output.accept(ModItems.PET_BOTTLE.get());
                        output.accept(ModItems.RECYCLED_PET_FLAKES.get());
                        output.accept(ModItems.FAN_BLADES.get());
                        output.accept(ModItems.FAN_CONTROL.get());
                        output.accept(ModItems.POLYSILICON.get());
                        output.accept(ModItems.SILICON_WAFER.get());
                        output.accept(ModItems.SILICON_DIE.get());
                        output.accept(ModItems.TESTED_DIE.get());
                        output.accept(ModItems.LEAD_FRAME.get());
                        output.accept(ModItems.MOLD_COMPOUND.get());
                        output.accept(ModItems.PACKAGED_CHIP.get());
                        output.accept(ModItems.CIRCUIT_BOARD.get());
                        output.accept(ModItems.PCB_SUBSTRATE.get());
                        output.accept(ModItems.DRILLED_PCB_PANEL.get());
                        output.accept(ModItems.ETCHED_PCB.get());
                        output.accept(ModItems.SOLDER_MASKED_PCB.get());
                        output.accept(ModItems.COPPER_FOIL.get());
                        output.accept(ModItems.SOLDER.get());
                        output.accept(ModItems.SMD_COMPONENTS.get());
                        output.accept(ModItems.PASSIVE_COMPONENTS.get());
                        output.accept(ModItems.POWER_MANAGEMENT_IC.get());
                        output.accept(ModItems.DISPLAY_DRIVER.get());
                        output.accept(ModItems.DISPLAY_PANEL.get());
                        output.accept(ModItems.PHONE_CASING.get());
                        output.accept(ModItems.CAMERA_MODULE.get());
                        output.accept(ModItems.SPEAKER_MODULE.get());
                        output.accept(ModItems.MICROPHONE_MODULE.get());
                        output.accept(ModItems.CHARGING_PORT.get());
                        output.accept(ModItems.BATTERY_CELL.get());
                        output.accept(ModItems.LITHIUM_MINERAL.get());
                        output.accept(ModItems.LITHIUM_CARBONATE.get());
                        output.accept(ModItems.GRAPHITE_ANODE.get());
                        output.accept(ModItems.CATHODE_ACTIVE_MATERIAL.get());
                        output.accept(ModItems.BATTERY_SEPARATOR.get());
                        output.accept(ModItems.BATTERY_ELECTROLYTE.get());
                        output.accept(ModItems.BATTERY_PACK.get());
                        output.accept(ModItems.BLACK_MASS.get());
                        output.accept(ModItems.BATTERY.get());
                        output.accept(ModItems.SMARTPHONE.get());
                        output.accept(ModItems.ELECTRIC_FAN.get());
                        output.accept(ModItems.POWER_ADAPTER.get());
                        output.accept(ModItems.TELEVISION.get());
                        output.accept(ModItems.LAPTOP.get());
                        output.accept(ModItems.WIRELESS_ROUTER.get());
                        output.accept(ModItems.REFURBISHED_SMARTPHONE.get());
                        output.accept(ModItems.REFURBISHED_TELEVISION.get());
                        output.accept(ModItems.REFURBISHED_LAPTOP.get());
                        output.accept(ModItems.REFURBISHED_WIRELESS_ROUTER.get());
                        output.accept(ModItems.PLASTIC_CONTAINER.get());
                        output.accept(ModItems.PACKAGING_FILM.get());
                        output.accept(ModItems.SYNGAS.get());
                        output.accept(ModItems.REFORMATE.get());
                        output.accept(ModItems.C4_FRACTION.get());
                        output.accept(ModItems.ETHYLBENZENE.get());
                        output.accept(ModItems.ACRYLONITRILE.get());
                        output.accept(ModItems.BISPHENOL_A.get());
                        output.accept(ModItems.EPICHLOROHYDRIN.get());
                        output.accept(ModItems.REFINERY_GAS.get());
                        output.accept(ModItems.KEROSENE.get());
                        output.accept(ModItems.GAS_OIL.get());
                        output.accept(ModItems.VACUUM_RESID.get());
                        output.accept(ModItems.HYDROGEN.get());
                        output.accept(ModItems.SULFURIC_ACID.get());
                        output.accept(ModItems.ETHYLENE_OXIDE.get());
                        output.accept(ModItems.PROPYLENE_OXIDE.get());
                        output.accept(ModItems.POLYETHYLENE_FILM.get());
                        output.accept(ModItems.POLYPROPYLENE_FIBER.get());
                        output.accept(ModItems.SALT.get());
                        output.accept(ModItems.CHLORINE.get());
                        output.accept(ModItems.CAUSTIC_SODA.get());
                        output.accept(ModItems.VINYL_CHLORIDE.get());
                        output.accept(ModItems.PVC_RESIN.get());
                        output.accept(ModItems.PVC_PIPE.get());
                        output.accept(ModItems.BUTADIENE.get());
                        output.accept(ModItems.PET_FIBER.get());
                        output.accept(ModItems.RECLAIMED_PLASTIC.get());
                        output.accept(ModItems.POLYETHER_POLYOL.get());
                        output.accept(ModItems.POLYURETHANE_FOAM.get());
                        output.accept(ModItems.CUMENE.get());
                        output.accept(ModItems.PHENOL.get());
                        output.accept(ModItems.ACETONE.get());
                        output.accept(ModItems.PHENOLIC_RESIN.get());
                        output.accept(ModItems.METHANOL.get());
                        output.accept(ModItems.FORMALDEHYDE.get());
                        output.accept(ModItems.ETHYLENE_DICHLORIDE.get());
                        output.accept(ModItems.NITROGEN.get());
                        output.accept(ModItems.OXYGEN.get());
                        output.accept(ModItems.AMMONIA.get());
                        output.accept(ModItems.UREA.get());
                        output.accept(ModItems.AMMONIUM_NITRATE.get());
                        output.accept(ModItems.PHOSPHATE_FERTILIZER.get());
                        output.accept(ModItems.HYDROCHLORIC_ACID.get());
                        output.accept(ModItems.NITRIC_ACID.get());
                        output.accept(ModItems.SODIUM_HYPOCHLORITE.get());
                        output.accept(ModItems.HYDROGEN_PEROXIDE.get());
                        output.accept(ModItems.SODA_ASH.get());
                        output.accept(ModItems.ACTIVATED_CARBON.get());
                        output.accept(ModItems.ALUM.get());
                        output.accept(ModItems.INDUSTRIAL_WATER.get());
                        output.accept(ModItems.INDUSTRIAL_COATING.get());
                        output.accept(ModItems.INDUSTRIAL_ADHESIVE.get());
                        output.accept(ModItems.INDUSTRIAL_DYE.get());
                        output.accept(ModItems.ETHANOL.get());
                        output.accept(ModItems.LACTIC_ACID.get());
                        output.accept(ModItems.PLA_PELLETS.get());
                        output.accept(ModItems.ACTIVE_PHARMACEUTICAL.get());
                        output.accept(ModItems.ANTIBIOTIC_TABLET.get());
                        output.accept(ModItems.DISINFECTANT.get());
                        output.accept(ModItems.BRINE.get());
                        output.accept(ModItems.SODIUM_BICARBONATE.get());
                        output.accept(ModItems.CALCIUM_CHLORIDE.get());
                        output.accept(ModItems.PHOSPHORIC_ACID.get());
                        output.accept(ModItems.AMMONIUM_SULFATE.get());
                        output.accept(ModItems.DAP_FERTILIZER.get());
                        output.accept(ModItems.BLEACHING_AGENT.get());
                        output.accept(ModItems.DYED_FIBER.get());
                        output.accept(ModItems.FINISHED_TEXTILE.get());
                        output.accept(ModItems.GLUCOSE.get());
                        output.accept(ModItems.CITRIC_ACID.get());
                        output.accept(ModItems.BIODEGRADABLE_PACKAGING.get());
                        output.accept(ModItems.SALICYLIC_ACID.get());
                        output.accept(ModItems.ASPIRIN_INTERMEDIATE.get());
                        output.accept(ModItems.PAINKILLER_TABLET.get());
                        output.accept(ModItems.WASTEWATER_SLUDGE.get());
                        output.accept(ModItems.COMPRESSED_AIR.get());
                        output.accept(ModItems.ARGON.get());
                        output.accept(ModItems.ACETIC_ACID.get());
                        output.accept(ModItems.PHARMACEUTICAL_EXCIPIENT.get());
                        output.accept(ModItems.STERILE_SOLUTION.get());
                        output.accept(ModItems.COATED_TABLET.get());
                        output.accept(ModItems.CULTURE_MEDIUM.get());
                        output.accept(ModItems.YEAST_CULTURE.get());
                        output.accept(ModItems.FERMENTATION_BROTH.get());
                        output.accept(ModItems.CARBON_DIOXIDE.get());
                        output.accept(ModItems.ETHYL_ACETATE.get());
                        output.accept(ModItems.SOLVENT_BLEND.get());
                        output.accept(ModItems.PESTICIDE_ACTIVE.get());
                        output.accept(ModItems.HERBICIDE.get());
                        output.accept(ModItems.FUNGICIDE.get());
                        output.accept(ModItems.CARBON_BLACK.get());
                        output.accept(ModItems.TIRE.get());
                        output.accept(ModItems.INDUSTRIAL_SEALANT.get());
                        output.accept(ModItems.HYDROFLUORIC_ACID.get());
                        output.accept(ModItems.ELECTRONIC_SOLVENT.get());
                        output.accept(ModItems.ELECTRONIC_ETCHANT.get());
                        output.accept(ModItems.PAPER_COATING.get());
                        output.accept(ModItems.PRINTING_INK.get());
                        output.accept(ModItems.COATED_PAPER.get());
                        output.accept(ModItems.PRINTED_PAPER.get());
                        output.accept(ModItems.SURFACTANT.get());
                        output.accept(ModItems.DETERGENT.get());
                        output.accept(ModItems.CLEANING_AGENT.get());
                        output.accept(ModItems.MEDICAL_OXYGEN.get());
                        output.accept(ModItems.WELDING_GAS.get());
                        output.accept(ModItems.FOOD_GRADE_CARBON_DIOXIDE.get());
                    })
                    .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> FINANCE_TAB =
            CREATIVE_TABS.register("finance_tab", () -> categoryTab("itemGroup.capitalismmod.finance", ModItems.USD_1.get(),
                    ModItems.USD_1C.get(), ModItems.USD_5C.get(), ModItems.USD_10C.get(), ModItems.USD_25C.get(), ModItems.USD_50C.get(),
                    ModItems.USD_1.get(), ModItems.USD_2.get(), ModItems.USD_5.get(), ModItems.USD_10.get(), ModItems.USD_20.get(), ModItems.USD_50.get(), ModItems.USD_100.get(),
                    ModItems.CNY_1F.get(), ModItems.CNY_5F.get(), ModItems.CNY_1J.get(), ModItems.CNY_5J.get(), ModItems.CNY_1.get(), ModItems.CNY_1_COIN.get(), ModItems.CNY_5.get(), ModItems.CNY_10.get(), ModItems.CNY_20.get(), ModItems.CNY_50.get(), ModItems.CNY_100.get(),
                    ModItems.EUR_1C.get(), ModItems.EUR_2C.get(), ModItems.EUR_5C.get(), ModItems.EUR_10C.get(), ModItems.EUR_20C.get(), ModItems.EUR_50C.get(), ModItems.EUR_1.get(), ModItems.EUR_2.get(), ModItems.EUR_5.get(), ModItems.EUR_10.get(), ModItems.EUR_20.get(), ModItems.EUR_50.get(), ModItems.EUR_100.get(), ModItems.EUR_200.get(), ModItems.EUR_500.get(),
                    ModItems.RUB_1K.get(), ModItems.RUB_5K.get(), ModItems.RUB_10K.get(), ModItems.RUB_50K.get(), ModItems.RUB_1.get(), ModItems.RUB_2.get(), ModItems.RUB_5.get(), ModItems.RUB_10.get(), ModItems.RUB_5_NOTE.get(), ModItems.RUB_10_NOTE.get(), ModItems.RUB_50.get(), ModItems.RUB_100.get(), ModItems.RUB_200.get(), ModItems.RUB_500.get(), ModItems.RUB_1000.get(), ModItems.RUB_2000.get(), ModItems.RUB_5000.get(),
                    ModItems.DEBIT_CARD.get(), ModItems.CREDIT_CARD.get(), ModItems.BANK.get(), ModItems.BUSINESS_BUREAU.get(), ModItems.BUSINESS_LICENSE.get(), ModItems.INVOICE.get(), ModItems.COMMODITY_EXCHANGE.get(), ModItems.STOCK_EXCHANGE.get(), ModItems.SECURITIES_COMMISSION.get(), ModItems.TAX_BUREAU.get(), ModItems.COMPANY.get(), ModItems.FUTURES_EXCHANGE.get(), ModItems.AUCTION_HOUSE.get(), ModItems.BOND_MARKET.get(), ModItems.PROCUREMENT.get()));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> LOGISTICS_TAB =
            CREATIVE_TABS.register("logistics_tab", () -> categoryTab("itemGroup.capitalismmod.logistics", ModItems.FACTORY.get(),
                    ModItems.FACTORY.get(), ModItems.FACTORY_INPUT_PORT.get(), ModItems.FACTORY_OUTPUT_PORT.get(), ModItems.FACTORY_MACHINE.get(), ModItems.WAREHOUSE.get(),
                    ModItems.LOGISTICS_CENTER.get(), ModItems.TRANSFER_STATION.get(), ModItems.PORT.get(), ModItems.MAILBOX.get(), ModItems.INDIVIDUAL_BUSINESS.get(), ModItems.WOODEN_CRATE.get(), ModItems.MACHINE_FRAME.get(), ModItems.ELECTRIC_MOTOR.get(), ModItems.REFRACTORY_BRICK.get(), ModItems.INDUSTRIAL_COIL.get(), ModItems.PRESSURE_PUMP.get(), ModItems.CONTROL_PANEL.get(), ModItems.MACHINE_CASING.get()));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MINING_TAB =
            CREATIVE_TABS.register("mining_tab", () -> categoryTab("itemGroup.capitalismmod.mining", ModItems.ALUMINUM_ORE.get(),
                    ModItems.ALUMINUM_ORE.get(), ModItems.DEEPSLATE_ALUMINUM_ORE.get(), ModItems.RAW_ALUMINUM.get(), ModItems.ALUMINUM_INGOT.get(), ModItems.SULFUR_ORE.get(), ModItems.DEEPSLATE_SULFUR_ORE.get(), ModItems.SULFUR.get(),
                    ModItems.BAUXITE_ORE.get(), ModItems.DEEPSLATE_BAUXITE_ORE.get(), ModItems.RAW_BAUXITE.get(), ModItems.LIMESTONE_ORE.get(), ModItems.LIMESTONE.get(), ModItems.PHOSPHATE_ORE.get(), ModItems.PHOSPHATE.get(), ModItems.POTASH_ORE.get(), ModItems.POTASH.get(),
                    ModItems.NICKEL_ORE.get(), ModItems.DEEPSLATE_NICKEL_ORE.get(), ModItems.RAW_NICKEL.get(), ModItems.NICKEL_INGOT.get(), ModItems.QUARTZ_SAND_ORE.get(), ModItems.QUARTZ_SAND.get(), ModItems.ZINC_ORE.get(), ModItems.DEEPSLATE_ZINC_ORE.get(), ModItems.RAW_ZINC.get(), ModItems.ZINC_INGOT.get(), ModItems.TIN_ORE.get(), ModItems.DEEPSLATE_TIN_ORE.get(), ModItems.RAW_TIN.get(), ModItems.TIN_INGOT.get(), ModItems.LEAD_ORE.get(), ModItems.DEEPSLATE_LEAD_ORE.get(), ModItems.RAW_LEAD.get(), ModItems.LEAD_INGOT.get(), ModItems.GRAPHITE_ORE.get(), ModItems.DEEPSLATE_GRAPHITE_ORE.get(), ModItems.GRAPHITE.get(), ModItems.COBALT_ORE.get(), ModItems.DEEPSLATE_COBALT_ORE.get(), ModItems.RAW_COBALT.get(), ModItems.COBALT_INGOT.get(), ModItems.MANGANESE_ORE.get(), ModItems.DEEPSLATE_MANGANESE_ORE.get(), ModItems.RAW_MANGANESE.get(), ModItems.MANGANESE_INGOT.get(), ModItems.CHROMIUM_ORE.get(), ModItems.DEEPSLATE_CHROMIUM_ORE.get(), ModItems.RAW_CHROMIUM.get(), ModItems.CHROMIUM_INGOT.get(), ModItems.TITANIUM_ORE.get(), ModItems.DEEPSLATE_TITANIUM_ORE.get(), ModItems.RAW_TITANIUM.get(), ModItems.TITANIUM_INGOT.get(), ModItems.TUNGSTEN_ORE.get(), ModItems.DEEPSLATE_TUNGSTEN_ORE.get(), ModItems.RAW_TUNGSTEN.get(), ModItems.TUNGSTEN_INGOT.get(), ModItems.MOLYBDENUM_ORE.get(), ModItems.DEEPSLATE_MOLYBDENUM_ORE.get(), ModItems.RAW_MOLYBDENUM.get(), ModItems.MOLYBDENUM_INGOT.get(), ModItems.FLUORITE_ORE.get(), ModItems.DEEPSLATE_FLUORITE_ORE.get(), ModItems.FLUORITE.get(), ModItems.RARE_EARTH_ORE.get(), ModItems.DEEPSLATE_RARE_EARTH_ORE.get(), ModItems.RARE_EARTH_CONCENTRATE.get(), ModItems.MIXED_RARE_EARTH_CARBONATE.get(), ModItems.NEODYMIUM_OXIDE.get(), ModItems.LANTHANUM_OXIDE.get(), ModItems.CERIUM_OXIDE.get(), ModItems.DYSPROSIUM_OXIDE.get(), ModItems.RARE_EARTH_MAGNET.get(), ModItems.URANIUM_ORE.get(), ModItems.DEEPSLATE_URANIUM_ORE.get(), ModItems.RAW_URANIUM.get(), ModItems.YELLOWCAKE.get(), ModItems.URANIUM_HEXAFLUORIDE.get(), ModItems.NUCLEAR_FUEL.get(), ModItems.RADIOACTIVE_TAILINGS.get(), ModItems.RADIATION_SHIELDING.get()));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CHEMICAL_TAB =
            CREATIVE_TABS.register("chemical_tab", () -> categoryTab("itemGroup.capitalismmod.chemical", ModItems.CRUDE_OIL.get(),
                    ModItems.SYNGAS.get(), ModItems.REFORMATE.get(), ModItems.C4_FRACTION.get(), ModItems.ETHYLBENZENE.get(), ModItems.ACRYLONITRILE.get(), ModItems.BISPHENOL_A.get(), ModItems.EPICHLOROHYDRIN.get(),
                    ModItems.CRUDE_OIL.get(), ModItems.NAPHTHA.get(), ModItems.FUEL_OIL.get(), ModItems.DIESEL.get(), ModItems.GASOLINE.get(), ModItems.LPG.get(), ModItems.BASE_OIL.get(), ModItems.LUBRICANT.get(), ModItems.ASPHALT.get(),
                    ModItems.ETHYLENE.get(), ModItems.PROPYLENE.get(), ModItems.POLYETHYLENE_PELLETS.get(), ModItems.POLYPROPYLENE_PELLETS.get(), ModItems.SYNTHETIC_RUBBER.get(), ModItems.ETHYLENE_GLYCOL.get(), ModItems.EPOXY_RESIN.get(), ModItems.BENZENE.get(), ModItems.STYRENE_MONOMER.get(), ModItems.ABS_RESIN.get(), ModItems.P_XYLENE.get(), ModItems.TEREPHTHALIC_ACID.get(), ModItems.PET_RESIN.get(), ModItems.PET_BOTTLE.get(), ModItems.RECYCLED_PET_FLAKES.get()));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ELECTRONICS_TAB =
            CREATIVE_TABS.register("electronics_tab", () -> categoryTab("itemGroup.capitalismmod.electronics", ModItems.COPPER_WIRE.get(),
                    ModItems.STEEL_SHEET.get(), ModItems.COPPER_WIRE.get(), ModItems.GLASS_LENS.get(), ModItems.ELECTRIC_LAMP.get(), ModItems.FAN_BLADES.get(), ModItems.FAN_CONTROL.get(), ModItems.POLYSILICON.get(), ModItems.SILICON_WAFER.get(), ModItems.SILICON_DIE.get(), ModItems.TESTED_DIE.get(), ModItems.LEAD_FRAME.get(), ModItems.MOLD_COMPOUND.get(), ModItems.PACKAGED_CHIP.get(), ModItems.CIRCUIT_BOARD.get(), ModItems.PCB_SUBSTRATE.get(), ModItems.DRILLED_PCB_PANEL.get(), ModItems.ETCHED_PCB.get(), ModItems.SOLDER_MASKED_PCB.get(), ModItems.COPPER_FOIL.get(), ModItems.SOLDER.get(), ModItems.SMD_COMPONENTS.get(), ModItems.PASSIVE_COMPONENTS.get(), ModItems.POWER_MANAGEMENT_IC.get(), ModItems.DISPLAY_DRIVER.get(), ModItems.DISPLAY_PANEL.get(), ModItems.PHONE_CASING.get(), ModItems.CAMERA_MODULE.get(), ModItems.SPEAKER_MODULE.get(), ModItems.MICROPHONE_MODULE.get(), ModItems.CHARGING_PORT.get(), ModItems.BATTERY_CELL.get(), ModItems.LITHIUM_MINERAL.get(), ModItems.LITHIUM_CARBONATE.get(), ModItems.GRAPHITE_ANODE.get(), ModItems.CATHODE_ACTIVE_MATERIAL.get(), ModItems.BATTERY_SEPARATOR.get(), ModItems.BATTERY_ELECTROLYTE.get(), ModItems.BATTERY_PACK.get(), ModItems.BLACK_MASS.get(), ModItems.BATTERY.get(), ModItems.POWER_ADAPTER.get(), ModItems.TELEVISION.get(), ModItems.LAPTOP.get(), ModItems.WIRELESS_ROUTER.get(), ModItems.SMARTPHONE.get(), ModItems.ELECTRIC_FAN.get()));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> GOODS_TAB =
            CREATIVE_TABS.register("goods_tab", () -> categoryTab("itemGroup.capitalismmod.goods", ModItems.CANNED_FOOD.get(),
                    ModItems.FLOUR.get(), ModItems.METAL_CAN.get(), ModItems.CANNED_FOOD.get(), ModItems.GREEN_LUMBER.get(), ModItems.DRIED_LUMBER.get(), ModItems.FURNITURE.get(), ModItems.REFURBISHED_SMARTPHONE.get(), ModItems.REFURBISHED_TELEVISION.get(), ModItems.REFURBISHED_LAPTOP.get(), ModItems.REFURBISHED_WIRELESS_ROUTER.get(), ModItems.PLASTIC_CONTAINER.get(), ModItems.PACKAGING_FILM.get()));

    private static CreativeModeTab categoryTab(String titleKey, ItemLike icon, ItemLike... items) {
        return CreativeModeTab.builder().title(Component.translatable(titleKey)).icon(() -> icon.asItem().getDefaultInstance())
                .displayItems((params, output) -> { for (ItemLike item : items) output.accept(item); }).build();
    }
}
