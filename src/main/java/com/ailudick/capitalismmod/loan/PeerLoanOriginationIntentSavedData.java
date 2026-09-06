package com.ailudick.capitalismmod.loan;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Durable intent for a peer-loan origination interrupted during disbursement. */
public final class PeerLoanOriginationIntentSavedData extends SavedData {
    private static final String ID = "capitalismmod_peer_loan_origination_intents";
    private static final int MAX_INTENTS = 8192;
    private final List<Intent> intents = new ArrayList<>();

    public record Intent(String loanId, UUID lender, UUID borrower, String currencyId, long principal,
                         double ratePerYear, int days, boolean paid) {
        public Intent {
            loanId = loanId == null ? "" : loanId;
            currencyId = currencyId == null ? "" : currencyId;
            principal = Math.max(0L, principal);
            days = Math.max(0, days);
        }
        public Intent withPaid(boolean value) {
            return new Intent(loanId, lender, borrower, currencyId, principal, ratePerYear, days, value);
        }
    }

    private PeerLoanOriginationIntentSavedData() {}
    public static PeerLoanOriginationIntentSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(PeerLoanOriginationIntentSavedData::new,
                        PeerLoanOriginationIntentSavedData::load), ID);
    }
    public List<Intent> intents() { return List.copyOf(intents); }
    public Intent find(String loanId) {
        if (loanId == null || loanId.isBlank()) return null;
        return intents.stream().filter(intent -> loanId.equals(intent.loanId())).findFirst().orElse(null);
    }
    public void add(Intent intent) {
        if (intent == null || intent.loanId().isBlank() || intent.lender() == null || intent.borrower() == null
                || intent.currencyId().isBlank() || intent.principal() <= 0L || intent.days() <= 0
                || !Double.isFinite(intent.ratePerYear()) || intent.ratePerYear() < 0.0 || find(intent.loanId()) != null) return;
        intents.add(intent);
        while (intents.size() > MAX_INTENTS) intents.remove(0);
        setDirty();
    }
    public void markPaid(String loanId) {
        Intent current = find(loanId);
        if (current == null || current.paid()) return;
        intents.set(intents.indexOf(current), current.withPaid(true));
        setDirty();
    }
    public void remove(String loanId) {
        if (loanId != null && intents.removeIf(intent -> loanId.equals(intent.loanId()))) setDirty();
    }
    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Intent intent : intents) {
            CompoundTag value = new CompoundTag();
            value.putString("loanId", intent.loanId()); value.putUUID("lender", intent.lender());
            value.putUUID("borrower", intent.borrower()); value.putString("currency", intent.currencyId());
            value.putLong("principal", intent.principal()); value.putDouble("rate", intent.ratePerYear());
            value.putInt("days", intent.days()); value.putBoolean("paid", intent.paid()); list.add(value);
        }
        tag.put("intents", list); return tag;
    }
    public static PeerLoanOriginationIntentSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        PeerLoanOriginationIntentSavedData data = new PeerLoanOriginationIntentSavedData();
        ListTag list = tag.getList("intents", Tag.TAG_COMPOUND);
        for (int i = Math.max(0, list.size() - MAX_INTENTS); i < list.size(); i++) {
            CompoundTag value = list.getCompound(i);
            if (!value.hasUUID("lender") || !value.hasUUID("borrower") || value.getString("loanId").isBlank()
                    || value.getString("currency").isBlank() || value.getLong("principal") <= 0L
                    || value.getInt("days") <= 0 || !Double.isFinite(value.getDouble("rate"))
                    || value.getDouble("rate") < 0.0) continue;
            data.intents.add(new Intent(value.getString("loanId"), value.getUUID("lender"), value.getUUID("borrower"),
                    value.getString("currency"), value.getLong("principal"), value.getDouble("rate"),
                    value.getInt("days"), value.getBoolean("paid")));
        }
        return data;
    }
}
