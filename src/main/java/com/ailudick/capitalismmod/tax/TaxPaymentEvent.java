package com.ailudick.capitalismmod.tax;

import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.Event;

/** Published for every successful tax payment, including partial payments. */
public final class TaxPaymentEvent extends Event {
    private final MinecraftServer server;
    private final TaxBill bill;
    private final TaxPayment payment;

    public TaxPaymentEvent(MinecraftServer server, TaxBill bill, TaxPayment payment) {
        this.server = server;
        this.bill = bill;
        this.payment = payment;
    }

    public MinecraftServer server() { return server; }
    public TaxBill bill() { return bill; }
    public TaxPayment payment() { return payment; }
}
