package com.ailudick.capitalismmod.tax;

import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.Event;

/** Published by the tax system; other systems decide what enforcement means for them. */
public final class TaxDelinquentEvent extends Event {
    private final MinecraftServer server;
    private final TaxBill bill;
    private final long gameTime;

    public TaxDelinquentEvent(MinecraftServer server, TaxBill bill, long gameTime) {
        this.server = server;
        this.bill = bill;
        this.gameTime = gameTime;
    }
    public MinecraftServer server() { return server; }
    public TaxBill bill() { return bill; }
    public long gameTime() { return gameTime; }
}
