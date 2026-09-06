package com.ailudick.capitalismmod.business;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

/** Persistent money custody journal for each individual-business order batch. */
public final class BusinessOrderEscrowSavedData extends SavedData {
    private static final String ID = "capitalismmod_business_order_escrow";
    private static final int MAX_ESCROWS = 16384;
    private final List<Escrow> escrows = new ArrayList<>();

    public record Escrow(String orderId, String batchId, String buyerId, int quantity, long originalMinor,
                         long heldMinor, long releasedMinor, long refundedMinor) {}

    private BusinessOrderEscrowSavedData() {}

    public static BusinessOrderEscrowSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(BusinessOrderEscrowSavedData::new, BusinessOrderEscrowSavedData::load), ID);
    }

    public Escrow find(String orderId, String batchId) {
        return escrows.stream().filter(e -> e.orderId().equals(orderId) && e.batchId().equals(batchId))
                .findFirst().orElse(null);
    }

    public List<Escrow> escrows() { return List.copyOf(escrows); }

    public boolean createOnce(String orderId, String batchId, String buyerId, long amountMinor) {
        return createOnce(orderId, batchId, buyerId, 0, amountMinor);
    }

    public boolean createOnce(String orderId, String batchId, String buyerId, int quantity, long amountMinor) {
        if (orderId == null || orderId.isBlank() || batchId == null || batchId.isBlank()
                || buyerId == null || buyerId.isBlank() || quantity < 0 || amountMinor <= 0L) return false;
        Escrow existing = find(orderId, batchId);
        if (existing != null) {
            return existing.buyerId().equals(buyerId) && existing.originalMinor() == amountMinor
                    && (existing.quantity() == 0 || quantity == 0 || existing.quantity() == quantity);
        }
        escrows.add(new Escrow(orderId, batchId, buyerId, quantity, amountMinor, amountMinor, 0L, 0L));
        while (escrows.size() > MAX_ESCROWS) escrows.remove(0);
        setDirty();
        return true;
    }

    public boolean releaseOnce(String orderId, String batchId, long amountMinor) {
        return move(orderId, batchId, amountMinor, true);
    }

    public boolean refundOnce(String orderId, String batchId, long amountMinor) {
        return move(orderId, batchId, amountMinor, false);
    }

    private boolean move(String orderId, String batchId, long amountMinor, boolean release) {
        if (amountMinor <= 0L) return false;
        Escrow current = find(orderId, batchId);
        if (current == null) return true; // legacy batches have no escrow journal
        if (release && current.releasedMinor() >= amountMinor) return true;
        if (!release && current.refundedMinor() >= amountMinor) return true;
        if (amountMinor > current.heldMinor()) return false;
        Escrow next = release
                ? new Escrow(current.orderId(), current.batchId(), current.buyerId(), current.quantity(), current.originalMinor(),
                current.heldMinor() - amountMinor, add(current.releasedMinor(), amountMinor), current.refundedMinor())
                : new Escrow(current.orderId(), current.batchId(), current.buyerId(), current.quantity(), current.originalMinor(),
                current.heldMinor() - amountMinor, current.releasedMinor(), add(current.refundedMinor(), amountMinor));
        escrows.set(escrows.indexOf(current), next);
        setDirty();
        return true;
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Escrow e : escrows) {
            CompoundTag n = new CompoundTag();
            n.putString("order", e.orderId()); n.putString("batch", e.batchId()); n.putString("buyer", e.buyerId());
            n.putInt("quantity", e.quantity());
            n.putLong("original", e.originalMinor()); n.putLong("held", e.heldMinor());
            n.putLong("released", e.releasedMinor()); n.putLong("refunded", e.refundedMinor());
            list.add(n);
        }
        tag.put("escrows", list);
        return tag;
    }

    public static BusinessOrderEscrowSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        BusinessOrderEscrowSavedData data = new BusinessOrderEscrowSavedData();
        ListTag list = tag.getList("escrows", Tag.TAG_COMPOUND);
        for (int i = Math.max(0, list.size() - MAX_ESCROWS); i < list.size(); i++) {
            CompoundTag n = list.getCompound(i);
            if (!n.getString("order").isBlank() && !n.getString("batch").isBlank()
                    && !n.getString("buyer").isBlank() && n.getLong("original") > 0L) {
                data.escrows.add(new Escrow(n.getString("order"), n.getString("batch"), n.getString("buyer"),
                        Math.max(0, n.getInt("quantity")), n.getLong("original"), Math.max(0L, n.getLong("held")),
                        Math.max(0L, n.getLong("released")), Math.max(0L, n.getLong("refunded"))));
            }
        }
        return data;
    }

    private static long add(long left, long right) {
        try { return Math.addExact(left, right); } catch (ArithmeticException e) { return Long.MAX_VALUE; }
    }
}
