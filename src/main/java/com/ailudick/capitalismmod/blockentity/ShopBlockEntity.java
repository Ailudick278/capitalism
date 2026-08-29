package com.ailudick.capitalismmod.blockentity;

import com.ailudick.capitalismmod.init.ModBlockEntities;
import com.ailudick.capitalismmod.shop.ShopOffer;
import com.ailudick.capitalismmod.shop.ShopOffers;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class ShopBlockEntity extends BlockEntity {
    private final List<ShopOffer> offers = new ArrayList<>();

    public ShopBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SHOP_BE.get(), pos, state);
        // Skeleton stage: every shop uses the same static default stock.
        offers.addAll(ShopOffers.defaultOffers());
    }

    public List<ShopOffer> getOffers() {
        return offers;
    }
}
