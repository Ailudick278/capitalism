package com.ailudick.capitalismmod.company;

/** Industrial equipment types used by production recipes. Prices are major USD units. */
public enum MachineType {
    NONE("none", 0L, 0L, Integer.MAX_VALUE),
    FARM_PLOT("farm_plot", 250L, 1L, 1),
    ORE_PROCESSOR("ore_processor", 500L, 2L, 1),
    GLASS_FURNACE("glass_furnace", 900L, 3L, 1),
    WIRE_MILL("wire_mill", 1100L, 3L, 1),
    LATHE("lathe", 1200L, 4L, 1),
    MILLING_MACHINE("milling_machine", 1500L, 5L, 1),
    FOOD_PROCESSOR("food_processor", 1000L, 3L, 1),
    ROLLING_MILL("rolling_mill", 2200L, 6L, 1),
    ASSEMBLY_LINE("assembly_line", 3000L, 8L, 2);

    private final String id;
    private final long purchasePrice;
    private final long maintenancePerCycle;
    MachineType(String id, long purchasePrice, long maintenancePerCycle, int unusedCapacity) {
        this.id = id;
        this.purchasePrice = purchasePrice;
        this.maintenancePerCycle = maintenancePerCycle;
    }

    public String id() { return id; }
    public long purchasePrice() { return purchasePrice; }
    public long maintenancePerCycle() { return maintenancePerCycle; }
    public static MachineType parse(String id) {
        for (MachineType type : values()) {
            if (type.id.equalsIgnoreCase(id)) return type;
        }
        return null;
    }
}
