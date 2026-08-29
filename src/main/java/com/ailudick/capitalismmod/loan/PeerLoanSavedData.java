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

/**
 * World-persisted registry of peer-to-peer loans.
 */
public final class PeerLoanSavedData extends SavedData {
    private static final String ID = "capitalismmod_peer_loans";

    private final List<PeerLoan> loans = new ArrayList<>();

    private record State(List<PeerLoan> loans) {
        static final Codec<State> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                PeerLoan.CODEC.listOf().fieldOf("loans").forGetter(State::loans)
        ).apply(instance, State::new));
    }

    private PeerLoanSavedData() {
    }

    public static PeerLoanSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(PeerLoanSavedData::new, PeerLoanSavedData::load), ID);
    }

    public List<PeerLoan> loans() {
        return loans;
    }

    public void addLoan(PeerLoan loan) {
        loans.add(loan);
        setDirty();
    }

    public void removeLoan(String loanId) {
        loans.removeIf(loan -> loan.id().equals(loanId));
        setDirty();
    }

    public PeerLoan findLoan(String loanId) {
        for (PeerLoan loan : loans) {
            if (loan.id().equals(loanId)) {
                return loan;
            }
        }
        return null;
    }

    public void replaceLoan(PeerLoan loan) {
        for (int i = 0; i < loans.size(); i++) {
            if (loans.get(i).id().equals(loan.id())) {
                loans.set(i, loan);
                return;
            }
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        State state = new State(new ArrayList<>(loans));
        State.CODEC.encodeStart(NbtOps.INSTANCE, state).result()
                .ifPresent(encoded -> tag.put("data", encoded));
        return tag;
    }

    public static PeerLoanSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        PeerLoanSavedData data = new PeerLoanSavedData();
        if (tag.contains("data")) {
            State.CODEC.parse(NbtOps.INSTANCE, tag.get("data")).result()
                    .ifPresent(state -> data.loans.addAll(state.loans()));
        }
        return data;
    }
}
