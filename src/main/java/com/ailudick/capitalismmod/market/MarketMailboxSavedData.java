package com.ailudick.capitalismmod.market;

import com.ailudick.capitalismmod.CapitalismMod;
import com.ailudick.capitalismmod.currency.Currencies;
import com.ailudick.capitalismmod.wallet.EconomyHelper;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * World-persisted mailbox for market payouts owed to offline players.
 * When a commodity trade completes and the counterparty is offline, their
 * escrowed money or items are parked here and redeemed on next login.
 */
public final class MarketMailboxSavedData extends SavedData {
    private static final String ID = "capitalismmod_market_mailbox";

    // player UUID -> currency id -> amount
    private final Map<UUID, Map<String, Long>> money = new HashMap<>();
    // player UUID -> item id -> count
    private final Map<UUID, Map<String, Integer>> items = new HashMap<>();

    public static MarketMailboxSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(MarketMailboxSavedData::new, MarketMailboxSavedData::load), ID);
    }

    public void creditMoney(UUID playerId, String currencyId, long amount) {
        if (amount <= 0) {
            return;
        }
        money.computeIfAbsent(playerId, k -> new HashMap<>()).merge(currencyId, amount, MarketMailboxSavedData::saturatingAdd);
        setDirty();
    }

    public void creditItems(UUID playerId, Item item, int count) {
        if (item == null || item == Items.AIR || count <= 0) {
            return;
        }
        String itemId = BuiltInRegistries.ITEM.getKey(item).toString();
        items.computeIfAbsent(playerId, k -> new HashMap<>()).merge(itemId, count, MarketMailboxSavedData::saturatingAdd);
        setDirty();
    }

    private static long saturatingAdd(long left, long right) {
        try {
            return Math.addExact(left, right);
        } catch (ArithmeticException e) {
            return Long.MAX_VALUE;
        }
    }

    private static int saturatingAdd(int left, int right) {
        if (right > Integer.MAX_VALUE - left) {
            return Integer.MAX_VALUE;
        }
        return left + right;
    }

    /** Hands over and clears everything owed to this player. */
    public void redeem(ServerPlayer player) {
        UUID id = player.getUUID();
        boolean changed = false;

        Map<String, Long> owedMoney = money.remove(id);
        if (owedMoney != null) {
            changed = true;
            for (Map.Entry<String, Long> entry : owedMoney.entrySet()) {
                if (Currencies.exists(entry.getKey())) {
                    EconomyHelper.giveMoney(player, Currencies.byId(entry.getKey()), entry.getValue());
                }
            }
        }

        Map<String, Integer> owedItems = items.remove(id);
        if (owedItems != null) {
            changed = true;
            for (Map.Entry<String, Integer> entry : owedItems.entrySet()) {
                Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(entry.getKey()));
                if (item == null || item == Items.AIR) {
                    CapitalismMod.LOGGER.warn("Unknown item in market mailbox: {}", entry.getKey());
                    continue;
                }
                int remaining = entry.getValue();
                while (remaining > 0) {
                    int stackSize = Math.min(remaining, item.getDefaultMaxStackSize());
                    ItemStack stack = new ItemStack(item, stackSize);
                    remaining -= stackSize;
                    if (!player.getInventory().add(stack)) {
                        player.drop(stack, false);
                    }
                }
            }
        }

        if (changed) {
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag moneyList = new ListTag();
        for (Map.Entry<UUID, Map<String, Long>> entry : money.entrySet()) {
            CompoundTag nbt = new CompoundTag();
            nbt.putUUID("uuid", entry.getKey());
            CompoundTag balances = new CompoundTag();
            for (Map.Entry<String, Long> e : entry.getValue().entrySet()) {
                balances.putLong(e.getKey(), e.getValue());
            }
            nbt.put("balances", balances);
            moneyList.add(nbt);
        }
        tag.put("money", moneyList);

        ListTag itemList = new ListTag();
        for (Map.Entry<UUID, Map<String, Integer>> entry : items.entrySet()) {
            CompoundTag nbt = new CompoundTag();
            nbt.putUUID("uuid", entry.getKey());
            CompoundTag counts = new CompoundTag();
            for (Map.Entry<String, Integer> e : entry.getValue().entrySet()) {
                counts.putInt(e.getKey(), e.getValue());
            }
            nbt.put("items", counts);
            itemList.add(nbt);
        }
        tag.put("items", itemList);
        return tag;
    }

    public static MarketMailboxSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        MarketMailboxSavedData data = new MarketMailboxSavedData();

        ListTag moneyList = tag.getList("money", Tag.TAG_COMPOUND);
        for (int i = 0; i < moneyList.size(); i++) {
            CompoundTag nbt = moneyList.getCompound(i);
            Map<String, Long> balances = new HashMap<>();
            CompoundTag balanceTag = nbt.getCompound("balances");
            for (String key : balanceTag.getAllKeys()) {
                balances.put(key, balanceTag.getLong(key));
            }
            if (!balances.isEmpty()) {
                data.money.put(nbt.getUUID("uuid"), balances);
            }
        }

        ListTag itemList = tag.getList("items", Tag.TAG_COMPOUND);
        for (int i = 0; i < itemList.size(); i++) {
            CompoundTag nbt = itemList.getCompound(i);
            Map<String, Integer> counts = new HashMap<>();
            CompoundTag countTag = nbt.getCompound("items");
            for (String key : countTag.getAllKeys()) {
                counts.put(key, countTag.getInt(key));
            }
            if (!counts.isEmpty()) {
                data.items.put(nbt.getUUID("uuid"), counts);
            }
        }

        return data;
    }
}
