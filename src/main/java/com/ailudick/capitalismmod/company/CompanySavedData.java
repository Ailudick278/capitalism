package com.ailudick.capitalismmod.company;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

/** Server-level registry of independent company entities. */
public final class CompanySavedData extends SavedData {
    private static final String ID = "capitalismmod_companies";
    private final Map<String, Company> companies = new HashMap<>();

    private record State(Map<String, Company> companies) {
        private static final Codec<State> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.unboundedMap(Codec.STRING, Company.CODEC).fieldOf("companies").forGetter(State::companies)
        ).apply(instance, State::new));
    }

    private CompanySavedData() {
    }

    public static CompanySavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(CompanySavedData::new, CompanySavedData::load), ID);
    }

    public Map<String, Company> companies() {
        return Map.copyOf(companies);
    }

    public Company get(String companyId) {
        return companies.get(companyId);
    }

    public void put(Company company) {
        if (company != null && company.companyId() != null) {
            companies.put(company.companyId(), company);
            setDirty();
        }
    }

    public void remove(String companyId) {
        if (companyId != null && companies.remove(companyId) != null) {
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        State.CODEC.encodeStart(NbtOps.INSTANCE, new State(companies)).result()
                .ifPresent(encoded -> tag.put("data", encoded));
        return tag;
    }

    public static CompanySavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        CompanySavedData data = new CompanySavedData();
        if (tag.contains("data")) {
            State.CODEC.parse(NbtOps.INSTANCE, tag.get("data")).result()
                    .ifPresent(state -> data.companies.putAll(state.companies()));
        }
        return data;
    }
}
