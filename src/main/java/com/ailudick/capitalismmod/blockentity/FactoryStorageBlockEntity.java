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

/** Local factory storage node. It is deliberately separate from the global company warehouse. */
public final class FactoryStorageBlockEntity extends BlockEntity implements IndustrialTransportPort {
    private static final int CAPACITY_PER_ITEM = 8192;
    private final Map<String, Integer> inputs = new HashMap<>();
    private final Map<String, Integer> outputs = new HashMap<>();

    public FactoryStorageBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FACTORY_STORAGE.get(), pos, state);
    }

    @Override public Map<String, Integer> inputBuffer() { return Map.copyOf(inputs); }
    @Override public Map<String, Integer> outputBuffer() { return Map.copyOf(outputs); }

    @Override public int insertInput(String itemId, int amount) { return insert(inputs, itemId, amount); }
    @Override public int extractOutput(String itemId, int amount) { return extract(outputs, itemId, amount); }
    public int insertOutput(String itemId, int amount) { return insert(outputs, itemId, amount); }

    private int insert(Map<String, Integer> target, String itemId, int amount) {
        if (itemId == null || itemId.isBlank() || amount <= 0) return 0;
        int current = target.getOrDefault(itemId, 0);
        int moved = Math.min(amount, CAPACITY_PER_ITEM - current);
        if (moved <= 0) return 0;
        target.put(itemId, current + moved); setChanged(); return moved;
    }

    private int extract(Map<String, Integer> source, String itemId, int amount) {
        if (itemId == null || itemId.isBlank() || amount <= 0) return 0;
        int available = source.getOrDefault(itemId, 0), moved = Math.min(amount, available);
        if (moved <= 0) return 0;
        if (moved == available) source.remove(itemId); else source.put(itemId, available - moved);
        setChanged(); return moved;
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries); save(tag, "inputs", inputs); save(tag, "outputs", outputs);
    }

    @Override public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries); inputs.clear(); outputs.clear();
        load(tag, "inputs", inputs); load(tag, "outputs", outputs);
    }

    private static void save(CompoundTag parent, String key, Map<String, Integer> values) {
        CompoundTag data = new CompoundTag();
        values.forEach((item, amount) -> { if (item != null && !item.isBlank() && amount > 0) data.putInt(item, amount); });
        parent.put(key, data);
    }

    private static void load(CompoundTag parent, String key, Map<String, Integer> target) {
        if (!parent.contains(key, Tag.TAG_COMPOUND)) return;
        CompoundTag data = parent.getCompound(key);
        for (String item : data.getAllKeys()) {
            int amount = Math.min(CAPACITY_PER_ITEM, Math.max(0, data.getInt(item)));
            if (!item.isBlank() && amount > 0) target.put(item, amount);
        }
    }
}
