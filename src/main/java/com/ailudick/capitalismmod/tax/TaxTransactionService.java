package com.ailudick.capitalismmod.tax;

import net.minecraft.server.MinecraftServer;

import java.util.UUID;

/** Creates an auditable transaction-tax bill after a completed sale. */
public final class TaxTransactionService {
    private TaxTransactionService() {}

    public static TaxBill assess(MinecraftServer server, UUID taxpayerUuid, String currencyId,
                                 long grossAmountMinor, String sourceEventId, long now) {
        return assess(server, TaxType.TRANSACTION, taxpayerUuid, currencyId, grossAmountMinor, sourceEventId, now);
    }

    public static TaxBill assess(MinecraftServer server, TaxType type, UUID taxpayerUuid, String currencyId,
                                 long grossAmountMinor, String sourceEventId, long now) {
        if (taxpayerUuid == null || grossAmountMinor <= 0L || sourceEventId == null || sourceEventId.isBlank()) return null;
        TaxLedgerSavedData ledger = TaxLedgerSavedData.get(server);
        if (ledger.findBySourceEvent(sourceEventId) != null) return ledger.findBySourceEvent(sourceEventId);
        TaxInvoiceSavedData invoices = TaxInvoiceSavedData.get(server);
        if (type == TaxType.VAT && invoices.findBySource(sourceEventId) != null) return null;
        TaxRule rule = TaxRuleService.current(server, type, now);
        long taxAmount = TaxRuleService.taxMinor(server, type, grossAmountMinor, now);
        if (taxAmount <= 0L) return null;
        TaxSubject subject = subjectFor(type, taxpayerUuid, sourceEventId);
        long originalTax = taxAmount;
        long creditApplied = 0L;
        if (type == TaxType.VAT) {
            TaxCreditSavedData credits = TaxCreditSavedData.get(server);
            if (credits.hasAppliedSource(sourceEventId)) return null;
            creditApplied = credits.consume(subject, currencyId, taxAmount);
            taxAmount -= creditApplied;
            credits.markAppliedSource(sourceEventId);
        }
        if (type == TaxType.VAT) {
            invoices.add(new TaxInvoiceSavedData.Invoice(UUID.randomUUID().toString(), sourceEventId,
                    taxpayerUuid, currencyId, grossAmountMinor, originalTax, creditApplied, now, "output"));
        }
        if (taxAmount <= 0L) return null;
        return TaxService.createBill(server, subject, currencyId, taxAmount, now, now, now,
                sourceEventId, now, now, grossAmountMinor, rule.rateBasisPoints());
    }

    /** Records VAT paid on a qualifying business input for later output-tax deduction. */
    public static long recordInputCredit(MinecraftServer server, UUID taxpayerUuid, String currencyId,
                                         long grossAmountMinor, String sourceEventId, long now) {
        if (taxpayerUuid == null || grossAmountMinor <= 0L || sourceEventId == null || sourceEventId.isBlank()) {
            return 0L;
        }
        TaxCreditSavedData credits = TaxCreditSavedData.get(server);
        String creditSource = "vat-input:" + sourceEventId;
        if (credits.hasAppliedSource(creditSource)) return 0L;
        long credit = TaxRuleService.taxMinor(server, TaxType.VAT, grossAmountMinor, now);
        if (credit <= 0L) return 0L;
        TaxSubject subject = subjectFor(TaxType.VAT, taxpayerUuid, sourceEventId);
        credits.add(subject, currencyId, credit, creditSource, now, now, now);
        credits.markAppliedSource(creditSource);
        TaxInvoiceSavedData.get(server).add(new TaxInvoiceSavedData.Invoice(UUID.randomUUID().toString(),
                creditSource, taxpayerUuid, currencyId, grossAmountMinor, credit, credit, now, "input"));
        return credit;
    }

    private static TaxSubject subjectFor(TaxType type, UUID taxpayerUuid, String sourceEventId) {
        String subjectId = type == TaxType.VAT ? "vat:" + taxpayerUuid : sourceEventId;
        return new TaxSubject(type, subjectId, taxpayerUuid);
    }
}
