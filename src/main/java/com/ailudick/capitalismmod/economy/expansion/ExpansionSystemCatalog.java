package com.ailudick.capitalismmod.economy.expansion;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Single source of truth for the first-stage scope of the ten new gameplay systems. */
public final class ExpansionSystemCatalog {
    private static final Map<ExpansionSystem, ExpansionSystemDescriptor> DESCRIPTORS = build();

    private ExpansionSystemCatalog() {
    }

    public static ExpansionSystemDescriptor descriptor(ExpansionSystem system) {
        return DESCRIPTORS.get(system);
    }

    public static Map<ExpansionSystem, ExpansionSystemDescriptor> all() {
        return Map.copyOf(DESCRIPTORS);
    }

    private static Map<ExpansionSystem, ExpansionSystemDescriptor> build() {
        EnumMap<ExpansionSystem, ExpansionSystemDescriptor> result = new EnumMap<>(ExpansionSystem.class);
        add(result, ExpansionSystem.CONTRACTS,
                List.of("contract", "contract_term", "fulfilment", "default_record"),
                List.of(ExpansionSystem.LABOR, ExpansionSystem.INTERNATIONAL_TRADE),
                List.of("obligation", "payment_schedule", "default_risk"));
        add(result, ExpansionSystem.LABOR,
                List.of("household", "skill_profile", "job_offer", "employment"),
                List.of(ExpansionSystem.CITIES, ExpansionSystem.MACROECONOMY),
                List.of("wage_flow", "employment", "production_capacity", "consumption_demand"));
        add(result, ExpansionSystem.CITIES,
                List.of("region", "facility", "capacity", "service_level"),
                List.of(ExpansionSystem.LABOR, ExpansionSystem.INTERNATIONAL_TRADE),
                List.of("logistics_capacity", "utility_service", "regional_attractiveness"));
        add(result, ExpansionSystem.MACROECONOMY,
                List.of("policy", "macro_snapshot", "government_budget", "economic_cycle"),
                List.of(ExpansionSystem.LABOR, ExpansionSystem.ENVIRONMENT),
                List.of("tax_policy", "interest_policy", "subsidy", "demand_shock"));
        add(result, ExpansionSystem.CORPORATE_GOVERNANCE,
                List.of("shareholder", "board", "disclosure", "corporate_action"),
                List.of(ExpansionSystem.CONTRACTS, ExpansionSystem.MARKET_INFORMATION),
                List.of("voting_result", "governance_score", "market_confidence"));
        add(result, ExpansionSystem.BANKING_RISK,
                List.of("bank_profile", "capital_position", "liquidity_position", "stress_scenario"),
                List.of(ExpansionSystem.MACROECONOMY, ExpansionSystem.MARKET_INFORMATION),
                List.of("credit_limit", "liquidity_alert", "default_event", "crisis_state"));
        add(result, ExpansionSystem.ENVIRONMENT,
                List.of("resource_stock", "emission_record", "pollution_state", "mitigation_project"),
                List.of(ExpansionSystem.CITIES, ExpansionSystem.MACROECONOMY),
                List.of("environmental_cost", "regulatory_penalty", "resilience_level"));
        add(result, ExpansionSystem.MARKET_INFORMATION,
                List.of("information_report", "source_quality", "publication_delay", "insider_signal"),
                List.of(ExpansionSystem.CORPORATE_GOVERNANCE, ExpansionSystem.ECONOMIC_EVENTS),
                List.of("known_price", "confidence_level", "decision_modifier"));
        add(result, ExpansionSystem.INTERNATIONAL_TRADE,
                List.of("trade_region", "tariff_rule", "quota", "trade_route", "external_actor"),
                List.of(ExpansionSystem.CITIES, ExpansionSystem.MACROECONOMY),
                List.of("landed_cost", "foreign_exchange_flow", "border_delay", "trade_exposure"));
        add(result, ExpansionSystem.ECONOMIC_EVENTS,
                List.of("event_definition", "shock", "duration", "propagation_rule", "recovery_plan"),
                List.of(ExpansionSystem.MACROECONOMY, ExpansionSystem.ENVIRONMENT),
                List.of("system_modifier", "risk_signal", "recovery_task"));
        return result;
    }

    private static void add(Map<ExpansionSystem, ExpansionSystemDescriptor> result, ExpansionSystem system,
                             List<String> objects, List<ExpansionSystem> dependencies, List<String> outputs) {
        result.put(system, new ExpansionSystemDescriptor(system, objects, dependencies, outputs));
    }
}
