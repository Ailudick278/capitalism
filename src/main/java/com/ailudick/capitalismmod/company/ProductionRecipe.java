package com.ailudick.capitalismmod.company;

import java.util.Map;

/** One selectable production operation within an industry. */
public record ProductionRecipe(String id, Map<String, Integer> inputs, Map<String, Integer> outputs,
                               long income, String machineType, int workersPerCycle,
                               long energyCost, long maintenanceCost, String materialClass,
                               String qualityTier, Map<String, Integer> byproducts,
                               int pollutionScore, boolean hazardous) {
    public ProductionRecipe {
        id = id == null || id.isBlank() ? "default" : id;
        inputs = inputs == null ? Map.of() : Map.copyOf(inputs);
        outputs = outputs == null ? Map.of() : Map.copyOf(outputs);
        machineType = machineType == null || machineType.isBlank() ? "none" : machineType;
        workersPerCycle = Math.max(0, workersPerCycle);
        energyCost = Math.max(0L, energyCost);
        maintenanceCost = Math.max(0L, maintenanceCost);
        materialClass = materialClass == null || materialClass.isBlank() ? "intermediate" : materialClass;
        qualityTier = qualityTier == null || qualityTier.isBlank() ? "industrial" : qualityTier;
        byproducts = byproducts == null ? Map.of() : Map.copyOf(byproducts);
        pollutionScore = Math.max(0, pollutionScore);
    }

    public ProductionRecipe(String id, Map<String, Integer> inputs, Map<String, Integer> outputs,
                             long income, String machineType, int workersPerCycle,
                             long energyCost, long maintenanceCost) {
        this(id, inputs, outputs, income, machineType, workersPerCycle, energyCost, maintenanceCost,
                "intermediate", "industrial", Map.of(), 0, false);
    }

    public ProductionRecipe(String id, Map<String, Integer> inputs, Map<String, Integer> outputs, long income) {
        this(id, inputs, outputs, income, "none", outputs == null || outputs.isEmpty() ? 0 : 1, 0L, 0L);
    }

    /** A service cycle settles revenue directly instead of producing warehouse items. */
    public boolean isService() {
        return outputs.isEmpty() && income > 0L;
    }

    public boolean isHighPurity() {
        return "high_purity".equals(qualityTier) || "regulated".equals(qualityTier);
    }
}
