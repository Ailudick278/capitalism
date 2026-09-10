package com.ailudick.capitalismmod.init;

import com.ailudick.capitalismmod.CapitalismMod;
import com.ailudick.capitalismmod.blockentity.FactoryBlockEntity;
import com.ailudick.capitalismmod.blockentity.FactoryStorageBlockEntity;
import com.ailudick.capitalismmod.blockentity.FactoryMachineBlockEntity;
import com.ailudick.capitalismmod.init.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CapitalismMod.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FactoryBlockEntity>> FACTORY =
            BLOCK_ENTITIES.register("factory", () -> BlockEntityType.Builder.of(
                    FactoryBlockEntity::new, ModBlocks.FACTORY_BLOCK.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FactoryStorageBlockEntity>> FACTORY_STORAGE =
            BLOCK_ENTITIES.register("factory_storage", () -> BlockEntityType.Builder.of(
                    FactoryStorageBlockEntity::new, ModBlocks.WAREHOUSE_BLOCK.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FactoryMachineBlockEntity>> FACTORY_MACHINE =
            BLOCK_ENTITIES.register("factory_machine", () -> BlockEntityType.Builder.of(
                    FactoryMachineBlockEntity::new, ModBlocks.FACTORY_MACHINE_BLOCK.get()).build(null));

}
