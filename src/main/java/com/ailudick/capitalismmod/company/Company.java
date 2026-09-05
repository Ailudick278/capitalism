package com.ailudick.capitalismmod.company;

import com.ailudick.capitalismmod.util.EconomyMath;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A company owned by a player. Its scale is expressed through capital, labor,
 * equipment and production capacity rather than a universal level.
 *
 * @param name     company name (unique per owner)
 * @param type     company type id (see {@link CompanyTypes})
 * @param registeredCapital subscribed/registered capital in major currency units
 * @param treasury currency id -> amount of undistributed profit (major units)
 * @param taxOwed  legacy compatibility mirror of unpaid corporate income tax (USD, major units)
 */
public record Company(String companyId, UUID ownerUuid, String name, String type, long registeredCapital,
                      Map<String, Long> treasury, long taxOwed, String productionRecipe) {

    public static final UUID UNASSIGNED_OWNER = new UUID(0L, 0L);
    private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);

    public static final Codec<Company> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("companyId").forGetter(Company::companyId),
            UUID_CODEC.optionalFieldOf("ownerUuid", UNASSIGNED_OWNER).forGetter(Company::ownerUuid),
            Codec.STRING.fieldOf("name").forGetter(Company::name),
            Codec.STRING.fieldOf("type").forGetter(Company::type),
            Codec.LONG.optionalFieldOf("registeredCapital", 1000L).forGetter(Company::registeredCapital),
            Codec.unboundedMap(Codec.STRING, Codec.LONG).fieldOf("treasury").forGetter(Company::treasury),
            Codec.LONG.fieldOf("taxOwed").forGetter(Company::taxOwed),
            Codec.STRING.optionalFieldOf("productionRecipe", "default").forGetter(Company::productionRecipe)
    ).apply(instance, Company::new));

    public static Company create(String name, String type, UUID ownerUuid) {
        return new Company(CompanyId.generate(), ownerUuid, name, type, 1000L, new HashMap<>(), 0L, "default");
    }

    public Company withIdentity(String newCompanyId, UUID newOwnerUuid) {
        return new Company(newCompanyId, newOwnerUuid, name, type, registeredCapital, treasury, taxOwed, productionRecipe);
    }

    public long treasuryOf(String currencyId) {
        return treasury.getOrDefault(currencyId, 0L);
    }

    public Company withRegisteredCapital(long newCapital) {
        return new Company(companyId, ownerUuid, name, type, Math.max(0L, newCapital), treasury, taxOwed, productionRecipe);
    }

    public Company withTreasury(Map<String, Long> newTreasury) {
        return new Company(companyId, ownerUuid, name, type, registeredCapital, newTreasury, taxOwed, productionRecipe);
    }

    public Company withTaxOwed(long newTaxOwed) {
        return new Company(companyId, ownerUuid, name, type, registeredCapital, treasury, newTaxOwed, productionRecipe);
    }

    public Company addTaxOwed(long amount) {
        long updated;
        try {
            updated = Math.addExact(taxOwed, amount);
        } catch (ArithmeticException e) {
            updated = Long.MAX_VALUE;
        }
        return new Company(companyId, ownerUuid, name, type, registeredCapital, treasury, updated, productionRecipe);
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

    public Company withProductionRecipe(String recipeId) {
        return new Company(companyId, ownerUuid, name, type, registeredCapital, treasury, taxOwed,
                recipeId == null || recipeId.isBlank() ? "default" : recipeId);
    }
}
