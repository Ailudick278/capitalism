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

/** Bounded persistent record of completed shop, commodity, and stock trades. */
public final class MarketTradeSavedData extends SavedData {
    private static final String ID = "capitalismmod_market_trades";
    private static final int MAX_TRADES = 5000;
    private final List<Trade> trades = new ArrayList<>();

    public record Trade(long gameTime, UUID buyer, UUID seller, String itemId, int quantity,
                        String currencyId, long total, String market, long fee) {
        public boolean involves(UUID playerId) {
            return playerId != null && (playerId.equals(buyer) || playerId.equals(seller));
        }
    }

    private MarketTradeSavedData() {
    }

    public static MarketTradeSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(MarketTradeSavedData::new, MarketTradeSavedData::load), ID);
    }

    public List<Trade> trades() {
        return List.copyOf(trades);
    }

    public void add(Trade trade) {
        if (trade == null || trade.quantity() <= 0 || trade.total() < 0 || trade.fee() < 0
                || trade.itemId() == null || trade.currencyId() == null || trade.market() == null) {
            return;
        }
        trades.add(trade);
        if (trades.size() > MAX_TRADES) {
            trades.subList(0, trades.size() - MAX_TRADES).clear();
        }
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Trade trade : trades) {
            CompoundTag nbt = new CompoundTag();
            nbt.putLong("time", trade.gameTime());
            if (trade.buyer() != null) nbt.putUUID("buyer", trade.buyer());
            if (trade.seller() != null) nbt.putUUID("seller", trade.seller());
            nbt.putString("item", trade.itemId());
            nbt.putInt("quantity", trade.quantity());
            nbt.putString("currency", trade.currencyId());
            nbt.putLong("total", trade.total());
            nbt.putString("market", trade.market());
            nbt.putLong("fee", trade.fee());
            list.add(nbt);
        }
        tag.put("trades", list);
        return tag;
    }

    public static MarketTradeSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        MarketTradeSavedData data = new MarketTradeSavedData();
        ListTag list = tag.getList("trades", Tag.TAG_COMPOUND);
        for (int i = Math.max(0, list.size() - MAX_TRADES); i < list.size(); i++) {
            CompoundTag nbt = list.getCompound(i);
            int quantity = nbt.getInt("quantity");
            long total = nbt.getLong("total");
            if (quantity > 0 && total >= 0) {
                data.trades.add(new Trade(nbt.getLong("time"),
                        nbt.hasUUID("buyer") ? nbt.getUUID("buyer") : null,
                        nbt.hasUUID("seller") ? nbt.getUUID("seller") : null,
                        nbt.getString("item"), quantity, nbt.getString("currency"), total,
                        nbt.contains("market") ? nbt.getString("market") : "unknown",
                        Math.max(0L, nbt.getLong("fee"))));
            }
        }
        return data;
    }
}
