package com.ailudick.capitalismmod.tax;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Persistent VAT invoice-like records used to audit output and input tax. */
public final class TaxInvoiceSavedData extends SavedData {
    private static final String ID = "capitalismmod_tax_invoices";
    public record Invoice(String id, String sourceEventId, UUID taxpayerUuid, String currencyId,
                          long grossAmount, long taxAmount, long creditApplied, long issuedAt,
                          String direction) {}

    private final List<Invoice> invoices = new ArrayList<>();

    private TaxInvoiceSavedData() {}

    public static TaxInvoiceSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(TaxInvoiceSavedData::new, TaxInvoiceSavedData::load), ID);
    }

    public List<Invoice> forTaxpayer(UUID taxpayerUuid) {
        return invoices.stream().filter(invoice -> invoice.taxpayerUuid().equals(taxpayerUuid)).toList();
    }

    public List<Invoice> invoices() { return List.copyOf(invoices); }

    public Invoice findBySource(String sourceEventId) {
        for (Invoice invoice : invoices) {
            if (invoice.sourceEventId().equals(sourceEventId)) return invoice;
        }
        return null;
    }

    public void add(Invoice invoice) {
        if (invoice == null || invoice.sourceEventId().isBlank() || invoice.taxpayerUuid() == null
                || findBySource(invoice.sourceEventId()) != null) return;
        invoices.add(invoice);
        while (invoices.size() > 8192) invoices.remove(0);
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Invoice invoice : invoices) {
            CompoundTag entry = new CompoundTag();
            entry.putString("id", invoice.id());
            entry.putString("source", invoice.sourceEventId());
            entry.putUUID("taxpayer", invoice.taxpayerUuid());
            entry.putString("currency", invoice.currencyId());
            entry.putLong("gross", invoice.grossAmount());
            entry.putLong("tax", invoice.taxAmount());
            entry.putLong("credit", invoice.creditApplied());
            entry.putLong("issuedAt", invoice.issuedAt());
            entry.putString("direction", invoice.direction());
            list.add(entry);
        }
        tag.put("invoices", list);
        return tag;
    }

    public static TaxInvoiceSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        TaxInvoiceSavedData data = new TaxInvoiceSavedData();
        ListTag list = tag.getList("invoices", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (!entry.hasUUID("taxpayer") || entry.getString("source").isBlank()) continue;
            Invoice invoice = new Invoice(entry.getString("id"), entry.getString("source"),
                    entry.getUUID("taxpayer"), entry.getString("currency"), entry.getLong("gross"),
                    entry.getLong("tax"), entry.getLong("credit"), entry.getLong("issuedAt"),
                    entry.getString("direction"));
            if (data.invoices.stream().noneMatch(existing -> existing.sourceEventId().equals(invoice.sourceEventId()))) {
                data.invoices.add(invoice);
            }
        }
        return data;
    }
}
