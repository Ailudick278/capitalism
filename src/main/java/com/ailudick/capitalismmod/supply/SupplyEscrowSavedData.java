package com.ailudick.capitalismmod.supply;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Persistent escrow balance for prepaid supply orders and their release/refund phases. */
public final class SupplyEscrowSavedData extends SavedData {
    private static final String ID = "capitalismmod_supply_escrow";
    private static final int MAX_ORDERS = 16384;
    private static final int MAX_OPERATIONS = 32768;
    private final List<Escrow> escrows = new ArrayList<>();
    private final Set<String> operations = new HashSet<>();

    public record Escrow(String orderId, long originalMinor, long heldMinor,
                         long releasedMinor, long refundedMinor) {}

    private SupplyEscrowSavedData() {}

    public static SupplyEscrowSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(SupplyEscrowSavedData::new, SupplyEscrowSavedData::load), ID);
    }

    public Escrow escrow(String orderId) { return escrows.stream().filter(e -> e.orderId().equals(orderId))
            .findFirst().orElse(null); }
    public List<Escrow> escrows() { return List.copyOf(escrows); }

    public boolean createOnce(String orderId, long amountMinor) {
        if (orderId == null || orderId.isBlank() || amountMinor <= 0L) return false;
        if (escrow(orderId) != null) return true;
        escrows.add(new Escrow(orderId, amountMinor, amountMinor, 0L, 0L));
        while (escrows.size() > MAX_ORDERS) escrows.remove(0);
        setDirty(); return true;
    }

    public boolean releaseOnce(String orderId, String operationId, long amountMinor) {
        return move(orderId, "release:" + operationId, amountMinor, true);
    }

    public boolean refundOnce(String orderId, String operationId, long amountMinor) {
        return move(orderId, "refund:" + operationId, amountMinor, false);
    }

    private boolean move(String orderId, String operationId, long amountMinor, boolean release) {
        if (orderId == null || orderId.isBlank() || operationId == null || operationId.isBlank() || amountMinor <= 0L) return false;
        if (operations.contains(operationId)) return true;
        Escrow current = escrow(orderId);
        if (current == null) return true; // legacy orders have no escrow journal
        if (amountMinor > current.heldMinor()) return false;
        Escrow next = release
                ? new Escrow(orderId, current.originalMinor(), current.heldMinor() - amountMinor,
                add(current.releasedMinor(), amountMinor), current.refundedMinor())
                : new Escrow(orderId, current.originalMinor(), current.heldMinor() - amountMinor,
                current.releasedMinor(), add(current.refundedMinor(), amountMinor));
        escrows.set(escrows.indexOf(current), next);
        operations.add(operationId);
        while (operations.size() > MAX_OPERATIONS) operations.remove(operations.iterator().next());
        setDirty(); return true;
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Escrow e : escrows) { CompoundTag n = new CompoundTag(); n.putString("order", e.orderId());
            n.putLong("original", e.originalMinor()); n.putLong("held", e.heldMinor());
            n.putLong("released", e.releasedMinor()); n.putLong("refunded", e.refundedMinor()); list.add(n); }
        tag.put("escrows", list);
        ListTag ops = new ListTag(); operations.forEach(id -> { CompoundTag n = new CompoundTag(); n.putString("id", id); ops.add(n); });
        tag.put("operations", ops); return tag;
    }

    public static SupplyEscrowSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        SupplyEscrowSavedData data = new SupplyEscrowSavedData();
        ListTag list = tag.getList("escrows", Tag.TAG_COMPOUND);
        for (int i = Math.max(0, list.size() - MAX_ORDERS); i < list.size(); i++) { CompoundTag n = list.getCompound(i);
            if (!n.getString("order").isBlank() && n.getLong("original") > 0L)
                data.escrows.add(new Escrow(n.getString("order"), n.getLong("original"), Math.max(0L, n.getLong("held")),
                        Math.max(0L, n.getLong("released")), Math.max(0L, n.getLong("refunded")))); }
        ListTag ops = tag.getList("operations", Tag.TAG_COMPOUND);
        for (int i = Math.max(0, ops.size() - MAX_OPERATIONS); i < ops.size(); i++) { String id = ops.getCompound(i).getString("id");
            if (!id.isBlank()) data.operations.add(id); }
        return data;
    }

    private static long add(long left, long right) { try { return Math.addExact(left, right); } catch (ArithmeticException e) { return Long.MAX_VALUE; } }
}
