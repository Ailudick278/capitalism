package com.ailudick.capitalismmod.bank;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BankExposureAuditRulesTest {
    @Test
    void comparesEconomicExposureWithoutTimestamp() {
        var expected = new BankExposureSavedData.Exposure(100L, 80L, 20L, 1, 100L);
        var same = new BankExposureSavedData.Exposure(100L, 80L, 20L, 1, 200L);
        var different = new BankExposureSavedData.Exposure(100L, 81L, 20L, 1, 200L);
        assertTrue(BankExposureAuditRules.matches(expected, same));
        assertFalse(BankExposureAuditRules.matches(expected, different));
        assertFalse(BankExposureAuditRules.matches(expected, null));
    }

    @Test
    void validatesPerAccountSnapshotsAndAggregates() {
        var snapshot = new BankExposureSavedData.AccountSnapshot("a", 100L, 80L, 20L, -1, 100L);
        var exposure = new BankExposureSavedData.Exposure(100L, 80L, 20L, 1, 100L);
        assertTrue(BankExposureAuditRules.validAccountSnapshot(snapshot));
        assertTrue(BankExposureAuditRules.totalsMatch(exposure, 100L, 80L, 20L, 1));
        assertTrue(BankExposureAuditRules.timestampMatches(exposure, snapshot));
        assertTrue(BankExposureAuditRules.transactionSummaryMatches(snapshot, 0, -1L));
        assertFalse(BankExposureAuditRules.transactionSummaryMatches(snapshot, 1, -1L));
        assertFalse(BankExposureAuditRules.timestampMatches(
                new BankExposureSavedData.Exposure(100L, 80L, 20L, 1, 101L), snapshot));
        assertFalse(BankExposureAuditRules.totalsMatch(exposure, 100L, 81L, 20L, 1));
        assertFalse(BankExposureAuditRules.isStale(100L, 100L + BankExposureAuditRules.SNAPSHOT_MAX_AGE_TICKS,  BankExposureAuditRules.SNAPSHOT_MAX_AGE_TICKS));
        assertTrue(BankExposureAuditRules.isStale(100L, 101L + BankExposureAuditRules.SNAPSHOT_MAX_AGE_TICKS, BankExposureAuditRules.SNAPSHOT_MAX_AGE_TICKS));
    }
}
