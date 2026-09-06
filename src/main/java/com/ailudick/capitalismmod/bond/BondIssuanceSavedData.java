package com.ailudick.capitalismmod.bond;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Durable batches between successful bond funding and holding creation. */
public final class BondIssuanceSavedData extends SavedData {
    private static final String ID = "capitalismmod_bond_issuances";
    private static final int MAX_RECORDS = 4096;
    private final List<Issuance> issuances = new ArrayList<>();

    public record Issuance(String id, UUID holder, int count, long faceValue,
                           double ratePerYear, int days, boolean paymentConfirmed,
                           boolean funded, boolean holdingsCreated) {}

    private BondIssuanceSavedData() {}

    public static BondIssuanceSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(BondIssuanceSavedData::new, BondIssuanceSavedData::load), ID);
    }

    public void add(Issuance issuance) {
        if (issuance == null || issuance.id() == null || issuance.id().isBlank()
                || issuance.holder() == null || issuance.count() <= 0 || issuance.faceValue() <= 0L
                || !Double.isFinite(issuance.ratePerYear()) || issuance.ratePerYear() < 0.0
                || issuance.days() <= 0 || issuances.size() >= MAX_RECORDS) return;
        issuances.add(issuance);
        setDirty();
    }

    public void replace(Issuance issuance) {
        if (issuance == null || issuance.id() == null || issuance.id().isBlank()) return;
        for (int i = 0; i < issuances.size(); i++) {
            if (issuances.get(i).id().equals(issuance.id())) {
                issuances.set(i, issuance);
                setDirty();
                return;
            }
        }
    }

    public void remove(String id) {
        if (id != null && issuances.removeIf(issuance -> id.equals(issuance.id()))) setDirty();
    }

    public List<Issuance> pendingFunded() {
        return issuances.stream().filter(issuance -> issuance.funded() && !issuance.holdingsCreated()).toList();
    }

    public List<Issuance> pendingPayment() {
        return issuances.stream().filter(issuance -> !issuance.funded()).toList();
    }

    public void markFunded(String id) {
        for (int i = 0; i < issuances.size(); i++) {
            Issuance current = issuances.get(i);
            if (!current.id().equals(id) || current.funded()) continue;
            issuances.set(i, new Issuance(current.id(), current.holder(), current.count(), current.faceValue(),
                    current.ratePerYear(), current.days(), current.paymentConfirmed(), true, current.holdingsCreated()));
            setDirty();
            return;
        }
    }

    public void markHoldingsCreated(String id) {
        for (int i = 0; i < issuances.size(); i++) {
            Issuance current = issuances.get(i);
            if (!current.id().equals(id) || current.holdingsCreated()) continue;
            issuances.set(i, new Issuance(current.id(), current.holder(), current.count(), current.faceValue(),
                    current.ratePerYear(), current.days(), current.paymentConfirmed(), current.funded(), true));
            setDirty();
            return;
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Issuance issuance : issuances) {
            CompoundTag entry = new CompoundTag();
            entry.putString("id", issuance.id());
            entry.putUUID("holder", issuance.holder());
            entry.putInt("count", issuance.count());
            entry.putLong("faceValue", issuance.faceValue());
            entry.putDouble("rate", issuance.ratePerYear());
            entry.putInt("days", issuance.days());
            entry.putBoolean("paymentConfirmed", issuance.paymentConfirmed());
            entry.putBoolean("funded", issuance.funded());
            entry.putBoolean("holdingsCreated", issuance.holdingsCreated());
            list.add(entry);
        }
        tag.put("issuances", list);
        return tag;
    }

    public static BondIssuanceSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        BondIssuanceSavedData data = new BondIssuanceSavedData();
        ListTag list = tag.getList("issuances", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(list.size(), MAX_RECORDS); i++) {
            CompoundTag entry = list.getCompound(i);
            if (!entry.hasUUID("holder") || entry.getString("id").isBlank()) continue;
            data.issuances.add(new Issuance(entry.getString("id"), entry.getUUID("holder"),
                    entry.getInt("count"), entry.getLong("faceValue"), entry.getDouble("rate"),
                    entry.getInt("days"), entry.getBoolean("paymentConfirmed"), entry.getBoolean("funded"),
                    entry.getBoolean("holdingsCreated")));
        }
        return data;
    }
}
