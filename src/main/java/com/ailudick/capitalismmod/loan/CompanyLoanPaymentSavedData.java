package com.ailudick.capitalismmod.loan;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

/** Persistent principal/interest allocation journal for company-loan payments. */
public final class CompanyLoanPaymentSavedData extends SavedData {
    private static final String ID = "capitalismmod_company_loan_payments";
    private static final int MAX_RECORDS = 2048;
    private final List<Payment> payments = new ArrayList<>();

    public record Payment(String loanId, String companyId, long timestamp, long total,
                          long interest, long principal, long remainingPrincipal,
                          int daysRemaining, boolean overdue) {
        public Payment(String loanId, String companyId, long timestamp, long total,
                       long interest, long principal, long remainingPrincipal) {
            this(loanId, companyId, timestamp, total, interest, principal, remainingPrincipal, 0, false);
        }

        private static final Codec<Payment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("loanId").forGetter(Payment::loanId),
                Codec.STRING.fieldOf("companyId").forGetter(Payment::companyId),
                Codec.LONG.fieldOf("timestamp").forGetter(Payment::timestamp),
                Codec.LONG.fieldOf("total").forGetter(Payment::total),
                Codec.LONG.fieldOf("interest").forGetter(Payment::interest),
                Codec.LONG.fieldOf("principal").forGetter(Payment::principal),
                Codec.LONG.fieldOf("remainingPrincipal").forGetter(Payment::remainingPrincipal),
                Codec.INT.optionalFieldOf("daysRemaining", 0).forGetter(Payment::daysRemaining),
                Codec.BOOL.optionalFieldOf("overdue", false).forGetter(Payment::overdue)
        ).apply(instance, Payment::new));
    }

    private record State(List<Payment> payments) {
        private static final Codec<State> CODEC = Payment.CODEC.listOf().xmap(State::new, State::payments);
    }

    private CompanyLoanPaymentSavedData() {}

    public static CompanyLoanPaymentSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(CompanyLoanPaymentSavedData::new, CompanyLoanPaymentSavedData::load), ID);
    }

    public void append(Payment payment) {
        if (payment == null || payment.loanId() == null || payment.loanId().isBlank()
                || payment.companyId() == null || payment.companyId().isBlank() || payment.total() <= 0L) return;
        payments.add(payment);
        while (payments.size() > MAX_RECORDS) payments.remove(0);
        setDirty();
    }

    public List<Payment> forLoan(String loanId) {
        if (loanId == null || loanId.isBlank()) return List.of();
        return payments.stream().filter(payment -> loanId.equals(payment.loanId())).toList();
    }

    public List<Payment> forCompany(String companyId) {
        if (companyId == null || companyId.isBlank()) return List.of();
        return payments.stream().filter(payment -> companyId.equals(payment.companyId())).toList();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        State.CODEC.encodeStart(NbtOps.INSTANCE, new State(payments)).result()
                .ifPresent(encoded -> tag.put("data", encoded));
        return tag;
    }

    public static CompanyLoanPaymentSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        CompanyLoanPaymentSavedData data = new CompanyLoanPaymentSavedData();
        if (tag.contains("data")) {
            State.CODEC.parse(NbtOps.INSTANCE, tag.get("data")).result()
                    .ifPresent(state -> data.payments.addAll(state.payments()));
        }
        while (data.payments.size() > MAX_RECORDS) data.payments.remove(0);
        return data;
    }
}
