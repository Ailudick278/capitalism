package com.ailudick.capitalismmod.data;

import com.ailudick.capitalismmod.data.CapitalismData.RecipeJson;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Exact historical defaults only: custom pack recipes retain every field. */
final class IndustrialChainRecipeMigration {
    private record Legacy(String industry, RecipeJson recipe) {}
    private static final List<Legacy> LEGACY = List.of(
            new Legacy("basic_inorganic_chemicals", new RecipeJson("air_compression", Map.of("minecraft:glass", 1, "minecraft:water_bucket", 1), Map.of("capitalismmod:compressed_air", 2), 70, "air_compressor", 1, 3, 5)),
            new Legacy("manufacturing", new RecipeJson("refining", Map.of("capitalismmod:crude_oil", 2), Map.of( "capitalismmod:naphtha", 1, "capitalismmod:fuel_oil", 1, "capitalismmod:diesel", 1, "capitalismmod:gasoline", 1, "capitalismmod:lpg", 1, "capitalismmod:base_oil", 1, "capitalismmod:asphalt", 1), 130, "oil_refinery", 4, 8, 14)),
            new Legacy("petrochemical_refining", new RecipeJson("gas_reforming", Map.of("capitalismmod:refinery_gas", 2, "minecraft:water_bucket", 1), Map.of("capitalismmod:hydrogen", 1, "capitalismmod:lpg", 1), 118, "steam_reformer", 3, 7, 11)),
            new Legacy("petrochemical_refining", new RecipeJson("naphtha_reforming", Map.of("capitalismmod:naphtha", 2), Map.of("capitalismmod:gasoline", 1, "capitalismmod:benzene", 1, "capitalismmod:hydrogen", 1), 155, "catalytic_reformer", 3, 9, 14)),
            new Legacy("manufacturing", new RecipeJson("benzene_reforming", Map.of("capitalismmod:naphtha", 1), Map.of("capitalismmod:benzene", 1), 108, "oil_refinery", 3, 6, 11)),
            new Legacy("manufacturing", new RecipeJson("aromatics_reforming", Map.of("capitalismmod:naphtha", 1), Map.of("capitalismmod:p_xylene", 1), 112, "oil_refinery", 3, 6, 11)),
            new Legacy("manufacturing", new RecipeJson("steam_cracking", Map.of("capitalismmod:naphtha", 1), Map.of("capitalismmod:ethylene", 1, "capitalismmod:propylene", 1), 115, "steam_cracker", 3, 7, 12)),
            new Legacy("petrochemical_refining", new RecipeJson("steam_cracking_fractionated", Map.of("capitalismmod:naphtha", 1), Map.of("capitalismmod:ethylene", 1, "capitalismmod:propylene", 1, "capitalismmod:refinery_gas", 1), 125, "steam_cracker", 3, 8, 13)),
            new Legacy("petrochemical_refining", new RecipeJson("butadiene_extraction", Map.of("capitalismmod:refinery_gas", 2), Map.of("capitalismmod:butadiene", 1), 105, "steam_cracker", 2, 6, 9)),
            new Legacy("manufacturing", new RecipeJson("styrene_monomer", Map.of("capitalismmod:benzene", 1, "capitalismmod:ethylene", 1), Map.of("capitalismmod:styrene_monomer", 1), 145, "chemical_reactor", 3, 6, 11)),
            new Legacy("manufacturing", new RecipeJson("synthetic_rubber", Map.of("capitalismmod:propylene", 1, "capitalismmod:naphtha", 1), Map.of("capitalismmod:synthetic_rubber", 2), 105, "polymer_reactor", 3, 5, 9)),
            new Legacy("manufacturing", new RecipeJson("abs_resin", Map.of("capitalismmod:styrene_monomer", 1, "capitalismmod:synthetic_rubber", 1, "capitalismmod:plastic_pellets", 1), Map.of("capitalismmod:abs_resin", 2), 210, "polymer_reactor", 3, 6, 11)),
            new Legacy("manufacturing", new RecipeJson("ethylene_glycol", Map.of("capitalismmod:ethylene", 1, "minecraft:water_bucket", 1), Map.of("capitalismmod:ethylene_glycol", 2), 115, "chemical_reactor", 3, 5, 9)),
            new Legacy("petrochemical_refining", new RecipeJson("ethylene_oxide_production", Map.of("capitalismmod:ethylene", 1, "minecraft:water_bucket", 1), Map.of("capitalismmod:ethylene_oxide", 1), 130, "chemical_reactor", 3, 6, 10)),
            new Legacy("petrochemical_refining", new RecipeJson("propylene_oxide", Map.of("capitalismmod:propylene", 1, "minecraft:water_bucket", 1), Map.of("capitalismmod:propylene_oxide", 1), 130, "chemical_reactor", 3, 6, 10)),
            new Legacy("manufacturing", new RecipeJson("epoxy_resin", Map.of("capitalismmod:ethylene_glycol", 1, "capitalismmod:plastic_pellets", 1), Map.of("capitalismmod:epoxy_resin", 2), 145, "polymer_reactor", 3, 5, 10)),
            new Legacy("petrochemical_refining", new RecipeJson("cumene_cleavage", Map.of("capitalismmod:cumene", 1, "minecraft:water_bucket", 1), Map.of("capitalismmod:phenol", 1, "capitalismmod:acetone", 1), 175, "chemical_reactor", 3, 8, 12)),
            new Legacy("petrochemical_refining", new RecipeJson("phenolic_resin_production", Map.of("capitalismmod:phenol", 1, "capitalismmod:plastic_pellets", 1), Map.of("capitalismmod:phenolic_resin", 2), 155, "polymer_reactor", 3, 7, 11)),
            new Legacy("petrochemical_refining", new RecipeJson("methanol_synthesis", Map.of("capitalismmod:refinery_gas", 2, "minecraft:water_bucket", 1), Map.of("capitalismmod:methanol", 2), 145, "methanol_synthesis_unit", 3, 7, 11)),
            new Legacy("petrochemical_refining", new RecipeJson("formaldehyde_oxidation", Map.of("capitalismmod:methanol", 1, "minecraft:water_bucket", 1), Map.of("capitalismmod:formaldehyde", 2), 105, "chemical_reactor", 2, 5, 8)),
            new Legacy("manufacturing", new RecipeJson("terephthalic_acid_oxidation", Map.of("capitalismmod:p_xylene", 1, "minecraft:water_bucket", 1), Map.of("capitalismmod:terephthalic_acid", 1), 132, "chemical_reactor", 3, 6, 11)),
            new Legacy("manufacturing", new RecipeJson("lubricant_blending", Map.of("capitalismmod:base_oil", 1, "capitalismmod:fuel_oil", 1), Map.of("capitalismmod:lubricant", 2), 95, "blending_unit", 2, 3, 7)),
            new Legacy("manufacturing", new RecipeJson("plastic_pellets", Map.of("capitalismmod:naphtha", 1, "minecraft:coal", 1), Map.of("capitalismmod:plastic_pellets", 3), 70, "polymer_reactor", 2, 4, 7)),
            new Legacy("petrochemical_refining", new RecipeJson("chlor_alkali_electrolysis", Map.of("capitalismmod:salt", 2, "minecraft:water_bucket", 1), Map.of("capitalismmod:chlorine", 1, "capitalismmod:caustic_soda", 1), 125, "chlor_alkali_cell", 3, 7, 10)),
            new Legacy("petrochemical_refining", new RecipeJson("ethylene_dichloride_cracking", Map.of("capitalismmod:ethylene_dichloride", 1), Map.of("capitalismmod:vinyl_chloride", 1, "capitalismmod:hydrogen", 1), 155, "vinyl_chloride_unit", 3, 7, 11)),
            new Legacy("petrochemical_refining", new RecipeJson("vinyl_chloride_synthesis", Map.of("capitalismmod:ethylene", 1, "capitalismmod:chlorine", 1), Map.of("capitalismmod:vinyl_chloride", 1), 155, "vinyl_chloride_unit", 3, 7, 11)),
            new Legacy("petrochemical_refining", new RecipeJson("pvc_pipe_recycling", Map.of("capitalismmod:pvc_pipe", 2), Map.of("capitalismmod:reclaimed_plastic", 1, "capitalismmod:chlorine", 1), 115, "plastic_recycler", 3, 6, 10)),
            new Legacy("manufacturing", new RecipeJson("packaging_film_recycling", Map.of("capitalismmod:packaging_film", 4), Map.of("capitalismmod:plastic_pellets", 2), 88, "plastic_recycler", 2, 5, 9)),
            new Legacy("manufacturing", new RecipeJson("plastic_container_recycling", Map.of("capitalismmod:plastic_container", 2), Map.of("capitalismmod:plastic_pellets", 1), 82, "plastic_recycler", 2, 5, 9)),
            new Legacy("basic_inorganic_chemicals", new RecipeJson("air_separation", Map.of("minecraft:glass", 1, "minecraft:water_bucket", 1), Map.of("capitalismmod:nitrogen", 2, "capitalismmod:oxygen", 1), 115, "chemical_reactor", 2, 5, 8)),
            new Legacy("basic_inorganic_chemicals", new RecipeJson("hydrochloric_acid_synthesis", Map.of("capitalismmod:chlorine", 1, "minecraft:water_bucket", 1), Map.of("capitalismmod:hydrochloric_acid", 1), 95, "chemical_reactor", 2, 4, 7)),
            new Legacy("basic_inorganic_chemicals", new RecipeJson("nitric_acid_synthesis", Map.of("capitalismmod:nitrogen", 1, "capitalismmod:oxygen", 2, "minecraft:water_bucket", 1), Map.of("capitalismmod:nitric_acid", 1), 135, "chemical_reactor", 3, 6, 9)),
            new Legacy("petrochemical_refining", new RecipeJson("sulfuric_acid_production", Map.of("capitalismmod:sulfur", 1, "minecraft:water_bucket", 1), Map.of("capitalismmod:sulfuric_acid", 1), 105, "chemical_reactor", 3, 6, 10)),
            new Legacy("fertilizer_chemicals", new RecipeJson("urea_synthesis", Map.of("capitalismmod:ammonia", 2, "capitalismmod:refinery_gas", 1), Map.of("capitalismmod:urea", 2), 135, "fertilizer_plant", 3, 6, 10)),
            new Legacy("basic_inorganic_chemicals", new RecipeJson("sodium_bicarbonate_production", Map.of("capitalismmod:soda_ash", 1, "minecraft:water_bucket", 1), Map.of("capitalismmod:sodium_bicarbonate", 2), 85, "chemical_reactor", 2, 4, 7)),
            new Legacy("carbon_circular_chemicals", new RecipeJson("co2_hydrogenation_to_methanol", Map.of("capitalismmod:carbon_dioxide", 1, "capitalismmod:hydrogen", 2), Map.of("capitalismmod:methanol", 1), 165, "methanol_synthesis_unit", 3, 8, 12)),
            new Legacy("mining", new RecipeJson("bauxite_refining", Map.of("capitalismmod:raw_bauxite", 2, "minecraft:coal", 1), Map.of("capitalismmod:aluminum_ingot", 1), 82, "blast_furnace", 2, 4, 6)),
            new Legacy("metallurgy", new RecipeJson("alumina_refining", Map.of("capitalismmod:raw_bauxite", 2, "capitalismmod:limestone", 1), Map.of("capitalismmod:alumina", 1), 76, "ore_processor", 2, 4, 6)),
            new Legacy("metallurgy", new RecipeJson("aluminum_electrolysis", Map.of("capitalismmod:alumina", 2), Map.of("capitalismmod:aluminum_ingot", 1), 115, "electric_arc_furnace", 3, 9, 12)),
            new Legacy("metallurgy", new RecipeJson("copper_concentrate_refining", Map.of("minecraft:copper_ore", 2), Map.of("capitalismmod:copper_concentrate", 1), 42, "ore_processor", 2, 2, 4)),
            new Legacy("manufacturing", new RecipeJson("copper_foil", Map.of("minecraft:copper_ingot", 1), Map.of("capitalismmod:copper_foil", 2), 58, "rolling_mill", 2, 2, 5)),
            new Legacy("metallurgy", new RecipeJson("nickel_sulfate_refining", Map.of("capitalismmod:raw_nickel", 1, "capitalismmod:sulfur", 1), Map.of("capitalismmod:nickel_sulfate", 1), 118, "chemical_reactor", 3, 7, 10)),
            new Legacy("manufacturing", new RecipeJson("solder", Map.of("minecraft:copper_ingot", 1, "minecraft:iron_ingot", 1), Map.of("capitalismmod:solder", 2), 52, "alloy_furnace", 2, 3, 6)),
            new Legacy("semiconductor_fabrication", new RecipeJson("silicon_ingot", Map.of("minecraft:quartz", 3, "minecraft:coal", 1), Map.of("capitalismmod:silicon_ingot", 1), 145, "silicon_refiner", 3, 7, 12)),
            new Legacy("manufacturing", new RecipeJson("silicon_wafer", Map.of("minecraft:quartz", 2, "minecraft:coal", 1), Map.of("capitalismmod:silicon_wafer", 1), 180, "semiconductor_fab", 3, 8, 16)),
            new Legacy("manufacturing", new RecipeJson("silicon_wafer_from_polysilicon", Map.of("capitalismmod:polysilicon", 2), Map.of("capitalismmod:silicon_wafer", 1), 230, "crystal_growth_furnace", 3, 9, 16)),
            new Legacy("semiconductor_fabrication", new RecipeJson("silicon_wafer_from_ingot", Map.of("capitalismmod:silicon_ingot", 1), Map.of("capitalismmod:silicon_wafer", 2), 185, "crystal_growth_furnace", 3, 8, 14)),
            new Legacy("manufacturing", new RecipeJson("wafer_dicing", Map.of("capitalismmod:silicon_wafer", 1), Map.of("capitalismmod:silicon_die", 4), 190, "wafer_dicing_saw", 3, 6, 12)),
            new Legacy("semiconductor_fabrication", new RecipeJson("wafer_fabrication", Map.of("capitalismmod:silicon_wafer", 1, "capitalismmod:photoresist", 1), Map.of("capitalismmod:tested_wafer", 1), 330, "semiconductor_fab", 4, 11, 18)),
            new Legacy("manufacturing", new RecipeJson("chip_packaging", Map.of("capitalismmod:silicon_wafer", 1, "capitalismmod:copper_wire", 1, "capitalismmod:plastic_pellets", 1), Map.of("capitalismmod:packaged_chip", 1), 300, "chip_packaging_line", 3, 7, 12)),
            new Legacy("manufacturing", new RecipeJson("circuit_board", Map.of("capitalismmod:silicon_wafer", 1, "capitalismmod:copper_wire", 2, "capitalismmod:plastic_pellets", 1), Map.of("capitalismmod:circuit_board", 1), 260, "electronics_assembly", 3, 5, 10)),
            new Legacy("manufacturing", new RecipeJson("circuit_board_with_packaged_chip", Map.of("capitalismmod:packaged_chip", 1, "capitalismmod:copper_wire", 2, "capitalismmod:plastic_pellets", 1), Map.of("capitalismmod:circuit_board", 1), 280, "electronics_assembly", 3, 5, 10)),
            new Legacy("manufacturing", new RecipeJson("printed_circuit_board", Map.of("capitalismmod:pcb_substrate", 1, "capitalismmod:copper_foil", 1, "capitalismmod:solder", 1, "capitalismmod:smd_components", 1, "capitalismmod:packaged_chip", 1), Map.of("capitalismmod:circuit_board", 1), 340, "pcb_assembly_line", 4, 7, 14)),
            new Legacy("manufacturing", new RecipeJson("assembled_fabricated_pcb", Map.of("capitalismmod:solder_masked_pcb", 1, "capitalismmod:solder", 1, "capitalismmod:smd_components", 1, "capitalismmod:packaged_chip", 1), Map.of("capitalismmod:circuit_board", 1), 390, "pcb_assembly_line", 4, 8, 15)),
            new Legacy("manufacturing", new RecipeJson("pcb_drilling", Map.of("capitalismmod:pcb_substrate", 1, "capitalismmod:copper_foil", 1), Map.of("capitalismmod:drilled_pcb_panel", 1), 130, "pcb_fabrication_line", 3, 5, 10)),
            new Legacy("manufacturing", new RecipeJson("pcb_etching", Map.of("capitalismmod:drilled_pcb_panel", 1, "minecraft:water_bucket", 1), Map.of("capitalismmod:etched_pcb", 1), 155, "pcb_fabrication_line", 3, 6, 11)),
            new Legacy("pcb_manufacturing", new RecipeJson("pcb_etching_realistic", Map.of("capitalismmod:drilled_pcb_panel", 1, "minecraft:water_bucket", 1), Map.of("capitalismmod:etched_pcb", 1), 165, "pcb_fabrication_line", 3, 6, 11)),
            new Legacy("pcb_manufacturing", new RecipeJson("pcb_assembly_realistic", Map.of("capitalismmod:solder_masked_pcb", 1, "capitalismmod:packaged_chip", 1, "capitalismmod:smd_components", 1, "capitalismmod:solder", 1), Map.of("capitalismmod:assembled_pcb", 1), 360, "pcb_assembly_line", 4, 8, 15)),
            new Legacy("manufacturing", new RecipeJson("graphite_anode_processing", Map.of("minecraft:coal", 2, "capitalismmod:lubricant", 1), Map.of("capitalismmod:graphite_anode", 1), 110, "battery_materials", 3, 5, 9)),
            new Legacy("battery_chemistry", new RecipeJson("cathode_precursor", Map.of("capitalismmod:nickel_sulfate", 1, "minecraft:iron_ingot", 1), Map.of("capitalismmod:cathode_precursor", 1), 145, "battery_materials", 3, 6, 10)),
            new Legacy("battery_chemistry", new RecipeJson("lfp_cathode", Map.of("capitalismmod:lithium_carbonate", 1, "capitalismmod:phosphate", 1, "minecraft:iron_ingot", 1), Map.of("capitalismmod:lfp_cathode", 1), 190, "battery_materials", 3, 7, 11)),
            new Legacy("manufacturing", new RecipeJson("cathode_active_material", Map.of("capitalismmod:lithium_carbonate", 1, "minecraft:iron_ingot", 1, "minecraft:redstone", 1), Map.of("capitalismmod:cathode_active_material", 1), 175, "battery_materials", 3, 7, 11)),
            new Legacy("battery_chemistry", new RecipeJson("lfp_cell_assembly", Map.of("capitalismmod:lfp_cathode", 1, "capitalismmod:graphite_anode", 1, "capitalismmod:battery_separator", 1, "capitalismmod:battery_electrolyte", 1), Map.of("capitalismmod:battery_cell", 2), 280, "battery_cell_line", 4, 9, 14)),
            new Legacy("battery_chemistry", new RecipeJson("nmc_cell_assembly", Map.of("capitalismmod:nmc_cathode", 1, "capitalismmod:graphite_anode", 1, "capitalismmod:battery_separator", 1, "capitalismmod:battery_electrolyte", 1), Map.of("capitalismmod:battery_cell", 2), 310, "battery_cell_line", 4, 10, 15)),
            new Legacy("manufacturing", new RecipeJson("lithium_ion_cell", Map.of("capitalismmod:cathode_active_material", 1, "capitalismmod:graphite_anode", 1, "capitalismmod:battery_separator", 1, "capitalismmod:battery_electrolyte", 1, "capitalismmod:copper_wire", 1), Map.of("capitalismmod:battery_cell", 2), 260, "battery_cell_line", 4, 8, 13)),
            new Legacy("manufacturing", new RecipeJson("black_mass_hydrometallurgy", Map.of("capitalismmod:black_mass", 2), Map.of("capitalismmod:lithium_carbonate", 1, "capitalismmod:graphite_anode", 1, "capitalismmod:cathode_active_material", 1), 330, "battery_recycler", 4, 9, 15)),
            new Legacy("electronic_chemicals", new RecipeJson("hydrofluoric_acid_preparation", Map.of("minecraft:quartz", 1, "capitalismmod:sulfuric_acid", 1), Map.of("capitalismmod:hydrofluoric_acid", 1), 155, "electronic_chemical_unit", 3, 7, 11)),
            new Legacy("manufacturing", new RecipeJson("chip_packaging", Map.of("capitalismmod:tested_die", 1, "capitalismmod:lead_frame", 1, "capitalismmod:copper_wire", 1, "capitalismmod:mold_compound", 1), Map.of("capitalismmod:packaged_chip", 1), 360, "chip_packaging_line", 4, 8, 15)),
            new Legacy("manufacturing", new RecipeJson("circuit_board", Map.of("capitalismmod:solder_masked_pcb", 1, "capitalismmod:solder", 1, "capitalismmod:smd_components", 1, "capitalismmod:packaged_chip", 1), Map.of("capitalismmod:circuit_board", 1), 390, "pcb_assembly_line", 4, 8, 15))
    );

    private IndustrialChainRecipeMigration() {}

    static boolean upgrade(String industry, RecipeJson configured, List<RecipeJson> maintained) {
        if (configured == null || maintained == null) return false;
        boolean matches = LEGACY.stream().anyMatch(old -> Objects.equals(industry, old.industry())
                && sameRecipe(configured, old.recipe()));
        if (!matches) return false;
        RecipeJson replacement = maintained.stream().filter(r -> r != null
                && Objects.equals(r.id, configured.id)).findFirst().orElse(null);
        if (replacement == null || sameRecipe(configured, replacement)) return false;
        configured.inputs = new HashMap<>(replacement.inputs);
        configured.outputs = new HashMap<>(replacement.outputs);
        configured.income = replacement.income;
        configured.machine_type = replacement.machine_type;
        configured.workers_per_cycle = replacement.workers_per_cycle;
        configured.energy_cost = replacement.energy_cost;
        configured.maintenance_cost = replacement.maintenance_cost;
        configured.material_class = replacement.material_class;
        configured.quality_tier = replacement.quality_tier;
        configured.byproducts = new HashMap<>(replacement.byproducts);
        configured.pollution_score = replacement.pollution_score;
        configured.hazardous = replacement.hazardous;
        return true;
    }

    private static boolean sameRecipe(RecipeJson a, RecipeJson b) {
        return Objects.equals(a.id, b.id) && Objects.equals(a.inputs, b.inputs)
                && Objects.equals(a.outputs, b.outputs) && a.income == b.income
                && Objects.equals(a.machine_type, b.machine_type)
                && a.workers_per_cycle == b.workers_per_cycle
                && a.energy_cost == b.energy_cost && a.maintenance_cost == b.maintenance_cost
                && Objects.equals(a.material_class, b.material_class)
                && Objects.equals(a.quality_tier, b.quality_tier)
                && Objects.equals(a.byproducts, b.byproducts)
                && a.pollution_score == b.pollution_score && a.hazardous == b.hazardous;
    }
}
