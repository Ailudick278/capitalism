- Material class distribution: {"industrial_intermediate":232,"polymer_material":28,"chemical_intermediate":19,"consumer_material":13,"agrochemical":6,"pharmaceutical":8}
- Quality tier distribution: {"industrial":298,"regulated":5,"high_purity":3}
- Hazardous process recipes: 59; pollution score total: 932
# 工业产业链数据审计（默认基线）

审计来源：`src/main/java/com/ailudick/capitalismmod/data/CapitalismData.java`
说明：这是对 `CapitalismData.defaultIndustries()` 中 Java 默认配方的静态审计；运行时 `config/capitalismmod/industries.json` 可能覆盖或补充结果。

## 总览

- 配方数：306
- 产出物料数：262
- 输入→输出关系数：581
- 未发现配方生产者的输入：110 条（其中 capitalismmod: 内部物料 19 条）
- 扣除方块掉落等资源来源后仍未解释的内部物料：1 条
- 自循环配方：1 条
- 检测到的循环路径：36 条
- 未识别机器类型：0 条
- 设备材料中未找到配方或资源来源的物料：0 种
- 有产出但能源成本为 0 的机器配方：0 条
- 重复配方 ID：0 个

## 重点问题

### 无配方生产者输入（前 80 条）

这些输入可能来自原版资源、矿石掉落或其他数据包，因此不自动判定为断链。

- copper_ingot 需要 minecraft:copper_ore，但默认配方没有产出它。
- gold_ingot 需要 minecraft:gold_ore，但默认配方没有产出它。
- copper_concentrate 需要 minecraft:copper_ore，但默认配方没有产出它。
- gold_concentrate 需要 minecraft:gold_ore，但默认配方没有产出它。
- iron_smelting 需要 minecraft:coal，但默认配方没有产出它。
- copper_smelting 需要 minecraft:coal，但默认配方没有产出它。
- gold_smelting 需要 minecraft:coal，但默认配方没有产出它。
- bauxite_refining 需要 capitalismmod:raw_bauxite，但默认配方没有产出它。
- bauxite_refining 需要 minecraft:water_bucket，但默认配方没有产出它。
- nickel_smelting 需要 capitalismmod:raw_nickel，但默认配方没有产出它。
- nickel_smelting 需要 minecraft:coal，但默认配方没有产出它。
- rail 需要 minecraft:coal，但默认配方没有产出它。
- steel_sheet 需要 minecraft:coal，但默认配方没有产出它。
- glass 需要 minecraft:coal，但默认配方没有产出它。
- flour 需要 minecraft:wheat，但默认配方没有产出它。
- bread 需要 minecraft:sugar，但默认配方没有产出它。
- canned_food 需要 minecraft:wheat，但默认配方没有产出它。
- electric_motor 需要 minecraft:redstone，但默认配方没有产出它。
- green_lumber 需要 minecraft:oak_log，但默认配方没有产出它。
- dried_lumber 需要 minecraft:coal，但默认配方没有产出它。
- steam_cracking 需要 minecraft:water_bucket，但默认配方没有产出它。
- ethylene_glycol 需要 minecraft:water_bucket，但默认配方没有产出它。
- fan_control 需要 minecraft:redstone，但默认配方没有产出它。
- polysilicon 需要 minecraft:quartz，但默认配方没有产出它。
- polysilicon 需要 minecraft:coal，但默认配方没有产出它。
- solder 需要 capitalismmod:tin_ingot，但默认配方没有产出它。
- smd_components 需要 minecraft:redstone，但默认配方没有产出它。
- passive_components 需要 minecraft:redstone，但默认配方没有产出它。
- microphone_module 需要 minecraft:redstone，但默认配方没有产出它。
- battery_cell 需要 minecraft:redstone，但默认配方没有产出它。
- lithium_mineral_concentration 需要 minecraft:quartz，但默认配方没有产出它。
- lithium_carbonate_refining 需要 minecraft:coal，但默认配方没有产出它。
- syngas_shift_separation 需要 minecraft:water_bucket，但默认配方没有产出它。
- coal_gasification 需要 minecraft:coal，但默认配方没有产出它。
- coal_gasification 需要 minecraft:water_bucket，但默认配方没有产出它。
- water_electrolysis 需要 minecraft:water_bucket，但默认配方没有产出它。
- epichlorohydrin_production 需要 minecraft:water_bucket，但默认配方没有产出它。
- gas_reforming 需要 minecraft:water_bucket，但默认配方没有产出它。
- sulfuric_acid_production 需要 minecraft:water_bucket，但默认配方没有产出它。
- steam_cracking_fractionated 需要 minecraft:water_bucket，但默认配方没有产出它。
- salt_extraction 需要 minecraft:water_bucket，但默认配方没有产出它。
- ethylene_dichloride_cracking 需要 minecraft:water_bucket，但默认配方没有产出它。
- vinyl_chloride_synthesis 需要 minecraft:water_bucket，但默认配方没有产出它。
- copper_cathode 需要 minecraft:coal，但默认配方没有产出它。
- alumina_refining 需要 capitalismmod:raw_bauxite，但默认配方没有产出它。
- alumina_refining 需要 minecraft:water_bucket，但默认配方没有产出它。
- cobalt_concentration 需要 capitalismmod:cobalt_ore，但默认配方没有产出它。
- cobalt_smelting 需要 minecraft:coal，但默认配方没有产出它。
- manganese_concentration 需要 capitalismmod:manganese_ore，但默认配方没有产出它。
- manganese_smelting 需要 minecraft:coal，但默认配方没有产出它。
- chromium_concentration 需要 capitalismmod:chromium_ore，但默认配方没有产出它。
- chromium_smelting 需要 minecraft:coal，但默认配方没有产出它。
- titanium_concentration 需要 capitalismmod:titanium_ore，但默认配方没有产出它。
- tungsten_concentration 需要 capitalismmod:tungsten_ore，但默认配方没有产出它。
- tungsten_smelting 需要 minecraft:coal，但默认配方没有产出它。
- molybdenum_concentration 需要 capitalismmod:molybdenum_ore，但默认配方没有产出它。
- molybdenum_smelting 需要 minecraft:coal，但默认配方没有产出它。
- fluorite_concentration 需要 capitalismmod:fluorite_ore，但默认配方没有产出它。
- rare_earth_concentration 需要 capitalismmod:rare_earth_ore，但默认配方没有产出它。
- uranium_ore_milling 需要 capitalismmod:uranium_ore，但默认配方没有产出它。
- radiation_shielding_fabrication 需要 capitalismmod:lead_ingot，但默认配方没有产出它。
- crushed_stone 需要 minecraft:stone，但默认配方没有产出它。
- ore_washing 需要 minecraft:water_bucket，但默认配方没有产出它。
- electric_arc_steel 需要 minecraft:coal，但默认配方没有产出它。
- cement_clinker 需要 minecraft:stone，但默认配方没有产出它。
- cement_clinker 需要 minecraft:coal，但默认配方没有产出它。
- glass_sand 需要 minecraft:coal，但默认配方没有产出它。
- spun_fiber 需要 minecraft:string，但默认配方没有产出它。
- leather_fabric 需要 minecraft:leather，但默认配方没有产出它。
- leather_fabric 需要 minecraft:string，但默认配方没有产出它。
- wood_pulp 需要 minecraft:oak_log，但默认配方没有产出它。
- wood_pulp 需要 minecraft:water_bucket，但默认配方没有产出它。
- bookbinding 需要 minecraft:leather，但默认配方没有产出它。
- flour_milling 需要 minecraft:wheat，但默认配方没有产出它。
- bread_baking 需要 minecraft:sugar，但默认配方没有产出它。
- pasteurized_milk 需要 minecraft:milk_bucket，但默认配方没有产出它。
- canned_rations 需要 minecraft:cooked_beef，但默认配方没有产出它。
- brine_preparation 需要 minecraft:water_bucket，但默认配方没有产出它。
- hydrochloric_acid_synthesis 需要 minecraft:water_bucket，但默认配方没有产出它。
- nitric_acid_synthesis 需要 minecraft:water_bucket，但默认配方没有产出它。

### 循环路径（前 30 条）

- capitalismmod:packaged_chip → capitalismmod:power_management_ic → capitalismmod:smartphone → capitalismmod:copper_wire → capitalismmod:packaged_chip
- capitalismmod:smartphone → capitalismmod:copper_wire → capitalismmod:smd_components → capitalismmod:assembled_pcb → capitalismmod:circuit_board → capitalismmod:camera_module → capitalismmod:smartphone
- capitalismmod:copper_wire → capitalismmod:smd_components → capitalismmod:assembled_pcb → capitalismmod:circuit_board → capitalismmod:storage_module → capitalismmod:laptop → capitalismmod:copper_wire
- capitalismmod:packaged_chip → capitalismmod:power_management_ic → capitalismmod:smartphone → capitalismmod:copper_wire → capitalismmod:smd_components → capitalismmod:assembled_pcb → capitalismmod:circuit_board → capitalismmod:storage_module → capitalismmod:laptop → capitalismmod:plastic_pellets → capitalismmod:mold_compound → capitalismmod:packaged_chip
- capitalismmod:smd_components → capitalismmod:assembled_pcb → capitalismmod:circuit_board → capitalismmod:storage_module → capitalismmod:laptop → capitalismmod:plastic_pellets → capitalismmod:smd_components
- capitalismmod:smartphone → capitalismmod:copper_wire → capitalismmod:smd_components → capitalismmod:assembled_pcb → capitalismmod:circuit_board → capitalismmod:storage_module → capitalismmod:laptop → capitalismmod:plastic_pellets → capitalismmod:backlight_module → capitalismmod:display_panel → capitalismmod:smartphone
- capitalismmod:copper_wire → capitalismmod:smd_components → capitalismmod:assembled_pcb → capitalismmod:circuit_board → capitalismmod:storage_module → capitalismmod:laptop → capitalismmod:plastic_pellets → capitalismmod:backlight_module → capitalismmod:display_panel → capitalismmod:television → capitalismmod:copper_wire
- capitalismmod:plastic_pellets → capitalismmod:backlight_module → capitalismmod:display_panel → capitalismmod:television → capitalismmod:plastic_pellets
- capitalismmod:copper_wire → capitalismmod:smd_components → capitalismmod:assembled_pcb → capitalismmod:circuit_board → capitalismmod:storage_module → capitalismmod:laptop → capitalismmod:plastic_pellets → capitalismmod:backlight_module → capitalismmod:display_panel → capitalismmod:television → capitalismmod:refurbished_television → capitalismmod:copper_wire
- capitalismmod:plastic_pellets → capitalismmod:backlight_module → capitalismmod:display_panel → capitalismmod:television → capitalismmod:refurbished_television → capitalismmod:plastic_pellets
- capitalismmod:laptop → capitalismmod:plastic_pellets → capitalismmod:backlight_module → capitalismmod:display_panel → capitalismmod:laptop
- capitalismmod:copper_wire → capitalismmod:smd_components → capitalismmod:assembled_pcb → capitalismmod:circuit_board → capitalismmod:storage_module → capitalismmod:laptop → capitalismmod:plastic_pellets → capitalismmod:backlight_module → capitalismmod:display_panel → capitalismmod:refurbished_smartphone → capitalismmod:copper_wire
- capitalismmod:plastic_pellets → capitalismmod:backlight_module → capitalismmod:display_panel → capitalismmod:refurbished_smartphone → capitalismmod:plastic_pellets
- capitalismmod:copper_wire → capitalismmod:smd_components → capitalismmod:assembled_pcb → capitalismmod:circuit_board → capitalismmod:storage_module → capitalismmod:laptop → capitalismmod:plastic_pellets → capitalismmod:backlight_module → capitalismmod:display_panel → capitalismmod:refurbished_laptop → capitalismmod:copper_wire
- capitalismmod:plastic_pellets → capitalismmod:backlight_module → capitalismmod:display_panel → capitalismmod:refurbished_laptop → capitalismmod:plastic_pellets
- capitalismmod:smartphone → capitalismmod:copper_wire → capitalismmod:smd_components → capitalismmod:assembled_pcb → capitalismmod:circuit_board → capitalismmod:storage_module → capitalismmod:laptop → capitalismmod:plastic_pellets → capitalismmod:speaker_module → capitalismmod:smartphone
- capitalismmod:smartphone → capitalismmod:copper_wire → capitalismmod:smd_components → capitalismmod:assembled_pcb → capitalismmod:circuit_board → capitalismmod:storage_module → capitalismmod:laptop → capitalismmod:plastic_pellets → capitalismmod:microphone_module → capitalismmod:smartphone
- capitalismmod:smartphone → capitalismmod:copper_wire → capitalismmod:smd_components → capitalismmod:assembled_pcb → capitalismmod:circuit_board → capitalismmod:storage_module → capitalismmod:laptop → capitalismmod:plastic_pellets → capitalismmod:charging_port → capitalismmod:smartphone
- capitalismmod:battery → capitalismmod:battery_pack → capitalismmod:black_mass → capitalismmod:lithium_carbonate → capitalismmod:battery_electrolyte → capitalismmod:battery_cell → capitalismmod:battery
- capitalismmod:smartphone → capitalismmod:copper_wire → capitalismmod:smd_components → capitalismmod:assembled_pcb → capitalismmod:circuit_board → capitalismmod:storage_module → capitalismmod:laptop → capitalismmod:plastic_pellets → capitalismmod:battery → capitalismmod:smartphone
- capitalismmod:laptop → capitalismmod:plastic_pellets → capitalismmod:battery → capitalismmod:laptop
- capitalismmod:copper_wire → capitalismmod:smd_components → capitalismmod:assembled_pcb → capitalismmod:circuit_board → capitalismmod:storage_module → capitalismmod:laptop → capitalismmod:plastic_pellets → capitalismmod:power_adapter → capitalismmod:wireless_router → capitalismmod:copper_wire
- capitalismmod:plastic_pellets → capitalismmod:power_adapter → capitalismmod:wireless_router → capitalismmod:plastic_pellets
- capitalismmod:copper_wire → capitalismmod:smd_components → capitalismmod:assembled_pcb → capitalismmod:circuit_board → capitalismmod:storage_module → capitalismmod:laptop → capitalismmod:plastic_pellets → capitalismmod:power_adapter → capitalismmod:wireless_router → capitalismmod:refurbished_wireless_router → capitalismmod:copper_wire
- capitalismmod:plastic_pellets → capitalismmod:power_adapter → capitalismmod:wireless_router → capitalismmod:refurbished_wireless_router → capitalismmod:plastic_pellets
- capitalismmod:packaged_chip → capitalismmod:power_management_ic → capitalismmod:smartphone → capitalismmod:copper_wire → capitalismmod:smd_components → capitalismmod:assembled_pcb → capitalismmod:circuit_board → capitalismmod:storage_module → capitalismmod:laptop → capitalismmod:plastic_pellets → capitalismmod:photoresist → capitalismmod:tested_wafer → capitalismmod:silicon_die → capitalismmod:tested_die → capitalismmod:packaged_chip
- capitalismmod:smartphone → capitalismmod:copper_wire → capitalismmod:smd_components → capitalismmod:assembled_pcb → capitalismmod:circuit_board → capitalismmod:smartphone
- capitalismmod:power_management_ic → capitalismmod:smartphone → capitalismmod:copper_wire → capitalismmod:passive_components → capitalismmod:power_management_ic
- capitalismmod:smartphone → capitalismmod:copper_wire → capitalismmod:passive_components → capitalismmod:display_driver → capitalismmod:smartphone
- capitalismmod:power_management_ic → capitalismmod:smartphone → capitalismmod:copper_wire → capitalismmod:power_management_ic

### 自循环

- 配方 soil_conditioner 同时消耗并产出 minecraft:bone_meal

## 可视化

下面的 Mermaid 图展示默认配方形成的物料流关系；同一物料的多个生产者会汇入同一个节点。

```mermaid
flowchart LR
  minecraft_iron_ore["minecraft:iron_ore"] --> minecraft_iron_ingot["minecraft:iron_ingot"]
  minecraft_copper_ore["minecraft:copper_ore"] --> minecraft_copper_ingot["minecraft:copper_ingot"]
  minecraft_gold_ore["minecraft:gold_ore"] --> minecraft_gold_ingot["minecraft:gold_ingot"]
  minecraft_iron_ore["minecraft:iron_ore"] --> minecraft_raw_iron["minecraft:raw_iron"]
  minecraft_copper_ore["minecraft:copper_ore"] --> minecraft_raw_copper["minecraft:raw_copper"]
  minecraft_gold_ore["minecraft:gold_ore"] --> minecraft_raw_gold["minecraft:raw_gold"]
  minecraft_raw_iron["minecraft:raw_iron"] --> minecraft_iron_ingot["minecraft:iron_ingot"]
  minecraft_coal["minecraft:coal"] --> minecraft_iron_ingot["minecraft:iron_ingot"]
  minecraft_raw_copper["minecraft:raw_copper"] --> minecraft_copper_ingot["minecraft:copper_ingot"]
  minecraft_coal["minecraft:coal"] --> minecraft_copper_ingot["minecraft:copper_ingot"]
  minecraft_raw_gold["minecraft:raw_gold"] --> minecraft_gold_ingot["minecraft:gold_ingot"]
  minecraft_coal["minecraft:coal"] --> minecraft_gold_ingot["minecraft:gold_ingot"]
  capitalismmod_raw_bauxite["capitalismmod:raw_bauxite"] --> capitalismmod_alumina["capitalismmod:alumina"]
  capitalismmod_caustic_soda["capitalismmod:caustic_soda"] --> capitalismmod_alumina["capitalismmod:alumina"]
  minecraft_water_bucket["minecraft:water_bucket"] --> capitalismmod_alumina["capitalismmod:alumina"]
  capitalismmod_raw_nickel["capitalismmod:raw_nickel"] --> capitalismmod_nickel_ingot["capitalismmod:nickel_ingot"]
  minecraft_coal["minecraft:coal"] --> capitalismmod_nickel_ingot["capitalismmod:nickel_ingot"]
  minecraft_iron_ingot["minecraft:iron_ingot"] --> minecraft_rail["minecraft:rail"]
  minecraft_coal["minecraft:coal"] --> minecraft_rail["minecraft:rail"]
  minecraft_iron_ingot["minecraft:iron_ingot"] --> capitalismmod_steel_sheet["capitalismmod:steel_sheet"]
  minecraft_coal["minecraft:coal"] --> capitalismmod_steel_sheet["capitalismmod:steel_sheet"]
  minecraft_copper_ingot["minecraft:copper_ingot"] --> capitalismmod_copper_wire["capitalismmod:copper_wire"]
  minecraft_sand["minecraft:sand"] --> minecraft_glass["minecraft:glass"]
  minecraft_coal["minecraft:coal"] --> minecraft_glass["minecraft:glass"]
  minecraft_glass["minecraft:glass"] --> capitalismmod_glass_lens["capitalismmod:glass_lens"]
  capitalismmod_steel_sheet["capitalismmod:steel_sheet"] --> capitalismmod_electric_lamp["capitalismmod:electric_lamp"]
  capitalismmod_copper_wire["capitalismmod:copper_wire"] --> capitalismmod_electric_lamp["capitalismmod:electric_lamp"]
  capitalismmod_glass_lens["capitalismmod:glass_lens"] --> capitalismmod_electric_lamp["capitalismmod:electric_lamp"]
  minecraft_wheat["minecraft:wheat"] --> capitalismmod_flour["capitalismmod:flour"]
  capitalismmod_flour["capitalismmod:flour"] --> minecraft_bread["minecraft:bread"]
  minecraft_sugar["minecraft:sugar"] --> minecraft_bread["minecraft:bread"]
  minecraft_iron_ingot["minecraft:iron_ingot"] --> capitalismmod_metal_can["capitalismmod:metal_can"]
  minecraft_wheat["minecraft:wheat"] --> capitalismmod_canned_food["capitalismmod:canned_food"]
  capitalismmod_metal_can["capitalismmod:metal_can"] --> capitalismmod_canned_food["capitalismmod:canned_food"]
  capitalismmod_steel_sheet["capitalismmod:steel_sheet"] --> capitalismmod_machine_frame["capitalismmod:machine_frame"]
  minecraft_iron_ingot["minecraft:iron_ingot"] --> capitalismmod_machine_frame["capitalismmod:machine_frame"]
  capitalismmod_copper_wire["capitalismmod:copper_wire"] --> capitalismmod_electric_motor["capitalismmod:electric_motor"]
  minecraft_iron_ingot["minecraft:iron_ingot"] --> capitalismmod_electric_motor["capitalismmod:electric_motor"]
  minecraft_redstone["minecraft:redstone"] --> capitalismmod_electric_motor["capitalismmod:electric_motor"]
  minecraft_oak_log["minecraft:oak_log"] --> capitalismmod_green_lumber["capitalismmod:green_lumber"]
  capitalismmod_green_lumber["capitalismmod:green_lumber"] --> capitalismmod_dried_lumber["capitalismmod:dried_lumber"]
  minecraft_coal["minecraft:coal"] --> capitalismmod_dried_lumber["capitalismmod:dried_lumber"]
  capitalismmod_dried_lumber["capitalismmod:dried_lumber"] --> capitalismmod_wooden_crate["capitalismmod:wooden_crate"]
  minecraft_iron_ingot["minecraft:iron_ingot"] --> capitalismmod_wooden_crate["capitalismmod:wooden_crate"]
  capitalismmod_dried_lumber["capitalismmod:dried_lumber"] --> capitalismmod_furniture["capitalismmod:furniture"]
  minecraft_iron_ingot["minecraft:iron_ingot"] --> capitalismmod_furniture["capitalismmod:furniture"]
  minecraft_glass["minecraft:glass"] --> capitalismmod_furniture["capitalismmod:furniture"]
  capitalismmod_crude_oil["capitalismmod:crude_oil"] --> capitalismmod_refinery_gas["capitalismmod:refinery_gas"]
  capitalismmod_crude_oil["capitalismmod:crude_oil"] --> capitalismmod_naphtha["capitalismmod:naphtha"]
  capitalismmod_crude_oil["capitalismmod:crude_oil"] --> capitalismmod_kerosene["capitalismmod:kerosene"]
  capitalismmod_crude_oil["capitalismmod:crude_oil"] --> capitalismmod_gas_oil["capitalismmod:gas_oil"]
  capitalismmod_crude_oil["capitalismmod:crude_oil"] --> capitalismmod_fuel_oil["capitalismmod:fuel_oil"]
  capitalismmod_naphtha["capitalismmod:naphtha"] --> capitalismmod_ethylene["capitalismmod:ethylene"]
  capitalismmod_naphtha["capitalismmod:naphtha"] --> capitalismmod_propylene["capitalismmod:propylene"]
  capitalismmod_naphtha["capitalismmod:naphtha"] --> capitalismmod_c4_fraction["capitalismmod:c4_fraction"]
  capitalismmod_naphtha["capitalismmod:naphtha"] --> capitalismmod_refinery_gas["capitalismmod:refinery_gas"]
  minecraft_water_bucket["minecraft:water_bucket"] --> capitalismmod_ethylene["capitalismmod:ethylene"]
  minecraft_water_bucket["minecraft:water_bucket"] --> capitalismmod_propylene["capitalismmod:propylene"]
  minecraft_water_bucket["minecraft:water_bucket"] --> capitalismmod_c4_fraction["capitalismmod:c4_fraction"]
  minecraft_water_bucket["minecraft:water_bucket"] --> capitalismmod_refinery_gas["capitalismmod:refinery_gas"]
  capitalismmod_ethylene["capitalismmod:ethylene"] --> capitalismmod_polyethylene_pellets["capitalismmod:polyethylene_pellets"]
  capitalismmod_propylene["capitalismmod:propylene"] --> capitalismmod_polypropylene_pellets["capitalismmod:polypropylene_pellets"]
  capitalismmod_butadiene["capitalismmod:butadiene"] --> capitalismmod_synthetic_rubber["capitalismmod:synthetic_rubber"]
  capitalismmod_styrene_monomer["capitalismmod:styrene_monomer"] --> capitalismmod_synthetic_rubber["capitalismmod:synthetic_rubber"]
  capitalismmod_ethylene_oxide["capitalismmod:ethylene_oxide"] --> capitalismmod_ethylene_glycol["capitalismmod:ethylene_glycol"]
  minecraft_water_bucket["minecraft:water_bucket"] --> capitalismmod_ethylene_glycol["capitalismmod:ethylene_glycol"]
  capitalismmod_bisphenol_a["capitalismmod:bisphenol_a"] --> capitalismmod_epoxy_resin["capitalismmod:epoxy_resin"]
  capitalismmod_epichlorohydrin["capitalismmod:epichlorohydrin"] --> capitalismmod_epoxy_resin["capitalismmod:epoxy_resin"]
  capitalismmod_caustic_soda["capitalismmod:caustic_soda"] --> capitalismmod_epoxy_resin["capitalismmod:epoxy_resin"]
  capitalismmod_reformate["capitalismmod:reformate"] --> capitalismmod_benzene["capitalismmod:benzene"]
  capitalismmod_reformate["capitalismmod:reformate"] --> capitalismmod_p_xylene["capitalismmod:p_xylene"]
  capitalismmod_ethylbenzene["capitalismmod:ethylbenzene"] --> capitalismmod_styrene_monomer["capitalismmod:styrene_monomer"]
  capitalismmod_ethylbenzene["capitalismmod:ethylbenzene"] --> capitalismmod_hydrogen["capitalismmod:hydrogen"]
  capitalismmod_acrylonitrile["capitalismmod:acrylonitrile"] --> capitalismmod_abs_resin["capitalismmod:abs_resin"]
  capitalismmod_butadiene["capitalismmod:butadiene"] --> capitalismmod_abs_resin["capitalismmod:abs_resin"]
  capitalismmod_styrene_monomer["capitalismmod:styrene_monomer"] --> capitalismmod_abs_resin["capitalismmod:abs_resin"]
  capitalismmod_p_xylene["capitalismmod:p_xylene"] --> capitalismmod_terephthalic_acid["capitalismmod:terephthalic_acid"]
  capitalismmod_oxygen["capitalismmod:oxygen"] --> capitalismmod_terephthalic_acid["capitalismmod:terephthalic_acid"]
  capitalismmod_terephthalic_acid["capitalismmod:terephthalic_acid"] --> capitalismmod_pet_resin["capitalismmod:pet_resin"]
  capitalismmod_ethylene_glycol["capitalismmod:ethylene_glycol"] --> capitalismmod_pet_resin["capitalismmod:pet_resin"]
  capitalismmod_base_oil["capitalismmod:base_oil"] --> capitalismmod_lubricant["capitalismmod:lubricant"]
  capitalismmod_hydrogen["capitalismmod:hydrogen"] --> capitalismmod_lubricant["capitalismmod:lubricant"]
  capitalismmod_polyethylene_pellets["capitalismmod:polyethylene_pellets"] --> capitalismmod_plastic_pellets["capitalismmod:plastic_pellets"]
  capitalismmod_plastic_pellets["capitalismmod:plastic_pellets"] --> capitalismmod_fan_blades["capitalismmod:fan_blades"]
  capitalismmod_steel_sheet["capitalismmod:steel_sheet"] --> capitalismmod_fan_blades["capitalismmod:fan_blades"]
  capitalismmod_copper_wire["capitalismmod:copper_wire"] --> capitalismmod_fan_control["capitalismmod:fan_control"]
  minecraft_redstone["minecraft:redstone"] --> capitalismmod_fan_control["capitalismmod:fan_control"]
  minecraft_iron_ingot["minecraft:iron_ingot"] --> capitalismmod_fan_control["capitalismmod:fan_control"]
  minecraft_quartz["minecraft:quartz"] --> capitalismmod_polysilicon["capitalismmod:polysilicon"]
  minecraft_coal["minecraft:coal"] --> capitalismmod_polysilicon["capitalismmod:polysilicon"]
  capitalismmod_silicon_ingot["capitalismmod:silicon_ingot"] --> capitalismmod_silicon_wafer["capitalismmod:silicon_wafer"]
  capitalismmod_tested_wafer["capitalismmod:tested_wafer"] --> capitalismmod_silicon_die["capitalismmod:silicon_die"]
  capitalismmod_silicon_die["capitalismmod:silicon_die"] --> capitalismmod_tested_die["capitalismmod:tested_die"]
  capitalismmod_copper_foil["capitalismmod:copper_foil"] --> capitalismmod_lead_frame["capitalismmod:lead_frame"]
  capitalismmod_steel_sheet["capitalismmod:steel_sheet"] --> capitalismmod_lead_frame["capitalismmod:lead_frame"]
  capitalismmod_epoxy_resin["capitalismmod:epoxy_resin"] --> capitalismmod_mold_compound["capitalismmod:mold_compound"]
  capitalismmod_plastic_pellets["capitalismmod:plastic_pellets"] --> capitalismmod_mold_compound["capitalismmod:mold_compound"]
  capitalismmod_tested_die["capitalismmod:tested_die"] --> capitalismmod_packaged_chip["capitalismmod:packaged_chip"]
  capitalismmod_lead_frame["capitalismmod:lead_frame"] --> capitalismmod_packaged_chip["capitalismmod:packaged_chip"]
  capitalismmod_copper_wire["capitalismmod:copper_wire"] --> capitalismmod_packaged_chip["capitalismmod:packaged_chip"]
  capitalismmod_mold_compound["capitalismmod:mold_compound"] --> capitalismmod_packaged_chip["capitalismmod:packaged_chip"]
  capitalismmod_assembled_pcb["capitalismmod:assembled_pcb"] --> capitalismmod_circuit_board["capitalismmod:circuit_board"]
  minecraft_glass["minecraft:glass"] --> capitalismmod_pcb_substrate["capitalismmod:pcb_substrate"]
  capitalismmod_epoxy_resin["capitalismmod:epoxy_resin"] --> capitalismmod_pcb_substrate["capitalismmod:pcb_substrate"]
  capitalismmod_copper_cathode["capitalismmod:copper_cathode"] --> capitalismmod_copper_foil["capitalismmod:copper_foil"]
  capitalismmod_tin_ingot["capitalismmod:tin_ingot"] --> capitalismmod_solder["capitalismmod:solder"]
  minecraft_copper_ingot["minecraft:copper_ingot"] --> capitalismmod_solder["capitalismmod:solder"]
  capitalismmod_copper_clad_laminate["capitalismmod:copper_clad_laminate"] --> capitalismmod_drilled_pcb_panel["capitalismmod:drilled_pcb_panel"]
  capitalismmod_drilled_pcb_panel["capitalismmod:drilled_pcb_panel"] --> capitalismmod_etched_pcb["capitalismmod:etched_pcb"]
  capitalismmod_electronic_etchant["capitalismmod:electronic_etchant"] --> capitalismmod_etched_pcb["capitalismmod:etched_pcb"]
  capitalismmod_etched_pcb["capitalismmod:etched_pcb"] --> capitalismmod_solder_masked_pcb["capitalismmod:solder_masked_pcb"]
  capitalismmod_epoxy_resin["capitalismmod:epoxy_resin"] --> capitalismmod_solder_masked_pcb["capitalismmod:solder_masked_pcb"]
  minecraft_redstone["minecraft:redstone"] --> capitalismmod_smd_components["capitalismmod:smd_components"]
  capitalismmod_copper_wire["capitalismmod:copper_wire"] --> capitalismmod_smd_components["capitalismmod:smd_components"]
  capitalismmod_plastic_pellets["capitalismmod:plastic_pellets"] --> capitalismmod_smd_components["capitalismmod:smd_components"]
  minecraft_redstone["minecraft:redstone"] --> capitalismmod_passive_components["capitalismmod:passive_components"]
  minecraft_iron_ingot["minecraft:iron_ingot"] --> capitalismmod_passive_components["capitalismmod:passive_components"]
  capitalismmod_copper_wire["capitalismmod:copper_wire"] --> capitalismmod_passive_components["capitalismmod:passive_components"]
  capitalismmod_packaged_chip["capitalismmod:packaged_chip"] --> capitalismmod_power_management_ic["capitalismmod:power_management_ic"]
  capitalismmod_passive_components["capitalismmod:passive_components"] --> capitalismmod_power_management_ic["capitalismmod:power_management_ic"]
  capitalismmod_copper_wire["capitalismmod:copper_wire"] --> capitalismmod_power_management_ic["capitalismmod:power_management_ic"]
  capitalismmod_packaged_chip["capitalismmod:packaged_chip"] --> capitalismmod_display_driver["capitalismmod:display_driver"]
  capitalismmod_passive_components["capitalismmod:passive_components"] --> capitalismmod_display_driver["capitalismmod:display_driver"]
  capitalismmod_copper_wire["capitalismmod:copper_wire"] --> capitalismmod_display_driver["capitalismmod:display_driver"]
  minecraft_glass["minecraft:glass"] --> capitalismmod_display_glass_substrate["capitalismmod:display_glass_substrate"]
  capitalismmod_packaged_chip["capitalismmod:packaged_chip"] --> capitalismmod_backlight_module["capitalismmod:backlight_module"]
  capitalismmod_copper_wire["capitalismmod:copper_wire"] --> capitalismmod_backlight_module["capitalismmod:backlight_module"]
  capitalismmod_plastic_pellets["capitalismmod:plastic_pellets"] --> capitalismmod_backlight_module["capitalismmod:backlight_module"]
  capitalismmod_display_glass_substrate["capitalismmod:display_glass_substrate"] --> capitalismmod_display_panel["capitalismmod:display_panel"]
  capitalismmod_backlight_module["capitalismmod:backlight_module"] --> capitalismmod_display_panel["capitalismmod:display_panel"]
  capitalismmod_display_driver["capitalismmod:display_driver"] --> capitalismmod_display_panel["capitalismmod:display_panel"]
  capitalismmod_plastic_pellets["capitalismmod:plastic_pellets"] --> capitalismmod_display_panel["capitalismmod:display_panel"]
  capitalismmod_abs_resin["capitalismmod:abs_resin"] --> capitalismmod_phone_casing["capitalismmod:phone_casing"]
  capitalismmod_polyethylene_pellets["capitalismmod:polyethylene_pellets"] --> capitalismmod_plastic_container["capitalismmod:plastic_container"]
  capitalismmod_polyethylene_pellets["capitalismmod:polyethylene_pellets"] --> capitalismmod_packaging_film["capitalismmod:packaging_film"]
  capitalismmod_pet_resin["capitalismmod:pet_resin"] --> capitalismmod_pet_bottle["capitalismmod:pet_bottle"]
  capitalismmod_pet_bottle["capitalismmod:pet_bottle"] --> capitalismmod_recycled_pet_flakes["capitalismmod:recycled_pet_flakes"]
  capitalismmod_recycled_pet_flakes["capitalismmod:recycled_pet_flakes"] --> capitalismmod_pet_resin["capitalismmod:pet_resin"]
  capitalismmod_packaging_film["capitalismmod:packaging_film"] --> capitalismmod_reclaimed_plastic["capitalismmod:reclaimed_plastic"]
  capitalismmod_plastic_container["capitalismmod:plastic_container"] --> capitalismmod_reclaimed_plastic["capitalismmod:reclaimed_plastic"]
  capitalismmod_glass_lens["capitalismmod:glass_lens"] --> capitalismmod_camera_module["capitalismmod:camera_module"]
  capitalismmod_circuit_board["capitalismmod:circuit_board"] --> capitalismmod_camera_module["capitalismmod:camera_module"]
  capitalismmod_plastic_pellets["capitalismmod:plastic_pellets"] --> capitalismmod_camera_module["capitalismmod:camera_module"]
  capitalismmod_copper_wire["capitalismmod:copper_wire"] --> capitalismmod_speaker_module["capitalismmod:speaker_module"]
  minecraft_iron_ingot["minecraft:iron_ingot"] --> capitalismmod_speaker_module["capitalismmod:speaker_module"]
  capitalismmod_plastic_pellets["capitalismmod:plastic_pellets"] --> capitalismmod_speaker_module["capitalismmod:speaker_module"]
  capitalismmod_copper_wire["capitalismmod:copper_wire"] --> capitalismmod_microphone_module["capitalismmod:microphone_module"]
  minecraft_redstone["minecraft:redstone"] --> capitalismmod_microphone_module["capitalismmod:microphone_module"]
  capitalismmod_plastic_pellets["capitalismmod:plastic_pellets"] --> capitalismmod_microphone_module["capitalismmod:microphone_module"]
  capitalismmod_copper_wire["capitalismmod:copper_wire"] --> capitalismmod_charging_port["capitalismmod:charging_port"]
  minecraft_iron_ingot["minecraft:iron_ingot"] --> capitalismmod_charging_port["capitalismmod:charging_port"]
  capitalismmod_plastic_pellets["capitalismmod:plastic_pellets"] --> capitalismmod_charging_port["capitalismmod:charging_port"]
  capitalismmod_abs_resin["capitalismmod:abs_resin"] --> capitalismmod_keyboard_module["capitalismmod:keyboard_module"]
  capitalismmod_passive_components["capitalismmod:passive_components"] --> capitalismmod_keyboard_module["capitalismmod:keyboard_module"]
  capitalismmod_copper_wire["capitalismmod:copper_wire"] --> capitalismmod_keyboard_module["capitalismmod:keyboard_module"]
  capitalismmod_packaged_chip["capitalismmod:packaged_chip"] --> capitalismmod_storage_module["capitalismmod:storage_module"]
  capitalismmod_circuit_board["capitalismmod:circuit_board"] --> capitalismmod_storage_module["capitalismmod:storage_module"]
  capitalismmod_passive_components["capitalismmod:passive_components"] --> capitalismmod_storage_module["capitalismmod:storage_module"]
  capitalismmod_packaged_chip["capitalismmod:packaged_chip"] --> capitalismmod_wireless_module["capitalismmod:wireless_module"]
  capitalismmod_passive_components["capitalismmod:passive_components"] --> capitalismmod_wireless_module["capitalismmod:wireless_module"]
  capitalismmod_copper_wire["capitalismmod:copper_wire"] --> capitalismmod_wireless_module["capitalismmod:wireless_module"]
  minecraft_iron_ingot["minecraft:iron_ingot"] --> capitalismmod_battery_cell["capitalismmod:battery_cell"]
  minecraft_copper_ingot["minecraft:copper_ingot"] --> capitalismmod_battery_cell["capitalismmod:battery_cell"]
  minecraft_redstone["minecraft:redstone"] --> capitalismmod_battery_cell["capitalismmod:battery_cell"]
  capitalismmod_battery_cell["capitalismmod:battery_cell"] --> capitalismmod_battery["capitalismmod:battery"]
  capitalismmod_copper_wire["capitalismmod:copper_wire"] --> capitalismmod_battery["capitalismmod:battery"]
  capitalismmod_plastic_pellets["capitalismmod:plastic_pellets"] --> capitalismmod_battery["capitalismmod:battery"]
  minecraft_quartz["minecraft:quartz"] --> capitalismmod_lithium_mineral["capitalismmod:lithium_mineral"]
  capitalismmod_lithium_mineral["capitalismmod:lithium_mineral"] --> capitalismmod_lithium_carbonate["capitalismmod:lithium_carbonate"]
  minecraft_coal["minecraft:coal"] --> capitalismmod_lithium_carbonate["capitalismmod:lithium_carbonate"]
  capitalismmod_graphite["capitalismmod:graphite"] --> capitalismmod_graphite_anode["capitalismmod:graphite_anode"]
  capitalismmod_lubricant["capitalismmod:lubricant"] --> capitalismmod_graphite_anode["capitalismmod:graphite_anode"]
  capitalismmod_lfp_cathode["capitalismmod:lfp_cathode"] --> capitalismmod_cathode_active_material["capitalismmod:cathode_active_material"]
  capitalismmod_polypropylene_pellets["capitalismmod:polypropylene_pellets"] --> capitalismmod_battery_separator["capitalismmod:battery_separator"]
  capitalismmod_lithium_carbonate["capitalismmod:lithium_carbonate"] --> capitalismmod_battery_electrolyte["capitalismmod:battery_electrolyte"]
  capitalismmod_lpg["capitalismmod:lpg"] --> capitalismmod_battery_electrolyte["capitalismmod:battery_electrolyte"]
  capitalismmod_cathode_active_material["capitalismmod:cathode_active_material"] --> capitalismmod_battery_cell["capitalismmod:battery_cell"]
  capitalismmod_graphite_anode["capitalismmod:graphite_anode"] --> capitalismmod_battery_cell["capitalismmod:battery_cell"]
  capitalismmod_battery_separator["capitalismmod:battery_separator"] --> capitalismmod_battery_cell["capitalismmod:battery_cell"]
  capitalismmod_battery_electrolyte["capitalismmod:battery_electrolyte"] --> capitalismmod_battery_cell["capitalismmod:battery_cell"]
  note["仅展示前 180 条边，共 581 条"]
```

## 审计结论

- `minecraft:` 开头的未生产物料通常是外部基础资源，不等同于错误。
- `capitalismmod:` 开头且没有生产者的物料是优先核查对象，可能代表断链、遗漏配方或依赖外部数据包。
- 循环路径需要区分“合法回收/再制造闭环”和“无法自然启动的死循环”，不能一律视为错误。
- 当前能源字段以每周期货币化成本表示；代码尚未声明统一的电力/燃料载体，因此能源可达性只能作为后续审计项。
- 下一步应将同一套审计逻辑接入运行时配置，并增加设备、能源、产能和配方可达性检查。
## 当前产业链清单

以下清单按默认行业定义整理；输入中的 minecraft: 物料通常来自原版资源，capitalismmod: 物料通常由其他工业配方或矿石掉落提供。

### mining

- iron_ingot：minecraft:iron_ore ×1 → minecraft:iron_ingot ×1；设备 ore_processor；工人 2；能源 1；维护 2
- copper_ingot：minecraft:copper_ore ×1 → minecraft:copper_ingot ×1；设备 ore_processor；工人 2；能源 1；维护 2
- gold_ingot：minecraft:gold_ore ×1 → minecraft:gold_ingot ×1；设备 ore_processor；工人 2；能源 1；维护 2
- iron_concentrate：minecraft:iron_ore ×2 → minecraft:raw_iron ×1；设备 ore_processor；工人 2；能源 1；维护 2
- copper_concentrate：minecraft:copper_ore ×2 → minecraft:raw_copper ×1；设备 ore_processor；工人 2；能源 1；维护 2
- gold_concentrate：minecraft:gold_ore ×2 → minecraft:raw_gold ×1；设备 ore_processor；工人 2；能源 1；维护 2
- iron_smelting：minecraft:raw_iron ×1 + minecraft:coal ×1 → minecraft:iron_ingot ×1；设备 blast_furnace；工人 2；能源 3；维护 5
- copper_smelting：minecraft:raw_copper ×1 + minecraft:coal ×1 → minecraft:copper_ingot ×1；设备 blast_furnace；工人 2；能源 3；维护 5
- gold_smelting：minecraft:raw_gold ×1 + minecraft:coal ×1 → minecraft:gold_ingot ×1；设备 blast_furnace；工人 2；能源 3；维护 5
- bauxite_refining：capitalismmod:raw_bauxite ×2 + capitalismmod:caustic_soda ×1 + minecraft:water_bucket ×1 → capitalismmod:alumina ×1；设备 ore_processor；工人 2；能源 4；维护 6
- nickel_smelting：capitalismmod:raw_nickel ×1 + minecraft:coal ×1 → capitalismmod:nickel_ingot ×1；设备 blast_furnace；工人 2；能源 5；维护 8

### agriculture

- agriculture（默认配方，未能从源码提取详细字段）

### manufacturing

- rail：minecraft:iron_ingot ×1 + minecraft:coal ×1 → minecraft:rail ×1；设备 rolling_mill；工人 2；能源 2；维护 4
- steel_sheet：minecraft:iron_ingot ×1 + minecraft:coal ×1 → capitalismmod:steel_sheet ×1；设备 rolling_mill；工人 2；能源 2；维护 4
- copper_wire：minecraft:copper_ingot ×1 → capitalismmod:copper_wire ×2；设备 wire_mill；工人 2；能源 1；维护 3
- glass：minecraft:sand ×1 + minecraft:coal ×1 → minecraft:glass ×1；设备 glass_furnace；工人 1；能源 2；维护 3
- glass_lens：minecraft:glass ×1 → capitalismmod:glass_lens ×1；设备 glass_furnace；工人 1；能源 1；维护 3
- electric_lamp：capitalismmod:steel_sheet ×1 + capitalismmod:copper_wire ×1 + capitalismmod:glass_lens ×1 → capitalismmod:electric_lamp ×1；设备 assembly_line；工人 3；能源 2；维护 8
- flour：minecraft:wheat ×1 → capitalismmod:flour ×1；设备 milling_machine；工人 1；能源 1；维护 3
- bread：capitalismmod:flour ×1 + minecraft:sugar ×1 → minecraft:bread ×1；设备 food_processor；工人 2；能源 1；维护 4
- metal_can：minecraft:iron_ingot ×1 → capitalismmod:metal_can ×1；设备 rolling_mill；工人 1；能源 1；维护 3
- canned_food：minecraft:wheat ×1 + capitalismmod:metal_can ×1 → capitalismmod:canned_food ×1；设备 assembly_line；工人 3；能源 2；维护 8
- machine_frame：capitalismmod:steel_sheet ×2 + minecraft:iron_ingot ×2 → capitalismmod:machine_frame ×1；设备 rolling_mill；工人 2；能源 2；维护 5
- electric_motor：capitalismmod:copper_wire ×2 + minecraft:iron_ingot ×1 + minecraft:redstone ×1 → capitalismmod:electric_motor ×1；设备 lathe；工人 2；能源 2；维护 5
- green_lumber：minecraft:oak_log ×1 → capitalismmod:green_lumber ×4；设备 sawmill；工人 2；能源 2；维护 4
- dried_lumber：capitalismmod:green_lumber ×2 + minecraft:coal ×1 → capitalismmod:dried_lumber ×1；设备 dry_kiln；工人 2；能源 4；维护 6
- wooden_crate：capitalismmod:dried_lumber ×3 + minecraft:iron_ingot ×1 → capitalismmod:wooden_crate ×1；设备 assembly_line；工人 2；能源 2；维护 8
- furniture：capitalismmod:dried_lumber ×4 + minecraft:iron_ingot ×2 + minecraft:glass ×1 → capitalismmod:furniture ×1；设备 assembly_line；工人 3；能源 3；维护 10
- crude_oil：无输入 → capitalismmod:crude_oil ×3；设备 oil_well；工人 2；能源 4；维护 12
- refining：capitalismmod:crude_oil ×3 → capitalismmod:refinery_gas ×1 + capitalismmod:naphtha ×2 + capitalismmod:kerosene ×1 + capitalismmod:gas_oil ×1 + capitalismmod:fuel_oil ×1；设备 distillation_column；工人 4；能源 8；维护 14
- steam_cracking：capitalismmod:naphtha ×2 + minecraft:water_bucket ×1 → capitalismmod:ethylene ×2 + capitalismmod:propylene ×1 + capitalismmod:c4_fraction ×1 + capitalismmod:refinery_gas ×1；设备 steam_cracker；工人 3；能源 7；维护 12
- polyethylene_pellets：capitalismmod:ethylene ×1 → capitalismmod:polyethylene_pellets ×3；设备 polymer_reactor；工人 2；能源 4；维护 7
- polypropylene_pellets：capitalismmod:propylene ×1 → capitalismmod:polypropylene_pellets ×3；设备 polymer_reactor；工人 2；能源 4；维护 7
- polyethylene_grade：capitalismmod:ethylene ×1 → capitalismmod:polyethylene_pellets ×3；设备 polymer_reactor；工人 2；能源 4；维护 7
- polypropylene_grade：capitalismmod:propylene ×1 → capitalismmod:polypropylene_pellets ×3；设备 polymer_reactor；工人 2；能源 4；维护 7
- synthetic_rubber：capitalismmod:butadiene ×1 + capitalismmod:styrene_monomer ×1 → capitalismmod:synthetic_rubber ×2；设备 rubber_polymerization_unit；工人 3；能源 5；维护 9
- ethylene_glycol：capitalismmod:ethylene_oxide ×1 + minecraft:water_bucket ×1 → capitalismmod:ethylene_glycol ×2；设备 chemical_reactor；工人 3；能源 5；维护 9
- epoxy_resin：capitalismmod:bisphenol_a ×1 + capitalismmod:epichlorohydrin ×1 + capitalismmod:caustic_soda ×1 → capitalismmod:epoxy_resin ×2；设备 polymer_reactor；工人 3；能源 5；维护 10
- benzene_reforming：capitalismmod:reformate ×2 → capitalismmod:benzene ×1 + capitalismmod:p_xylene ×1；设备 distillation_column；工人 3；能源 6；维护 11
- aromatics_reforming：capitalismmod:reformate ×2 → capitalismmod:benzene ×1 + capitalismmod:p_xylene ×1；设备 distillation_column；工人 3；能源 6；维护 11
- styrene_monomer：capitalismmod:ethylbenzene ×1 → capitalismmod:styrene_monomer ×1 + capitalismmod:hydrogen ×1；设备 chemical_reactor；工人 3；能源 6；维护 11
- abs_resin：capitalismmod:acrylonitrile ×1 + capitalismmod:butadiene ×1 + capitalismmod:styrene_monomer ×1 → capitalismmod:abs_resin ×2；设备 polymer_reactor；工人 3；能源 6；维护 11
- terephthalic_acid_oxidation：capitalismmod:p_xylene ×1 + capitalismmod:oxygen ×2 → capitalismmod:terephthalic_acid ×1；设备 chemical_reactor；工人 3；能源 6；维护 11
- pet_resin_polycondensation：capitalismmod:terephthalic_acid ×1 + capitalismmod:ethylene_glycol ×1 → capitalismmod:pet_resin ×2；设备 polymer_reactor；工人 3；能源 7；维护 12
- lubricant_blending：capitalismmod:base_oil ×1 + capitalismmod:hydrogen ×1 → capitalismmod:lubricant ×2；设备 hydrotreater；工人 2；能源 3；维护 7
- plastic_pellets：capitalismmod:polyethylene_pellets ×2 → capitalismmod:plastic_pellets ×2；设备 extrusion_line；工人 2；能源 4；维护 7
- fan_blades：capitalismmod:plastic_pellets ×2 + capitalismmod:steel_sheet ×1 → capitalismmod:fan_blades ×1；设备 injection_molder；工人 2；能源 3；维护 8
- fan_control：capitalismmod:copper_wire ×1 + minecraft:redstone ×1 + minecraft:iron_ingot ×1 → capitalismmod:fan_control ×1；设备 assembly_line；工人 2；能源 2；维护 6
- polysilicon：minecraft:quartz ×2 + minecraft:coal ×1 → capitalismmod:polysilicon ×2；设备 silicon_refiner；工人 3；能源 8；维护 15
- silicon_wafer：capitalismmod:silicon_ingot ×1 → capitalismmod:silicon_wafer ×2；设备 wafer_dicing_saw；工人 3；能源 8；维护 16
- silicon_wafer_from_polysilicon：capitalismmod:silicon_ingot ×1 → capitalismmod:silicon_wafer ×2；设备 wafer_dicing_saw；工人 3；能源 9；维护 16
- wafer_dicing：capitalismmod:tested_wafer ×1 → capitalismmod:silicon_die ×4；设备 wafer_dicing_saw；工人 3；能源 6；维护 12
- die_testing：capitalismmod:silicon_die ×2 → capitalismmod:tested_die ×2；设备 chip_testing_station；工人 3；能源 6；维护 13
- lead_frame_stamping：capitalismmod:copper_foil ×1 + capitalismmod:steel_sheet ×1 → capitalismmod:lead_frame ×2；设备 stamping_press；工人 2；能源 4；维护 8
- mold_compound：capitalismmod:epoxy_resin ×1 + capitalismmod:plastic_pellets ×1 → capitalismmod:mold_compound ×2；设备 molding_compound_unit；工人 2；能源 4；维护 8
- chip_packaging：capitalismmod:tested_die ×1 + capitalismmod:lead_frame ×1 + capitalismmod:copper_wire ×1 + capitalismmod:mold_compound ×1 → capitalismmod:packaged_chip ×1；设备 chip_packaging_line；工人 3；能源 7；维护 12
- tested_die_packaging：capitalismmod:tested_die ×1 + capitalismmod:lead_frame ×1 + capitalismmod:copper_wire ×1 + capitalismmod:mold_compound ×1 → capitalismmod:packaged_chip ×1；设备 chip_packaging_line；工人 4；能源 8；维护 15
- circuit_board：capitalismmod:assembled_pcb ×1 → capitalismmod:circuit_board ×1；设备 chip_testing_station；工人 3；能源 5；维护 10
- circuit_board_with_packaged_chip：capitalismmod:assembled_pcb ×1 → capitalismmod:circuit_board ×1；设备 chip_testing_station；工人 3；能源 5；维护 10
- pcb_substrate：minecraft:glass ×1 + capitalismmod:epoxy_resin ×1 → capitalismmod:pcb_substrate ×2；设备 lamination_press；工人 2；能源 4；维护 8
- copper_foil：capitalismmod:copper_cathode ×1 → capitalismmod:copper_foil ×2；设备 rolling_mill；工人 2；能源 2；维护 5
- solder：capitalismmod:tin_ingot ×2 + minecraft:copper_ingot ×1 → capitalismmod:solder ×2；设备 alloy_furnace；工人 2；能源 3；维护 6
- pcb_drilling：capitalismmod:copper_clad_laminate ×1 → capitalismmod:drilled_pcb_panel ×1；设备 pcb_fabrication_line；工人 3；能源 5；维护 10
- pcb_etching：capitalismmod:drilled_pcb_panel ×1 + capitalismmod:electronic_etchant ×1 → capitalismmod:etched_pcb ×1；设备 pcb_fabrication_line；工人 3；能源 6；维护 11
- pcb_solder_mask：capitalismmod:etched_pcb ×1 + capitalismmod:epoxy_resin ×1 → capitalismmod:solder_masked_pcb ×1；设备 pcb_fabrication_line；工人 3；能源 6；维护 11
- smd_components：minecraft:redstone ×1 + capitalismmod:copper_wire ×1 + capitalismmod:plastic_pellets ×1 → capitalismmod:smd_components ×2；设备 smt_line；工人 3；能源 5；维护 11
- passive_components：minecraft:redstone ×1 + minecraft:iron_ingot ×1 + capitalismmod:copper_wire ×1 → capitalismmod:passive_components ×4；设备 smt_line；工人 3；能源 5；维护 10
- power_management_ic：capitalismmod:packaged_chip ×1 + capitalismmod:passive_components ×1 + capitalismmod:copper_wire ×1 → capitalismmod:power_management_ic ×1；设备 smt_line；工人 3；能源 6；维护 12
- display_driver：capitalismmod:packaged_chip ×1 + capitalismmod:passive_components ×1 + capitalismmod:copper_wire ×1 → capitalismmod:display_driver ×1；设备 smt_line；工人 3；能源 6；维护 12
- printed_circuit_board：capitalismmod:assembled_pcb ×1 → capitalismmod:circuit_board ×1；设备 chip_testing_station；工人 4；能源 7；维护 14
- assembled_fabricated_pcb：capitalismmod:assembled_pcb ×1 → capitalismmod:circuit_board ×1；设备 chip_testing_station；工人 4；能源 8；维护 15
- display_glass_substrate：minecraft:glass ×2 → capitalismmod:display_glass_substrate ×1；设备 glass_furnace；工人 2；能源 4；维护 8
- backlight_module：capitalismmod:packaged_chip ×1 + capitalismmod:copper_wire ×1 + capitalismmod:plastic_pellets ×1 → capitalismmod:backlight_module ×1；设备 electronics_assembly；工人 3；能源 6；维护 11
- display_panel：capitalismmod:display_glass_substrate ×1 + capitalismmod:backlight_module ×1 + capitalismmod:display_driver ×1 + capitalismmod:plastic_pellets ×1 → capitalismmod:display_panel ×1；设备 electronics_assembly；工人 4；能源 8；维护 15
- phone_casing：capitalismmod:abs_resin ×1 → capitalismmod:phone_casing ×1；设备 injection_molder；工人 2；能源 3；维护 8
- plastic_container：capitalismmod:polyethylene_pellets ×2 → capitalismmod:plastic_container ×1；设备 injection_molder；工人 2；能源 3；维护 8
- packaging_film：capitalismmod:polyethylene_pellets ×1 → capitalismmod:packaging_film ×4；设备 extrusion_line；工人 2；能源 3；维护 8
- pet_bottle：capitalismmod:pet_resin ×1 → capitalismmod:pet_bottle ×1；设备 injection_molder；工人 2；能源 3；维护 8
- pet_bottle_recycling：capitalismmod:pet_bottle ×2 → capitalismmod:recycled_pet_flakes ×1；设备 plastic_recycler；工人 2；能源 5；维护 9
- recycled_pet_resin：capitalismmod:recycled_pet_flakes ×2 → capitalismmod:pet_resin ×1；设备 extrusion_line；工人 2；能源 5；维护 10
- packaging_film_recycling：capitalismmod:packaging_film ×4 → capitalismmod:reclaimed_plastic ×1；设备 plastic_recycler；工人 2；能源 5；维护 9
- plastic_container_recycling：capitalismmod:plastic_container ×2 → capitalismmod:reclaimed_plastic ×1；设备 plastic_recycler；工人 2；能源 5；维护 9
- camera_module：capitalismmod:glass_lens ×1 + capitalismmod:circuit_board ×1 + capitalismmod:plastic_pellets ×1 → capitalismmod:camera_module ×1；设备 electronics_assembly；工人 3；能源 5；维护 10
- speaker_module：capitalismmod:copper_wire ×1 + minecraft:iron_ingot ×1 + capitalismmod:plastic_pellets ×1 → capitalismmod:speaker_module ×1；设备 electronics_assembly；工人 2；能源 3；维护 8
- microphone_module：capitalismmod:copper_wire ×1 + minecraft:redstone ×1 + capitalismmod:plastic_pellets ×1 → capitalismmod:microphone_module ×1；设备 electronics_assembly；工人 2；能源 3；维护 8
- charging_port：capitalismmod:copper_wire ×1 + minecraft:iron_ingot ×1 + capitalismmod:plastic_pellets ×1 → capitalismmod:charging_port ×1；设备 electronics_assembly；工人 2；能源 3；维护 8
- keyboard_module：capitalismmod:abs_resin ×1 + capitalismmod:passive_components ×1 + capitalismmod:copper_wire ×1 → capitalismmod:keyboard_module ×1；设备 electronics_assembly；工人 3；能源 5；维护 10
- storage_module：capitalismmod:packaged_chip ×1 + capitalismmod:circuit_board ×1 + capitalismmod:passive_components ×1 → capitalismmod:storage_module ×1；设备 smt_line；工人 3；能源 6；维护 12
- wireless_module：capitalismmod:packaged_chip ×1 + capitalismmod:passive_components ×1 + capitalismmod:copper_wire ×1 → capitalismmod:wireless_module ×1；设备 smt_line；工人 3；能源 6；维护 12
- battery_cell：minecraft:iron_ingot ×1 + minecraft:copper_ingot ×1 + minecraft:redstone ×1 → capitalismmod:battery_cell ×2；设备 electronics_assembly；工人 2；能源 3；维护 7
- battery：capitalismmod:battery_cell ×2 + capitalismmod:copper_wire ×1 + capitalismmod:plastic_pellets ×1 → capitalismmod:battery ×1；设备 electronics_assembly；工人 2；能源 3；维护 7
- lithium_mineral_concentration：minecraft:quartz ×2 → capitalismmod:lithium_mineral ×1；设备 ore_processor；工人 2；能源 2；维护 5
- lithium_carbonate_refining：capitalismmod:lithium_mineral ×1 + minecraft:coal ×1 → capitalismmod:lithium_carbonate ×1；设备 battery_materials；工人 3；能源 6；维护 10
- graphite_anode_processing：capitalismmod:graphite ×2 + capitalismmod:lubricant ×1 → capitalismmod:graphite_anode ×1；设备 battery_materials；工人 3；能源 5；维护 9
- cathode_active_material：capitalismmod:lfp_cathode ×1 → capitalismmod:cathode_active_material ×1；设备 battery_materials；工人 3；能源 7；维护 11
- battery_separator：capitalismmod:polypropylene_pellets ×1 → capitalismmod:battery_separator ×2；设备 polymer_reactor；工人 2；能源 4；维护 7
- battery_electrolyte：capitalismmod:lithium_carbonate ×1 + capitalismmod:lpg ×1 → capitalismmod:battery_electrolyte ×1；设备 battery_materials；工人 3；能源 6；维护 10
- lithium_ion_cell：capitalismmod:cathode_active_material ×1 + capitalismmod:graphite_anode ×1 + capitalismmod:battery_separator ×1 + capitalismmod:battery_electrolyte ×1 + capitalismmod:copper_foil ×1 → capitalismmod:battery_cell ×2；设备 battery_cell_line；工人 4；能源 8；维护 13
- lithium_ion_battery：capitalismmod:battery_cell ×2 + capitalismmod:copper_wire ×1 + capitalismmod:plastic_pellets ×1 → capitalismmod:battery ×1；设备 electronics_assembly；工人 2；能源 4；维护 8
- battery_pack：capitalismmod:battery ×4 + capitalismmod:circuit_board ×1 + capitalismmod:steel_sheet ×1 + capitalismmod:plastic_pellets ×2 → capitalismmod:battery_pack ×1；设备 electronics_assembly；工人 4；能源 8；维护 14
- battery_pack_shredding：capitalismmod:battery_pack ×1 → capitalismmod:black_mass ×2；设备 battery_recycler；工人 3；能源 7；维护 12
- black_mass_hydrometallurgy：capitalismmod:black_mass ×2 + capitalismmod:sulfuric_acid ×1 + capitalismmod:industrial_water ×1 → capitalismmod:lithium_carbonate ×1 + capitalismmod:graphite ×1；设备 battery_recycler；工人 4；能源 9；维护 15
- smartphone：capitalismmod:circuit_board ×1 + capitalismmod:battery ×1 + capitalismmod:display_panel ×1 + capitalismmod:phone_casing ×1 + capitalismmod:camera_module ×1 + capitalismmod:speaker_module ×1 + capitalismmod:microphone_module ×1 + capitalismmod:charging_port ×1 + capitalismmod:power_management_ic ×1 + capitalismmod:display_driver ×1 → capitalismmod:smartphone ×1；设备 electronics_assembly；工人 5；能源 8；维护 15
- electric_fan：capitalismmod:electric_motor ×1 + capitalismmod:fan_blades ×1 + capitalismmod:fan_control ×1 + capitalismmod:machine_frame ×1 → capitalismmod:electric_fan ×1；设备 assembly_line；工人 3；能源 3；维护 9
- power_adapter：capitalismmod:circuit_board ×1 + capitalismmod:copper_wire ×1 + capitalismmod:plastic_pellets ×1 → capitalismmod:power_adapter ×1；设备 smt_line；工人 2；能源 4；维护 9
- television：capitalismmod:display_panel ×1 + capitalismmod:packaged_chip ×1 + capitalismmod:circuit_board ×1 + capitalismmod:speaker_module ×1 + capitalismmod:abs_resin ×1 + capitalismmod:display_driver ×1 → capitalismmod:television ×1；设备 electronics_assembly；工人 5；能源 8；维护 16
- laptop：capitalismmod:display_panel ×1 + capitalismmod:packaged_chip ×2 + capitalismmod:circuit_board ×1 + capitalismmod:battery ×1 + capitalismmod:abs_resin ×1 + capitalismmod:power_management_ic ×1 + capitalismmod:display_driver ×1 + capitalismmod:keyboard_module ×1 + capitalismmod:storage_module ×1 → capitalismmod:laptop ×1；设备 electronics_assembly；工人 6；能源 10；维护 20
- wireless_router：capitalismmod:circuit_board ×1 + capitalismmod:packaged_chip ×1 + capitalismmod:power_adapter ×1 + capitalismmod:abs_resin ×1 + capitalismmod:wireless_module ×1 → capitalismmod:wireless_router ×1；设备 electronics_assembly；工人 4；能源 7；维护 14
- power_bank：capitalismmod:battery_pack ×1 + capitalismmod:charging_port ×1 + capitalismmod:power_management_ic ×1 + capitalismmod:abs_resin ×1 → capitalismmod:power_bank ×1；设备 electronics_assembly；工人 4；能源 7；维护 14
- smart_speaker：capitalismmod:wireless_module ×1 + capitalismmod:speaker_module ×1 + capitalismmod:microphone_module ×1 + capitalismmod:packaged_chip ×1 + capitalismmod:abs_resin ×1 → capitalismmod:smart_speaker ×1；设备 electronics_assembly；工人 4；能源 7；维护 14
- smartphone_recycling：capitalismmod:smartphone ×1 → capitalismmod:copper_wire ×2 + capitalismmod:plastic_pellets ×1；设备 electronics_recycler；工人 3；能源 6；维护 12
- television_recycling：capitalismmod:television ×1 → capitalismmod:copper_wire ×3 + capitalismmod:plastic_pellets ×2；设备 electronics_recycler；工人 4；能源 7；维护 14
- laptop_recycling：capitalismmod:laptop ×1 → capitalismmod:copper_wire ×3 + capitalismmod:plastic_pellets ×2；设备 electronics_recycler；工人 4；能源 8；维护 15
- wireless_router_recycling：capitalismmod:wireless_router ×1 → capitalismmod:copper_wire ×1 + capitalismmod:plastic_pellets ×1；设备 electronics_recycler；工人 2；能源 5；维护 10
- smartphone_refurbishment：capitalismmod:smartphone ×1 + capitalismmod:battery ×1 + capitalismmod:display_panel ×1 → capitalismmod:refurbished_smartphone ×1；设备 refurbishment_line；工人 3；能源 6；维护 12
- television_refurbishment：capitalismmod:television ×1 + capitalismmod:display_panel ×1 + capitalismmod:circuit_board ×1 → capitalismmod:refurbished_television ×1；设备 refurbishment_line；工人 3；能源 7；维护 14
- laptop_refurbishment：capitalismmod:laptop ×1 + capitalismmod:battery ×1 + capitalismmod:display_panel ×1 → capitalismmod:refurbished_laptop ×1；设备 refurbishment_line；工人 4；能源 8；维护 15
- wireless_router_refurbishment：capitalismmod:wireless_router ×1 + capitalismmod:packaged_chip ×1 → capitalismmod:refurbished_wireless_router ×1；设备 refurbishment_line；工人 2；能源 5；维护 10
- refurbished_smartphone_recycling：capitalismmod:refurbished_smartphone ×1 → capitalismmod:copper_wire ×1 + capitalismmod:plastic_pellets ×1；设备 electronics_recycler；工人 2；能源 5；维护 10
- refurbished_television_recycling：capitalismmod:refurbished_television ×1 → capitalismmod:copper_wire ×2 + capitalismmod:plastic_pellets ×1；设备 electronics_recycler；工人 3；能源 6；维护 12
- refurbished_laptop_recycling：capitalismmod:refurbished_laptop ×1 → capitalismmod:copper_wire ×2 + capitalismmod:plastic_pellets ×1；设备 electronics_recycler；工人 3；能源 6；维护 13
- refurbished_wireless_router_recycling：capitalismmod:refurbished_wireless_router ×1 → capitalismmod:copper_wire ×1 + capitalismmod:plastic_pellets ×1；设备 electronics_recycler；工人 2；能源 4；维护 9

### petrochemical_refining

- atmospheric_distillation：capitalismmod:crude_oil ×3 → capitalismmod:refinery_gas ×1 + capitalismmod:naphtha ×2 + capitalismmod:kerosene ×1 + capitalismmod:gas_oil ×1 + capitalismmod:fuel_oil ×1；设备 distillation_column；工人 4；能源 10；维护 16
- refinery_gas_fractionation：capitalismmod:refinery_gas ×2 → capitalismmod:lpg ×1；设备 distillation_column；工人 2；能源 6；维护 9
- gasoline_blending：capitalismmod:reformate ×1 + capitalismmod:naphtha ×1 → capitalismmod:gasoline ×2；设备 blending_unit；工人 3；能源 5；维护 9
- syngas_shift_separation：capitalismmod:syngas ×1 + minecraft:water_bucket ×1 → capitalismmod:hydrogen ×2 + capitalismmod:carbon_dioxide ×1；设备 steam_reformer；工人 3；能源 8；维护 12
- coal_gasification：minecraft:coal ×2 + minecraft:water_bucket ×1 + capitalismmod:oxygen ×1 → capitalismmod:syngas ×2；设备 steam_reformer；工人 3；能源 9；维护 13
- water_electrolysis：minecraft:water_bucket ×1 → capitalismmod:hydrogen ×2 + capitalismmod:oxygen ×1；设备 chlor_alkali_cell；工人 2；能源 12；维护 10
- ethylbenzene_alkylation：capitalismmod:benzene ×1 + capitalismmod:ethylene ×1 → capitalismmod:ethylbenzene ×1；设备 chemical_reactor；工人 3；能源 6；维护 10
- acrylonitrile_ammoxidation：capitalismmod:propylene ×1 + capitalismmod:ammonia ×1 + capitalismmod:oxygen ×1 → capitalismmod:acrylonitrile ×1；设备 chemical_reactor；工人 3；能源 8；维护 12
- bisphenol_a_condensation：capitalismmod:phenol ×2 + capitalismmod:acetone ×1 → capitalismmod:bisphenol_a ×1；设备 chemical_reactor；工人 3；能源 7；维护 11
- epichlorohydrin_production：capitalismmod:propylene ×1 + capitalismmod:chlorine ×1 + capitalismmod:caustic_soda ×1 + minecraft:water_bucket ×1 → capitalismmod:epichlorohydrin ×1 + capitalismmod:salt ×1；设备 chemical_reactor；工人 3；能源 8；维护 12
- vacuum_distillation：capitalismmod:fuel_oil ×2 → capitalismmod:vacuum_resid ×1 + capitalismmod:base_oil ×1；设备 vacuum_distillation_unit；工人 3；能源 8；维护 13
- resid_to_asphalt：capitalismmod:vacuum_resid ×1 → capitalismmod:asphalt ×2；设备 vacuum_distillation_unit；工人 3；能源 6；维护 10
- gas_reforming：capitalismmod:refinery_gas ×2 + minecraft:water_bucket ×1 → capitalismmod:syngas ×2；设备 steam_reformer；工人 3；能源 7；维护 11
- naphtha_reforming：capitalismmod:naphtha ×2 → capitalismmod:reformate ×2 + capitalismmod:hydrogen ×1；设备 catalytic_reformer；工人 3；能源 9；维护 14
- cumene_alkylation：capitalismmod:benzene ×1 + capitalismmod:propylene ×1 → capitalismmod:cumene ×1；设备 chemical_reactor；工人 3；能源 7；维护 11
- cumene_cleavage：capitalismmod:cumene ×1 + capitalismmod:oxygen ×1 → capitalismmod:phenol ×1 + capitalismmod:acetone ×1；设备 chemical_reactor；工人 3；能源 8；维护 12
- phenolic_resin_production：capitalismmod:phenol ×1 + capitalismmod:formaldehyde ×1 → capitalismmod:phenolic_resin ×2；设备 polymer_reactor；工人 3；能源 7；维护 11
- methanol_synthesis：capitalismmod:syngas ×2 → capitalismmod:methanol ×2；设备 methanol_synthesis_unit；工人 3；能源 7；维护 11
- formaldehyde_oxidation：capitalismmod:methanol ×1 + capitalismmod:oxygen ×1 → capitalismmod:formaldehyde ×2；设备 chemical_reactor；工人 2；能源 5；维护 8
- phenolic_resin_condensation：capitalismmod:phenol ×1 + capitalismmod:formaldehyde ×1 → capitalismmod:phenolic_resin ×2；设备 polymer_reactor；工人 3；能源 7；维护 11
- gas_oil_hydrotreating：capitalismmod:gas_oil ×1 + capitalismmod:hydrogen ×1 → capitalismmod:diesel ×1 + capitalismmod:sulfur ×1；设备 hydrotreater；工人 3；能源 8；维护 13
- sulfuric_acid_production：capitalismmod:sulfur ×1 + capitalismmod:oxygen ×2 + minecraft:water_bucket ×1 → capitalismmod:sulfuric_acid ×1；设备 chemical_reactor；工人 3；能源 6；维护 10
- steam_cracking_fractionated：capitalismmod:naphtha ×2 + minecraft:water_bucket ×1 → capitalismmod:ethylene ×2 + capitalismmod:propylene ×1 + capitalismmod:c4_fraction ×1 + capitalismmod:refinery_gas ×1；设备 steam_cracker；工人 3；能源 8；维护 13
- ethylene_oxide_production：capitalismmod:ethylene ×1 + capitalismmod:oxygen ×1 → capitalismmod:ethylene_oxide ×1；设备 chemical_reactor；工人 3；能源 6；维护 10
- propylene_oxide：capitalismmod:propylene ×1 + capitalismmod:hydrogen_peroxide ×1 → capitalismmod:propylene_oxide ×1；设备 chemical_reactor；工人 3；能源 6；维护 10
- polyethylene_film_extrusion：capitalismmod:polyethylene_pellets ×1 → capitalismmod:polyethylene_film ×3；设备 extrusion_line；工人 2；能源 5；维护 8
- polypropylene_fiber_spinning：capitalismmod:polypropylene_pellets ×1 → capitalismmod:polypropylene_fiber ×3；设备 extrusion_line；工人 2；能源 5；维护 8
- salt_extraction：minecraft:water_bucket ×1 + minecraft:sand ×1 → capitalismmod:salt ×2；设备 chemical_reactor；工人 1；能源 2；维护 4
- chlor_alkali_electrolysis：capitalismmod:brine ×2 → capitalismmod:chlorine ×1 + capitalismmod:caustic_soda ×1 + capitalismmod:hydrogen ×1；设备 chlor_alkali_cell；工人 3；能源 7；维护 10
- ethylene_dichloride_synthesis：capitalismmod:ethylene ×1 + capitalismmod:chlorine ×1 → capitalismmod:ethylene_dichloride ×1；设备 chemical_reactor；工人 3；能源 7；维护 10
- ethylene_dichloride_cracking：capitalismmod:ethylene_dichloride ×1 + minecraft:water_bucket ×1 → capitalismmod:vinyl_chloride ×1 + capitalismmod:hydrochloric_acid ×1；设备 vinyl_chloride_unit；工人 3；能源 7；维护 11
- vinyl_chloride_synthesis：capitalismmod:ethylene_dichloride ×1 + minecraft:water_bucket ×1 → capitalismmod:vinyl_chloride ×1 + capitalismmod:hydrochloric_acid ×1；设备 vinyl_chloride_unit；工人 3；能源 7；维护 11
- pvc_polymerization：capitalismmod:vinyl_chloride ×2 → capitalismmod:pvc_resin ×3；设备 polymer_reactor；工人 3；能源 7；维护 11
- pvc_pipe_extrusion：capitalismmod:pvc_resin ×2 → capitalismmod:pvc_pipe ×1；设备 extrusion_line；工人 2；能源 6；维护 9
- pvc_pipe_recycling：capitalismmod:pvc_pipe ×2 → capitalismmod:pvc_resin ×1；设备 plastic_recycler；工人 3；能源 6；维护 10
- butadiene_extraction：capitalismmod:c4_fraction ×2 → capitalismmod:butadiene ×1；设备 steam_cracker；工人 2；能源 6；维护 9
- sbr_rubber_polymerization：capitalismmod:butadiene ×1 + capitalismmod:styrene_monomer ×1 → capitalismmod:synthetic_rubber ×2；设备 rubber_polymerization_unit；工人 3；能源 7；维护 11
- polyether_polyol_synthesis：capitalismmod:propylene_oxide ×1 + capitalismmod:ethylene_oxide ×1 → capitalismmod:polyether_polyol ×2；设备 chemical_reactor；工人 3；能源 7；维护 11
- polyurethane_foam_production：capitalismmod:polyether_polyol ×1 + capitalismmod:plastic_pellets ×1 → capitalismmod:polyurethane_foam ×2；设备 polymer_reactor；工人 3；能源 6；维护 10
- pet_fiber_spinning：capitalismmod:pet_resin ×1 → capitalismmod:pet_fiber ×3；设备 extrusion_line；工人 2；能源 5；维护 8
- plastic_reclamation：capitalismmod:plastic_container ×2 + capitalismmod:packaging_film ×2 → capitalismmod:reclaimed_plastic ×2；设备 plastic_recycler；工人 3；能源 6；维护 10
- reclaimed_plastic_reprocessing：capitalismmod:reclaimed_plastic ×2 → capitalismmod:plastic_pellets ×1；设备 plastic_recycler；工人 2；能源 5；维护 8

### metallurgy

- copper_concentrate_refining：minecraft:raw_copper ×2 → capitalismmod:copper_concentrate ×1；设备 ore_processor；工人 2；能源 2；维护 4
- cathode_wire_drawing：capitalismmod:copper_cathode ×1 → capitalismmod:copper_wire ×2；设备 wire_mill；工人 2；能源 2；维护 4
- copper_cathode：capitalismmod:copper_concentrate ×2 + minecraft:coal ×1 → capitalismmod:copper_cathode ×1；设备 blast_furnace；工人 3；能源 5；维护 8
- alumina_refining：capitalismmod:raw_bauxite ×2 + capitalismmod:caustic_soda ×1 + minecraft:water_bucket ×1 → capitalismmod:alumina ×1；设备 ore_processor；工人 2；能源 4；维护 6
- aluminum_electrolysis：capitalismmod:alumina ×2 + capitalismmod:graphite ×1 → capitalismmod:aluminum_ingot ×1；设备 electric_arc_furnace；工人 3；能源 9；维护 12
- nickel_sulfate_refining：capitalismmod:nickel_ingot ×1 + capitalismmod:sulfuric_acid ×1 → capitalismmod:nickel_sulfate ×1；设备 chemical_reactor；工人 3；能源 7；维护 10
- cobalt_concentration：capitalismmod:cobalt_ore ×2 → capitalismmod:raw_cobalt ×1；设备 flotation_cell；工人 2；能源 3；维护 6
- cobalt_smelting：capitalismmod:raw_cobalt ×2 + minecraft:coal ×1 → capitalismmod:cobalt_ingot ×1；设备 electric_arc_furnace；工人 3；能源 7；维护 10
- manganese_concentration：capitalismmod:manganese_ore ×2 → capitalismmod:raw_manganese ×1；设备 flotation_cell；工人 2；能源 3；维护 6
- manganese_smelting：capitalismmod:raw_manganese ×2 + minecraft:coal ×1 → capitalismmod:manganese_ingot ×1；设备 electric_arc_furnace；工人 3；能源 7；维护 10
- chromium_concentration：capitalismmod:chromium_ore ×2 → capitalismmod:raw_chromium ×1；设备 flotation_cell；工人 2；能源 3；维护 6
- chromium_smelting：capitalismmod:raw_chromium ×2 + minecraft:coal ×1 → capitalismmod:chromium_ingot ×1；设备 electric_arc_furnace；工人 3；能源 8；维护 11
- titanium_concentration：capitalismmod:titanium_ore ×2 → capitalismmod:raw_titanium ×1；设备 flotation_cell；工人 2；能源 4；维护 7
- titanium_smelting：capitalismmod:raw_titanium ×2 + capitalismmod:chlorine ×1 → capitalismmod:titanium_ingot ×1；设备 electric_arc_furnace；工人 3；能源 9；维护 12
- tungsten_concentration：capitalismmod:tungsten_ore ×2 → capitalismmod:raw_tungsten ×1；设备 flotation_cell；工人 3；能源 4；维护 8
- tungsten_smelting：capitalismmod:raw_tungsten ×2 + minecraft:coal ×1 → capitalismmod:tungsten_ingot ×1；设备 electric_arc_furnace；工人 4；能源 10；维护 14
- molybdenum_concentration：capitalismmod:molybdenum_ore ×2 → capitalismmod:raw_molybdenum ×1；设备 flotation_cell；工人 3；能源 4；维护 8
- molybdenum_smelting：capitalismmod:raw_molybdenum ×2 + minecraft:coal ×1 → capitalismmod:molybdenum_ingot ×1；设备 electric_arc_furnace；工人 4；能源 9；维护 13
- fluorite_concentration：capitalismmod:fluorite_ore ×2 → capitalismmod:fluorite ×2；设备 flotation_cell；工人 2；能源 3；维护 5
- rare_earth_concentration：capitalismmod:rare_earth_ore ×2 → capitalismmod:rare_earth_concentrate ×1；设备 flotation_cell；工人 3；能源 5；维护 9
- rare_earth_cracking：capitalismmod:rare_earth_concentrate ×1 + capitalismmod:sulfuric_acid ×1 → capitalismmod:mixed_rare_earth_carbonate ×2 + capitalismmod:radioactive_tailings ×1；设备 chemical_reactor；工人 3；能源 8；维护 12
- rare_earth_solvent_separation：capitalismmod:mixed_rare_earth_carbonate ×2 → capitalismmod:neodymium_oxide ×1 + capitalismmod:lanthanum_oxide ×1 + capitalismmod:cerium_oxide ×1 + capitalismmod:dysprosium_oxide ×1；设备 rare_earth_separation_unit；工人 4；能源 11；维护 16
- rare_earth_magnet_sintering：capitalismmod:neodymium_oxide ×1 + minecraft:iron_ingot ×2 + capitalismmod:cobalt_ingot ×1 → capitalismmod:rare_earth_magnet ×2；设备 electric_arc_furnace；工人 3；能源 9；维护 13
- uranium_ore_milling：capitalismmod:uranium_ore ×2 → capitalismmod:raw_uranium ×1 + capitalismmod:radioactive_tailings ×1；设备 uranium_milling_circuit；工人 3；能源 7；维护 11
- yellowcake_leaching：capitalismmod:raw_uranium ×2 + capitalismmod:sulfuric_acid ×1 → capitalismmod:yellowcake ×1 + capitalismmod:radioactive_tailings ×1；设备 chemical_reactor；工人 3；能源 8；维护 13
- uranium_hexafluoride_conversion：capitalismmod:yellowcake ×1 + capitalismmod:fluorite ×1 → capitalismmod:uranium_hexafluoride ×1；设备 chemical_reactor；工人 4；能源 10；维护 15
- nuclear_fuel_pellet_fabrication：capitalismmod:uranium_hexafluoride ×1 → capitalismmod:nuclear_fuel ×2；设备 nuclear_fuel_fabricator；工人 4；能源 12；维护 18
- radiation_shielding_fabrication：capitalismmod:lead_ingot ×2 + capitalismmod:steel_sheet ×1 → capitalismmod:radiation_shielding ×2；设备 radiation_shielding_station；工人 2；能源 6；维护 9

### semiconductor_fabrication

- silicon_ingot：capitalismmod:polysilicon ×2 → capitalismmod:silicon_ingot ×1；设备 crystal_growth_furnace；工人 3；能源 7；维护 12
- silicon_wafer_from_ingot：capitalismmod:silicon_ingot ×1 → capitalismmod:silicon_wafer ×2；设备 wafer_dicing_saw；工人 3；能源 8；维护 14
- photoresist_coating：capitalismmod:plastic_pellets ×1 + minecraft:glass ×1 → capitalismmod:photoresist ×2；设备 chemical_reactor；工人 2；能源 5；维护 9
- wafer_fabrication：capitalismmod:silicon_wafer ×1 + capitalismmod:photoresist ×1 + capitalismmod:electronic_etchant ×1 + capitalismmod:electronic_solvent ×1 → capitalismmod:tested_wafer ×1；设备 semiconductor_fab；工人 4；能源 11；维护 18
- wafer_test：capitalismmod:tested_wafer ×1 → capitalismmod:silicon_die ×4；设备 chip_testing_station；工人 3；能源 7；维护 12
- final_chip_test：capitalismmod:packaged_chip ×1 → capitalismmod:final_chip_test ×1；设备 chip_testing_station；工人 3；能源 6；维护 12

### pcb_manufacturing

- copper_clad_laminate：capitalismmod:pcb_substrate ×1 + capitalismmod:copper_foil ×1 → capitalismmod:copper_clad_laminate ×1；设备 lamination_press；工人 2；能源 5；维护 9
- pcb_drilling_realistic：capitalismmod:copper_clad_laminate ×1 → capitalismmod:drilled_pcb_panel ×1；设备 pcb_fabrication_line；工人 3；能源 5；维护 10
- pcb_etching_realistic：capitalismmod:drilled_pcb_panel ×1 + capitalismmod:electronic_etchant ×1 → capitalismmod:etched_pcb ×1；设备 pcb_fabrication_line；工人 3；能源 6；维护 11
- pcb_assembly_realistic：capitalismmod:solder_masked_pcb ×1 + capitalismmod:final_chip_test ×1 + capitalismmod:smd_components ×1 + capitalismmod:solder ×1 → capitalismmod:assembled_pcb ×1；设备 pcb_assembly_line；工人 4；能源 8；维护 15

### battery_chemistry

- cathode_precursor：capitalismmod:nickel_sulfate ×1 + capitalismmod:cobalt_ingot ×1 + capitalismmod:manganese_ingot ×1 + capitalismmod:sulfuric_acid ×1 → capitalismmod:cathode_precursor ×1；设备 battery_materials；工人 3；能源 6；维护 10
- lfp_cathode：capitalismmod:lithium_carbonate ×1 + capitalismmod:phosphoric_acid ×1 + minecraft:iron_ingot ×1 → capitalismmod:lfp_cathode ×1；设备 battery_materials；工人 3；能源 7；维护 11
- nmc_cathode：capitalismmod:lithium_carbonate ×1 + capitalismmod:cathode_precursor ×1 → capitalismmod:nmc_cathode ×1；设备 battery_materials；工人 3；能源 8；维护 12
- lfp_cell_assembly：capitalismmod:lfp_cathode ×1 + capitalismmod:graphite_anode ×1 + capitalismmod:battery_separator ×1 + capitalismmod:battery_electrolyte ×1 + capitalismmod:copper_foil ×1 → capitalismmod:battery_cell ×2；设备 battery_cell_line；工人 4；能源 9；维护 14
- nmc_cell_assembly：capitalismmod:nmc_cathode ×1 + capitalismmod:graphite_anode ×1 + capitalismmod:battery_separator ×1 + capitalismmod:battery_electrolyte ×1 + capitalismmod:copper_foil ×1 → capitalismmod:battery_cell ×2；设备 battery_cell_line；工人 4；能源 10；维护 15

### mineral_processing

- crushed_stone：minecraft:stone ×2 → minecraft:gravel ×2；设备 crusher；工人 2；能源 2；维护 4
- sand_screening：minecraft:gravel ×2 → minecraft:sand ×2；设备 flotation_cell；工人 2；能源 3；维护 5
- ore_washing：minecraft:raw_iron ×2 + minecraft:water_bucket ×1 → minecraft:iron_ore ×1；设备 flotation_cell；工人 3；能源 4；维护 7

### steelmaking

- electric_arc_steel：minecraft:iron_ingot ×2 + minecraft:coal ×1 → capitalismmod:steel_sheet ×2；设备 electric_arc_furnace；工人 3；能源 8；维护 12
- steel_casting：capitalismmod:steel_sheet ×1 → minecraft:iron_block ×1；设备 electric_arc_furnace；工人 3；能源 9；维护 14

### construction_materials

- cement_clinker：minecraft:stone ×2 + minecraft:coal ×1 → minecraft:brick ×2；设备 cement_kiln；工人 2；能源 5；维护 8
- glass_sand：minecraft:sand ×2 + minecraft:coal ×1 → minecraft:glass ×2；设备 cement_kiln；工人 2；能源 4；维护 7

### textiles

- spun_fiber：minecraft:string ×4 → minecraft:white_wool ×1；设备 textile_mill；工人 2；能源 3；维护 6
- leather_fabric：minecraft:leather ×2 + minecraft:string ×2 → minecraft:rabbit_hide ×2；设备 textile_mill；工人 2；能源 4；维护 7

### paper_products

- wood_pulp：minecraft:oak_log ×1 + minecraft:water_bucket ×1 → minecraft:paper ×4；设备 pulp_digester；工人 2；能源 3；维护 5
- bookbinding：minecraft:paper ×3 + minecraft:leather ×1 → minecraft:book ×1；设备 paper_mill；工人 2；能源 3；维护 6

### food_processing

- flour_milling：minecraft:wheat ×2 → capitalismmod:flour ×2；设备 milling_machine；工人 1；能源 2；维护 4
- bread_baking：capitalismmod:flour ×2 + minecraft:sugar ×1 → minecraft:bread ×2；设备 food_processor；工人 2；能源 3；维护 6
- pasteurized_milk：minecraft:milk_bucket ×1 → minecraft:honey_bottle ×1；设备 pasteurizer；工人 2；能源 4；维护 7
- canned_rations：minecraft:cooked_beef ×1 + capitalismmod:metal_can ×1 → capitalismmod:canned_food ×1；设备 cannery；工人 3；能源 5；维护 9

### basic_inorganic_chemicals

- air_compression：无输入 → capitalismmod:compressed_air ×2；设备 air_compressor；工人 1；能源 3；维护 5
- cryogenic_air_separation：capitalismmod:compressed_air ×2 → capitalismmod:nitrogen ×2 + capitalismmod:oxygen ×1 + capitalismmod:argon ×1；设备 cryogenic_air_separation；工人 3；能源 9；维护 13
- brine_preparation：capitalismmod:salt ×2 + minecraft:water_bucket ×1 → capitalismmod:brine ×2；设备 chemical_reactor；工人 1；能源 2；维护 4
- air_separation：capitalismmod:compressed_air ×2 → capitalismmod:nitrogen ×2 + capitalismmod:oxygen ×1 + capitalismmod:argon ×1；设备 cryogenic_air_separation；工人 2；能源 5；维护 8
- hydrochloric_acid_synthesis：capitalismmod:chlorine ×1 + capitalismmod:hydrogen ×1 + minecraft:water_bucket ×1 → capitalismmod:hydrochloric_acid ×1；设备 chemical_reactor；工人 2；能源 4；维护 7
- nitric_acid_synthesis：capitalismmod:ammonia ×1 + capitalismmod:oxygen ×2 + minecraft:water_bucket ×1 → capitalismmod:nitric_acid ×1；设备 chemical_reactor；工人 3；能源 6；维护 9
- sodium_hypochlorite_synthesis：capitalismmod:chlorine ×1 + capitalismmod:caustic_soda ×1 + minecraft:water_bucket ×1 → capitalismmod:sodium_hypochlorite ×2；设备 chemical_reactor；工人 2；能源 5；维护 8
- hydrogen_peroxide_synthesis：capitalismmod:hydrogen ×1 + capitalismmod:oxygen ×1 + minecraft:water_bucket ×1 → capitalismmod:hydrogen_peroxide ×1；设备 chemical_reactor；工人 2；能源 6；维护 9
- soda_ash_production：capitalismmod:salt ×1 + capitalismmod:limestone ×1 → capitalismmod:soda_ash ×2；设备 chemical_reactor；工人 2；能源 5；维护 8
- sodium_bicarbonate_production：capitalismmod:soda_ash ×1 + capitalismmod:carbon_dioxide ×1 + minecraft:water_bucket ×1 → capitalismmod:sodium_bicarbonate ×2；设备 chemical_reactor；工人 2；能源 4；维护 7
- calcium_chloride_production：capitalismmod:limestone ×1 + capitalismmod:hydrochloric_acid ×1 → capitalismmod:calcium_chloride ×2；设备 chemical_reactor；工人 2；能源 4；维护 7

### fertilizer_chemicals

- ammonia_synthesis：capitalismmod:hydrogen ×3 + capitalismmod:nitrogen ×1 → capitalismmod:ammonia ×2；设备 fertilizer_plant；工人 3；能源 7；维护 11
- urea_synthesis：capitalismmod:ammonia ×2 + capitalismmod:carbon_dioxide ×1 → capitalismmod:urea ×2；设备 fertilizer_plant；工人 3；能源 6；维护 10
- ammonium_nitrate_synthesis：capitalismmod:ammonia ×1 + capitalismmod:nitric_acid ×1 → capitalismmod:ammonium_nitrate ×2；设备 fertilizer_plant；工人 3；能源 6；维护 9
- phosphate_fertilizer_production：capitalismmod:phosphate ×2 + capitalismmod:sulfuric_acid ×1 → capitalismmod:phosphate_fertilizer ×2；设备 fertilizer_plant；工人 3；能源 7；维护 10
- phosphoric_acid_wet_process：capitalismmod:phosphate ×2 + capitalismmod:sulfuric_acid ×1 + minecraft:water_bucket ×1 → capitalismmod:phosphoric_acid ×2；设备 chemical_reactor；工人 3；能源 6；维护 10
- ammonium_sulfate_production：capitalismmod:ammonia ×1 + capitalismmod:sulfuric_acid ×1 → capitalismmod:ammonium_sulfate ×2；设备 fertilizer_plant；工人 3；能源 6；维护 9
- dap_fertilizer_granulation：capitalismmod:ammonia ×2 + capitalismmod:phosphoric_acid ×1 → capitalismmod:dap_fertilizer ×2；设备 fertilizer_plant；工人 3；能源 7；维护 10

### water_treatment

- activated_carbon：minecraft:coal ×2 → capitalismmod:activated_carbon ×2；设备 chemical_reactor；工人 1；能源 3；维护 5
- alum_coagulation_agent：capitalismmod:alumina ×1 + capitalismmod:sulfuric_acid ×1 → capitalismmod:alum ×2；设备 chemical_reactor；工人 2；能源 5；维护 8
- industrial_water_purification：minecraft:water_bucket ×1 + capitalismmod:alum ×1 + capitalismmod:activated_carbon ×1 → capitalismmod:industrial_water ×2；设备 chemical_reactor；工人 2；能源 6；维护 9
- chlorine_disinfectant：capitalismmod:sodium_hypochlorite ×1 + minecraft:water_bucket ×1 → capitalismmod:disinfectant ×2；设备 chemical_reactor；工人 2；能源 4；维护 7
- coagulation_and_settling：minecraft:water_bucket ×1 + capitalismmod:alum ×1 → capitalismmod:wastewater_sludge ×1；设备 chemical_reactor；工人 2；能源 4；维护 7
- sludge_carbonization：capitalismmod:wastewater_sludge ×2 → capitalismmod:activated_carbon ×1；设备 chemical_reactor；工人 2；能源 5；维护 8

### coatings_and_adhesives

- industrial_coating：capitalismmod:epoxy_resin ×1 + capitalismmod:industrial_dye ×1 → capitalismmod:industrial_coating ×2；设备 chemical_reactor；工人 2；能源 5；维护 8
- industrial_adhesive：capitalismmod:phenolic_resin ×1 + capitalismmod:epoxy_resin ×1 → capitalismmod:industrial_adhesive ×2；设备 polymer_reactor；工人 3；能源 6；维护 10
- industrial_dye：capitalismmod:benzene ×1 + minecraft:lapis_lazuli ×1 → capitalismmod:industrial_dye ×2；设备 chemical_reactor；工人 2；能源 5；维护 8

### biochemical_industry

- culture_medium_preparation：capitalismmod:glucose ×1 + minecraft:wheat ×1 + minecraft:water_bucket ×1 → capitalismmod:culture_medium ×2；设备 chemical_reactor；工人 2；能源 3；维护 6
- yeast_culture_propagation：capitalismmod:culture_medium ×1 → capitalismmod:yeast_culture ×2；设备 fermentation_tank；工人 2；能源 4；维护 7
- glucose_hydrolysis：minecraft:wheat ×2 + minecraft:water_bucket ×1 → capitalismmod:glucose ×2；设备 chemical_reactor；工人 2；能源 3；维护 5
- citric_acid_fermentation：capitalismmod:glucose ×2 → capitalismmod:citric_acid ×2；设备 chemical_reactor；工人 2；能源 4；维护 7
- bioethanol_fermentation：capitalismmod:glucose ×2 + capitalismmod:yeast_culture ×1 → capitalismmod:fermentation_broth ×2 + capitalismmod:carbon_dioxide ×1；设备 fermentation_tank；工人 2；能源 5；维护 8
- ethanol_distillation：capitalismmod:fermentation_broth ×2 → capitalismmod:ethanol ×2；设备 chemical_reactor；工人 2；能源 4；维护 7
- lactic_acid_fermentation：capitalismmod:glucose ×2 + capitalismmod:yeast_culture ×1 → capitalismmod:lactic_acid ×2；设备 fermentation_tank；工人 2；能源 5；维护 8
- pla_polymerization：capitalismmod:lactic_acid ×2 → capitalismmod:pla_pellets ×2；设备 polymer_reactor；工人 3；能源 6；维护 10
- biodegradable_packaging：capitalismmod:pla_pellets ×1 → capitalismmod:biodegradable_packaging ×3；设备 extrusion_line；工人 2；能源 4；维护 7

### pharmaceuticals

- pharmaceutical_excipient_blending：minecraft:wheat ×1 + capitalismmod:glucose ×1 → capitalismmod:pharmaceutical_excipient ×2；设备 pharmaceutical_reactor；工人 2；能源 3；维护 6
- salicylic_acid_synthesis：capitalismmod:phenol ×1 + capitalismmod:oxygen ×1 → capitalismmod:salicylic_acid ×1；设备 pharmaceutical_reactor；工人 3；能源 6；维护 10
- acetic_acid_bioconversion：capitalismmod:ethanol ×1 + capitalismmod:oxygen ×1 → capitalismmod:acetic_acid ×1；设备 fermentation_tank；工人 2；能源 4；维护 7
- aspirin_active_ingredient：capitalismmod:salicylic_acid ×1 + capitalismmod:acetic_acid ×1 → capitalismmod:active_pharmaceutical ×1；设备 pharmaceutical_reactor；工人 3；能源 7；维护 11
- aspirin_intermediate_synthesis：capitalismmod:salicylic_acid ×1 + capitalismmod:acetone ×1 → capitalismmod:aspirin_intermediate ×1；设备 pharmaceutical_reactor；工人 3；能源 7；维护 10
- painkiller_tablet_forming：capitalismmod:active_pharmaceutical ×1 + capitalismmod:pharmaceutical_excipient ×1 → capitalismmod:painkiller_tablet ×2；设备 tablet_press；工人 2；能源 5；维护 8
- tablet_film_coating：capitalismmod:painkiller_tablet ×2 + capitalismmod:industrial_coating ×1 → capitalismmod:coated_tablet ×2；设备 pharmaceutical_reactor；工人 2；能源 5；维护 8
- sterile_solution_filling：capitalismmod:active_pharmaceutical ×1 + capitalismmod:industrial_water ×1 → capitalismmod:sterile_solution ×2；设备 sterile_filling_line；工人 3；能源 6；维护 10
- active_pharmaceutical_synthesis：capitalismmod:phenol ×1 + capitalismmod:sulfuric_acid ×1 → capitalismmod:active_pharmaceutical ×1；设备 pharmaceutical_reactor；工人 3；能源 7；维护 11
- antibiotic_tablet_forming：capitalismmod:active_pharmaceutical ×1 + minecraft:paper ×1 → capitalismmod:antibiotic_tablet ×2；设备 pharmaceutical_reactor；工人 2；能源 5；维护 8

### textile_chemistry

- peroxide_bleaching_agent：capitalismmod:hydrogen_peroxide ×1 + capitalismmod:caustic_soda ×1 → capitalismmod:bleaching_agent ×2；设备 chemical_reactor；工人 2；能源 4；维护 7
- fiber_bleaching：minecraft:string ×2 + capitalismmod:bleaching_agent ×1 → capitalismmod:dyed_fiber ×2；设备 textile_mill；工人 2；能源 4；维护 7
- fiber_dyeing：capitalismmod:pet_fiber ×1 + capitalismmod:industrial_dye ×1 → capitalismmod:dyed_fiber ×2；设备 textile_mill；工人 2；能源 4；维护 7
- textile_finishing：capitalismmod:dyed_fiber ×2 + capitalismmod:industrial_coating ×1 → capitalismmod:finished_textile ×2；设备 textile_mill；工人 2；能源 5；维护 8

### fine_chemicals_and_solvents

- ethyl_acetate_esterification：capitalismmod:ethanol ×1 + capitalismmod:acetic_acid ×1 → capitalismmod:ethyl_acetate ×2；设备 chemical_reactor；工人 2；能源 5；维护 8
- solvent_blend_formulation：capitalismmod:acetone ×1 + capitalismmod:ethanol ×1 + capitalismmod:ethyl_acetate ×1 → capitalismmod:solvent_blend ×3；设备 solvent_recovery_unit；工人 2；能源 4；维护 7

### crop_protection_chemicals

- pesticide_active_synthesis：capitalismmod:phenol ×1 + capitalismmod:chlorine ×1 → capitalismmod:pesticide_active ×1；设备 pesticide_reactor；工人 3；能源 7；维护 11
- herbicide_formulation：capitalismmod:pesticide_active ×1 + capitalismmod:caustic_soda ×1 → capitalismmod:herbicide ×2；设备 pesticide_reactor；工人 2；能源 5；维护 8
- fungicide_formulation：capitalismmod:pesticide_active ×1 + capitalismmod:sulfuric_acid ×1 → capitalismmod:fungicide ×2；设备 pesticide_reactor；工人 2；能源 6；维护 9

### rubber_and_tire_manufacturing

- carbon_black_production：capitalismmod:refinery_gas ×2 + minecraft:coal ×1 → capitalismmod:carbon_black ×2；设备 chemical_reactor；工人 2；能源 5；维护 8
- tire_compounding_and_vulcanization：capitalismmod:synthetic_rubber ×2 + capitalismmod:carbon_black ×1 + capitalismmod:sulfur ×1 → capitalismmod:tire ×2；设备 tire_press；工人 3；能源 8；维护 12
- rubber_sealant_formulation：capitalismmod:synthetic_rubber ×1 + capitalismmod:phenolic_resin ×1 → capitalismmod:industrial_sealant ×2；设备 polymer_reactor；工人 2；能源 6；维护 9

### electronic_chemicals

- hydrofluoric_acid_preparation：capitalismmod:fluorite ×1 + capitalismmod:sulfuric_acid ×1 → capitalismmod:hydrofluoric_acid ×1；设备 electronic_chemical_unit；工人 3；能源 7；维护 11
- electronic_solvent_purification：capitalismmod:acetone ×1 + capitalismmod:ethyl_acetate ×1 → capitalismmod:electronic_solvent ×2；设备 solvent_recovery_unit；工人 2；能源 5；维护 8
- electronic_etchant_blending：capitalismmod:sulfuric_acid ×1 + capitalismmod:hydrogen_peroxide ×1 → capitalismmod:electronic_etchant ×2；设备 electronic_chemical_unit；工人 3；能源 6；维护 10

### paper_chemicals

- paper_coating_compound：capitalismmod:epoxy_resin ×1 + capitalismmod:alum ×1 → capitalismmod:paper_coating ×2；设备 chemical_reactor；工人 2；能源 5；维护 8
- printing_ink_formulation：capitalismmod:industrial_dye ×1 + capitalismmod:solvent_blend ×1 → capitalismmod:printing_ink ×2；设备 chemical_reactor；工人 2；能源 4；维护 7
- paper_printing：capitalismmod:coated_paper ×1 + capitalismmod:printing_ink ×1 → capitalismmod:printed_paper ×2；设备 paper_coating_line；工人 2；能源 4；维护 6

### surfactants_and_personal_care

- surfactant_synthesis：capitalismmod:ethylene_oxide ×1 + capitalismmod:sulfuric_acid ×1 → capitalismmod:surfactant ×2；设备 surfactant_reactor；工人 3；能源 6；维护 10
- detergent_formulation：capitalismmod:surfactant ×1 + capitalismmod:soda_ash ×1 → capitalismmod:detergent ×2；设备 surfactant_reactor；工人 2；能源 5；维护 8
- cleaning_agent_blending：capitalismmod:detergent ×1 + capitalismmod:industrial_water ×1 → capitalismmod:cleaning_agent ×2；设备 chemical_reactor；工人 2；能源 4；维护 7

### carbon_circular_chemicals

- carbon_capture_and_purification：minecraft:coal ×2 + minecraft:water_bucket ×1 → capitalismmod:carbon_dioxide ×2；设备 carbon_capture_unit；工人 3；能源 7；维护 11
- co2_hydrogenation_to_methanol：capitalismmod:carbon_dioxide ×1 + capitalismmod:hydrogen ×3 → capitalismmod:methanol ×1；设备 methanol_synthesis_unit；工人 3；能源 8；维护 12

### industrial_gases

- medical_oxygen_purification：capitalismmod:oxygen ×1 → capitalismmod:medical_oxygen ×1；设备 chemical_reactor；工人 2；能源 4；维护 7
- argon_welding_gas：capitalismmod:argon ×1 + capitalismmod:oxygen ×1 → capitalismmod:welding_gas ×2；设备 chemical_reactor；工人 2；能源 4；维护 7
- food_grade_co2_purification：capitalismmod:carbon_dioxide ×1 → capitalismmod:food_grade_carbon_dioxide ×1；设备 chemical_reactor；工人 2；能源 3；维护 6

### agrochemicals

- nitrogen_fertilizer：minecraft:bone ×1 + minecraft:coal ×1 → minecraft:bone_meal ×3；设备 fertilizer_plant；工人 2；能源 5；维护 8
- soil_conditioner：minecraft:bone_meal ×2 + minecraft:rotten_flesh ×1 → minecraft:bone_meal ×4；设备 fertilizer_plant；工人 2；能源 5；维护 9

### precision_machinery

- precision_part：minecraft:iron_ingot ×2 + minecraft:diamond ×1 → minecraft:anvil ×1；设备 cnc_machining_center；工人 3；能源 7；维护 11
- tooling：minecraft:iron_ingot ×1 + minecraft:redstone ×1 → minecraft:iron_nugget ×8；设备 cnc_machining_center；工人 2；能源 5；维护 8

### utilities

- coal_generation：minecraft:coal ×1 → 服务收入 60；设备 none；工人 1；能源 0；维护 0
- fuel_generation：capitalismmod:fuel_oil ×1 → 服务收入 90；设备 none；工人 1；能源 0；维护 0
- diesel_generation：capitalismmod:diesel ×1 → 服务收入 100；设备 none；工人 1；能源 0；维护 0
- lpg_generation：capitalismmod:lpg ×1 → 服务收入 92；设备 none；工人 1；能源 0；维护 0

### construction

- construction（默认配方，未能从源码提取详细字段）

### transport

- coal_transport：minecraft:coal ×1 → 服务收入 45；设备 none；工人 1；能源 0；维护 0
- fuel_transport：capitalismmod:fuel_oil ×1 → 服务收入 70；设备 none；工人 1；能源 0；维护 0
- diesel_transport：capitalismmod:diesel ×1 → 服务收入 82；设备 none；工人 1；能源 0；维护 0
- gasoline_transport：capitalismmod:gasoline ×1 → 服务收入 78；设备 none；工人 1；能源 0；维护 0
- lpg_transport：capitalismmod:lpg ×1 → 服务收入 74；设备 none；工人 1；能源 0；维护 0

### hospitality

- hospitality（默认配方，未能从源码提取详细字段）

### retail

- retail（默认配方，未能从源码提取详细字段）

### it_services

- it_services（默认配方，未能从源码提取详细字段）

### research

- research（默认配方，未能从源码提取详细字段）

### healthcare

- healthcare（默认配方，未能从源码提取详细字段）

### real_estate

- real_estate（默认配方，未能从源码提取详细字段）

### business_services

- business_services（默认配方，未能从源码提取详细字段）

### environment

- environment（默认配方，未能从源码提取详细字段）

### consumer_services

- consumer_services（默认配方，未能从源码提取详细字段）

### education

- education（默认配方，未能从源码提取详细字段）

### culture

- culture（默认配方，未能从源码提取详细字段）

### public_admin

- public_admin（默认配方，未能从源码提取详细字段）

### intl_org

- intl_org（默认配方，未能从源码提取详细字段）

### finance

- finance（默认配方，未能从源码提取详细字段）
