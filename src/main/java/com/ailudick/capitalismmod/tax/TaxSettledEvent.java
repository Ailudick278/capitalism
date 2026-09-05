package com.ailudick.capitalismmod.tax;

import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.Event;

/** Published when a tax bill is fully paid. */
public final class TaxSettledEvent extends Event {
    private final MinecraftServer server;
    private final TaxBill bill;
    public TaxSettledEvent(MinecraftServer server, TaxBill bill) { this.server = server; this.bill = bill; }
    public MinecraftServer server() { return server; }
    public TaxBill bill() { return bill; }
}
