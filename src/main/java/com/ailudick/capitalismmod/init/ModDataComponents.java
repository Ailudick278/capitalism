package com.ailudick.capitalismmod.init;

import com.ailudick.capitalismmod.CapitalismMod;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, CapitalismMod.MODID);

    // Account number bound to a bank card. Absent = blank card.
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> ACCOUNT_ID =
            DATA_COMPONENT_TYPES.register("account_id",
                    () -> DataComponentType.<String>builder().persistent(Codec.STRING).build());

    // Company info bound to a business license. Absent = blank license.
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> COMPANY_NAME =
            DATA_COMPONENT_TYPES.register("company_name",
                    () -> DataComponentType.<String>builder().persistent(Codec.STRING).build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> COMPANY_TYPE =
            DATA_COMPONENT_TYPES.register("company_type",
                    () -> DataComponentType.<String>builder().persistent(Codec.STRING).build());

    // Invoice amount (minor units) bound to an invoice item. Absent = blank invoice.
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> INVOICE_AMOUNT =
            DATA_COMPONENT_TYPES.register("invoice_amount",
                    () -> DataComponentType.<Long>builder().persistent(Codec.LONG).build());
}
