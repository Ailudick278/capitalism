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

    // owner storage key (player:<uuid> or company:<companyId>) -> (item id -> count)
    private final Map<String, Map<String, Integer>> storage = new HashMap<>();
    private final java.util.List<AuditEntry> audit = new java.util.ArrayList<>();

    public record AuditEntry(String action, String from, String to, String itemId, int count) {
        static final Codec<AuditEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("action").forGetter(AuditEntry::action),
                Codec.STRING.fieldOf("from").forGetter(AuditEntry::from),
                Codec.STRING.fieldOf("to").forGetter(AuditEntry::to),
                Codec.STRING.fieldOf("itemId").forGetter(AuditEntry::itemId),
                Codec.INT.fieldOf("count").forGetter(AuditEntry::count)
        ).apply(instance, AuditEntry::new));
    }

    private record State(Map<String, Map<String, Integer>> storage, java.util.List<AuditEntry> audit) {
        static final Codec<State> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.unboundedMap(Codec.STRING, Codec.unboundedMap(Codec.STRING, Codec.INT))
                        .fieldOf("storage").forGetter(State::storage),
                AuditEntry.CODEC.listOf().optionalFieldOf("audit", java.util.List.of()).forGetter(State::audit)
        ).apply(instance, State::new));
    }

    private WarehouseSavedData() {
    }

    public static WarehouseSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(WarehouseSavedData::new, WarehouseSavedData::load), ID);
    }

    public Map<String, Integer> storage(InventoryOwner owner) {
        return storage.getOrDefault(owner.storageKey(), Map.of());
    }

    public int count(InventoryOwner owner, String itemId) {
        return storage(owner).getOrDefault(itemId, 0);
    }

    /** Compatibility accessor for legacy player-owned orders and old integrations. */
    public Map<String, Integer> storage(UUID playerId) { return storage(InventoryOwner.player(playerId)); }

    /** Compatibility accessor for legacy player-owned orders and old integrations. */
    public int count(UUID playerId, String itemId) { return count(InventoryOwner.player(playerId), itemId); }

    /** Adds {@code count} of {@code item} to the player's warehouse inventory (no backpack interaction). */
    public void credit(InventoryOwner owner, Item item, int count) {
        if (item == null || item == Items.AIR || count <= 0) {
            return;
        }
        String itemId = BuiltInRegistries.ITEM.getKey(item).toString();
        storage.computeIfAbsent(owner.storageKey(), k -> new HashMap<>()).merge(itemId, count, WarehouseSavedData::saturatingAdd);
        setDirty();
    }

    public void credit(UUID playerId, Item item, int count) { credit(InventoryOwner.player(playerId), item, count); }

    private static int saturatingAdd(int left, int right) {
        if (right > Integer.MAX_VALUE - left) {
            return Integer.MAX_VALUE;
        }
        return left + right;
    }

    /** Removes {@code count} of {@code item} from the warehouse, returning {@code false} if insufficient. */
    public boolean consume(InventoryOwner owner, Item item, int count) {
        if (item == null || item == Items.AIR || count <= 0) {
            return false;
        }
        String itemId = BuiltInRegistries.ITEM.getKey(item).toString();
        Map<String, Integer> inv = storage.get(owner.storageKey());
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
            storage.remove(owner.storageKey());
        }
        setDirty();
        return true;
    }

    public boolean consume(UUID playerId, Item item, int count) { return consume(InventoryOwner.player(playerId), item, count); }

    /** Moves items between two persistent owner inventories, recording no player interaction. */
    public boolean transfer(InventoryOwner from, InventoryOwner to, Item item, int count) {
        if (from.equals(to) || !consume(from, item, count)) return false;
        credit(to, item, count);
        audit.add(new AuditEntry("transfer", from.storageKey(), to.storageKey(),
                BuiltInRegistries.ITEM.getKey(item).toString(), count));
        trimAudit();
        setDirty();
        return true;
    }

    public java.util.List<AuditEntry> audit() { return java.util.List.copyOf(audit); }

    private void trimAudit() {
        while (audit.size() > 500) audit.remove(0);
    }

    /** Moves every item from one owner inventory to another. */
    public int transferAll(InventoryOwner from, InventoryOwner to) {
        if (from.equals(to)) return 0;
        Map<String, Integer> source = new HashMap<>(storage(from));
        int moved = 0;
        for (Map.Entry<String, Integer> entry : source.entrySet()) {
            Item item = itemById(entry.getKey());
            if (item != null && transfer(from, to, item, entry.getValue())) moved++;
        }
        return moved;
    }

    /** Moves {@code count} of {@code item} from the player's backpack into the warehouse. */
    public boolean deposit(Player player, Item item, int count) {
        return deposit(player, InventoryOwner.player(player.getUUID()), item, count);
    }

    public boolean deposit(Player player, InventoryOwner owner, Item item, int count) {
        if (item == null || item == Items.AIR || count <= 0) {
            return false;
        }
        if (!consumeInventory(player, item, count)) {
            return false;
        }
        credit(owner, item, count);
        return true;
    }

    /** Moves {@code count} of {@code item} from the warehouse into the player's backpack. */
    public boolean withdraw(Player player, Item item, int count) {
        return withdraw(player, InventoryOwner.player(player.getUUID()), item, count);
    }

    public boolean withdraw(Player player, InventoryOwner owner, Item item, int count) {
        if (item == null || item == Items.AIR || count <= 0) {
            return false;
        }
        if (!consume(owner, item, count)) {
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

    private static Item itemById(String itemId) {
        try {
            Item item = BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(itemId));
            return item == null || item == Items.AIR ? null : item;
        } catch (IllegalArgumentException | NullPointerException ignored) {
            return null;
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        State state = new State(new HashMap<>(storage), new java.util.ArrayList<>(audit));
        State.CODEC.encodeStart(NbtOps.INSTANCE, state).result()
                .ifPresent(encoded -> tag.put("data", encoded));
        return tag;
    }

    public static WarehouseSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        WarehouseSavedData data = new WarehouseSavedData();
        if (tag.contains("data")) {
            State.CODEC.parse(NbtOps.INSTANCE, tag.get("data")).result().ifPresent(state -> {
                    state.storage().forEach((k, v) -> {
                        // Version 1 used the raw player UUID as the key. Keep all
                        // existing stock in the player's personal warehouse.
                        String ownerKey = InventoryOwner.parse(k) == null && isUuid(k)
                                ? "player:" + k : k;
                        data.storage.put(ownerKey, new HashMap<>(v));
                    });
                    data.audit.addAll(state.audit());
            });
        }
        return data;
    }

    private static boolean isUuid(String value) {
        try { UUID.fromString(value); return true; }
        catch (IllegalArgumentException | NullPointerException ignored) { return false; }
    }
}
