package com.ailudick.capitalismmod.company;

import com.ailudick.capitalismmod.currency.Currencies;
import com.ailudick.capitalismmod.currency.Money;
import com.ailudick.capitalismmod.tax.TaxTransactionService;
import com.ailudick.capitalismmod.tax.TaxType;
import net.minecraft.server.MinecraftServer;

/**
 * Shared, retry-safe settlement for a delivered company freight shipment.
 * The phase record is deliberately kept separate from the payable so a server
 * restart cannot turn a half-completed transfer into a second payment.
 */
public final class CompanyFreightSettlementService {
    private CompanyFreightSettlementService() {}

    public static boolean settleIfFunded(MinecraftServer server, String shipmentId,
                                         String buyerCompanyId, String carrierCompanyId, long now) {
        if (server == null || shipmentId == null || shipmentId.isBlank()
                || buyerCompanyId == null || buyerCompanyId.isBlank()
                || carrierCompanyId == null || carrierCompanyId.isBlank()) return false;
        Company buyer = CompanySavedData.get(server).get(buyerCompanyId);
        Company carrier = CompanySavedData.get(server).get(carrierCompanyId);
        if (buyer == null || carrier == null || buyerCompanyId.equals(carrierCompanyId)) return false;
        CompanyLogisticsCostSavedData costs = CompanyLogisticsCostSavedData.get(server);
        CompanyLogisticsCostSavedData.CapitalizedCost payable = costs.forCompany(buyerCompanyId).stream()
                .filter(cost -> shipmentId.equals(cost.shipmentId())).findFirst().orElse(null);
        if (payable == null || payable.estimatedCost() <= 0L) return false;
        long amount = payable.estimatedCost();
        CompanyFreightSettlementSavedData phases = CompanyFreightSettlementSavedData.get(server);
        CompanyFreightSettlementSavedData.Settlement settlement =
                phases.begin(shipmentId, buyerCompanyId, carrierCompanyId, amount);
        if (settlement == null || settlement.amount() != amount
                || !buyerCompanyId.equals(settlement.buyerCompanyId())
                || !carrierCompanyId.equals(settlement.carrierCompanyId())) return false;

        String debitSource = "freight-settlement-debit:" + shipmentId;
        if (!settlement.buyerDebited()) {
            if (buyer.treasuryOf(Currencies.USD.id()) < amount
                    || !CompanyHelper.debitTreasuryNonOperatingOnce(server, buyerCompanyId,
                    Currencies.USD.id(), amount, "freight_payable_settlement",
                    "Settlement for shipment " + shipmentId, debitSource)) return false;
            settlement = phases.update(settlement.withBuyerDebited(true));
        }
        String creditSource = "freight-settlement-credit:" + shipmentId;
        if (!settlement.carrierCredited()) {
            if (!CompanyHelper.creditTreasuryNonOperatingOnce(server, carrierCompanyId,
                    Currencies.USD.id(), amount, "freight_revenue",
                    "Freight revenue for shipment " + shipmentId, creditSource)) return false;
            settlement = phases.update(settlement.withCarrierCredited(true));
        }
        if (!settlement.payableClosed()) {
            if (!costs.settle(shipmentId, carrierCompanyId, now)) {
                CompanyLogisticsCostSavedData.CapitalizedCost current = costs.forCompany(buyerCompanyId).stream()
                        .filter(cost -> shipmentId.equals(cost.shipmentId())).findFirst().orElse(null);
                if (current == null || !current.settled()) return false;
            }
            phases.update(settlement.withPayableClosed(true));
        }
        CompanyHelper.recordTaxableIncome(server, carrier, "freight:" + shipmentId,
                amount, Currencies.USD.id(), now);
        long amountMinor = Money.toMinorSaturated(amount);
        TaxTransactionService.assess(server, TaxType.VAT, carrier.ownerUuid(), Currencies.USD.id(),
                amountMinor, "freight-vat:" + shipmentId, now);
        TaxTransactionService.recordInputCredit(server, buyer.ownerUuid(), Currencies.USD.id(),
                amountMinor, "freight-vat:" + shipmentId, now);
        return true;
    }
}
