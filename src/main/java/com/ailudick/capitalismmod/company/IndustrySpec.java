package com.ailudick.capitalismmod.company;

import java.util.List;
import java.util.Map;

/**
 * The data-driven production recipe for one industry.
 *
 * @param id      industry id (see {@link CompanyTypes})
 * @param inputs  item id -> quantity consumed per level per tick
 * @param outputs item id -> quantity produced per level per tick
 * @param income  USD income per level per tick (finance uses a special formula)
 */
public record IndustrySpec(String id, Map<String, Integer> inputs, Map<String, Integer> outputs, long income,
                           String machineType, int workersPerCycle, long energyCost, long maintenanceCost,
                           List<ProductionRecipe> recipes) {
    public IndustrySpec(String id, Map<String, Integer> inputs, Map<String, Integer> outputs, long income) {
        this(id, inputs, outputs, income, "none", outputs.isEmpty() ? 0 : 1, 0L, 0L,
                List.of(new ProductionRecipe("default", inputs, outputs, income)));
    }

    public IndustrySpec(String id, Map<String, Integer> inputs, Map<String, Integer> outputs, long income,
                        String machineType, int workersPerCycle, long energyCost, long maintenanceCost) {
        this(id, inputs, outputs, income, machineType, workersPerCycle, energyCost, maintenanceCost,
                List.of(new ProductionRecipe("default", inputs, outputs, income, machineType,
                        workersPerCycle, energyCost, maintenanceCost)));
    }

    public IndustrySpec {
        recipes = recipes == null || recipes.isEmpty()
                ? List.of(new ProductionRecipe("default", inputs, outputs, income, machineType,
                workersPerCycle, energyCost, maintenanceCost)) : List.copyOf(recipes);
    }

    public ProductionRecipe recipe(String recipeId) {
        if (recipeId != null) {
            for (ProductionRecipe recipe : recipes) {
                if (recipe.id().equals(recipeId)) return recipe;
            }
        }
        return recipes.get(0);
    }
}
