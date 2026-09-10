package com.ailudick.capitalismmod.progression;

import com.ailudick.capitalismmod.company.MachineType;
import java.util.Arrays;

/** Ordered technology eras used by machines, recipes, NPC jobs and logistics. */
public enum TechnologyEra {
    PRE_INDUSTRIAL("pre_industrial", 0),
    STEAM_INDUSTRY("steam_industry", 1),
    ELECTRIFICATION("electrification", 2),
    PETROCHEMICAL("petrochemical", 3),
    INFORMATION("information", 4);

    private final String id;
    private final int level;

    TechnologyEra(String id, int level) {
        this.id = id;
        this.level = level;
    }

    public String id() {
        return id;
    }

    public int level() {
        return level;
    }

    public boolean includes(TechnologyEra required) {
        return required != null && level >= required.level;
    }

    public static TechnologyEra forMachine(String machineId) {
        MachineType machine = MachineType.parse(machineId);
        if (machine == null || machine == MachineType.NONE || machine == MachineType.FARM_PLOT) return PRE_INDUSTRIAL;
        String id = machine.id();
        if (id.contains("oil") || id.contains("chemical") || id.contains("polymer") || id.contains("fertilizer") || id.contains("pharmaceutical")) return PETROCHEMICAL;
        if (id.contains("silicon") || id.contains("chip") || id.contains("semiconductor") || id.contains("electronics") || id.contains("smt") || id.contains("battery") || id.contains("pcb") || id.contains("wafer")) return INFORMATION;
        if (id.contains("wire") || id.contains("electric") || id.contains("motor") || id.contains("cnc") || id.contains("assembly") || id.contains("milling") || id.contains("lathe")) return ELECTRIFICATION;
        return STEAM_INDUSTRY;
    }

    public static TechnologyEra fromId(String id) {
        return Arrays.stream(values()).filter(era -> era.id.equals(id)).findFirst().orElse(PRE_INDUSTRIAL);
    }
}
