package com.ailudick.capitalismmod.government;

/** Stable fiscal categories derived from government transaction source IDs. */
public enum GovernmentBudgetCategory {
    SOCIAL_SUPPORT,
    PUBLIC_SERVICES,
    PUBLIC_CONSTRUCTION,
    MONETARY_POLICY,
    FINANCIAL_STABILITY,
    OTHER;

    public static GovernmentBudgetCategory fromTransactionId(String transactionId) {
        if (transactionId == null) return OTHER;
        if (transactionId.startsWith("government-benefit:")
                || transactionId.startsWith("government-regional-support:")) return SOCIAL_SUPPORT;
        if (transactionId.startsWith("public-maintenance:")) return PUBLIC_SERVICES;
        if (transactionId.startsWith("public-construction:")) return PUBLIC_CONSTRUCTION;
        if (transactionId.startsWith("open-market:")) return MONETARY_POLICY;
        if (transactionId.startsWith("bank-capital-injection:")) return FINANCIAL_STABILITY;
        return OTHER;
    }
}
