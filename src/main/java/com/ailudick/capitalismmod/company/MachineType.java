package com.ailudick.capitalismmod.company;

/** Industrial equipment types used by production recipes. Prices are major USD units. */
public enum MachineType {
    NONE("none", 0L, 0L),
    FARM_PLOT("farm_plot", 250L, 1L),
    ORE_PROCESSOR("ore_processor", 500L, 2L),
    LATHE("lathe", 1200L, 4L),
    MILLING_MACHINE("milling_machine", 1500L, 5L),
    ROLLING_MILL("rolling_mill", 2200L, 6L),
    ASSEMBLY_LINE("assembly_line", 3000L, 8L);

    private final String id;
    private final long purchasePrice;
    private final long maintenancePerCycle;

    MachineType(String id, long purchasePrice, long maintenancePerCycle) {
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
