package com.ailudick.capitalismmod.tax;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Persistent delivery queue for tax notices generated while a player is offline. */
public final class TaxNotificationSavedData extends SavedData {
    private static final String ID = "capitalismmod_tax_notifications";
    private final List<Notification> notifications = new ArrayList<>();

    public record Notification(UUID playerUuid, String noticeId, long time, String message, boolean read) {}

    private TaxNotificationSavedData() {}

    public static TaxNotificationSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(TaxNotificationSavedData::new, TaxNotificationSavedData::load), ID);
    }

    public void add(Notification notification) {
        if (notifications.stream().anyMatch(existing -> existing.playerUuid().equals(notification.playerUuid())
                && existing.noticeId().equals(notification.noticeId()))) return;
        notifications.add(notification);
        while (notifications.size() > 1024) notifications.remove(0);
        setDirty();
    }

    public List<Notification> unreadFor(UUID playerUuid) {
        return notifications.stream().filter(entry -> entry.playerUuid().equals(playerUuid) && !entry.read()).toList();
    }

    public void markRead(UUID playerUuid) {
        boolean changed = false;
        for (int i = 0; i < notifications.size(); i++) {
            Notification entry = notifications.get(i);
            if (entry.playerUuid().equals(playerUuid) && !entry.read()) {
                notifications.set(i, new Notification(entry.playerUuid(), entry.noticeId(), entry.time(), entry.message(), true));
                changed = true;
            }
        }
        if (changed) setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Notification notification : notifications) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("player", notification.playerUuid());
            entry.putString("noticeId", notification.noticeId());
            entry.putLong("time", notification.time());
            entry.putString("message", notification.message());
            entry.putBoolean("read", notification.read());
            list.add(entry);
        }
        tag.put("notifications", list);
        return tag;
    }

    public static TaxNotificationSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        TaxNotificationSavedData data = new TaxNotificationSavedData();
        ListTag list = tag.getList("notifications", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (entry.hasUUID("player") && !entry.getString("message").isBlank()) {
                data.notifications.add(new Notification(entry.getUUID("player"), entry.getString("noticeId"),
                        entry.getLong("time"), entry.getString("message"), entry.getBoolean("read")));
            }
        }
        return data;
    }
}
