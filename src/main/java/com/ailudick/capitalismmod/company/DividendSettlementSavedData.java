package com.ailudick.capitalismmod.company;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Durable shareholder payout instructions for crash-safe dividend recovery. */
public final class DividendSettlementSavedData extends SavedData {
    private static final String ID = "capitalismmod_dividend_settlements";
    private static final int MAX_PAYOUTS = 32768;
    private final List<Payout> payouts = new ArrayList<>();

    public record Payout(String source, String companyId, UUID recipient, String currencyId,
                         long amountMinor, long gameTime) {}

    private DividendSettlementSavedData() {}

    public static DividendSettlementSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(DividendSettlementSavedData::new, DividendSettlementSavedData::load), ID);
    }

    public List<Payout> payouts() { return List.copyOf(payouts); }

    public void append(Payout payout) {
        if (payout == null || payout.source() == null || payout.source().isBlank()
                || payout.companyId() == null || payout.companyId().isBlank() || payout.recipient() == null
                || payout.currencyId() == null || payout.currencyId().isBlank() || payout.amountMinor() <= 0L
                || payouts.stream().anyMatch(existing -> payout.source().equals(existing.source()))) return;
        payouts.add(payout);
        while (payouts.size() > MAX_PAYOUTS) payouts.remove(0);
        setDirty();
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Payout payout : payouts) {
            CompoundTag entry = new CompoundTag();
            entry.putString("source", payout.source());
            entry.putString("company", payout.companyId());
            entry.putUUID("recipient", payout.recipient());
            entry.putString("currency", payout.currencyId());
            entry.putLong("amount", payout.amountMinor());
            entry.putLong("gameTime", payout.gameTime());
            list.add(entry);
        }
        tag.put("payouts", list);
        return tag;
    }

    public static DividendSettlementSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        DividendSettlementSavedData data = new DividendSettlementSavedData();
        ListTag list = tag.getList("payouts", Tag.TAG_COMPOUND);
        for (int i = Math.max(0, list.size() - MAX_PAYOUTS); i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (!entry.getString("source").isBlank() && !entry.getString("company").isBlank()
                    && entry.hasUUID("recipient") && !entry.getString("currency").isBlank()
                    && entry.getLong("amount") > 0L) {
                data.payouts.add(new Payout(entry.getString("source"), entry.getString("company"),
                        entry.getUUID("recipient"), entry.getString("currency"), entry.getLong("amount"),
                        entry.getLong("gameTime")));
            }
        }
        return data;
    }
}
