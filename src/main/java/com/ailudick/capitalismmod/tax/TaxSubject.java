package com.ailudick.capitalismmod.tax;

import java.util.UUID;

/** Identifies the business object that generated a tax liability. */
public record TaxSubject(TaxType type, String subjectId, UUID taxpayerUuid) {
    public TaxSubject {
        if (type == null) throw new IllegalArgumentException("Tax type is required");
        if (subjectId == null || subjectId.isBlank()) throw new IllegalArgumentException("Tax subject id is required");
        if (taxpayerUuid == null) throw new IllegalArgumentException("Taxpayer is required");
    }
}
