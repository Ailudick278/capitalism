package com.ailudick.capitalismmod.screen;

import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Small client-only config for restoring the last world-map viewport. */
final class WorldMapClientState {
    private static Path path() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("config/capitalismmod-world-map.properties");
    }

    static void load(WorldMapViewport viewport) {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(path())) {
            properties.load(input);
            viewport.centerOn(Double.parseDouble(properties.getProperty("centerX")),
                    Double.parseDouble(properties.getProperty("centerZ")));
            viewport.setZoom(Float.parseFloat(properties.getProperty("zoom")));
        } catch (IOException | NumberFormatException ignored) {
            // First launch or a damaged optional config falls back to the player position.
        }
    }

    static void save(WorldMapViewport viewport) {
        Path file = path();
        try {
            Files.createDirectories(file.getParent());
            Properties properties = new Properties();
            properties.setProperty("centerX", Double.toString(viewport.centerX()));
            properties.setProperty("centerZ", Double.toString(viewport.centerZ()));
            properties.setProperty("zoom", Float.toString(viewport.zoom()));
            try (OutputStream output = Files.newOutputStream(file)) {
                properties.store(output, "Capitalism Mod world map viewport");
            }
        } catch (IOException ignored) {
            // Map state is optional and must not prevent the screen from closing.
        }
    }
}
