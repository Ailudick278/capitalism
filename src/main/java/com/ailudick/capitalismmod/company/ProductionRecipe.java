package com.ailudick.capitalismmod.company;

import java.util.Map;

/** One selectable production operation within an industry. */
public record ProductionRecipe(String id, Map<String, Integer> inputs, Map<String, Integer> outputs,
                               long income, String machineType, int workersPerCycle,
                               long energyCost, long maintenanceCost) {
    public ProductionRecipe {
        id = id == null || id.isBlank() ? "default" : id;
        inputs = inputs == null ? Map.of() : Map.copyOf(inputs);
        outputs = outputs == null ? Map.of() : Map.copyOf(outputs);
        machineType = machineType == null || machineType.isBlank() ? "none" : machineType;
        workersPerCycle = Math.max(0, workersPerCycle);
        energyCost = Math.max(0L, energyCost);
        maintenanceCost = Math.max(0L, maintenanceCost);
    }

    public ProductionRecipe(String id, Map<String, Integer> inputs, Map<String, Integer> outputs, long income) {
        this(id, inputs, outputs, income, "none", outputs == null || outputs.isEmpty() ? 0 : 1, 0L, 0L);
    }
}
