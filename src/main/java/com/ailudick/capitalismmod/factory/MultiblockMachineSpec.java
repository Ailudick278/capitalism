package com.ailudick.capitalismmod.factory;

import com.ailudick.capitalismmod.company.MachineType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import java.util.Map;

/** Real-world-inspired physical envelope for a factory process unit. */
public record MultiblockMachineSpec(String id, MachineType machine, int width, int height, int depth,
                                    Block casing, Block accent) {
    private static final Map<MachineType, MultiblockMachineSpec> SPECS = Map.ofEntries(
            entry(MachineType.ORE_PROCESSOR, 3, 3, 3, Blocks.IRON_BLOCK, Blocks.COPPER_BLOCK),
            entry(MachineType.BLAST_FURNACE, 3, 4, 3, Blocks.IRON_BLOCK, Blocks.BLAST_FURNACE),
            entry(MachineType.GLASS_FURNACE, 3, 3, 3, Blocks.IRON_BLOCK, Blocks.GLASS),
            entry(MachineType.ROLLING_MILL, 5, 3, 3, Blocks.IRON_BLOCK, Blocks.COPPER_BLOCK),
            entry(MachineType.SAWMILL, 3, 3, 3, Blocks.IRON_BLOCK, Blocks.OAK_PLANKS),
            entry(MachineType.OIL_REFINERY, 5, 4, 5, Blocks.IRON_BLOCK, Blocks.COPPER_BLOCK),
            entry(MachineType.STEAM_CRACKER, 5, 4, 5, Blocks.IRON_BLOCK, Blocks.GLASS),
            entry(MachineType.POLYMER_REACTOR, 3, 4, 3, Blocks.COPPER_BLOCK, Blocks.GLASS),
            entry(MachineType.CHEMICAL_REACTOR, 3, 4, 3, Blocks.COPPER_BLOCK, Blocks.GLASS),
            entry(MachineType.SEMICONDUCTOR_FAB, 5, 3, 5, Blocks.IRON_BLOCK, Blocks.GLASS),
            entry(MachineType.CRUSHER, 3, 3, 3, Blocks.IRON_BLOCK, Blocks.COBBLESTONE),
            entry(MachineType.FLOTATION_CELL, 5, 3, 3, Blocks.IRON_BLOCK, Blocks.COPPER_BLOCK),
            entry(MachineType.ELECTRIC_ARC_FURNACE, 3, 4, 3, Blocks.IRON_BLOCK, Blocks.LIGHTNING_ROD),
            entry(MachineType.CEMENT_KILN, 5, 4, 3, Blocks.IRON_BLOCK, Blocks.STONE),
            entry(MachineType.TEXTILE_MILL, 5, 3, 3, Blocks.IRON_BLOCK, Blocks.OAK_PLANKS),
            entry(MachineType.PULP_DIGESTER, 3, 4, 3, Blocks.IRON_BLOCK, Blocks.COPPER_BLOCK),
            entry(MachineType.PAPER_MILL, 5, 3, 3, Blocks.IRON_BLOCK, Blocks.OAK_PLANKS),
            entry(MachineType.PASTEURIZER, 3, 3, 3, Blocks.IRON_BLOCK, Blocks.GLASS),
            entry(MachineType.CANNERY, 5, 3, 3, Blocks.IRON_BLOCK, Blocks.COPPER_BLOCK),
            entry(MachineType.FERTILIZER_PLANT, 5, 4, 5, Blocks.IRON_BLOCK, Blocks.COPPER_BLOCK),
            entry(MachineType.PHARMACEUTICAL_REACTOR, 3, 4, 3, Blocks.COPPER_BLOCK, Blocks.GLASS),
            entry(MachineType.CNC_MACHINING_CENTER, 5, 3, 3, Blocks.IRON_BLOCK, Blocks.REDSTONE_BLOCK),
            entry(MachineType.CHIP_PACKAGING_LINE, 5, 3, 3, Blocks.IRON_BLOCK, Blocks.COPPER_BLOCK),
            entry(MachineType.ELECTRONICS_ASSEMBLY, 5, 3, 3, Blocks.IRON_BLOCK, Blocks.COPPER_BLOCK),
            entry(MachineType.ASSEMBLY_LINE, 7, 3, 3, Blocks.IRON_BLOCK, Blocks.COPPER_BLOCK));

    private static Map.Entry<MachineType, MultiblockMachineSpec> entry(MachineType machine, int width, int height,
                                                                          int depth, Block casing, Block accent) {
        return Map.entry(machine, new MultiblockMachineSpec(machine.id(), machine, width, height, depth, casing, accent));
    }
    public static MultiblockMachineSpec forMachine(String machineId) {
        MachineType type = MachineType.parse(machineId);
        return SPECS.getOrDefault(type, SPECS.get(MachineType.ASSEMBLY_LINE));
    }
}
