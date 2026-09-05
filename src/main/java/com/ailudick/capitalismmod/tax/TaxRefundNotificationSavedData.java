package com.ailudick.capitalismmod.tax;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class TaxRefundNotificationSavedData extends SavedData {
    private static final String ID = "capitalismmod_tax_refund_notifications";
    private final List<Notification> notifications = new ArrayList<>();
    public record Notification(UUID playerUuid, String requestId, long time, String message, boolean read) {
        public Notification(UUID playerUuid, long time, String message) { this(playerUuid, "", time, message, false); }
        public Notification(UUID playerUuid, String requestId, long time, String message) { this(playerUuid, requestId, time, message, false); }
    }
    private TaxRefundNotificationSavedData() {}
    public static TaxRefundNotificationSavedData get(MinecraftServer server) { return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(TaxRefundNotificationSavedData::new, TaxRefundNotificationSavedData::load), ID); }
    public void add(Notification notification) { notifications.add(notification); while (notifications.size() > 1024) notifications.remove(0); setDirty(); }
    public List<Notification> forPlayer(UUID uuid) { return notifications.stream().filter(n -> n.playerUuid().equals(uuid)).toList(); }
    public List<Notification> unreadFor(UUID uuid) { return notifications.stream().filter(n -> n.playerUuid().equals(uuid) && !n.read()).toList(); }
    public void markRead(UUID uuid) { for (int i = 0; i < notifications.size(); i++) { Notification n = notifications.get(i); if (n.playerUuid().equals(uuid) && !n.read()) notifications.set(i, new Notification(n.playerUuid(), n.requestId(), n.time(), n.message(), true)); } setDirty(); }
    public void clearReadFor(UUID uuid) { notifications.removeIf(n -> n.playerUuid().equals(uuid) && n.read()); setDirty(); }
    public void markReadOne(UUID uuid, String requestId) { for (int i = 0; i < notifications.size(); i++) { Notification n = notifications.get(i); if (n.playerUuid().equals(uuid) && n.requestId().equals(requestId) && !n.read()) notifications.set(i, new Notification(n.playerUuid(), n.requestId(), n.time(), n.message(), true)); } setDirty(); }
    public void deleteOne(UUID uuid, String requestId) { notifications.removeIf(n -> n.playerUuid().equals(uuid) && n.requestId().equals(requestId)); setDirty(); }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) { ListTag list = new ListTag(); notifications.forEach(n -> { CompoundTag e = new CompoundTag(); e.putUUID("player", n.playerUuid()); e.putString("request", n.requestId()); e.putLong("time", n.time()); e.putString("message", n.message()); e.putBoolean("read", n.read()); list.add(e); }); tag.put("notifications", list); return tag; }
    public static TaxRefundNotificationSavedData load(CompoundTag tag, HolderLookup.Provider registries) { TaxRefundNotificationSavedData data = new TaxRefundNotificationSavedData(); ListTag list = tag.getList("notifications", 10); for (int i = 0; i < list.size(); i++) { CompoundTag e = list.getCompound(i); if (e.hasUUID("player")) data.notifications.add(new Notification(e.getUUID("player"), e.getString("request"), e.getLong("time"), e.getString("message"), e.getBoolean("read"))); } return data; }
}
