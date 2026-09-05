package com.ailudick.capitalismmod.company;

import com.ailudick.capitalismmod.util.EconomyMath;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A company owned by a player. Produces income over time based on its type and level.
 *
 * @param name     company name (unique per owner)
 * @param type     company type id (see {@link CompanyTypes})
 * @param level    company level, drives income (starts at 1)
 * @param treasury currency id -> amount of undistributed profit (major units)
 * @param taxOwed  legacy compatibility mirror of unpaid corporate income tax (USD, major units)
 */
public record Company(String companyId, UUID ownerUuid, String name, String type, int level,
                      Map<String, Long> treasury, long taxOwed) {

    public static final UUID UNASSIGNED_OWNER = new UUID(0L, 0L);
    private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);

    public static final Codec<Company> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("companyId").forGetter(Company::companyId),
            UUID_CODEC.optionalFieldOf("ownerUuid", UNASSIGNED_OWNER).forGetter(Company::ownerUuid),
            Codec.STRING.fieldOf("name").forGetter(Company::name),
            Codec.STRING.fieldOf("type").forGetter(Company::type),
            Codec.INT.fieldOf("level").forGetter(Company::level),
            Codec.unboundedMap(Codec.STRING, Codec.LONG).fieldOf("treasury").forGetter(Company::treasury),
            Codec.LONG.fieldOf("taxOwed").forGetter(Company::taxOwed)
    ).apply(instance, Company::new));

    public static Company create(String name, String type, UUID ownerUuid) {
        return new Company(CompanyId.generate(), ownerUuid, name, type, 1, new HashMap<>(), 0L);
    }

    public Company withIdentity(String newCompanyId, UUID newOwnerUuid) {
        return new Company(newCompanyId, newOwnerUuid, name, type, level, treasury, taxOwed);
    }

    public long treasuryOf(String currencyId) {
        return treasury.getOrDefault(currencyId, 0L);
    }

    public Company withLevel(int newLevel) {
        return new Company(companyId, ownerUuid, name, type, newLevel, treasury, taxOwed);
    }

    public Company withTreasury(Map<String, Long> newTreasury) {
        return new Company(companyId, ownerUuid, name, type, level, newTreasury, taxOwed);
    }

    public Company withTaxOwed(long newTaxOwed) {
        return new Company(companyId, ownerUuid, name, type, level, treasury, newTaxOwed);
    }

    public Company addTaxOwed(long amount) {
        long updated;
        try {
            updated = Math.addExact(taxOwed, amount);
        } catch (ArithmeticException e) {
            updated = Long.MAX_VALUE;
        }
        return new Company(companyId, ownerUuid, name, type, level, treasury, updated);
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
