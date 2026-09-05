package com.ailudick.capitalismmod.tax;

import com.ailudick.capitalismmod.calendar.PerpetualCalendar;
import com.ailudick.capitalismmod.currency.Currencies;
import com.ailudick.capitalismmod.currency.Money;
import com.ailudick.capitalismmod.wallet.EconomyHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;

import java.util.List;
import java.util.UUID;
import com.ailudick.capitalismmod.company.Company;
import com.ailudick.capitalismmod.company.CompanyHelper;
import com.ailudick.capitalismmod.company.CompanyLifecycleService;
import com.ailudick.capitalismmod.company.CompanySavedData;

/** Entry point for creating, querying and settling tax liabilities. */
public final class TaxService {
    private TaxService() {}

    public static TaxBill createBill(MinecraftServer server, TaxSubject subject, String currencyId,
                                     long amount, long createdAt, long dueAt, long graceUntil) {
        return createBill(server, subject, currencyId, amount, createdAt, dueAt, graceUntil,
                "", 0L, 0L, 0L, 0);
    }

    public static TaxBill createBill(MinecraftServer server, TaxSubject subject, String currencyId,
                                     long amount, long createdAt, long dueAt, long graceUntil,
                                     String sourceEventId, long periodStart, long periodEnd,
                                     long taxableBase, int rateBasisPoints) {
        if (amount <= 0L) return null;
        TaxBill bill = new TaxBill(UUID.randomUUID().toString(), subject, currencyId, amount, 0L,
                createdAt, dueAt, graceUntil, sourceEventId, periodStart, periodEnd,
                taxableBase, rateBasisPoints);
        TaxLedgerSavedData.get(server).add(bill);
        return bill;
    }

    /** Creates one bill for a tax period and prevents duplicate settlement of that period. */
    public static TaxBill createPeriodicBill(MinecraftServer server, TaxSubject subject, String currencyId,
                                             long amount, TaxPeriod period, long taxableBase,
                                             int rateBasisPoints, String sourceEventId) {
        TaxLedgerSavedData ledger = TaxLedgerSavedData.get(server);
        TaxBill existing = ledger.findBySourceEvent(sourceEventId);
        if (existing != null) return existing;
        if (amount <= 0L) return null;
        TaxBill bill = new TaxBill(UUID.randomUUID().toString(), subject, currencyId, amount, 0L,
                period.endAt(), period.paymentDueAt(), 0L, sourceEventId, period.startAt(), period.endAt(),
                taxableBase, rateBasisPoints, period.declarationDueAt(), 0L, "", 0L, 0L);
        ledger.add(bill);
        return bill;
    }

    public static List<TaxBill> outstanding(MinecraftServer server, UUID taxpayerUuid) {
        return TaxLedgerSavedData.get(server).outstandingFor(taxpayerUuid);
    }

    public static long outstanding(MinecraftServer server, TaxSubject subject) {
        return TaxLedgerSavedData.get(server).bills().stream()
                .filter(bill -> bill.subject().equals(subject) && !bill.paid())
                .mapToLong(TaxBill::outstanding)
                .reduce(0L, TaxService::addSaturated);
    }

    /** Ensures the subject has at least the requested outstanding liability. */
    public static TaxBill ensureOutstanding(MinecraftServer server, TaxSubject subject, String currencyId,
                                            long targetAmount, long createdAt, long dueAt, long graceUntil) {
        return ensureOutstanding(server, subject, currencyId, targetAmount, createdAt, dueAt, graceUntil,
                "", 0L, 0L, 0L, 0);
    }

    public static TaxBill ensureOutstanding(MinecraftServer server, TaxSubject subject, String currencyId,
                                            long targetAmount, long createdAt, long dueAt, long graceUntil,
                                            String sourceEventId, long periodStart, long periodEnd,
                                            long taxableBase, int rateBasisPoints) {
        if (targetAmount <= 0L) return null;
        TaxLedgerSavedData ledger = TaxLedgerSavedData.get(server);
        TaxBill existing = ledger.bills().stream()
                .filter(bill -> bill.subject().equals(subject) && !bill.paid())
                .findFirst().orElse(null);
        long current = existing == null ? 0L : existing.outstanding();
        if (existing == null) return createBill(server, subject, currencyId, targetAmount, createdAt, dueAt, graceUntil,
                sourceEventId, periodStart, periodEnd, taxableBase, rateBasisPoints);
        if (current < targetAmount) {
            existing = existing.withAccrual(targetAmount - current, dueAt, graceUntil);
            ledger.replace(existing);
        }
        return existing;
    }

    public static boolean pay(ServerPlayer player, TaxSubject subject, long amount) {
        if (player == null || subject == null || amount <= 0L) return false;
        TaxLedgerSavedData ledger = TaxLedgerSavedData.get(player.getServer());
        TaxBill first = ledger.bills().stream()
                .filter(entry -> entry.subject().equals(subject) && !entry.paid())
                .findFirst().orElse(null);
        if (first == null || !first.declared() || !Currencies.exists(first.currencyId())) return false;

        // A subject-level payment is denominated in the first outstanding
        // bill's currency. This prevents an ambiguous cross-currency payment
        // from silently consuming a different currency's liability.
        String currencyId = first.currencyId();
        List<TaxBill> payable = new java.util.ArrayList<>();
        long remaining = amount;
        for (TaxBill candidate : ledger.bills()) {
            if (remaining <= 0L) break;
            if (!candidate.subject().equals(subject) || candidate.paid()
                    || !candidate.declared() || !currencyId.equals(candidate.currencyId())) continue;
            TaxBill updated = updateLateFee(player.getServer(), candidate,
                    player.getServer().overworld().getGameTime());
            long payment = Math.min(remaining, updated.outstanding());
            if (payment <= 0L) continue;
            payable.add(updated);
            remaining -= payment;
        }
        if (payable.isEmpty()) return false;
        long requested = amount - remaining;
        if (EconomyHelper.getBalance(player, Currencies.byId(currencyId)) < requested) return false;

        long left = requested;
        for (TaxBill bill : payable) {
            long payment = Math.min(left, bill.outstanding());
            if (payment <= 0L || !EconomyHelper.tryPay(player, Currencies.byId(currencyId), payment)) {
                return false;
            }
            TaxBill paidBill = bill.withPayment(payment);
            ledger.replace(paidBill);
            ledger.addPayment(new TaxPayment(UUID.randomUUID().toString(), bill.id(), player.getUUID(),
                    bill.currencyId(), payment, player.getServer().overworld().getGameTime()));
            if (paidBill.paid()) NeoForge.EVENT_BUS.post(new TaxSettledEvent(player.getServer(), paidBill));
            left -= payment;
        }
        return left == 0L;
    }

    public static boolean pay(ServerPlayer player, String billId, long amount) {
        if (amount <= 0L) return false;
        TaxLedgerSavedData ledger = TaxLedgerSavedData.get(player.getServer());
        TaxBill bill = ledger.get(billId);
        if (bill == null || bill.paid() || !bill.declared() || !bill.subject().taxpayerUuid().equals(player.getUUID())) return false;
        bill = updateLateFee(player.getServer(), bill, player.getServer().overworld().getGameTime());
        long payment = Math.min(amount, bill.outstanding());
        if (!Currencies.exists(bill.currencyId()) || !EconomyHelper.tryPay(player, Currencies.byId(bill.currencyId()), payment)) {
            return false;
        }
        TaxBill paidBill = bill.withPayment(payment);
        ledger.replace(paidBill);
        ledger.addPayment(new TaxPayment(UUID.randomUUID().toString(), bill.id(), player.getUUID(),
                bill.currencyId(), payment, player.getServer().overworld().getGameTime()));
        if (paidBill.paid()) NeoForge.EVENT_BUS.post(new TaxSettledEvent(player.getServer(), paidBill));
        return true;
    }

    /** Pays a corporate tax bill from the company's treasury, not its owner's wallet. */
    public static boolean payFromCompany(MinecraftServer server, Company company, String billId, long amount) {
        if (server == null || company == null || billId == null || amount <= 0L) return false;
        TaxLedgerSavedData ledger = TaxLedgerSavedData.get(server);
        TaxBill bill = ledger.get(billId);
        if (bill == null || bill.paid() || !bill.declared()
                || bill.subject().type() != TaxType.CORPORATE_INCOME
                || !bill.subject().subjectId().equals(company.companyId())
                || !bill.subject().taxpayerUuid().equals(company.ownerUuid())
                || !Currencies.exists(bill.currencyId())) return false;
        bill = updateLateFee(server, bill, server.overworld().getGameTime());
        long payment = Math.min(amount, bill.outstanding());
        long majorPayment = Math.min(Math.max(0L, company.treasuryOf(bill.currencyId())),
                payment / Money.MINOR_UNITS_PER_UNIT);
        if (majorPayment <= 0L || !CompanyHelper.debitTreasuryNonOperating(server, company.companyId(),
                bill.currencyId(), majorPayment, "tax_payment", "Corporate tax payment")) {
            return false;
        }
        payment = Money.toMinorSaturated(majorPayment);
        TaxBill paidBill = bill.withPayment(payment);
        ledger.replace(paidBill);
        ledger.addPayment(new TaxPayment(UUID.randomUUID().toString(), bill.id(), company.ownerUuid(),
                bill.currencyId(), payment, server.overworld().getGameTime()));
        if (paidBill.paid()) NeoForge.EVENT_BUS.post(new TaxSettledEvent(server, paidBill));
        return true;
    }

    /**
     * Settles a liability from an external proceeds source, such as a land
     * auction. No player wallet is charged; the caller has already withheld
     * the proceeds and must provide an auditable source id.
     */
    public static long settleFromProceeds(MinecraftServer server, TaxSubject subject,
                                          long amount, String sourceId, long now) {
        if (amount <= 0L || subject == null || sourceId == null || sourceId.isBlank()) return 0L;
        TaxLedgerSavedData ledger = TaxLedgerSavedData.get(server);
        if (ledger.hasExternalSettlement(sourceId)) return 0L;

        long remaining = amount;
        long settled = 0L;
        for (TaxBill bill : ledger.bills()) {
            if (remaining <= 0L) break;
            if (!bill.subject().equals(subject) || bill.paid()) continue;
            long payment = Math.min(remaining, bill.outstanding());
            if (payment <= 0L) continue;
            TaxBill updated = bill.withPayment(payment);
            ledger.replace(updated);
            ledger.addPayment(new TaxPayment(UUID.randomUUID().toString(), bill.id(),
                    subject.taxpayerUuid(), bill.currencyId(), payment, now));
            remaining -= payment;
            settled = addSaturated(settled, payment);
            if (updated.paid()) NeoForge.EVENT_BUS.post(new TaxSettledEvent(server, updated));
        }
        if (settled > 0L) ledger.recordExternalSettlement(sourceId);
        return settled;
    }

    public static boolean declare(ServerPlayer player, String billId) {
        TaxLedgerSavedData ledger = TaxLedgerSavedData.get(player.getServer());
        TaxBill bill = ledger.get(billId);
        if (bill == null || bill.declared() || !bill.subject().taxpayerUuid().equals(player.getUUID())) return false;
        ledger.replace(bill.withDeclaration(player.getServer().overworld().getGameTime(), player.getUUID().toString()));
        return true;
    }

    public static TaxBill updateLateFee(MinecraftServer server, TaxBill bill, long now) {
        if (!bill.declared() || bill.dueAt() <= 0L || now <= bill.dueAt() || bill.paid()) return bill;
        long daysLate = Math.max(1L, (now - bill.dueAt()) / PerpetualCalendar.TICKS_PER_DAY);
        long daily = bill.amount() > Long.MAX_VALUE / 5L
                ? Long.MAX_VALUE : (bill.amount() * 5L) / 10_000L;
        daily = Math.max(1L, daily);
        long fee = daily > Long.MAX_VALUE / daysLate ? Long.MAX_VALUE : daily * daysLate;
        if (fee <= bill.lateFeeAmount() && bill.lateFeeUpdatedAt() == now) return bill;
        TaxBill updated = bill.withLateFee(fee, now);
        TaxLedgerSavedData.get(server).replace(updated);
        return updated;
    }

    /** Records delinquency and sends periodic collection notices. Land disposal remains external. */
    public static void processEnforcement(MinecraftServer server, TaxBill bill, long now) {
        if (bill.status(now) != TaxBill.Status.DELINQUENT) return;
        if (bill.subject().type() == TaxType.CORPORATE_INCOME) {
            Company company = CompanySavedData.get(server).get(bill.subject().subjectId());
            if (company != null && company.ownerUuid().equals(bill.subject().taxpayerUuid())) {
                CompanyLifecycleService.forceSuspend(server, company.companyId(), "corporate tax delinquency");
            }
        }
        TaxEnforcementSavedData enforcement = TaxEnforcementSavedData.get(server);
        if (!enforcement.shouldNotify(bill.id(), now)) return;
        enforcement.recordNotice(bill.id(), now);
        NeoForge.EVENT_BUS.post(new TaxDelinquentEvent(server, bill, now));
        TaxNotificationService.notify(server, bill.subject().taxpayerUuid(),
                "delinquent:" + bill.id() + ":" + now,
                "Tax delinquent: " + bill.currencyId().toUpperCase() + " "
                        + Money.format(bill.outstanding()) + ". Please pay immediately.");
    }

    /** Assesses income tax in one place; revenue is expressed in major currency units. */
    public static TaxBill assessIncomeTax(MinecraftServer server, TaxSubject subject, String currencyId,
                                          long revenue, double rate, long now) {
        if (revenue <= 0L || rate <= 0.0) return null;
        long taxMajor = Math.max(0L, Math.round(revenue * rate));
        long taxAmount = Money.toMinor(taxMajor);
        if (taxAmount <= 0L) return null;
        long target = addSaturated(outstanding(server, subject), taxAmount);
        TaxBill bill = ensureOutstanding(server, subject, currencyId, target, now, 0L, 0L);
        if (bill != null && bill.taxableBase() == 0L) {
            TaxLedgerSavedData.get(server).replace(new TaxBill(bill.id(), bill.subject(), bill.currencyId(),
                    bill.amount(), bill.paidAmount(), bill.createdAt(), bill.dueAt(), bill.graceUntil(),
                    "income", now, now, Money.toMinorSaturated(revenue),
                    (int) Math.min(Integer.MAX_VALUE, Math.round(rate * 10_000.0))));
            bill = TaxLedgerSavedData.get(server).get(bill.id());
        }
        return bill;
    }

    /** Assesses one income event and ignores duplicate submissions by event id. */
    public static TaxBill assessIncomeEvent(MinecraftServer server, TaxableIncomeEvent event, double rate) {
        TaxIncomeEventSavedData events = TaxIncomeEventSavedData.get(server);
        if (events.contains(event.eventId())) return null;
        TaxBill bill = assessIncomeTax(server, event.subject(), event.currencyId(), event.revenue(), rate,
                event.occurredAt());
        if (bill != null) events.add(event.eventId());
        return bill;
    }

    /** Assesses an income event with the centralized, persisted tax rule. */
    public static TaxBill assessIncomeEvent(MinecraftServer server, TaxableIncomeEvent event) {
        TaxIncomeEventSavedData events = TaxIncomeEventSavedData.get(server);
        if (events.contains(event.eventId())) return null;
        TaxRule rule = TaxRuleService.current(server, event.subject().type(), event.occurredAt());
        long baseMinor = Money.toMinorSaturated(event.revenue());
        long taxAmount = TaxRuleService.taxMinor(server, event.subject().type(), baseMinor, event.occurredAt());
        if (taxAmount <= 0L) return null;
        long target = addSaturated(outstanding(server, event.subject()), taxAmount);
        TaxBill bill = ensureOutstanding(server, event.subject(), event.currencyId(), target,
                event.occurredAt(), 0L, 0L, event.eventId(), event.occurredAt(), event.occurredAt(),
                baseMinor, rule.rateBasisPoints());
        if (bill != null) events.add(event.eventId());
        return bill;
    }

    private static long addSaturated(long left, long right) {
        return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
    }
}
