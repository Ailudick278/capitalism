package com.ailudick.capitalismmod.init;

import com.ailudick.capitalismmod.CapitalismMod;
import com.ailudick.capitalismmod.blockentity.ShopBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CapitalismMod.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ShopBlockEntity>> SHOP_BE =
            BLOCK_ENTITIES.register("shop",
                    () -> BlockEntityType.Builder.of(ShopBlockEntity::new, ModBlocks.SHOP_BLOCK.get()).build(null));
}
