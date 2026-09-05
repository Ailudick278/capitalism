package com.ailudick.capitalismmod.business;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Immutable accounting entry for a sole proprietor. Amounts are major currency units. */
public record BusinessLedgerEntry(String businessId, long timestamp, String type, String currencyId, long amount,
                                  long balanceAfter, String description) {
    public static final Codec<BusinessLedgerEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("businessId").forGetter(BusinessLedgerEntry::businessId),
            Codec.LONG.fieldOf("timestamp").forGetter(BusinessLedgerEntry::timestamp),
            Codec.STRING.fieldOf("type").forGetter(BusinessLedgerEntry::type),
            Codec.STRING.fieldOf("currencyId").forGetter(BusinessLedgerEntry::currencyId),
            Codec.LONG.fieldOf("amount").forGetter(BusinessLedgerEntry::amount),
            Codec.LONG.fieldOf("balanceAfter").forGetter(BusinessLedgerEntry::balanceAfter),
            Codec.STRING.fieldOf("description").forGetter(BusinessLedgerEntry::description)
    ).apply(instance, BusinessLedgerEntry::new));
}
