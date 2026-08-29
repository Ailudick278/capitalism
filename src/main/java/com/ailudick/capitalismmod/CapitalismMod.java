package com.ailudick.capitalismmod;

import com.ailudick.capitalismmod.data.CapitalismData;
import com.ailudick.capitalismmod.init.ModAttachments;
import com.ailudick.capitalismmod.init.ModBlockEntities;
import com.ailudick.capitalismmod.init.ModBlocks;
import com.ailudick.capitalismmod.init.ModCreativeTabs;
import com.ailudick.capitalismmod.init.ModDataComponents;
import com.ailudick.capitalismmod.init.ModItems;
import com.ailudick.capitalismmod.init.ModMenuTypes;
import com.ailudick.capitalismmod.network.NetworkHandler;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

// The value here must match the modId in META-INF/neoforge.mods.toml
@Mod(CapitalismMod.MODID)
public class CapitalismMod {
    public static final String MODID = "capitalismmod";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CapitalismMod(IEventBus modEventBus, ModContainer modContainer) {
        // Load data-driven config first, before any data class is touched.
        CapitalismData.load();

        // Register all DeferredRegisters under our namespace.
        ModItems.ITEMS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModMenuTypes.MENUS.register(modEventBus);
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);
        ModCreativeTabs.CREATIVE_TABS.register(modEventBus);
        ModDataComponents.DATA_COMPONENT_TYPES.register(modEventBus);

        // Register network payload handlers on the mod bus.
        modEventBus.addListener(NetworkHandler::register);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
}
