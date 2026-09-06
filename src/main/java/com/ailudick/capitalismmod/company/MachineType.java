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
    OIL_WELL("oil_well", 3500L, 12L, Map.of("capitalismmod:steel_sheet", 4,
            "capitalismmod:machine_frame", 2)),
    OIL_REFINERY("oil_refinery", 4200L, 14L, Map.of("capitalismmod:steel_sheet", 6,
            "capitalismmod:machine_frame", 2, "minecraft:iron_ingot", 4)),
    STEAM_CRACKER("steam_cracker", 3900L, 12L, Map.of("capitalismmod:steel_sheet", 5,
            "capitalismmod:machine_frame", 2, "minecraft:iron_ingot", 4,
            "minecraft:glass", 2)),
    POLYMER_REACTOR("polymer_reactor", 2400L, 7L, Map.of("minecraft:iron_ingot", 6,
            "minecraft:copper_ingot", 3, "minecraft:redstone", 2, "minecraft:glass", 2)),
    BLENDING_UNIT("blending_unit", 2800L, 8L, Map.of("capitalismmod:machine_frame", 2,
            "capitalismmod:steel_sheet", 3, "minecraft:iron_ingot", 4, "minecraft:redstone", 2)),
    INJECTION_MOLDER("injection_molder", 2600L, 7L, Map.of("capitalismmod:machine_frame", 2,
            "capitalismmod:electric_motor", 1, "capitalismmod:steel_sheet", 2)),
    EXTRUSION_LINE("extrusion_line", 3000L, 8L, Map.of("capitalismmod:machine_frame", 2,
            "capitalismmod:electric_motor", 2, "capitalismmod:steel_sheet", 3)),
    PLASTIC_RECYCLER("plastic_recycler", 3400L, 9L, Map.of("capitalismmod:machine_frame", 2,
            "capitalismmod:electric_motor", 2, "capitalismmod:steel_sheet", 3,
            "minecraft:iron_ingot", 4, "minecraft:water_bucket", 1)),
    SILICON_REFINER("silicon_refiner", 4600L, 13L, Map.of("capitalismmod:machine_frame", 2,
            "capitalismmod:steel_sheet", 4, "minecraft:quartz", 2, "minecraft:redstone", 2)),
    CRYSTAL_GROWTH_FURNACE("crystal_growth_furnace", 5000L, 14L, Map.of("capitalismmod:machine_frame", 2,
            "capitalismmod:steel_sheet", 4, "capitalismmod:electric_motor", 1, "minecraft:glass", 2)),
    CHIP_PACKAGING_LINE("chip_packaging_line", 4300L, 11L, Map.of("capitalismmod:machine_frame", 2,
            "capitalismmod:electric_motor", 1, "capitalismmod:copper_wire", 3, "minecraft:glass", 1)),
    SEMICONDUCTOR_FAB("semiconductor_fab", 5200L, 16L, Map.of("capitalismmod:machine_frame", 2,
            "capitalismmod:steel_sheet", 4, "minecraft:quartz", 2, "minecraft:redstone", 3)),
    ELECTRONICS_ASSEMBLY("electronics_assembly", 4200L, 10L, Map.of("capitalismmod:machine_frame", 2,
            "capitalismmod:electric_motor", 1, "capitalismmod:copper_wire", 3, "minecraft:redstone", 3,
            "minecraft:glass", 1)),
    SMT_LINE("smt_line", 4800L, 12L, Map.of("capitalismmod:machine_frame", 2,
            "capitalismmod:electric_motor", 2, "capitalismmod:copper_wire", 4,
            "minecraft:redstone", 4, "minecraft:glass", 2)),
    CHEMICAL_REACTOR("chemical_reactor", 3200L, 9L, Map.of("capitalismmod:machine_frame", 2,
            "capitalismmod:steel_sheet", 3, "minecraft:glass", 2, "minecraft:redstone", 2)),
    LAMINATION_PRESS("lamination_press", 2700L, 7L, Map.of("capitalismmod:machine_frame", 2,
            "capitalismmod:steel_sheet", 3, "minecraft:iron_ingot", 4)),
    ALLOY_FURNACE("alloy_furnace", 2300L, 7L, Map.of("minecraft:iron_ingot", 6,
            "minecraft:coal", 3, "minecraft:redstone", 1)),
    PCB_FABRICATION_LINE("pcb_fabrication_line", 4700L, 12L, Map.of("capitalismmod:machine_frame", 2,
            "capitalismmod:electric_motor", 2, "capitalismmod:copper_wire", 3,
            "minecraft:redstone", 4, "minecraft:glass", 2)),
    PCB_ASSEMBLY_LINE("pcb_assembly_line", 5100L, 13L, Map.of("capitalismmod:machine_frame", 2,
            "capitalismmod:electric_motor", 2, "capitalismmod:copper_wire", 4,
            "minecraft:redstone", 4, "minecraft:glass", 2)),
    WAFER_DICING_SAW("wafer_dicing_saw", 4400L, 11L, Map.of("capitalismmod:machine_frame", 2,
            "capitalismmod:steel_sheet", 3, "minecraft:diamond", 1, "minecraft:redstone", 2)),
    CHIP_TESTING_STATION("chip_testing_station", 4600L, 12L, Map.of("capitalismmod:machine_frame", 2,
            "capitalismmod:copper_wire", 3, "minecraft:redstone", 4, "minecraft:glass", 2)),
    STAMPING_PRESS("stamping_press", 2800L, 8L, Map.of("capitalismmod:machine_frame", 2,
            "capitalismmod:steel_sheet", 4, "minecraft:iron_ingot", 5)),
    MOLDING_COMPOUND_UNIT("molding_compound_unit", 3000L, 8L, Map.of("capitalismmod:machine_frame", 2,
            "capitalismmod:electric_motor", 1, "capitalismmod:steel_sheet", 3)),
    BATTERY_MATERIALS("battery_materials", 3600L, 9L, Map.of("capitalismmod:machine_frame", 2,
            "capitalismmod:steel_sheet", 3, "minecraft:iron_ingot", 4,
            "minecraft:redstone", 2, "minecraft:glass", 2)),
    BATTERY_CELL_LINE("battery_cell_line", 4500L, 11L, Map.of("capitalismmod:machine_frame", 2,
            "capitalismmod:electric_motor", 2, "capitalismmod:copper_wire", 3,
            "capitalismmod:steel_sheet", 3, "minecraft:glass", 2)),
    BATTERY_RECYCLER("battery_recycler", 3800L, 10L, Map.of("capitalismmod:machine_frame", 2,
            "capitalismmod:steel_sheet", 4, "capitalismmod:electric_motor", 1,
            "minecraft:iron_ingot", 4, "minecraft:redstone", 2)),
    ELECTRONICS_RECYCLER("electronics_recycler", 4100L, 11L, Map.of("capitalismmod:machine_frame", 2,
            "capitalismmod:steel_sheet", 4, "capitalismmod:electric_motor", 2,
            "capitalismmod:copper_wire", 2, "minecraft:iron_ingot", 4, "minecraft:redstone", 2)),
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
