package com.ailudick.capitalismmod.population;

import com.ailudick.capitalismmod.Config;
import com.ailudick.capitalismmod.wallet.EconomyHelper;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Offline-safe receivables for player-owned housing; balances are paid out through the normal wallet. */
public final class PrivateLandlordSavedData extends SavedData {
    private static final String ID = "capitalismmod_private_landlords";
    private static final int MAX_RECEIPTS = 16384;
    private static final int MAX_WITHDRAWALS = 8192;
    private final Map<String, Long> balances = new HashMap<>();
    private final List<Receipt> receipts = new ArrayList<>();
    private final List<Withdrawal> withdrawals = new ArrayList<>();

    public record Receipt(String id, long day, String ownerId, long amount, long balanceAfter) {}
    public record Withdrawal(String id, long day, String ownerId, long amount, long balanceAfter) {}

    private PrivateLandlordSavedData() {}

    public static PrivateLandlordSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(PrivateLandlordSavedData::new, PrivateLandlordSavedData::load), ID);
    }

    public long balance(String ownerId) { return balances.getOrDefault(ownerId, 0L); }
    public List<Receipt> receipts() { return List.copyOf(receipts); }
    public Map<String, Long> balances() { return Map.copyOf(balances); }
    public List<Withdrawal> withdrawals() { return List.copyOf(withdrawals); }

    public boolean creditOnce(String paymentId, long day, String ownerId, long amount) {
        if (paymentId == null || paymentId.isBlank() || ownerId == null || ownerId.isBlank() || amount <= 0L) return false;
        Receipt existing = receipts.stream().filter(r -> r.id().equals(paymentId)).findFirst().orElse(null);
        if (existing != null) return existing.ownerId().equals(ownerId) && existing.amount() == amount;
        long current = balance(ownerId);
        if (current > Long.MAX_VALUE - amount) return false;
        long next = current + amount;
        balances.put(ownerId, next);
        receipts.add(new Receipt(paymentId, day, ownerId, amount, next));
        while (receipts.size() > MAX_RECEIPTS) receipts.remove(0);
        setDirty(); return true;
    }

    public boolean withdraw(ServerPlayer player, long amount) {
        if (player == null || amount <= 0L) return false;
        String ownerId = player.getUUID().toString();
        long current = balance(ownerId);
        if (current < amount) return false;
        long next = current - amount;
        balances.put(ownerId, next);
        withdrawals.add(new Withdrawal("withdraw:" + ownerId + ":" + player.getServer().overworld().getGameTime()
                + ":" + UUID.randomUUID(), player.getServer().overworld().getGameTime(), ownerId, amount, next));
        while (withdrawals.size() > MAX_WITHDRAWALS) withdrawals.remove(0);
        setDirty();
        EconomyHelper.giveMoney(player, Config.defaultCurrency(), amount);
        return true;
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag balanceList = new ListTag();
        balances.forEach((owner, balance) -> { CompoundTag e = new CompoundTag(); e.putString("owner", owner);
            e.putLong("balance", balance); balanceList.add(e); });
        tag.put("balances", balanceList);
        ListTag receiptList = new ListTag();
        for (Receipt r : receipts) { CompoundTag e = new CompoundTag(); e.putString("id", r.id()); e.putLong("day", r.day());
            e.putString("owner", r.ownerId()); e.putLong("amount", r.amount()); e.putLong("balance", r.balanceAfter()); receiptList.add(e); }
        tag.put("receipts", receiptList);
        ListTag withdrawalList = new ListTag();
        for (Withdrawal w : withdrawals) { CompoundTag e = new CompoundTag(); e.putString("id", w.id()); e.putLong("day", w.day());
            e.putString("owner", w.ownerId()); e.putLong("amount", w.amount()); e.putLong("balance", w.balanceAfter()); withdrawalList.add(e); }
        tag.put("withdrawals", withdrawalList); return tag;
    }

    public static PrivateLandlordSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        PrivateLandlordSavedData data = new PrivateLandlordSavedData();
        ListTag bs = tag.getList("balances", Tag.TAG_COMPOUND);
        for (int i = 0; i < bs.size(); i++) { CompoundTag e = bs.getCompound(i);
            if (!e.getString("owner").isBlank() && e.getLong("balance") > 0L) data.balances.put(e.getString("owner"), e.getLong("balance")); }
        ListTag rs = tag.getList("receipts", Tag.TAG_COMPOUND);
        for (int i = Math.max(0, rs.size() - MAX_RECEIPTS); i < rs.size(); i++) { CompoundTag e = rs.getCompound(i);
            if (!e.getString("id").isBlank() && !e.getString("owner").isBlank() && e.getLong("amount") > 0L)
                data.receipts.add(new Receipt(e.getString("id"), e.getLong("day"), e.getString("owner"),
                        e.getLong("amount"), Math.max(0L, e.getLong("balance")))); }
        ListTag ws = tag.getList("withdrawals", Tag.TAG_COMPOUND);
        for (int i = Math.max(0, ws.size() - MAX_WITHDRAWALS); i < ws.size(); i++) { CompoundTag e = ws.getCompound(i);
            if (!e.getString("id").isBlank() && !e.getString("owner").isBlank() && e.getLong("amount") > 0L)
                data.withdrawals.add(new Withdrawal(e.getString("id"), e.getLong("day"), e.getString("owner"),
                        e.getLong("amount"), Math.max(0L, e.getLong("balance")))); }
        return data;
    }
}
