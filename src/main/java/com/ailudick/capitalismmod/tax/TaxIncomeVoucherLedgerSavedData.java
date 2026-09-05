package com.ailudick.capitalismmod.tax;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Persistent sales-income vouchers used to audit taxable business revenue. */
public final class TaxIncomeVoucherLedgerSavedData extends SavedData {
    private static final String ID = "capitalismmod_tax_income_vouchers";
    public record Voucher(String id, UUID taxpayerUuid, String subjectId, String category,
                          String currencyId, long amount, long occurredAt, String sourceId, String details) {}
    private final List<Voucher> vouchers = new ArrayList<>();
    private TaxIncomeVoucherLedgerSavedData() {}
    public static TaxIncomeVoucherLedgerSavedData get(MinecraftServer server) { return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(TaxIncomeVoucherLedgerSavedData::new, TaxIncomeVoucherLedgerSavedData::load), ID); }
    public List<Voucher> all() { return List.copyOf(vouchers); }
    public List<Voucher> forTaxpayer(UUID uuid) { return vouchers.stream().filter(v -> v.taxpayerUuid().equals(uuid)).toList(); }
    public boolean containsSource(String source) { return vouchers.stream().anyMatch(v -> v.sourceId().equals(source)); }
    public void add(Voucher voucher) { if (voucher == null || voucher.amount() <= 0L || containsSource(voucher.sourceId())) return; vouchers.add(voucher); setDirty(); }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Voucher v : vouchers) { CompoundTag e = new CompoundTag(); e.putString("id", v.id()); e.putUUID("taxpayer", v.taxpayerUuid()); e.putString("subject", v.subjectId()); e.putString("category", v.category()); e.putString("currency", v.currencyId()); e.putLong("amount", v.amount()); e.putLong("occurredAt", v.occurredAt()); e.putString("source", v.sourceId()); e.putString("details", v.details()); list.add(e); }
        tag.put("vouchers", list); return tag;
    }
    public static TaxIncomeVoucherLedgerSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        TaxIncomeVoucherLedgerSavedData data = new TaxIncomeVoucherLedgerSavedData(); ListTag list = tag.getList("vouchers", 10);
        for (int i = 0; i < list.size(); i++) { CompoundTag e = list.getCompound(i); if (!e.hasUUID("taxpayer") || e.getLong("amount") <= 0L) continue; data.vouchers.add(new Voucher(e.getString("id"), e.getUUID("taxpayer"), e.getString("subject"), e.getString("category"), e.getString("currency"), e.getLong("amount"), e.getLong("occurredAt"), e.getString("source"), e.getString("details"))); }
        return data;
    }
}
