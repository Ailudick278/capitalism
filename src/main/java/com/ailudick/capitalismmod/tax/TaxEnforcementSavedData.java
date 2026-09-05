package com.ailudick.capitalismmod.tax;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

/** Tracks tax delinquency notices without owning land freezes or auctions. */
public final class TaxEnforcementSavedData extends SavedData {
    private static final String ID = "capitalismmod_tax_enforcement";
    private final Map<String, Notice> notices = new HashMap<>();
    public record Notice(long firstAt, long lastNoticeAt, int noticeCount) {}
    private TaxEnforcementSavedData() {}

    public static TaxEnforcementSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(TaxEnforcementSavedData::new, TaxEnforcementSavedData::load), ID);
    }

    public Notice notice(String billId) { return notices.get(billId); }

    public boolean shouldNotify(String billId, long now) {
        Notice notice = notices.get(billId);
        return notice == null || now - notice.lastNoticeAt() >= 3L * 24000L;
    }

    public void recordNotice(String billId, long now) {
        Notice old = notices.get(billId);
        notices.put(billId, old == null ? new Notice(now, now, 1)
                : new Notice(old.firstAt(), now, old.noticeCount() + 1));
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        notices.forEach((billId, notice) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("billId", billId);
            entry.putLong("firstAt", notice.firstAt());
            entry.putLong("lastNoticeAt", notice.lastNoticeAt());
            entry.putInt("count", notice.noticeCount());
            list.add(entry);
        });
        tag.put("notices", list);
        return tag;
    }

    public static TaxEnforcementSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        TaxEnforcementSavedData data = new TaxEnforcementSavedData();
        ListTag list = tag.getList("notices", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            String billId = entry.getString("billId");
            if (!billId.isBlank()) data.notices.put(billId, new Notice(entry.getLong("firstAt"),
                    entry.getLong("lastNoticeAt"), entry.getInt("count")));
        }
        return data;
    }
}
