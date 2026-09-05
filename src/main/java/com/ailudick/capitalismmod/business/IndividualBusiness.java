package com.ailudick.capitalismmod.business;

import com.ailudick.capitalismmod.company.CompanyId;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** A sole proprietor registration owned and operated by one player. */
public record IndividualBusiness(String businessId, UUID ownerUuid, String name, String scope,
                                 String status, Map<String, Long> account, long taxOwed) {
    private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);

    public static final Codec<IndividualBusiness> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("businessId").forGetter(IndividualBusiness::businessId),
            UUID_CODEC.fieldOf("ownerUuid").forGetter(IndividualBusiness::ownerUuid),
            Codec.STRING.fieldOf("name").forGetter(IndividualBusiness::name),
            Codec.STRING.fieldOf("scope").forGetter(IndividualBusiness::scope),
            Codec.STRING.fieldOf("status").forGetter(IndividualBusiness::status),
            Codec.unboundedMap(Codec.STRING, Codec.LONG).fieldOf("account").forGetter(IndividualBusiness::account),
            Codec.LONG.fieldOf("taxOwed").forGetter(IndividualBusiness::taxOwed)
    ).apply(instance, IndividualBusiness::new));

    public static IndividualBusiness create(UUID ownerUuid, String name, String scope) {
        return new IndividualBusiness(CompanyId.generate(), ownerUuid, name, scope, "active", new HashMap<>(), 0L);
    }

    public long balance(String currencyId) {
        return account.getOrDefault(currencyId, 0L);
    }

    public IndividualBusiness withAccount(Map<String, Long> newAccount) {
        return new IndividualBusiness(businessId, ownerUuid, name, scope, status, newAccount, taxOwed);
    }

    public IndividualBusiness withStatus(String newStatus) {
        return new IndividualBusiness(businessId, ownerUuid, name, scope, newStatus, account, taxOwed);
    }

    public IndividualBusiness afterSettlement(Map<String, Long> newAccount, long newTaxOwed) {
        return new IndividualBusiness(businessId, ownerUuid, name, scope, status, newAccount, newTaxOwed);
    }
}
