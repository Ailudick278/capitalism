package com.ailudick.capitalismmod.loan;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Persistent delivery queue for peer-loan notices generated while players are offline. */
public final class PeerLoanNotificationSavedData extends SavedData {
    private static final String ID = "capitalismmod_peer_loan_notifications";
    private final List<Notification> notifications = new ArrayList<>();

    public record Notification(UUID playerUuid, String noticeId, long time, String message, boolean read) {}

    private PeerLoanNotificationSavedData() {}

    public static PeerLoanNotificationSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(PeerLoanNotificationSavedData::new, PeerLoanNotificationSavedData::load), ID);
    }

    public void add(Notification notification) {
        if (notification == null || notification.playerUuid() == null || notification.noticeId().isBlank()) return;
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

    public static PeerLoanNotificationSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        PeerLoanNotificationSavedData data = new PeerLoanNotificationSavedData();
        ListTag list = tag.getList("notifications", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (entry.hasUUID("player") && !entry.getString("noticeId").isBlank()
                    && !entry.getString("message").isBlank()) {
                data.notifications.add(new Notification(entry.getUUID("player"), entry.getString("noticeId"),
                        entry.getLong("time"), entry.getString("message"), entry.getBoolean("read")));
            }
        }
        return data;
    }
}
