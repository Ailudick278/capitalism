package com.ailudick.capitalismmod.population;

import java.util.Locale;

/** Industrial occupations mapped to real process equipment. */
public enum NpcProfession {
    UNEMPLOYED("unemployed"), MINER("miner"), METALLURGIST("metallurgist"),
    ROLLING_OPERATOR("rolling_operator"), WOODWORKER("woodworker"),
    MINERAL_PROCESSOR("mineral_processor"), TEXTILE_WORKER("textile_worker"),
    FOOD_TECHNICIAN("food_technician"), PAPERMAKER("papermaker"),
    REFINERY_OPERATOR("refinery_operator"), CHEMICAL_ENGINEER("chemical_engineer"),
    SEMICONDUCTOR_ENGINEER("semiconductor_engineer"),
    ASSEMBLY_TECHNICIAN("assembly_technician"), QUALITY_INSPECTOR("quality_inspector");

    private final String id;
    NpcProfession(String id) { this.id = id; }
    public String id() { return id; }
    public static NpcProfession parse(String id) {
        if (id == null) return null;
        for (NpcProfession value : values()) if (value.id.equalsIgnoreCase(id) || value.name().equalsIgnoreCase(id)) return value;
        return null;
    }
    public boolean canOperate(String machineType) {
        if (this == QUALITY_INSPECTOR) return true;
        String type = machineType == null ? "" : machineType.toLowerCase(Locale.ROOT);
        return switch (this) {
            case MINER -> type.contains("ore_processor") || type.contains("oil_well");
            case METALLURGIST -> type.contains("blast_furnace") || type.contains("alloy_furnace") || type.contains("glass_furnace")
                    || type.contains("arc_furnace") || type.contains("cement_kiln") || type.contains("cnc");
            case ROLLING_OPERATOR -> type.contains("rolling_mill") || type.contains("stamping_press") || type.contains("lamination");
            case WOODWORKER -> type.contains("sawmill") || type.contains("dry_kiln");
            case MINERAL_PROCESSOR -> type.contains("crusher") || type.contains("flotation");
            case TEXTILE_WORKER -> type.contains("textile");
            case PAPERMAKER -> type.contains("pulp") || type.contains("paper");
            case REFINERY_OPERATOR -> type.contains("refinery") || type.contains("steam_cracker");
            case CHEMICAL_ENGINEER -> type.contains("reactor") || type.contains("cracker") || type.contains("polymer") || type.contains("blending");
            case SEMICONDUCTOR_ENGINEER -> type.contains("semiconductor") || type.contains("chip_") || type.contains("wafer_") || type.contains("pcb_");
            case FOOD_TECHNICIAN -> type.contains("food") || type.contains("farm") || type.contains("milling");
            case ASSEMBLY_TECHNICIAN -> type.contains("assembly") || type.contains("molder") || type.contains("battery");
            default -> false;
        };
    }

    /** Default training path for a new worker entering a process unit. */
    public static NpcProfession preferredFor(String machineType) {
        String type = machineType == null ? "" : machineType.toLowerCase(Locale.ROOT);
        if (type.contains("ore_processor") || type.contains("oil_well")) return MINER;
        if (type.contains("blast_furnace") || type.contains("alloy_furnace") || type.contains("glass_furnace")) return METALLURGIST;
        if (type.contains("rolling") || type.contains("stamping") || type.contains("lamination")) return ROLLING_OPERATOR;
        if (type.contains("sawmill") || type.contains("kiln")) return WOODWORKER;
        if (type.contains("crusher") || type.contains("flotation")) return MINERAL_PROCESSOR;
        if (type.contains("textile")) return TEXTILE_WORKER;
        if (type.contains("pulp") || type.contains("paper")) return PAPERMAKER;
        if (type.contains("pasteur") || type.contains("cannery")) return FOOD_TECHNICIAN;
        if (type.contains("fertilizer") || type.contains("pharmaceutical")) return CHEMICAL_ENGINEER;
        if (type.contains("cnc") || type.contains("arc_furnace") || type.contains("cement_kiln")) return METALLURGIST;
        if (type.contains("refinery") || type.contains("cracker")) return REFINERY_OPERATOR;
        if (type.contains("reactor") || type.contains("polymer") || type.contains("blending")) return CHEMICAL_ENGINEER;
        if (type.contains("semiconductor") || type.contains("chip_") || type.contains("wafer_") || type.contains("pcb_")) return SEMICONDUCTOR_ENGINEER;
        if (type.contains("food") || type.contains("farm") || type.contains("milling")) return FOOD_TECHNICIAN;
        return ASSEMBLY_TECHNICIAN;
    }
}
