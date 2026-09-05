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

/** Persistent registry of company bank loans. */
public final class CompanyLoanSavedData extends SavedData {
    private static final String ID = "capitalismmod_company_loans";
    private final List<CompanyLoan> loans = new ArrayList<>();

    private record State(List<CompanyLoan> loans) {
        static final Codec<State> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                CompanyLoan.codec().listOf().fieldOf("loans").forGetter(State::loans)
        ).apply(instance, State::new));
    }

    private CompanyLoanSavedData() {}

    public static CompanyLoanSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(CompanyLoanSavedData::new, CompanyLoanSavedData::load), ID);
    }

    public List<CompanyLoan> loans() { return List.copyOf(loans); }

    public void add(CompanyLoan loan) {
        if (loan == null || loan.id().isBlank() || loan.companyId().isBlank() || loan.principal() <= 0L) return;
        loans.add(loan);
        setDirty();
    }

    public CompanyLoan find(String id) {
        for (CompanyLoan loan : loans) if (loan.id().equals(id)) return loan;
        return null;
    }

    public List<CompanyLoan> forCompany(String companyId) {
        return loans.stream().filter(loan -> loan.companyId().equals(companyId)).toList();
    }

    public void replace(CompanyLoan updated) {
        for (int i = 0; i < loans.size(); i++) {
            if (loans.get(i).id().equals(updated.id())) {
                loans.set(i, updated);
                setDirty();
                return;
            }
        }
    }

    public void remove(String id) {
        if (loans.removeIf(loan -> loan.id().equals(id))) setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        State.CODEC.encodeStart(NbtOps.INSTANCE, new State(new ArrayList<>(loans))).result()
                .ifPresent(encoded -> tag.put("data", encoded));
        return tag;
    }

    public static CompanyLoanSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        CompanyLoanSavedData data = new CompanyLoanSavedData();
        if (tag.contains("data")) {
            State.CODEC.parse(NbtOps.INSTANCE, tag.get("data")).result()
                    .ifPresent(state -> data.loans.addAll(state.loans()));
        }
        return data;
    }
}
