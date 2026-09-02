package com.ailudick.capitalismmod.currency;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * The current exchange rates, in base units (fen per currency unit).
 *
 * <p>The <b>anchor</b> rate is the real-world rate, fetched periodically and falling
 * back to each currency's fixed {@link Currency#baseValue()}. The <b>effective</b> rate
 * is the anchor adjusted by in-game supply/demand — a hook reserved for a future
 * foreign-exchange market, currently a no-op.
 */
public final class ExchangeRateProvider {
    private static final Map<String, Long> ANCHORS = new ConcurrentHashMap<>();
    private static volatile boolean live = false;
    private static volatile String lastUpdated = "尚未更新";
    private static final DateTimeFormatter UPDATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    static {
        for (Currency currency : Currencies.ALL) {
            ANCHORS.put(currency.id(), currency.baseValue());
        }
    }

    private ExchangeRateProvider() {
    }

    /** The effective rate (fen per unit) used for all conversions. */
    public static long effective(Currency currency) {
        return applySupplyDemand(anchor(currency), currency);
    }

    /** The real-world anchor rate, falling back to the fixed default. */
    public static long anchor(Currency currency) {
        return ANCHORS.getOrDefault(currency.id(), currency.baseValue());
    }

    /** Whether the anchors come from a live fetch (as opposed to the fixed defaults). */
    public static boolean isLive() {
        return live;
    }

    /** Updates the anchor for a currency (called by the fetcher). */
    public static void setAnchor(String currencyId, long fenPerUnit) {
        if (fenPerUnit > 0) {
            ANCHORS.put(currencyId, fenPerUnit);
        }
    }

    /** Marks the anchors as freshly fetched. */
    public static void setLive(boolean value) {
        live = value;
    }

    /** Records the local time when a complete live-rate update finished. */
    public static void markUpdated() {
        lastUpdated = OffsetDateTime.now(ZoneId.systemDefault()).format(UPDATE_FORMAT);
    }

    public static String lastUpdated() {
        return lastUpdated;
    }

    public static Map<String, Long> snapshot() {
        return new HashMap<>(ANCHORS);
    }

    public static void applySnapshot(Map<String, Long> anchors, String updatedAt, boolean liveValue) {
        anchors.forEach(ExchangeRateProvider::setAnchor);
        lastUpdated = updatedAt;
        live = liveValue;
    }

    /**
     * Reserved supply/demand adjustment: how in-game trading deviates the effective
     * rate from its anchor. Returns the anchor unchanged for now; a future market
     * can drive this with trading volume (e.g. {@code anchor + offset}).
     */
    public static long applySupplyDemand(long anchor, Currency currency) {
        return anchor;
    }
}
