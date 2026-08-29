package com.ailudick.capitalismmod.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

/**
 * The keybinding for opening the conglomerate GUI (default: C).
 */
public final class ConglomerateKeyMapping {
    public static final KeyMapping OPEN_CONGLOMERATE = new KeyMapping(
            "key.capitalismmod.open_conglomerate",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_C,
            "key.categories.capitalismmod"
    );

    private ConglomerateKeyMapping() {
    }

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(OPEN_CONGLOMERATE);
    }
}
