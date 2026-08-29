package com.ailudick.capitalismmod.bond;

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
 * World-persisted registry of government bond holdings.
 */
public final class BondSavedData extends SavedData {
    private static final String ID = "capitalismmod_bonds";

    private final List<BondHolding> holdings = new ArrayList<>();

    private record State(List<BondHolding> holdings) {
        static final Codec<State> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BondHolding.CODEC.listOf().fieldOf("holdings").forGetter(State::holdings)
        ).apply(instance, State::new));
    }

    private BondSavedData() {
    }

    public static BondSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(BondSavedData::new, BondSavedData::load), ID);
    }

    public List<BondHolding> holdings() {
        return holdings;
    }

    public void addHolding(BondHolding holding) {
        holdings.add(holding);
        setDirty();
    }

    public void removeHolding(String holdingId) {
        holdings.removeIf(holding -> holding.id().equals(holdingId));
        setDirty();
    }

    public BondHolding findHolding(String holdingId) {
        for (BondHolding holding : holdings) {
            if (holding.id().equals(holdingId)) {
                return holding;
            }
        }
        return null;
    }

    public void replaceHolding(BondHolding holding) {
        for (int i = 0; i < holdings.size(); i++) {
            if (holdings.get(i).id().equals(holding.id())) {
                holdings.set(i, holding);
                return;
            }
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        State state = new State(new ArrayList<>(holdings));
        State.CODEC.encodeStart(NbtOps.INSTANCE, state).result()
                .ifPresent(encoded -> tag.put("data", encoded));
        return tag;
    }

    public static BondSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        BondSavedData data = new BondSavedData();
        if (tag.contains("data")) {
            State.CODEC.parse(NbtOps.INSTANCE, tag.get("data")).result()
                    .ifPresent(state -> data.holdings.addAll(state.holdings()));
        }
        return data;
    }
}
