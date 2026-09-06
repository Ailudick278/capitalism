package com.ailudick.capitalismmod.economy.expansion;

/** The ten extensible gameplay domains built on the shared economic layer. */
public enum ExpansionSystem {
    CONTRACTS("contracts"),
    LABOR("labor"),
    CITIES("cities"),
    MACROECONOMY("macroeconomy"),
    CORPORATE_GOVERNANCE("corporate_governance"),
    BANKING_RISK("banking_risk"),
    ENVIRONMENT("environment"),
    MARKET_INFORMATION("market_information"),
    INTERNATIONAL_TRADE("international_trade"),
    ECONOMIC_EVENTS("economic_events");

    private final String id;

    ExpansionSystem(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
