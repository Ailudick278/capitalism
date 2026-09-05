package com.ailudick.capitalismmod.business;

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

/** Server-level append-only ledger for business transactions. */
public final class BusinessLedgerSavedData extends SavedData {
    private static final String ID = "capitalismmod_business_ledger";
    private final Map<String, List<BusinessLedgerEntry>> entries = new HashMap<>();

    private record State(Map<String, List<BusinessLedgerEntry>> entries) {
        private static final Codec<State> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.unboundedMap(Codec.STRING, BusinessLedgerEntry.CODEC.listOf()).fieldOf("entries")
                        .forGetter(State::entries)
        ).apply(instance, State::new));
    }

    private BusinessLedgerSavedData() {
    }

    public static BusinessLedgerSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(BusinessLedgerSavedData::new, BusinessLedgerSavedData::load), ID);
    }

    public List<BusinessLedgerEntry> entries(String businessId) {
        return List.copyOf(entries.getOrDefault(businessId, List.of()));
    }

    public void append(BusinessLedgerEntry entry) {
        entries.computeIfAbsent(entry.businessId(), ignored -> new ArrayList<>()).add(entry);
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        State.CODEC.encodeStart(NbtOps.INSTANCE, new State(entries)).result()
                .ifPresent(encoded -> tag.put("data", encoded));
        return tag;
    }

    public static BusinessLedgerSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        BusinessLedgerSavedData data = new BusinessLedgerSavedData();
        if (tag.contains("data")) {
            State.CODEC.parse(NbtOps.INSTANCE, tag.get("data")).result()
                    .ifPresent(state -> state.entries().forEach((id, list) -> data.entries.put(id, new ArrayList<>(list))));
        }
        return data;
    }
}
