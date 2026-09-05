package com.ailudick.capitalismmod.init;

import com.ailudick.capitalismmod.CapitalismMod;
import com.ailudick.capitalismmod.entity.PlaceholderNpc;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, CapitalismMod.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<PlaceholderNpc>> PLACEHOLDER_NPC =
            ENTITY_TYPES.register("placeholder_npc", () -> EntityType.Builder.of(PlaceholderNpc::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F)
                    .build("capitalismmod:placeholder_npc"));

    private ModEntities() {
    }
}
