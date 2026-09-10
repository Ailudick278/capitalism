package com.ailudick.capitalismmod.client;

import net.minecraft.client.Minecraft;
import com.ailudick.capitalismmod.screen.TechnologyTreeScreen;

public final class TechnologySnapshotHandler {
    private TechnologySnapshotHandler() {}
    public static void accept(int mask) {
        if (Minecraft.getInstance().screen instanceof TechnologyTreeScreen tree) tree.acceptSnapshot(mask);
    }
}
