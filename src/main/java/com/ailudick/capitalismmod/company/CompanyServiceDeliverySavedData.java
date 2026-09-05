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

/** Append-only audit trail for service production cycles. */
public final class CompanyServiceDeliverySavedData extends SavedData {
    private static final String ID = "capitalismmod_company_service_deliveries";
    private static final int MAX_PER_COMPANY = 512;
    private final Map<String, List<ServiceDelivery>> deliveries = new HashMap<>();

    public record ServiceDelivery(String companyId, long timestamp, String recipeId,
                                  long revenue, long inputCost, long operatingCost,
                                  long depreciation, int workers) {
        private static final Codec<ServiceDelivery> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("companyId").forGetter(ServiceDelivery::companyId),
                Codec.LONG.fieldOf("timestamp").forGetter(ServiceDelivery::timestamp),
                Codec.STRING.fieldOf("recipeId").forGetter(ServiceDelivery::recipeId),
                Codec.LONG.fieldOf("revenue").forGetter(ServiceDelivery::revenue),
                Codec.LONG.fieldOf("inputCost").forGetter(ServiceDelivery::inputCost),
                Codec.LONG.fieldOf("operatingCost").forGetter(ServiceDelivery::operatingCost),
                Codec.LONG.fieldOf("depreciation").forGetter(ServiceDelivery::depreciation),
                Codec.INT.fieldOf("workers").forGetter(ServiceDelivery::workers)
        ).apply(instance, ServiceDelivery::new));

        public long directCost() {
            return safeAdd(inputCost, operatingCost);
        }

        public long contributionBeforePayroll() {
            return revenue - directCost() - Math.max(0L, depreciation);
        }

        private static long safeAdd(long left, long right) {
            if (left < 0L || right < 0L || left > Long.MAX_VALUE - right) return Long.MAX_VALUE;
            return left + right;
        }
    }

    private record State(Map<String, List<ServiceDelivery>> deliveries) {
        private static final Codec<State> CODEC = Codec.unboundedMap(Codec.STRING,
                ServiceDelivery.CODEC.listOf()).xmap(State::new, State::deliveries);
    }

    private CompanyServiceDeliverySavedData() {
    }

    public static CompanyServiceDeliverySavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(CompanyServiceDeliverySavedData::new, CompanyServiceDeliverySavedData::load), ID);
    }

    public List<ServiceDelivery> recent(String companyId, int limit) {
        if (companyId == null || limit <= 0) return List.of();
        List<ServiceDelivery> source = deliveries.getOrDefault(companyId, List.of());
        int start = Math.max(0, source.size() - Math.min(limit, MAX_PER_COMPANY));
        List<ServiceDelivery> result = new ArrayList<>(source.subList(start, source.size()));
        java.util.Collections.reverse(result);
        return List.copyOf(result);
    }

    public void append(ServiceDelivery delivery) {
        if (delivery == null || delivery.companyId() == null || delivery.companyId().isBlank()
                || delivery.recipeId() == null || delivery.recipeId().isBlank()) return;
        List<ServiceDelivery> history = deliveries.computeIfAbsent(delivery.companyId(), ignored -> new ArrayList<>());
        history.add(delivery);
        while (history.size() > MAX_PER_COMPANY) history.remove(0);
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        State.CODEC.encodeStart(NbtOps.INSTANCE, new State(deliveries)).result()
                .ifPresent(encoded -> tag.put("data", encoded));
        return tag;
    }

    public static CompanyServiceDeliverySavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        CompanyServiceDeliverySavedData data = new CompanyServiceDeliverySavedData();
        if (tag.contains("data")) {
            State.CODEC.parse(NbtOps.INSTANCE, tag.get("data")).result()
                    .ifPresent(state -> state.deliveries().forEach((companyId, list) ->
                            data.deliveries.put(companyId, new ArrayList<>(list))));
        }
        return data;
    }
}
