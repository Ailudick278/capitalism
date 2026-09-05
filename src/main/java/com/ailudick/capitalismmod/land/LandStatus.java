package com.ailudick.capitalismmod.land;

/** Single source of truth for the user-visible lifecycle state of a land claim. */
public enum LandStatus {
    NORMAL("正常"), TAX_DEBT("欠税"), GRACE_PERIOD("宽限期"), TAX_FROZEN("逾期冻结"), AUCTION("拍卖中");
    private final String displayName;
    LandStatus(String displayName) { this.displayName = displayName; }
    public String displayName() { return displayName; }
    public static LandStatus resolve(long taxOwed, long dueAt, long graceUntil,
                                     boolean auction, long gameTime) {
        if (auction) return AUCTION;
        if (taxOwed <= 0L) return NORMAL;
        if (graceUntil > 0L && gameTime > graceUntil) return TAX_FROZEN;
        if (dueAt > 0L && gameTime > dueAt) return GRACE_PERIOD;
        return TAX_DEBT;
    }
}
