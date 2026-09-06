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
import java.util.HashSet;
import java.util.Set;
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
    // Transfers such as currency exchange are not new income and use a separate queue.
    private final Map<UUID, Map<String, Long>> transferMoney = new HashMap<>();
    private final Set<String> creditedSources = new HashSet<>();
    private final Set<String> transferSources = new HashSet<>();
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

    /** Credits a payout source at most once, allowing crash-safe retry by callers. */
    public boolean creditMoneyOnce(UUID playerId, String currencyId, long amount, String sourceId) {
        if (sourceId == null || sourceId.isBlank() || amount <= 0L || playerId == null
                || currencyId == null || currencyId.isBlank() || creditedSources.contains(sourceId)) return false;
        creditMoney(playerId, currencyId, amount);
        creditedSources.add(sourceId);
        while (creditedSources.size() > 8192) creditedSources.remove(creditedSources.iterator().next());
        setDirty();
        return true;
    }

    /** Returns whether a durable money-credit receipt already exists. */
    public boolean hasCreditSource(String sourceId) {
        return sourceId != null && !sourceId.isBlank() && creditedSources.contains(sourceId);
    }

    public boolean hasTransferSource(String sourceId) {
        return sourceId != null && !sourceId.isBlank() && transferSources.contains(sourceId);
    }

    public boolean creditTransferOnce(UUID playerId, String currencyId, long amount, String sourceId) {
        if (sourceId == null || sourceId.isBlank() || amount <= 0L || playerId == null
                || currencyId == null || currencyId.isBlank() || transferSources.contains(sourceId)) return false;
        transferMoney.computeIfAbsent(playerId, k -> new HashMap<>()).merge(currencyId, amount, MarketMailboxSavedData::saturatingAdd);
        transferSources.add(sourceId);
        while (transferSources.size() > 8192) transferSources.remove(transferSources.iterator().next());
        setDirty();
        return true;
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

    /** Redeems only money, leaving any queued item delivery untouched. */
    public void redeemMoneyOnly(ServerPlayer player) {
        if (player == null) return;
        Map<String, Long> owedMoney = money.get(player.getUUID());
        if (owedMoney == null) return;
        boolean changed = false;
        var iterator = owedMoney.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            if (!Currencies.exists(entry.getKey())) continue;
            EconomyHelper.giveMoney(player, Currencies.byId(entry.getKey()), entry.getValue());
            iterator.remove();
            changed = true;
        }
        if (owedMoney.isEmpty()) money.remove(player.getUUID());
        if (changed) setDirty();
    }

    /** Redeems queued currency transfers without recording them as new income. */
    public void redeemTransferOnly(ServerPlayer player) {
        if (player == null) return;
        Map<String, Long> owed = transferMoney.get(player.getUUID());
        if (owed == null) return;
        boolean changed = false;
        var iterator = owed.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            if (!Currencies.exists(entry.getKey())) continue;
            EconomyHelper.giveMoneyWithoutIncome(player, Currencies.byId(entry.getKey()), entry.getValue());
            iterator.remove(); changed = true;
        }
        if (owed.isEmpty()) transferMoney.remove(player.getUUID());
        if (changed) setDirty();
    }

    /** Hands over and clears everything owed to this player. */
    public void redeem(ServerPlayer player) {
        UUID id = player.getUUID();
        boolean changed = false;

        Map<String, Long> owedMoney = money.get(id);
        if (owedMoney != null) {
            var iterator = owedMoney.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, Long> entry = iterator.next();
                if (!Currencies.exists(entry.getKey())) continue;
                EconomyHelper.giveMoney(player, Currencies.byId(entry.getKey()), entry.getValue());
                iterator.remove();
                changed = true;
            }
            if (owedMoney.isEmpty()) money.remove(id);
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

        ListTag transferList = new ListTag();
        for (Map.Entry<UUID, Map<String, Long>> entry : transferMoney.entrySet()) {
            CompoundTag nbt = new CompoundTag(); nbt.putUUID("uuid", entry.getKey());
            CompoundTag balances = new CompoundTag();
            for (Map.Entry<String, Long> e : entry.getValue().entrySet()) balances.putLong(e.getKey(), e.getValue());
            nbt.put("balances", balances); transferList.add(nbt);
        }
        tag.put("transferMoney", transferList);

        ListTag sourceList = new ListTag();
        for (String source : creditedSources) {
            CompoundTag entry = new CompoundTag();
            entry.putString("source", source);
            sourceList.add(entry);
        }
        tag.put("creditedSources", sourceList);

        ListTag transferSourceList = new ListTag();
        for (String source : transferSources) { CompoundTag entry = new CompoundTag(); entry.putString("source", source); transferSourceList.add(entry); }
        tag.put("transferSources", transferSourceList);

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

        ListTag transferList = tag.getList("transferMoney", Tag.TAG_COMPOUND);
        for (int i = 0; i < transferList.size(); i++) {
            CompoundTag nbt = transferList.getCompound(i); Map<String, Long> balances = new HashMap<>();
            CompoundTag balanceTag = nbt.getCompound("balances");
            for (String key : balanceTag.getAllKeys()) balances.put(key, balanceTag.getLong(key));
            if (!balances.isEmpty() && nbt.hasUUID("uuid")) data.transferMoney.put(nbt.getUUID("uuid"), balances);
        }

        ListTag sourceList = tag.getList("creditedSources", Tag.TAG_COMPOUND);
        for (int i = 0; i < sourceList.size(); i++) {
            String source = sourceList.getCompound(i).getString("source");
            if (!source.isBlank()) data.creditedSources.add(source);
        }
        while (data.creditedSources.size() > 8192) data.creditedSources.remove(data.creditedSources.iterator().next());

        ListTag transferSourceList = tag.getList("transferSources", Tag.TAG_COMPOUND);
        for (int i = 0; i < transferSourceList.size(); i++) {
            String source = transferSourceList.getCompound(i).getString("source");
            if (!source.isBlank()) data.transferSources.add(source);
        }
        while (data.transferSources.size() > 8192) data.transferSources.remove(data.transferSources.iterator().next());

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
