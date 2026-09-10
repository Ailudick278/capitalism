package com.ailudick.capitalismmod.blockentity;

import com.ailudick.capitalismmod.compat.IndustrialTransportPort;
import com.ailudick.capitalismmod.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;

/** Persistent factory buffers exposed through the future Create compatibility layer. */
public final class FactoryBlockEntity extends BlockEntity implements IndustrialTransportPort {
    private static final String INPUTS_TAG = "InputBuffer";
    private static final String OUTPUTS_TAG = "OutputBuffer";
    private static final int MAX_ITEM_BUFFER = 4096;
    private final Map<String, Integer> inputs = new HashMap<>();
    private final Map<String, Integer> outputs = new HashMap<>();

    public FactoryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FACTORY.get(), pos, state);
    }

    @Override
    public Map<String, Integer> inputBuffer() { return Map.copyOf(inputs); }

    @Override
    public Map<String, Integer> outputBuffer() { return Map.copyOf(outputs); }

    @Override
    public int insertInput(String itemId, int amount) {
        return insert(inputs, itemId, amount);
    }

    @Override
    public int extractOutput(String itemId, int amount) {
        if (itemId == null || itemId.isBlank() || amount <= 0) return 0;
        int available = outputs.getOrDefault(itemId, 0);
        int moved = Math.min(available, amount);
        if (moved <= 0) return 0;
        if (moved == available) outputs.remove(itemId);
        else outputs.put(itemId, available - moved);
        setChanged();
        return moved;
    }

    /** Internal production hook for a later factory ticker. */
    public int insertOutput(String itemId, int amount) {
        return insert(outputs, itemId, amount);
    }

    private int insert(Map<String, Integer> buffer, String itemId, int amount) {
        if (itemId == null || itemId.isBlank() || amount <= 0) return 0;
        int current = buffer.getOrDefault(itemId, 0);
        int moved = Math.min(amount, MAX_ITEM_BUFFER - current);
        if (moved <= 0) return 0;
        buffer.put(itemId, current + moved);
        setChanged();
        return moved;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        saveBuffer(tag, INPUTS_TAG, inputs);
        saveBuffer(tag, OUTPUTS_TAG, outputs);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inputs.clear();
        outputs.clear();
        loadBuffer(tag, INPUTS_TAG, inputs);
        loadBuffer(tag, OUTPUTS_TAG, outputs);
    }

    private static void saveBuffer(CompoundTag parent, String key, Map<String, Integer> buffer) {
        CompoundTag data = new CompoundTag();
        buffer.forEach((item, count) -> {
            if (item != null && !item.isBlank() && count != null && count > 0) data.putInt(item, count);
        });
        parent.put(key, data);
    }

    private static void loadBuffer(CompoundTag parent, String key, Map<String, Integer> target) {
        if (!parent.contains(key, Tag.TAG_COMPOUND)) return;
        CompoundTag data = parent.getCompound(key);
        for (String item : data.getAllKeys()) {
            int count = Math.max(0, Math.min(MAX_ITEM_BUFFER, data.getInt(item)));
            if (!item.isBlank() && count > 0) target.put(item, count);
        }
    }
}
