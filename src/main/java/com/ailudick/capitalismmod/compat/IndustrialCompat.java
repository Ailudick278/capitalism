package com.ailudick.capitalismmod.compat;

import net.neoforged.fml.ModList;

/** Optional-mod detection kept free of Create class references. */
public final class IndustrialCompat {
    private IndustrialCompat() {}

    public static boolean createLoaded() {
        return ModList.get().isLoaded("create");
    }

    public static String transportBackend() {
        return createLoaded() ? "create" : "capitalismmod";
    }
}
