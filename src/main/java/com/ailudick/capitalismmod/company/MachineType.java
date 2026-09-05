package com.ailudick.capitalismmod.company;

import java.util.Map;

/** Industrial equipment types used by production recipes. Prices are major USD units. */
public enum MachineType {
    NONE("none", 0L, 0L, Map.of()),
    FARM_PLOT("farm_plot", 250L, 1L, Map.of("minecraft:iron_ingot", 2, "minecraft:oak_planks", 4)),
    ORE_PROCESSOR("ore_processor", 500L, 2L, Map.of("minecraft:iron_ingot", 4, "minecraft:copper_ingot", 2, "minecraft:redstone", 1)),
    BLAST_FURNACE("blast_furnace", 1800L, 9L, Map.of("minecraft:iron_ingot", 5, "minecraft:coal", 4, "minecraft:stone", 3)),
    GLASS_FURNACE("glass_furnace", 900L, 3L, Map.of("minecraft:iron_ingot", 4, "minecraft:copper_ingot", 2, "minecraft:glass", 2)),
    WIRE_MILL("wire_mill", 1100L, 3L, Map.of("minecraft:iron_ingot", 4, "minecraft:copper_ingot", 3)),
    LATHE("lathe", 1200L, 4L, Map.of("minecraft:iron_ingot", 5, "minecraft:redstone", 2)),
    MILLING_MACHINE("milling_machine", 1500L, 5L, Map.of("minecraft:iron_ingot", 5, "minecraft:redstone", 2)),
    FOOD_PROCESSOR("food_processor", 1000L, 3L, Map.of("minecraft:iron_ingot", 3, "minecraft:copper_ingot", 2)),
    SAWMILL("sawmill", 1400L, 4L, Map.of("minecraft:iron_ingot", 5,
            "minecraft:copper_ingot", 2, "minecraft:oak_planks", 4)),
    DRY_KILN("dry_kiln", 1800L, 6L, Map.of("minecraft:iron_ingot", 6,
            "minecraft:copper_ingot", 2, "minecraft:glass", 2, "minecraft:coal", 2)),
    ROLLING_MILL("rolling_mill", 2200L, 6L, Map.of("minecraft:iron_ingot", 6, "minecraft:coal", 2)),
    ASSEMBLY_LINE("assembly_line", 3000L, 8L, Map.of("capitalismmod:machine_frame", 2,
            "capitalismmod:electric_motor", 2, "minecraft:glass", 1));

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
