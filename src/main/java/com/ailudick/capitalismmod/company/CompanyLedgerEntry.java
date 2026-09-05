package com.ailudick.capitalismmod.company;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Immutable accounting entry for a company. Amounts are major currency units. */
public record CompanyLedgerEntry(String companyId, long timestamp, String type, String currencyId,
                                 long amount, long balanceAfter, String description) {
    public static final Codec<CompanyLedgerEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("companyId").forGetter(CompanyLedgerEntry::companyId),
            Codec.LONG.fieldOf("timestamp").forGetter(CompanyLedgerEntry::timestamp),
            Codec.STRING.fieldOf("type").forGetter(CompanyLedgerEntry::type),
            Codec.STRING.fieldOf("currencyId").forGetter(CompanyLedgerEntry::currencyId),
            Codec.LONG.fieldOf("amount").forGetter(CompanyLedgerEntry::amount),
            Codec.LONG.fieldOf("balanceAfter").forGetter(CompanyLedgerEntry::balanceAfter),
            Codec.STRING.fieldOf("description").forGetter(CompanyLedgerEntry::description)
    ).apply(instance, CompanyLedgerEntry::new));
}
