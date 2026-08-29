package com.ailudick.capitalismmod.company;

import com.ailudick.capitalismmod.util.EconomyMath;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * A company owned by a player. Produces income over time based on its type and level.
 *
 * @param name     company name (unique per owner)
 * @param type     company type id (see {@link CompanyTypes})
 * @param level    company level, drives income (starts at 1)
 * @param treasury currency id -> amount of undistributed profit (major units)
 * @param taxOwed  accumulated unpaid corporate income tax (USD, major units)
 */
public record Company(String name, String type, int level, Map<String, Long> treasury, long taxOwed) {

    public static final Codec<Company> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(Company::name),
            Codec.STRING.fieldOf("type").forGetter(Company::type),
            Codec.INT.fieldOf("level").forGetter(Company::level),
            Codec.unboundedMap(Codec.STRING, Codec.LONG).fieldOf("treasury").forGetter(Company::treasury),
            Codec.LONG.fieldOf("taxOwed").forGetter(Company::taxOwed)
    ).apply(instance, Company::new));

    public static Company create(String name, String type) {
        return new Company(name, type, 1, new HashMap<>(), 0L);
    }

    public long treasuryOf(String currencyId) {
        return treasury.getOrDefault(currencyId, 0L);
    }

    public Company withLevel(int newLevel) {
        return new Company(name, type, newLevel, treasury, taxOwed);
    }

    public Company withTreasury(Map<String, Long> newTreasury) {
        return new Company(name, type, level, newTreasury, taxOwed);
    }

    public Company withTaxOwed(long newTaxOwed) {
        return new Company(name, type, level, treasury, newTaxOwed);
    }

    public Company addTaxOwed(long amount) {
        return new Company(name, type, level, treasury, taxOwed + amount);
    }

    /** Adds {@code amount} to the given currency's treasury balance. Returns {@code this} unchanged on overflow. */
    public Company addTreasury(String currencyId, long amount) {
        Map<String, Long> updated = new HashMap<>(treasury);
        long sum = EconomyMath.add(treasuryOf(currencyId), amount);
        if (sum < 0) {
            return this;
        }
        updated.put(currencyId, sum);
        return withTreasury(updated);
    }
}
