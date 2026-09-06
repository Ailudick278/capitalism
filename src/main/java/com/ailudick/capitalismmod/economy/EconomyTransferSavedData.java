package com.ailudick.capitalismmod.economy;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Persistent request receipts for user-initiated economic transfers. */
public final class EconomyTransferSavedData extends SavedData {
    private static final String ID = "capitalismmod_economy_transfers";
    private static final int MAX_RECEIPTS = 4096;
    private final List<Receipt> receipts = new ArrayList<>();

    public record Receipt(UUID requestId, UUID senderId, String fromAccountId, String targetAccountId,
                          String currencyId, long amount, long gameTime) {
    }

    private EconomyTransferSavedData() {
    }

    public static EconomyTransferSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(EconomyTransferSavedData::new, EconomyTransferSavedData::load), ID);
    }

    public Receipt find(UUID requestId) {
        if (requestId == null) return null;
        for (int i = receipts.size() - 1; i >= 0; i--) {
            Receipt receipt = receipts.get(i);
            if (requestId.equals(receipt.requestId())) return receipt;
        }
        return null;
    }

    public void record(Receipt receipt) {
        if (receipt == null || receipt.requestId() == null || receipt.senderId() == null
                || receipt.amount() <= 0L || find(receipt.requestId()) != null) return;
        receipts.add(receipt);
        while (receipts.size() > MAX_RECEIPTS) receipts.remove(0);
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Receipt receipt : receipts) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("requestId", receipt.requestId());
            entry.putUUID("sender", receipt.senderId());
            entry.putString("from", receipt.fromAccountId());
            entry.putString("target", receipt.targetAccountId());
            entry.putString("currency", receipt.currencyId());
            entry.putLong("amount", receipt.amount());
            entry.putLong("time", receipt.gameTime());
            list.add(entry);
        }
        tag.put("receipts", list);
        return tag;
    }

    public static EconomyTransferSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        EconomyTransferSavedData data = new EconomyTransferSavedData();
        ListTag list = tag.getList("receipts", Tag.TAG_COMPOUND);
        for (int i = Math.max(0, list.size() - MAX_RECEIPTS); i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (!entry.hasUUID("requestId") || !entry.hasUUID("sender")
                    || entry.getString("from").isBlank() || entry.getString("target").isBlank()
                    || entry.getString("currency").isBlank() || entry.getLong("amount") <= 0L) continue;
            data.receipts.add(new Receipt(entry.getUUID("requestId"), entry.getUUID("sender"),
                    entry.getString("from"), entry.getString("target"), entry.getString("currency"),
                    entry.getLong("amount"), entry.getLong("time")));
        }
        return data;
    }
}
