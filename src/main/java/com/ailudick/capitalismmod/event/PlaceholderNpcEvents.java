package com.ailudick.capitalismmod.event;

import com.ailudick.capitalismmod.entity.PlaceholderNpc;
import com.ailudick.capitalismmod.init.ModEntities;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

public final class PlaceholderNpcEvents {
    private PlaceholderNpcEvents() {
    }

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.PLACEHOLDER_NPC.get(), PlaceholderNpc.createAttributes().build());
    }
}
