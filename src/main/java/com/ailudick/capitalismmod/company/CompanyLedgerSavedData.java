package com.ailudick.capitalismmod.company;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Server-level append-only accounting ledger for companies. */
public final class CompanyLedgerSavedData extends SavedData {
    private static final String ID = "capitalismmod_company_ledger";
    private final Map<String, List<CompanyLedgerEntry>> entries = new HashMap<>();

    private record State(Map<String, List<CompanyLedgerEntry>> entries) {
        private static final Codec<State> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.unboundedMap(Codec.STRING, CompanyLedgerEntry.codec().listOf()).fieldOf("entries")
                        .forGetter(State::entries)
        ).apply(instance, State::new));
    }

    private CompanyLedgerSavedData() {}

    public static CompanyLedgerSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(CompanyLedgerSavedData::new, CompanyLedgerSavedData::load), ID);
    }

    public List<CompanyLedgerEntry> entries(String companyId) {
        return List.copyOf(entries.getOrDefault(companyId, List.of()));
    }

    public void append(CompanyLedgerEntry entry) {
        if (entry == null || entry.companyId() == null || entry.companyId().isBlank()) return;
        entries.computeIfAbsent(entry.companyId(), ignored -> new ArrayList<>()).add(entry);
        setDirty();
    }

    /** Keeps historical accounting entries reachable after a company merger. */
    public void transferCompany(String sourceId, String targetId) {
        if (sourceId == null || targetId == null || sourceId.isBlank()
                || targetId.isBlank() || sourceId.equals(targetId)) return;
        List<CompanyLedgerEntry> source = entries.remove(sourceId);
        if (source == null || source.isEmpty()) return;
        List<CompanyLedgerEntry> target = entries.computeIfAbsent(targetId, ignored -> new ArrayList<>());
        for (CompanyLedgerEntry entry : source) {
            target.add(new CompanyLedgerEntry(targetId, entry.timestamp(), entry.type(), entry.currencyId(),
                    entry.amount(), entry.balanceAfter(), entry.description()));
        }
        target.sort(java.util.Comparator.comparingLong(CompanyLedgerEntry::timestamp));
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        State.CODEC.encodeStart(NbtOps.INSTANCE, new State(entries)).result()
                .ifPresent(encoded -> tag.put("data", encoded));
        return tag;
    }

    public static CompanyLedgerSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        CompanyLedgerSavedData data = new CompanyLedgerSavedData();
        if (tag.contains("data")) {
            State.CODEC.parse(NbtOps.INSTANCE, tag.get("data")).result()
                    .ifPresent(state -> state.entries().forEach((id, list) ->
                            data.entries.put(id, new ArrayList<>(list))));
        }
        return data;
    }
}
