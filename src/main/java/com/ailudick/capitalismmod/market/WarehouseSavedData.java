package com.ailudick.capitalismmod.market;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The exchange's global delivery warehouse. Each player has a per-commodity
 * inventory here, independent of their backpack. Commodity sell orders are
 * escrowed from this warehouse; trades deliver into the buyer's warehouse.
 */
public final class WarehouseSavedData extends SavedData {
    private static final String ID = "capitalismmod_warehouse";

    // player uuid string -> (item id -> count)
    private final Map<String, Map<String, Integer>> storage = new HashMap<>();

    private record State(Map<String, Map<String, Integer>> storage) {
        static final Codec<State> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.unboundedMap(Codec.STRING, Codec.unboundedMap(Codec.STRING, Codec.INT))
                        .fieldOf("storage").forGetter(State::storage)
        ).apply(instance, State::new));
    }

    private WarehouseSavedData() {
    }

    public static WarehouseSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(WarehouseSavedData::new, WarehouseSavedData::load), ID);
    }

    public Map<String, Integer> storage(UUID playerId) {
        return storage.getOrDefault(playerId.toString(), Map.of());
    }

    public int count(UUID playerId, String itemId) {
        return storage.getOrDefault(playerId.toString(), Map.of()).getOrDefault(itemId, 0);
    }

    /** Adds {@code count} of {@code item} to the player's warehouse inventory (no backpack interaction). */
    public void credit(UUID playerId, Item item, int count) {
        if (item == null || item == Items.AIR || count <= 0) {
            return;
        }
        String itemId = BuiltInRegistries.ITEM.getKey(item).toString();
        storage.computeIfAbsent(playerId.toString(), k -> new HashMap<>()).merge(itemId, count, Integer::sum);
        setDirty();
    }

    /** Removes {@code count} of {@code item} from the warehouse, returning {@code false} if insufficient. */
    public boolean consume(UUID playerId, Item item, int count) {
        if (item == null || item == Items.AIR || count <= 0) {
            return false;
        }
        String itemId = BuiltInRegistries.ITEM.getKey(item).toString();
        Map<String, Integer> inv = storage.get(playerId.toString());
        if (inv == null) {
            return false;
        }
        int available = inv.getOrDefault(itemId, 0);
        if (available < count) {
            return false;
        }
        int remaining = available - count;
        if (remaining == 0) {
            inv.remove(itemId);
        } else {
            inv.put(itemId, remaining);
        }
        if (inv.isEmpty()) {
            storage.remove(playerId.toString());
        }
        setDirty();
        return true;
    }

    /** Moves {@code count} of {@code item} from the player's backpack into the warehouse. */
    public boolean deposit(Player player, Item item, int count) {
        if (item == null || item == Items.AIR || count <= 0) {
            return false;
        }
        if (!consumeInventory(player, item, count)) {
            return false;
        }
        credit(player.getUUID(), item, count);
        return true;
    }

    /** Moves {@code count} of {@code item} from the warehouse into the player's backpack. */
    public boolean withdraw(Player player, Item item, int count) {
        if (item == null || item == Items.AIR || count <= 0) {
            return false;
        }
        if (!consume(player.getUUID(), item, count)) {
            return false;
        }
        giveInventory(player, item, count);
        return true;
    }

    private static boolean consumeInventory(Player player, Item item, int count) {
        int available = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() == item) {
                available += stack.getCount();
            }
        }
        if (available < count) {
            return false;
        }
        int remaining = count;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() == item) {
                int take = Math.min(stack.getCount(), remaining);
                stack.shrink(take);
                remaining -= take;
                if (remaining <= 0) {
                    break;
                }
            }
        }
        return remaining <= 0;
    }

    private static void giveInventory(Player player, Item item, int count) {
        int remaining = count;
        while (remaining > 0) {
            int stackSize = Math.min(remaining, item.getDefaultMaxStackSize());
            ItemStack stack = new ItemStack(item, stackSize);
            remaining -= stackSize;
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        State state = new State(new HashMap<>(storage));
        State.CODEC.encodeStart(NbtOps.INSTANCE, state).result()
                .ifPresent(encoded -> tag.put("data", encoded));
        return tag;
    }

    public static WarehouseSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        WarehouseSavedData data = new WarehouseSavedData();
        if (tag.contains("data")) {
            State.CODEC.parse(NbtOps.INSTANCE, tag.get("data")).result().ifPresent(state ->
                    state.storage().forEach((k, v) -> data.storage.put(k, new HashMap<>(v))));
        }
        return data;
    }
}
