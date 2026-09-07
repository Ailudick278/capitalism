package com.ailudick.capitalismmod.tax;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CorporateTaxEventRulesTest {
    @Test
    void createsStableDistinctIncomeAndExpenseKeys() {
        assertEquals("income:company-1:freight:s-1", CorporateTaxEventRules.income("company-1", "freight:s-1"));
        assertEquals("expense:company-1:freight:s-1", CorporateTaxEventRules.expense("company-1", "freight:s-1"));
        assertTrue(CorporateTaxEventRules.income("", "source").isBlank());
    }
}
