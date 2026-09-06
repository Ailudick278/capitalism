package com.ailudick.capitalismmod.land;

import java.util.Set;

/** Pure business rules for the states of a land-rent bill. */
public final class LandRentBillStatus {
    private static final Set<String> VALID = Set.of("PENDING", "PAID", "DEFAULTED");

    private LandRentBillStatus() {}

    public static boolean isValid(String status) {
        return status != null && VALID.contains(status);
    }
}
