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
import java.util.UUID;

/** Persistent repayment journal for player-to-player loans. */
public final class PeerLoanPaymentSavedData extends SavedData {
    private static final String ID = "capitalismmod_peer_loan_payments";
    private static final int MAX_RECORDS = 2048;
    private final List<Payment> payments = new ArrayList<>();

    public record Payment(String loanId, UUID lender, UUID borrower, String currencyId, long timestamp, long total,
                          long interest, long principal, long remainingPrincipal,
                          int daysRemaining, boolean overdue) {
        public Payment(String loanId, UUID lender, UUID borrower, long timestamp, long total,
                       long interest, long principal, long remainingPrincipal,
                       int daysRemaining, boolean overdue) {
            this(loanId, lender, borrower, "", timestamp, total, interest, principal,
                    remainingPrincipal, daysRemaining, overdue);
        }

        public Payment {
            currencyId = currencyId == null ? "" : currencyId;
        }

        private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);
        private static final Codec<Payment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("loanId").forGetter(Payment::loanId),
                UUID_CODEC.fieldOf("lender").forGetter(Payment::lender),
                UUID_CODEC.fieldOf("borrower").forGetter(Payment::borrower),
                Codec.STRING.optionalFieldOf("currencyId", "").forGetter(Payment::currencyId),
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

    private PeerLoanPaymentSavedData() {}

    public static PeerLoanPaymentSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(PeerLoanPaymentSavedData::new, PeerLoanPaymentSavedData::load), ID);
    }

    public void append(Payment payment) {
        if (payment == null || payment.loanId() == null || payment.loanId().isBlank()
                || payment.lender() == null || payment.borrower() == null || payment.total() <= 0L) return;
        payments.add(payment);
        while (payments.size() > MAX_RECORDS) payments.remove(0);
        setDirty();
    }

    public static String payoutSource(Payment payment) {
        if (payment == null || payment.currencyId().isBlank()) return "";
        return "peer-loan-payout:" + payment.loanId() + ":" + payment.timestamp() + ":"
                + payment.total() + ":" + payment.remainingPrincipal();
    }

    public List<Payment> forLoan(String loanId) {
        if (loanId == null || loanId.isBlank()) return List.of();
        return payments.stream().filter(payment -> loanId.equals(payment.loanId())).toList();
    }

    public List<Payment> forPlayer(UUID playerId) {
        if (playerId == null) return List.of();
        return payments.stream().filter(payment -> playerId.equals(payment.lender())
                || playerId.equals(payment.borrower())).toList();
    }

    public List<Payment> forAll() { return List.copyOf(payments); }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        State.CODEC.encodeStart(NbtOps.INSTANCE, new State(payments)).result()
                .ifPresent(encoded -> tag.put("data", encoded));
        return tag;
    }

    public static PeerLoanPaymentSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        PeerLoanPaymentSavedData data = new PeerLoanPaymentSavedData();
        if (tag.contains("data")) {
            State.CODEC.parse(NbtOps.INSTANCE, tag.get("data")).result()
                    .ifPresent(state -> data.payments.addAll(state.payments()));
        }
        while (data.payments.size() > MAX_RECORDS) data.payments.remove(0);
        return data;
    }
}
