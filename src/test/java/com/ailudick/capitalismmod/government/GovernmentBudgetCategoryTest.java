package com.ailudick.capitalismmod.government;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GovernmentBudgetCategoryTest {
    @Test
    void classifiesStableSpendingSources() {
        assertEquals(GovernmentBudgetCategory.SOCIAL_SUPPORT,
                GovernmentBudgetCategory.fromTransactionId("government-benefit:1:h"));
        assertEquals(GovernmentBudgetCategory.PUBLIC_SERVICES,
                GovernmentBudgetCategory.fromTransactionId("public-maintenance:1:r:housing"));
        assertEquals(GovernmentBudgetCategory.MONETARY_POLICY,
                GovernmentBudgetCategory.fromTransactionId("open-market:bond-1"));
        assertEquals(GovernmentBudgetCategory.OTHER,
                GovernmentBudgetCategory.fromTransactionId("manual:1"));
    }
}
