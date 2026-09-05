package com.ailudick.capitalismmod.client;

import com.ailudick.capitalismmod.Config;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

/**
 * The keybinding for opening the conglomerate GUI (default: C).
 */
public final class ConglomerateKeyMapping {
    public static final KeyMapping OPEN_LAND = new KeyMapping(
            "key.capitalismmod.open_world_map", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M,
            "key.categories.capitalismmod");
    public static final KeyMapping OPEN_LAND_MENU = new KeyMapping(
            "key.capitalismmod.open_land_menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_L,
            "key.categories.capitalismmod");

    private ConglomerateKeyMapping() {
    }

    public static void register(RegisterKeyMappingsEvent event) {
        if (Config.WORLD_MAP_STANDALONE_ENABLED.get()) {
            event.register(OPEN_LAND);
        }
        event.register(OPEN_LAND_MENU);
    }
}
