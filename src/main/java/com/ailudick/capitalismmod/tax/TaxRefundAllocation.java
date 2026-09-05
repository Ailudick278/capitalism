package com.ailudick.capitalismmod.tax;

/** Structured portion of a refund backed by one tax-credit source. */
public record TaxRefundAllocation(String sourceId, String subjectType, String subjectId,
                                  long periodStart, long periodEnd, long originalCredit, long refundAmount) {}
