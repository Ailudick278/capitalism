package com.ailudick.capitalismmod.company;

import java.util.Map;

/** Industrial equipment types used by production recipes. Prices are major USD units. */
public enum MachineType {
    NONE("none", 0L, 0L, Map.of()),
    FARM_PLOT("farm_plot", 250L, 1L, Map.of("minecraft:iron_ingot", 2, "minecraft:oak_planks", 4)),
    ORE_PROCESSOR("ore_processor", 500L, 2L, Map.of("minecraft:iron_ingot", 4, "minecraft:copper_ingot", 2, "minecraft:redstone", 1)),
    GLASS_FURNACE("glass_furnace", 900L, 3L, Map.of("capitalismmod:steel_sheet", 3, "minecraft:glass", 2)),
    WIRE_MILL("wire_mill", 1100L, 3L, Map.of("capitalismmod:steel_sheet", 4, "capitalismmod:copper_wire", 2)),
    LATHE("lathe", 1200L, 4L, Map.of("capitalismmod:steel_sheet", 5, "capitalismmod:copper_wire", 1)),
    MILLING_MACHINE("milling_machine", 1500L, 5L, Map.of("capitalismmod:steel_sheet", 5, "capitalismmod:copper_wire", 1)),
    FOOD_PROCESSOR("food_processor", 1000L, 3L, Map.of("capitalismmod:steel_sheet", 3, "capitalismmod:copper_wire", 2)),
    ROLLING_MILL("rolling_mill", 2200L, 6L, Map.of("capitalismmod:steel_sheet", 6, "capitalismmod:copper_wire", 2)),
    ASSEMBLY_LINE("assembly_line", 3000L, 8L, Map.of("capitalismmod:steel_sheet", 8, "capitalismmod:copper_wire", 4,
            "capitalismmod:electric_lamp", 1));

    private final String id;
    private final long purchasePrice;
    private final long maintenancePerCycle;
    private final Map<String, Integer> materials;
    MachineType(String id, long purchasePrice, long maintenancePerCycle, Map<String, Integer> materials) {
        this.id = id;
        this.purchasePrice = purchasePrice;
        this.maintenancePerCycle = maintenancePerCycle;
        this.materials = Map.copyOf(materials);
    }

    public String id() { return id; }
    public long purchasePrice() { return purchasePrice; }
    public long maintenancePerCycle() { return maintenancePerCycle; }
    public Map<String, Integer> materials() { return materials; }
    public static MachineType parse(String id) {
        for (MachineType type : values()) {
            if (type.id.equalsIgnoreCase(id)) return type;
        }
        return null;
    }
}
