package com.ailudick.capitalismmod.bank;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BankAccountTest {
    @Test
    void preservesInterestRemainders() {
        BankAccount account = new BankAccount("1", true, Map.of("usd", 100L), Map.of("eur", 200L),
                List.of(), List.of(), 5, Map.of("usd", 123L), Map.of("eur", 456L));

        assertEquals(123L, account.depositInterestRemainders().get("usd"));
        assertEquals(456L, account.loanInterestRemainders().get("eur"));
    }

    @Test
    void compatibilityConstructorDefaultsToEmptyRemainders() {
        BankAccount legacy = new BankAccount("1", false, Map.of(), Map.of(), List.of(), List.of(), 0);

        assertTrue(legacy.depositInterestRemainders().isEmpty());
        assertTrue(legacy.loanInterestRemainders().isEmpty());
    }

    @Test
    void clearingFundsClearsTheirInterestRemainder() {
        BankAccount account = new BankAccount("1", true, Map.of("usd", 100L), Map.of("usd", 200L),
                List.of(), List.of(), 5, Map.of("usd", 123L), Map.of("usd", 456L));

        BankAccount cleared = account.withBalance("usd", 0L).withDebt("usd", 0L);

        assertTrue(cleared.depositInterestRemainders().isEmpty());
        assertTrue(cleared.loanInterestRemainders().isEmpty());
    }
}
