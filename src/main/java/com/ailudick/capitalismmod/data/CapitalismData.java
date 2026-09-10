package com.ailudick.capitalismmod.data;

import com.ailudick.capitalismmod.CapitalismMod;
import com.ailudick.capitalismmod.company.IndustrySpec;
import com.ailudick.capitalismmod.company.ProductionRecipe;
import com.ailudick.capitalismmod.company.MachineType;
import com.ailudick.capitalismmod.shop.ShopOffer;
import com.ailudick.capitalismmod.stock.Stock;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Loads data-driven config (shop offers, commodities, stocks) from config/capitalismmod/*.json.
 * Missing files are created with defaults so pack authors can edit them.
 */
public final class CapitalismData {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Set<String> REMOVED_RESERVED_STOCKS = Set.of(
            "mining", "tech", "energy", "agriculture");

    private static List<ShopOffer> shopOffers = new ArrayList<>();
    private static List<ItemStack> commodities = new ArrayList<>();
    private static List<String> commodityItemIds = new ArrayList<>();
    private static Map<String, Long> commodityPrices = new HashMap<>();
    private static List<Stock> stocks = new ArrayList<>();
    private static List<IndustrySpec> industries = new ArrayList<>();

    private CapitalismData() {
    }

    /** Must be called early (first line of the mod constructor), before any data class is loaded. */
    public static void load() {
        Path dir = Path.of("config", "capitalismmod");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            CapitalismMod.LOGGER.error("Failed to create config directory", e);
        }
        shopOffers = loadShopOffers(dir);
        commodities = loadCommodities(dir);
        stocks = loadStocks(dir);
        industries = loadIndustries(dir);
    }

    public static List<ShopOffer> getShopOffers() {
        return shopOffers;
    }

    public static List<ItemStack> getCommodities() {
        // Custom items are registered after the initial config load. Resolve the
        // configured ids lazily so they also appear in the commodity exchange.
        List<ItemStack> resolved = new ArrayList<>();
        for (String id : commodityItemIds) {
            try {
                Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
                if (item != null && item != Items.AIR) resolved.add(new ItemStack(item));
            } catch (Exception ignored) {
                // Invalid ids are already filtered by load-time validation.
            }
        }
        return resolved.isEmpty() ? commodities : resolved;
    }

    /** itemId (e.g. "minecraft:diamond") -> initial price per unit, in USD. */
    public static Map<String, Long> getCommodityPrices() {
        return commodityPrices;
    }

    public static List<Stock> getStocks() {
        return stocks;
    }

    public static List<IndustrySpec> getIndustries() {
        return industries;
    }

    private static List<ShopOffer> loadShopOffers(Path dir) {
        List<ShopOfferJson> defaults = defaultShopOffers();
        List<ShopOfferJson> raw = new ArrayList<>(read(dir.resolve("shop_offers.json"), ShopOfferJson[].class, defaults));
        Set<String> configuredOffers = new java.util.HashSet<>();
        for (ShopOfferJson offer : raw) {
            if (offer != null && offer.item != null && !offer.item.isBlank()) {
                configuredOffers.add(offer.item + "|" + offer.currency);
            }
        }
        for (ShopOfferJson offer : defaults) {
            if (offer != null && configuredOffers.add(offer.item + "|" + offer.currency)) raw.add(offer);
        }
        List<ShopOffer> result = new ArrayList<>();
        for (ShopOfferJson j : raw) {
            Item item = parseItem(j.item);
            if (item == null) {
                continue;
            }
            result.add(new ShopOffer(new ItemStack(item, j.quantity), j.price, j.currency));
        }
        return result;
    }

    /** Validates recipe equipment at config-load time instead of first production attempt. */
    private static void validateIndustryMachines(List<IndustrySpec> industries) {
        for (IndustrySpec industry : industries) {
            if (industry == null) continue;
            if (MachineType.parse(industry.machineType()) == null) {
                CapitalismMod.LOGGER.error("Unknown machine type '{}' on industry '{}'", 
                        industry.machineType(), industry.id());
            }
            validateQuantities(industry.id(), "industry", industry.inputs(), industry.outputs());
            Set<String> recipeIds = new java.util.HashSet<>();
            for (ProductionRecipe recipe : industry.recipes()) {
                if (recipe == null) continue;
                if (MachineType.parse(recipe.machineType()) == null) {
                    CapitalismMod.LOGGER.error("Unknown machine type '{}' on recipe '{}' (industry '{}')",
                            recipe.machineType(), recipe.id(), industry.id());
                }
                if (!recipeIds.add(recipe.id())) {
                    CapitalismMod.LOGGER.warn("Duplicate recipe id '{}' on industry '{}'",
                            recipe.id(), industry.id());
                }
                validateQuantities(industry.id(), "recipe " + recipe.id(), recipe.inputs(), recipe.outputs());
                if (recipe.income() < 0L) {
                    CapitalismMod.LOGGER.error("Negative income {} on recipe '{}' (industry '{}')",
                            recipe.income(), recipe.id(), industry.id());
                }
                if (recipe.outputs().isEmpty() && recipe.income() <= 0L) {
                    CapitalismMod.LOGGER.warn("Recipe '{}' on industry '{}' has no outputs and no positive service income",
                            recipe.id(), industry.id());
                }
            }
        }
    }

    private static void validateQuantities(String industryId, String source,
                                           Map<String, Integer> inputs, Map<String, Integer> outputs) {
        validateQuantityMap(industryId, source + " input", inputs);
        validateQuantityMap(industryId, source + " output", outputs);
    }

    private static void validateQuantityMap(String industryId, String source, Map<String, Integer> values) {
        if (values == null) return;
        for (Map.Entry<String, Integer> entry : values.entrySet()) {
            String itemId = entry.getKey();
            Integer quantity = entry.getValue();
            if (itemId == null || itemId.isBlank()) {
                CapitalismMod.LOGGER.error("Blank item id on {} (industry '{}')", source, industryId);
            }
            if (quantity == null || quantity <= 0) {
                CapitalismMod.LOGGER.error("Non-positive quantity {} for item '{}' on {} (industry '{}')",
                        quantity, itemId, source, industryId);
            }
        }
    }

    private static List<ItemStack> loadCommodities(Path dir) {
        List<CommodityJson> defaults = defaultCommodities();
        List<CommodityJson> configured = read(dir.resolve("commodities.json"), CommodityJson[].class, defaults);
        List<CommodityJson> raw = new ArrayList<>(configured);
        Set<String> configuredIds = new java.util.HashSet<>();
        for (CommodityJson commodity : raw) {
            if (commodity != null && commodity.item != null && !commodity.item.isBlank()) {
                configuredIds.add(commodity.item);
            }
        }
        for (CommodityJson commodity : defaults) {
            if (commodity != null && commodity.item != null && configuredIds.add(commodity.item)) {
                raw.add(commodity);
            }
        }
        List<ItemStack> result = new ArrayList<>();
        Map<String, Long> prices = new HashMap<>();
        List<String> itemIds = new ArrayList<>();
        for (CommodityJson j : raw) {
            // Keep configured prices even before custom mod items are registered;
            // production and market data are loaded before the NeoForge registry event.
            if (j.item != null && !j.item.isBlank()) {
                itemIds.add(j.item);
                prices.put(j.item, Math.max(1L, j.initial_price));
            }
            Item item = parseItem(j.item);
            if (item != null) {
                result.add(new ItemStack(item));
                prices.put(BuiltInRegistries.ITEM.getKey(item).toString(), j.initial_price);
            }
        }
        commodityPrices = prices;
        commodityItemIds = itemIds;
        return result;
    }

    private static List<Stock> loadStocks(Path dir) {
        List<StockJson> defaults = defaultStocks();
        List<StockJson> raw = new ArrayList<>(read(dir.resolve("stocks.json"), StockJson[].class, defaults));
        Set<String> configuredStockIds = new java.util.HashSet<>();
        for (StockJson stock : raw) {
            if (stock != null && stock.id != null && !stock.id.isBlank()) configuredStockIds.add(stock.id);
        }
        for (StockJson stock : defaults) {
            if (stock != null && configuredStockIds.add(stock.id)) raw.add(stock);
        }
        List<Stock> result = new ArrayList<>();
        for (StockJson j : raw) {
            if (REMOVED_RESERVED_STOCKS.contains(j.id)) {
                continue;
            }
            // Accept the old field name so existing worlds keep their US tab
            // classification after live-data support was removed.
            if (j.us_market || j.real_market || isFormerUsStock(j.id)) continue;
            result.add(new Stock(j.id, j.name_key, j.initial_price));
        }
        if (result.stream().noneMatch(stock -> stock.id().equals("test_company"))) {
            result.add(new Stock("test_company", "stock.capitalismmod.test_company", 100));
        }
        return result;
    }

    private static boolean isFormerUsStock(String id) {
        return Set.of("AAPL", "MSFT", "NVDA", "AMZN", "GOOGL", "TSLA").contains(id);
    }

    private static List<IndustrySpec> loadIndustries(Path dir) {
        List<IndustryJson> defaults = defaultIndustries();
        List<IndustryJson> raw = read(dir.resolve("industries.json"), IndustryJson[].class, defaults);
        Map<String, IndustryJson> defaultById = new HashMap<>();
        for (IndustryJson industry : defaults) defaultById.put(industry.id, industry);
        List<IndustrySpec> result = new ArrayList<>();
        for (IndustryJson j : raw) {
            // Merge maintained recipes by ID. This keeps pack-author recipes intact
            // while making newly added official recipes available to existing worlds.
            IndustryJson maintained = defaultById.get(j.id);
            if (maintained != null) {
                // Older generated configs used zero workers for service industries
                // because they had no item outputs. Upgrade only an unchanged
                // legacy default; keep deliberately customized labor settings.
                if ((j.recipes == null || j.recipes.isEmpty())
                        && j.workers_per_cycle == 0
                        && maintained.workers_per_cycle > 0
                        && j.income == maintained.income
                        && java.util.Objects.equals(j.inputs, maintained.inputs)
                        && java.util.Objects.equals(j.outputs, maintained.outputs)) {
                    j.workers_per_cycle = maintained.workers_per_cycle;
                }
                List<RecipeJson> configured = j.recipes == null
                        ? new ArrayList<>() : new ArrayList<>(j.recipes);
                for (RecipeJson recipe : configured) {
                    upgradeLegacyPolymerRecipe(recipe);
                    upgradeLegacyDisplayRecipe(recipe);
                    IndustrialChainRecipeMigration.upgrade(j.id, recipe, maintained.recipes);
                }
                Set<String> configuredIds = new java.util.HashSet<>();
                for (RecipeJson recipe : configured) {
                    if (recipe != null && recipe.id != null) configuredIds.add(recipe.id);
                }
                for (RecipeJson recipe : maintained.recipes) {
                    if (recipe != null && configuredIds.add(recipe.id)) configured.add(recipe);
                }
                j.recipes = configured;
            }
            List<ProductionRecipe> recipes = new ArrayList<>();
            if (j.recipes != null) {
                for (RecipeJson recipe : j.recipes) {
                    recipes.add(new ProductionRecipe(recipe.id, recipe.inputs, recipe.outputs, recipe.income,
                            recipe.machine_type, recipe.workers_per_cycle, recipe.energy_cost,
                            recipe.maintenance_cost, recipe.material_class, recipe.quality_tier,
                            recipe.byproducts, recipe.pollution_score, recipe.hazardous));
                }
            }
            result.add(recipes.isEmpty()
                    ? new IndustrySpec(j.id, j.inputs, j.outputs, j.income, j.machine_type,
                    j.workers_per_cycle, j.energy_cost, j.maintenance_cost)
                    : new IndustrySpec(j.id, j.inputs, j.outputs, j.income, j.machine_type,
                    j.workers_per_cycle, j.energy_cost, j.maintenance_cost, recipes));
        }
        // Add newly shipped official chains to existing config files without
        // overwriting pack-author industries already present in the file.
        Set<String> configuredIndustryIds = new java.util.HashSet<>();
        for (IndustryJson industry : raw) if (industry != null && industry.id != null) configuredIndustryIds.add(industry.id);
        for (IndustryJson maintained : defaults) {
            if (maintained != null && maintained.id != null && configuredIndustryIds.add(maintained.id)) {
                List<ProductionRecipe> recipes = new ArrayList<>();
                if (maintained.recipes != null) for (RecipeJson recipe : maintained.recipes)
                    recipes.add(new ProductionRecipe(recipe.id, recipe.inputs, recipe.outputs, recipe.income,
                            recipe.machine_type, recipe.workers_per_cycle, recipe.energy_cost, recipe.maintenance_cost,
                            recipe.material_class, recipe.quality_tier, recipe.byproducts, recipe.pollution_score, recipe.hazardous));
                result.add(recipes.isEmpty() ? new IndustrySpec(maintained.id, maintained.inputs, maintained.outputs,
                        maintained.income, maintained.machine_type, maintained.workers_per_cycle,
                        maintained.energy_cost, maintained.maintenance_cost) : new IndustrySpec(maintained.id,
                        maintained.inputs, maintained.outputs, maintained.income, maintained.machine_type,
                        maintained.workers_per_cycle, maintained.energy_cost, maintained.maintenance_cost, recipes));
            }
        }
        validateIndustryMachines(result);
        return result;
    }

    private static Item parseItem(String id) {
        try {
            ResourceLocation rl = ResourceLocation.parse(id);
            Item item = BuiltInRegistries.ITEM.get(rl);
            if (item == null || item == Items.AIR) {
                CapitalismMod.LOGGER.warn("Unknown item in config: {}", id);
                return null;
            }
            return item;
        } catch (Exception e) {
            CapitalismMod.LOGGER.warn("Invalid item id in config: {}", id);
            return null;
        }
    }

    /**
     * Corrects the first-generation polymer recipes without overwriting a
     * server owner's deliberately customized recipe outputs.
     */
    private static void upgradeLegacyPolymerRecipe(RecipeJson recipe) {
        if (recipe == null || recipe.id == null || recipe.outputs == null) return;
        recipe.outputs = PolymerRecipeMigration.upgradeOutputs(recipe.id, recipe.outputs);
    }

    /** Upgrade only the original built-in display recipe, preserving custom recipes. */
    private static void upgradeLegacyDisplayRecipe(RecipeJson recipe) {
        if (recipe == null || !"display_panel".equals(recipe.id)
                || recipe.inputs == null || recipe.outputs == null) return;
        if (DisplayRecipeMigration.isLegacyDefault(recipe.id, recipe.inputs, recipe.outputs)) {
            recipe.inputs = new HashMap<>(DisplayRecipeMigration.upgradedInputs());
            recipe.income = 390;
            recipe.machine_type = "electronics_assembly";
            recipe.workers_per_cycle = 4;
            recipe.energy_cost = 8;
            recipe.maintenance_cost = 15;
        }
    }

    private static <T> List<T> read(Path file, Class<T[]> arrayClass, List<T> defaults) {
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file)) {
                T[] arr = GSON.fromJson(reader, arrayClass);
                if (arr == null) {
                    return new ArrayList<>(defaults);
                }
                return new ArrayList<>(List.of(arr));
            } catch (Exception e) {
                CapitalismMod.LOGGER.error("Failed to read {}", file, e);
                return new ArrayList<>(defaults);
            }
        } else {
            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(defaults, writer);
            } catch (IOException e) {
                CapitalismMod.LOGGER.error("Failed to write {}", file, e);
            }
            return new ArrayList<>(defaults);
        }
    }

    private static List<ShopOfferJson> defaultShopOffers() {
        return List.of(
                new ShopOfferJson("minecraft:diamond", 1, 10, "usd"),
                new ShopOfferJson("minecraft:iron_ingot", 8, 20, "cny"),
                new ShopOfferJson("minecraft:emerald", 1, 5, "eur"),
                new ShopOfferJson("minecraft:golden_apple", 1, 50, "rub")
        );
    }

    private static List<CommodityJson> defaultCommodities() {
        return List.of(
                new CommodityJson("minecraft:diamond", 100),
                new CommodityJson("minecraft:gold_ingot", 40),
                new CommodityJson("minecraft:iron_ingot", 20),
                new CommodityJson("minecraft:emerald", 50),
                new CommodityJson("minecraft:coal", 10),
                new CommodityJson("minecraft:wheat", 5),
                new CommodityJson("minecraft:rail", 30),
                new CommodityJson("capitalismmod:steel_sheet", 35),
                new CommodityJson("capitalismmod:copper_wire", 20),
                new CommodityJson("capitalismmod:glass_lens", 25),
                new CommodityJson("capitalismmod:electric_lamp", 120),
                new CommodityJson("capitalismmod:flour", 8),
                new CommodityJson("capitalismmod:metal_can", 18),
                new CommodityJson("capitalismmod:canned_food", 50),
                new CommodityJson("capitalismmod:machine_frame", 90),
                new CommodityJson("capitalismmod:electric_motor", 110),
                new CommodityJson("capitalismmod:green_lumber", 12),
                new CommodityJson("capitalismmod:dried_lumber", 28),
                new CommodityJson("capitalismmod:wooden_crate", 65),
                new CommodityJson("capitalismmod:furniture", 180),
                new CommodityJson("capitalismmod:syngas", 45),
                new CommodityJson("capitalismmod:reformate", 75),
                new CommodityJson("capitalismmod:c4_fraction", 68),
                new CommodityJson("capitalismmod:ethylbenzene", 110),
                new CommodityJson("capitalismmod:acrylonitrile", 130),
                new CommodityJson("capitalismmod:bisphenol_a", 150),
                new CommodityJson("capitalismmod:epichlorohydrin", 155),
                new CommodityJson("capitalismmod:crude_oil", 35),
                new CommodityJson("capitalismmod:naphtha", 55),
                new CommodityJson("capitalismmod:ethylene", 72),
                new CommodityJson("capitalismmod:propylene", 75),
                new CommodityJson("capitalismmod:fuel_oil", 45),
                new CommodityJson("capitalismmod:diesel", 82),
                new CommodityJson("capitalismmod:gasoline", 88),
                new CommodityJson("capitalismmod:lpg", 68),
                new CommodityJson("capitalismmod:base_oil", 72),
                new CommodityJson("capitalismmod:lubricant", 95),
                new CommodityJson("capitalismmod:asphalt", 42),
                new CommodityJson("capitalismmod:plastic_pellets", 18),
                new CommodityJson("capitalismmod:polyethylene_pellets", 22),
                new CommodityJson("capitalismmod:polypropylene_pellets", 24),
                new CommodityJson("capitalismmod:synthetic_rubber", 48),
                new CommodityJson("capitalismmod:ethylene_glycol", 62),
                new CommodityJson("capitalismmod:epoxy_resin", 78),
                new CommodityJson("capitalismmod:benzene", 74),
                new CommodityJson("capitalismmod:styrene_monomer", 92),
                new CommodityJson("capitalismmod:abs_resin", 115),
                new CommodityJson("capitalismmod:p_xylene", 86),
                new CommodityJson("capitalismmod:terephthalic_acid", 102),
                new CommodityJson("capitalismmod:pet_resin", 138),
                new CommodityJson("capitalismmod:pet_bottle", 165),
                new CommodityJson("capitalismmod:recycled_pet_flakes", 92),
                new CommodityJson("capitalismmod:fan_blades", 60),
                new CommodityJson("capitalismmod:fan_control", 50),
                new CommodityJson("capitalismmod:polysilicon", 125),
                new CommodityJson("capitalismmod:silicon_wafer", 160),
                new CommodityJson("capitalismmod:silicon_die", 185),
                new CommodityJson("capitalismmod:tested_die", 235),
                new CommodityJson("capitalismmod:lead_frame", 62),
                new CommodityJson("capitalismmod:mold_compound", 88),
                new CommodityJson("capitalismmod:packaged_chip", 310),
                new CommodityJson("capitalismmod:circuit_board", 260),
                new CommodityJson("capitalismmod:pcb_substrate", 95),
                new CommodityJson("capitalismmod:drilled_pcb_panel", 125),
                new CommodityJson("capitalismmod:etched_pcb", 165),
                new CommodityJson("capitalismmod:solder_masked_pcb", 205),
                new CommodityJson("capitalismmod:copper_foil", 48),
                new CommodityJson("capitalismmod:solder", 38),
                new CommodityJson("capitalismmod:smd_components", 145),
                new CommodityJson("capitalismmod:passive_components", 125),
                new CommodityJson("capitalismmod:power_management_ic", 260),
                new CommodityJson("capitalismmod:display_driver", 285),
                new CommodityJson("capitalismmod:display_panel", 330),
                new CommodityJson("capitalismmod:display_glass_substrate", 145),
                new CommodityJson("capitalismmod:backlight_module", 190),
                new CommodityJson("capitalismmod:phone_casing", 75),
                new CommodityJson("capitalismmod:camera_module", 180),
                new CommodityJson("capitalismmod:speaker_module", 85),
                new CommodityJson("capitalismmod:microphone_module", 70),
                new CommodityJson("capitalismmod:charging_port", 90),
                new CommodityJson("capitalismmod:keyboard_module", 115),
                new CommodityJson("capitalismmod:storage_module", 240),
                new CommodityJson("capitalismmod:wireless_module", 185),
                new CommodityJson("capitalismmod:battery_cell", 55),
                new CommodityJson("capitalismmod:lithium_mineral", 65),
                new CommodityJson("capitalismmod:lithium_carbonate", 115),
                new CommodityJson("capitalismmod:graphite_anode", 85),
                new CommodityJson("capitalismmod:cathode_active_material", 145),
                new CommodityJson("capitalismmod:battery_separator", 42),
                new CommodityJson("capitalismmod:battery_electrolyte", 75),
                new CommodityJson("capitalismmod:battery_pack", 420),
                new CommodityJson("capitalismmod:black_mass", 155),
                new CommodityJson("capitalismmod:battery", 95),
                new CommodityJson("capitalismmod:smartphone", 650),
                new CommodityJson("capitalismmod:electric_fan", 240)
                ,new CommodityJson("capitalismmod:power_adapter", 180)
                ,new CommodityJson("capitalismmod:television", 820)
                ,new CommodityJson("capitalismmod:laptop", 1150)
                ,new CommodityJson("capitalismmod:wireless_router", 420)
                ,new CommodityJson("capitalismmod:power_bank", 360)
                ,new CommodityJson("capitalismmod:smart_speaker", 480)
                ,new CommodityJson("capitalismmod:refurbished_smartphone", 390)
                ,new CommodityJson("capitalismmod:refurbished_television", 490)
                ,new CommodityJson("capitalismmod:refurbished_laptop", 690)
                ,new CommodityJson("capitalismmod:refurbished_wireless_router", 250)
                ,new CommodityJson("capitalismmod:plastic_container", 105)
                ,new CommodityJson("capitalismmod:packaging_film", 70)
        );
    }

    private static List<StockJson> defaultStocks() {
        return List.of(new StockJson("test_company", "stock.capitalismmod.test_company", 100));
    }

    // JSON types (public fields for Gson deserialization).
    public static class ShopOfferJson {
        public String item = "minecraft:diamond";
        public int quantity = 1;
        public int price = 10;
        public String currency = "usd";

        public ShopOfferJson() {
        }

        public ShopOfferJson(String item, int quantity, int price, String currency) {
            this.item = item;
            this.quantity = quantity;
            this.price = price;
            this.currency = currency;
        }
    }

    public static class CommodityJson {
        public String item = "minecraft:diamond";
        public long initial_price = 100;

        public CommodityJson() {
        }

        public CommodityJson(String item) {
            this.item = item;
        }

        public CommodityJson(String item, long initialPrice) {
            this.item = item;
            this.initial_price = initialPrice;
        }
    }

    public static class StockJson {
        public String id = "mining";
        public String name_key = "stock.capitalismmod.mining";
        public long initial_price = 100;
        public boolean us_market = false;
        /** Legacy config compatibility; this no longer enables any network feed. */
        public boolean real_market = false;

        public StockJson() {
        }

        public StockJson(String id, String name_key, long initial_price) {
            this.id = id;
            this.name_key = name_key;
            this.initial_price = initial_price;
        }

    }

    private static List<IndustryJson> defaultIndustries() {
        return List.of(
                withRecipes(new IndustryJson("mining", Map.of("minecraft:iron_ore", 1), Map.of("minecraft:iron_ingot", 1), 55, "ore_processor", 2, 1, 2),
                        new RecipeJson("iron_ingot", Map.of("minecraft:iron_ore", 1), Map.of("minecraft:iron_ingot", 1), 55, "ore_processor", 2, 1, 2),
                        new RecipeJson("copper_ingot", Map.of("minecraft:copper_ore", 1), Map.of("minecraft:copper_ingot", 1), 50, "ore_processor", 2, 1, 2),
                        new RecipeJson("gold_ingot", Map.of("minecraft:gold_ore", 1), Map.of("minecraft:gold_ingot", 1), 65, "ore_processor", 2, 1, 2),
                        new RecipeJson("iron_concentrate", Map.of("minecraft:iron_ore", 2), Map.of("minecraft:raw_iron", 1), 38, "ore_processor", 2, 1, 2),
                        new RecipeJson("copper_concentrate", Map.of("minecraft:copper_ore", 2), Map.of("minecraft:raw_copper", 1), 35, "ore_processor", 2, 1, 2),
                        new RecipeJson("gold_concentrate", Map.of("minecraft:gold_ore", 2), Map.of("minecraft:raw_gold", 1), 48, "ore_processor", 2, 1, 2),
                        new RecipeJson("iron_smelting", Map.of("minecraft:raw_iron", 1, "minecraft:coal", 1), Map.of("minecraft:iron_ingot", 1), 52, "blast_furnace", 2, 3, 5),
                        new RecipeJson("copper_smelting", Map.of("minecraft:raw_copper", 1, "minecraft:coal", 1), Map.of("minecraft:copper_ingot", 1), 48, "blast_furnace", 2, 3, 5),
                        new RecipeJson("gold_smelting", Map.of("minecraft:raw_gold", 1, "minecraft:coal", 1), Map.of("minecraft:gold_ingot", 1), 62, "blast_furnace", 2, 3, 5),
                        new RecipeJson("bauxite_refining", Map.of("capitalismmod:raw_bauxite", 2, "capitalismmod:caustic_soda", 1, "minecraft:water_bucket", 1), Map.of("capitalismmod:alumina", 1), 82, "ore_processor", 2, 4, 6),
                        new RecipeJson("nickel_smelting", Map.of("capitalismmod:raw_nickel", 1, "minecraft:coal", 1), Map.of("capitalismmod:nickel_ingot", 1), 92, "blast_furnace", 2, 5, 8)),
                new IndustryJson("agriculture", Map.of("minecraft:wheat_seeds", 1), Map.of("minecraft:wheat", 1), 35, "farm_plot", 1, 1, 1),
                withRecipes(new IndustryJson("manufacturing", Map.of("minecraft:iron_ingot", 1, "minecraft:coal", 1), Map.of("minecraft:rail", 1), 80, "rolling_mill", 2, 2, 4),
                        new RecipeJson("rail", Map.of("minecraft:iron_ingot", 1, "minecraft:coal", 1), Map.of("minecraft:rail", 1), 80, "rolling_mill", 2, 2, 4),
                        new RecipeJson("steel_sheet", Map.of("minecraft:iron_ingot", 1, "minecraft:coal", 1), Map.of("capitalismmod:steel_sheet", 1), 70, "rolling_mill", 2, 2, 4),
                        new RecipeJson("copper_wire", Map.of("minecraft:copper_ingot", 1), Map.of("capitalismmod:copper_wire", 2), 65, "wire_mill", 2, 1, 3),
                        new RecipeJson("glass", Map.of("minecraft:sand", 1, "minecraft:coal", 1), Map.of("minecraft:glass", 1), 40, "glass_furnace", 1, 2, 3),
                        new RecipeJson("glass_lens", Map.of("minecraft:glass", 1), Map.of("capitalismmod:glass_lens", 1), 45, "glass_furnace", 1, 1, 3),
                        new RecipeJson("electric_lamp", Map.of("capitalismmod:steel_sheet", 1, "capitalismmod:copper_wire", 1, "capitalismmod:glass_lens", 1), Map.of("capitalismmod:electric_lamp", 1), 120, "assembly_line", 3, 2, 8),
                        new RecipeJson("flour", Map.of("minecraft:wheat", 1), Map.of("capitalismmod:flour", 1), 35, "milling_machine", 1, 1, 3),
                        new RecipeJson("bread", Map.of("capitalismmod:flour", 1, "minecraft:sugar", 1), Map.of("minecraft:bread", 1), 55, "food_processor", 2, 1, 4),
                        new RecipeJson("metal_can", Map.of("minecraft:iron_ingot", 1), Map.of("capitalismmod:metal_can", 1), 45, "rolling_mill", 1, 1, 3),
                        new RecipeJson("canned_food", Map.of("minecraft:wheat", 1, "capitalismmod:metal_can", 1), Map.of("capitalismmod:canned_food", 1), 90, "assembly_line", 3, 2, 8),
                        new RecipeJson("machine_frame", Map.of("capitalismmod:steel_sheet", 2, "minecraft:iron_ingot", 2), Map.of("capitalismmod:machine_frame", 1), 105, "rolling_mill", 2, 2, 5),
                        new RecipeJson("electric_motor", Map.of("capitalismmod:copper_wire", 2, "minecraft:iron_ingot", 1, "minecraft:redstone", 1), Map.of("capitalismmod:electric_motor", 1), 130, "lathe", 2, 2, 5),
                        new RecipeJson("green_lumber", Map.of("minecraft:oak_log", 1), Map.of("capitalismmod:green_lumber", 4), 55, "sawmill", 2, 2, 4),
                        new RecipeJson("dried_lumber", Map.of("capitalismmod:green_lumber", 2, "minecraft:coal", 1), Map.of("capitalismmod:dried_lumber", 1), 85, "dry_kiln", 2, 4, 6),
                        new RecipeJson("wooden_crate", Map.of("capitalismmod:dried_lumber", 3, "minecraft:iron_ingot", 1), Map.of("capitalismmod:wooden_crate", 1), 120, "assembly_line", 2, 2, 8),
                        new RecipeJson("furniture", Map.of("capitalismmod:dried_lumber", 4, "minecraft:iron_ingot", 2, "minecraft:glass", 1), Map.of("capitalismmod:furniture", 1), 260, "assembly_line", 3, 3, 10),
                        new RecipeJson("crude_oil", Map.of(), Map.of("capitalismmod:crude_oil", 3), 35, "oil_well", 2, 4, 12),
                        new RecipeJson("refining", Map.of("capitalismmod:crude_oil", 3), Map.of("capitalismmod:refinery_gas", 1, "capitalismmod:naphtha", 2, "capitalismmod:kerosene", 1, "capitalismmod:gas_oil", 1, "capitalismmod:fuel_oil", 1), 130, "distillation_column", 4, 8, 14),
                        new RecipeJson("steam_cracking", Map.of("capitalismmod:naphtha", 2, "minecraft:water_bucket", 1), Map.of("capitalismmod:ethylene", 2, "capitalismmod:propylene", 1, "capitalismmod:c4_fraction", 1, "capitalismmod:refinery_gas", 1), 115, "steam_cracker", 3, 7, 12),
                        new RecipeJson("polyethylene_pellets", Map.of("capitalismmod:ethylene", 1), Map.of("capitalismmod:polyethylene_pellets", 3), 78, "polymer_reactor", 2, 4, 7),
                        new RecipeJson("polypropylene_pellets", Map.of("capitalismmod:propylene", 1), Map.of("capitalismmod:polypropylene_pellets", 3), 82, "polymer_reactor", 2, 4, 7),
                        new RecipeJson("polyethylene_grade", Map.of("capitalismmod:ethylene", 1), Map.of("capitalismmod:polyethylene_pellets", 3), 82, "polymer_reactor", 2, 4, 7),
                        new RecipeJson("polypropylene_grade", Map.of("capitalismmod:propylene", 1), Map.of("capitalismmod:polypropylene_pellets", 3), 86, "polymer_reactor", 2, 4, 7),
                        new RecipeJson("synthetic_rubber", Map.of("capitalismmod:butadiene", 1, "capitalismmod:styrene_monomer", 1), Map.of("capitalismmod:synthetic_rubber", 2), 105, "rubber_polymerization_unit", 3, 5, 9),
                        new RecipeJson("ethylene_glycol", Map.of("capitalismmod:ethylene_oxide", 1, "minecraft:water_bucket", 1), Map.of("capitalismmod:ethylene_glycol", 2), 115, "chemical_reactor", 3, 5, 9),
                        new RecipeJson("epoxy_resin", Map.of("capitalismmod:bisphenol_a", 1, "capitalismmod:epichlorohydrin", 1, "capitalismmod:caustic_soda", 1), Map.of("capitalismmod:epoxy_resin", 2), 145, "polymer_reactor", 3, 5, 10),
                        new RecipeJson("benzene_reforming", Map.of("capitalismmod:reformate", 2), Map.of("capitalismmod:benzene", 1, "capitalismmod:p_xylene", 1), 108, "distillation_column", 3, 6, 11),
                        new RecipeJson("aromatics_reforming", Map.of("capitalismmod:reformate", 2), Map.of("capitalismmod:benzene", 1, "capitalismmod:p_xylene", 1), 112, "distillation_column", 3, 6, 11),
                        new RecipeJson("styrene_monomer", Map.of("capitalismmod:ethylbenzene", 1), Map.of("capitalismmod:styrene_monomer", 1, "capitalismmod:hydrogen", 1), 145, "chemical_reactor", 3, 6, 11),
                        new RecipeJson("abs_resin", Map.of("capitalismmod:acrylonitrile", 1, "capitalismmod:butadiene", 1, "capitalismmod:styrene_monomer", 1), Map.of("capitalismmod:abs_resin", 2), 210, "polymer_reactor", 3, 6, 11),
                        new RecipeJson("terephthalic_acid_oxidation", Map.of("capitalismmod:p_xylene", 1, "capitalismmod:oxygen", 2), Map.of("capitalismmod:terephthalic_acid", 1), 132, "chemical_reactor", 3, 6, 11),
                        new RecipeJson("pet_resin_polycondensation", Map.of("capitalismmod:terephthalic_acid", 1, "capitalismmod:ethylene_glycol", 1), Map.of("capitalismmod:pet_resin", 2), 175, "polymer_reactor", 3, 7, 12),
                        new RecipeJson("lubricant_blending", Map.of("capitalismmod:base_oil", 1, "capitalismmod:hydrogen", 1), Map.of("capitalismmod:lubricant", 2), 95, "hydrotreater", 2, 3, 7),
                        new RecipeJson("plastic_pellets", Map.of("capitalismmod:polyethylene_pellets", 2), Map.of("capitalismmod:plastic_pellets", 2), 70, "extrusion_line", 2, 4, 7),
                        new RecipeJson("fan_blades", Map.of("capitalismmod:plastic_pellets", 2, "capitalismmod:steel_sheet", 1), Map.of("capitalismmod:fan_blades", 1), 105, "injection_molder", 2, 3, 8),
                        new RecipeJson("fan_control", Map.of("capitalismmod:copper_wire", 1, "minecraft:redstone", 1, "minecraft:iron_ingot", 1), Map.of("capitalismmod:fan_control", 1), 95, "assembly_line", 2, 2, 6),
                        new RecipeJson("polysilicon", Map.of("minecraft:quartz", 2, "minecraft:coal", 1), Map.of("capitalismmod:polysilicon", 2), 210, "silicon_refiner", 3, 8, 15),
                        new RecipeJson("silicon_wafer", Map.of("capitalismmod:silicon_ingot", 1), Map.of("capitalismmod:silicon_wafer", 2), 180, "wafer_dicing_saw", 3, 8, 16),
                        new RecipeJson("silicon_wafer_from_polysilicon", Map.of("capitalismmod:silicon_ingot", 1), Map.of("capitalismmod:silicon_wafer", 2), 230, "wafer_dicing_saw", 3, 9, 16),
                        new RecipeJson("wafer_dicing", Map.of("capitalismmod:tested_wafer", 1), Map.of("capitalismmod:silicon_die", 4), 190, "wafer_dicing_saw", 3, 6, 12),
                        new RecipeJson("die_testing", Map.of("capitalismmod:silicon_die", 2), Map.of("capitalismmod:tested_die", 2), 225, "chip_testing_station", 3, 6, 13),
                        new RecipeJson("lead_frame_stamping", Map.of("capitalismmod:copper_foil", 1, "capitalismmod:steel_sheet", 1), Map.of("capitalismmod:lead_frame", 2), 105, "stamping_press", 2, 4, 8),
                        new RecipeJson("mold_compound", Map.of("capitalismmod:epoxy_resin", 1, "capitalismmod:plastic_pellets", 1), Map.of("capitalismmod:mold_compound", 2), 125, "molding_compound_unit", 2, 4, 8),
                        new RecipeJson("chip_packaging", Map.of("capitalismmod:tested_die", 1, "capitalismmod:lead_frame", 1, "capitalismmod:copper_wire", 1, "capitalismmod:mold_compound", 1), Map.of("capitalismmod:packaged_chip", 1), 300, "chip_packaging_line", 3, 7, 12),
                        new RecipeJson("tested_die_packaging", Map.of("capitalismmod:tested_die", 1, "capitalismmod:lead_frame", 1, "capitalismmod:copper_wire", 1, "capitalismmod:mold_compound", 1), Map.of("capitalismmod:packaged_chip", 1), 360, "chip_packaging_line", 4, 8, 15),
                        new RecipeJson("circuit_board", Map.of("capitalismmod:assembled_pcb", 1), Map.of("capitalismmod:circuit_board", 1), 260, "chip_testing_station", 3, 5, 10),
                        new RecipeJson("circuit_board_with_packaged_chip", Map.of("capitalismmod:assembled_pcb", 1), Map.of("capitalismmod:circuit_board", 1), 280, "chip_testing_station", 3, 5, 10),
                        new RecipeJson("pcb_substrate", Map.of("minecraft:glass", 1, "capitalismmod:epoxy_resin", 1), Map.of("capitalismmod:pcb_substrate", 2), 105, "lamination_press", 2, 4, 8),
                        new RecipeJson("copper_foil", Map.of("capitalismmod:copper_cathode", 1), Map.of("capitalismmod:copper_foil", 2), 58, "rolling_mill", 2, 2, 5),
                        new RecipeJson("solder", Map.of("capitalismmod:tin_ingot", 2, "minecraft:copper_ingot", 1), Map.of("capitalismmod:solder", 2), 52, "alloy_furnace", 2, 3, 6),
                        new RecipeJson("pcb_drilling", Map.of("capitalismmod:copper_clad_laminate", 1), Map.of("capitalismmod:drilled_pcb_panel", 1), 130, "pcb_fabrication_line", 3, 5, 10),
                        new RecipeJson("pcb_etching", Map.of("capitalismmod:drilled_pcb_panel", 1, "capitalismmod:electronic_etchant", 1), Map.of("capitalismmod:etched_pcb", 1), 155, "pcb_fabrication_line", 3, 6, 11),
                        new RecipeJson("pcb_solder_mask", Map.of("capitalismmod:etched_pcb", 1, "capitalismmod:epoxy_resin", 1), Map.of("capitalismmod:solder_masked_pcb", 1), 180, "pcb_fabrication_line", 3, 6, 11),
                        new RecipeJson("smd_components", Map.of("minecraft:redstone", 1, "capitalismmod:copper_wire", 1, "capitalismmod:plastic_pellets", 1), Map.of("capitalismmod:smd_components", 2), 175, "smt_line", 3, 5, 11),
                        new RecipeJson("passive_components", Map.of("minecraft:redstone", 1, "minecraft:iron_ingot", 1, "capitalismmod:copper_wire", 1), Map.of("capitalismmod:passive_components", 4), 155, "smt_line", 3, 5, 10),
                        new RecipeJson("power_management_ic", Map.of("capitalismmod:packaged_chip", 1, "capitalismmod:passive_components", 1, "capitalismmod:copper_wire", 1), Map.of("capitalismmod:power_management_ic", 1), 330, "smt_line", 3, 6, 12),
                        new RecipeJson("display_driver", Map.of("capitalismmod:packaged_chip", 1, "capitalismmod:passive_components", 1, "capitalismmod:copper_wire", 1), Map.of("capitalismmod:display_driver", 1), 350, "smt_line", 3, 6, 12),
                        new RecipeJson("printed_circuit_board", Map.of("capitalismmod:assembled_pcb", 1), Map.of("capitalismmod:circuit_board", 1), 340, "chip_testing_station", 4, 7, 14),
                        new RecipeJson("assembled_fabricated_pcb", Map.of("capitalismmod:assembled_pcb", 1), Map.of("capitalismmod:circuit_board", 1), 390, "chip_testing_station", 4, 8, 15),
                        new RecipeJson("display_glass_substrate", Map.of("minecraft:glass", 2), Map.of("capitalismmod:display_glass_substrate", 1), 145, "glass_furnace", 2, 4, 8),
                        new RecipeJson("backlight_module", Map.of("capitalismmod:packaged_chip", 1, "capitalismmod:copper_wire", 1, "capitalismmod:plastic_pellets", 1), Map.of("capitalismmod:backlight_module", 1), 210, "electronics_assembly", 3, 6, 11),
                        new RecipeJson("display_panel", Map.of("capitalismmod:display_glass_substrate", 1, "capitalismmod:backlight_module", 1, "capitalismmod:display_driver", 1, "capitalismmod:plastic_pellets", 1), Map.of("capitalismmod:display_panel", 1), 390, "electronics_assembly", 4, 8, 15),
                        new RecipeJson("phone_casing", Map.of("capitalismmod:abs_resin", 1), Map.of("capitalismmod:phone_casing", 1), 145, "injection_molder", 2, 3, 8),
                        new RecipeJson("plastic_container", Map.of("capitalismmod:polyethylene_pellets", 2), Map.of("capitalismmod:plastic_container", 1), 125, "injection_molder", 2, 3, 8),
                        new RecipeJson("packaging_film", Map.of("capitalismmod:polyethylene_pellets", 1), Map.of("capitalismmod:packaging_film", 4), 95, "extrusion_line", 2, 3, 8),
                        new RecipeJson("pet_bottle", Map.of("capitalismmod:pet_resin", 1), Map.of("capitalismmod:pet_bottle", 1), 135, "injection_molder", 2, 3, 8),
                        new RecipeJson("pet_bottle_recycling", Map.of("capitalismmod:pet_bottle", 2), Map.of("capitalismmod:recycled_pet_flakes", 1), 105, "plastic_recycler", 2, 5, 9),
                        new RecipeJson("recycled_pet_resin", Map.of("capitalismmod:recycled_pet_flakes", 2), Map.of("capitalismmod:pet_resin", 1), 125, "extrusion_line", 2, 5, 10),
                        new RecipeJson("packaging_film_recycling", Map.of("capitalismmod:packaging_film", 4), Map.of("capitalismmod:reclaimed_plastic", 1), 88, "plastic_recycler", 2, 5, 9),
                        new RecipeJson("plastic_container_recycling", Map.of("capitalismmod:plastic_container", 2), Map.of("capitalismmod:reclaimed_plastic", 1), 82, "plastic_recycler", 2, 5, 9),
                        new RecipeJson("camera_module", Map.of("capitalismmod:glass_lens", 1, "capitalismmod:circuit_board", 1, "capitalismmod:plastic_pellets", 1), Map.of("capitalismmod:camera_module", 1), 280, "electronics_assembly", 3, 5, 10),
                        new RecipeJson("speaker_module", Map.of("capitalismmod:copper_wire", 1, "minecraft:iron_ingot", 1, "capitalismmod:plastic_pellets", 1), Map.of("capitalismmod:speaker_module", 1), 145, "electronics_assembly", 2, 3, 8),
                        new RecipeJson("microphone_module", Map.of("capitalismmod:copper_wire", 1, "minecraft:redstone", 1, "capitalismmod:plastic_pellets", 1), Map.of("capitalismmod:microphone_module", 1), 125, "electronics_assembly", 2, 3, 8),
                        new RecipeJson("charging_port", Map.of("capitalismmod:copper_wire", 1, "minecraft:iron_ingot", 1, "capitalismmod:plastic_pellets", 1), Map.of("capitalismmod:charging_port", 1), 135, "electronics_assembly", 2, 3, 8),
                        new RecipeJson("keyboard_module", Map.of("capitalismmod:abs_resin", 1, "capitalismmod:passive_components", 1, "capitalismmod:copper_wire", 1), Map.of("capitalismmod:keyboard_module", 1), 170, "electronics_assembly", 3, 5, 10),
                        new RecipeJson("storage_module", Map.of("capitalismmod:packaged_chip", 1, "capitalismmod:circuit_board", 1, "capitalismmod:passive_components", 1), Map.of("capitalismmod:storage_module", 1), 360, "smt_line", 3, 6, 12),
                        new RecipeJson("wireless_module", Map.of("capitalismmod:packaged_chip", 1, "capitalismmod:passive_components", 1, "capitalismmod:copper_wire", 1), Map.of("capitalismmod:wireless_module", 1), 285, "smt_line", 3, 6, 12),
                        new RecipeJson("battery_cell", Map.of("minecraft:iron_ingot", 1, "minecraft:copper_ingot", 1, "minecraft:redstone", 1), Map.of("capitalismmod:battery_cell", 2), 80, "electronics_assembly", 2, 3, 7),
                        new RecipeJson("battery", Map.of("capitalismmod:battery_cell", 2, "capitalismmod:copper_wire", 1, "capitalismmod:plastic_pellets", 1), Map.of("capitalismmod:battery", 1), 120, "electronics_assembly", 2, 3, 7),
                        new RecipeJson("lithium_mineral_concentration", Map.of("minecraft:quartz", 2), Map.of("capitalismmod:lithium_mineral", 1), 72, "ore_processor", 2, 2, 5),
                        new RecipeJson("lithium_carbonate_refining", Map.of("capitalismmod:lithium_mineral", 1, "minecraft:coal", 1), Map.of("capitalismmod:lithium_carbonate", 1), 135, "battery_materials", 3, 6, 10),
                        new RecipeJson("graphite_anode_processing", Map.of("capitalismmod:graphite", 2, "capitalismmod:lubricant", 1), Map.of("capitalismmod:graphite_anode", 1), 110, "battery_materials", 3, 5, 9),
                        new RecipeJson("cathode_active_material", Map.of("capitalismmod:lfp_cathode", 1), Map.of("capitalismmod:cathode_active_material", 1), 175, "battery_materials", 3, 7, 11),
                        new RecipeJson("battery_separator", Map.of("capitalismmod:polypropylene_pellets", 1), Map.of("capitalismmod:battery_separator", 2), 68, "polymer_reactor", 2, 4, 7),
                        new RecipeJson("battery_electrolyte", Map.of("capitalismmod:lithium_carbonate", 1, "capitalismmod:lpg", 1), Map.of("capitalismmod:battery_electrolyte", 1), 125, "battery_materials", 3, 6, 10),
                        new RecipeJson("lithium_ion_cell", Map.of("capitalismmod:cathode_active_material", 1, "capitalismmod:graphite_anode", 1, "capitalismmod:battery_separator", 1, "capitalismmod:battery_electrolyte", 1, "capitalismmod:copper_foil", 1), Map.of("capitalismmod:battery_cell", 2), 260, "battery_cell_line", 4, 8, 13),
                        new RecipeJson("lithium_ion_battery", Map.of("capitalismmod:battery_cell", 2, "capitalismmod:copper_wire", 1, "capitalismmod:plastic_pellets", 1), Map.of("capitalismmod:battery", 1), 145, "electronics_assembly", 2, 4, 8),
                        new RecipeJson("battery_pack", Map.of("capitalismmod:battery", 4, "capitalismmod:circuit_board", 1, "capitalismmod:steel_sheet", 1, "capitalismmod:plastic_pellets", 2), Map.of("capitalismmod:battery_pack", 1), 520, "electronics_assembly", 4, 8, 14),
                        new RecipeJson("battery_pack_shredding", Map.of("capitalismmod:battery_pack", 1), Map.of("capitalismmod:black_mass", 2), 210, "battery_recycler", 3, 7, 12),
                        new RecipeJson("black_mass_hydrometallurgy", Map.of("capitalismmod:black_mass", 2, "capitalismmod:sulfuric_acid", 1, "capitalismmod:industrial_water", 1), Map.of("capitalismmod:lithium_carbonate", 1, "capitalismmod:graphite", 1), 330, "battery_recycler", 4, 9, 15),
                        new RecipeJson("smartphone", Map.of("capitalismmod:circuit_board", 1, "capitalismmod:battery", 1, "capitalismmod:display_panel", 1, "capitalismmod:phone_casing", 1, "capitalismmod:camera_module", 1, "capitalismmod:speaker_module", 1, "capitalismmod:microphone_module", 1, "capitalismmod:charging_port", 1, "capitalismmod:power_management_ic", 1, "capitalismmod:display_driver", 1), Map.of("capitalismmod:smartphone", 1), 820, "electronics_assembly", 5, 8, 15),
                        new RecipeJson("electric_fan", Map.of("capitalismmod:electric_motor", 1, "capitalismmod:fan_blades", 1, "capitalismmod:fan_control", 1, "capitalismmod:machine_frame", 1), Map.of("capitalismmod:electric_fan", 1), 240, "assembly_line", 3, 3, 9),
                        new RecipeJson("power_adapter", Map.of("capitalismmod:circuit_board", 1, "capitalismmod:copper_wire", 1, "capitalismmod:plastic_pellets", 1), Map.of("capitalismmod:power_adapter", 1), 180, "smt_line", 2, 4, 9),
                        new RecipeJson("television", Map.of("capitalismmod:display_panel", 1, "capitalismmod:packaged_chip", 1, "capitalismmod:circuit_board", 1, "capitalismmod:speaker_module", 1, "capitalismmod:abs_resin", 1, "capitalismmod:display_driver", 1), Map.of("capitalismmod:television", 1), 980, "electronics_assembly", 5, 8, 16),
                        new RecipeJson("laptop", Map.of("capitalismmod:display_panel", 1, "capitalismmod:packaged_chip", 2, "capitalismmod:circuit_board", 1, "capitalismmod:battery", 1, "capitalismmod:abs_resin", 1, "capitalismmod:power_management_ic", 1, "capitalismmod:display_driver", 1, "capitalismmod:keyboard_module", 1, "capitalismmod:storage_module", 1), Map.of("capitalismmod:laptop", 1), 1560, "electronics_assembly", 6, 10, 20),
                        new RecipeJson("wireless_router", Map.of("capitalismmod:circuit_board", 1, "capitalismmod:packaged_chip", 1, "capitalismmod:power_adapter", 1, "capitalismmod:abs_resin", 1, "capitalismmod:wireless_module", 1), Map.of("capitalismmod:wireless_router", 1), 420, "electronics_assembly", 4, 7, 14),
                        new RecipeJson("power_bank", Map.of("capitalismmod:battery_pack", 1, "capitalismmod:charging_port", 1, "capitalismmod:power_management_ic", 1, "capitalismmod:abs_resin", 1), Map.of("capitalismmod:power_bank", 1), 360, "electronics_assembly", 4, 7, 14),
                        new RecipeJson("smart_speaker", Map.of("capitalismmod:wireless_module", 1, "capitalismmod:speaker_module", 1, "capitalismmod:microphone_module", 1, "capitalismmod:packaged_chip", 1, "capitalismmod:abs_resin", 1), Map.of("capitalismmod:smart_speaker", 1), 480, "electronics_assembly", 4, 7, 14),
                        new RecipeJson("smartphone_recycling", Map.of("capitalismmod:smartphone", 1), Map.of("capitalismmod:copper_wire", 2, "capitalismmod:plastic_pellets", 1), 190, "electronics_recycler", 3, 6, 12),
                        new RecipeJson("television_recycling", Map.of("capitalismmod:television", 1), Map.of("capitalismmod:copper_wire", 3, "capitalismmod:plastic_pellets", 2), 260, "electronics_recycler", 4, 7, 14),
                        new RecipeJson("laptop_recycling", Map.of("capitalismmod:laptop", 1), Map.of("capitalismmod:copper_wire", 3, "capitalismmod:plastic_pellets", 2), 300, "electronics_recycler", 4, 8, 15),
                        new RecipeJson("wireless_router_recycling", Map.of("capitalismmod:wireless_router", 1), Map.of("capitalismmod:copper_wire", 1, "capitalismmod:plastic_pellets", 1), 145, "electronics_recycler", 2, 5, 10),
                        new RecipeJson("smartphone_refurbishment", Map.of("capitalismmod:smartphone", 1, "capitalismmod:battery", 1, "capitalismmod:display_panel", 1), Map.of("capitalismmod:refurbished_smartphone", 1), 310, "refurbishment_line", 3, 6, 12),
                        new RecipeJson("television_refurbishment", Map.of("capitalismmod:television", 1, "capitalismmod:display_panel", 1, "capitalismmod:circuit_board", 1), Map.of("capitalismmod:refurbished_television", 1), 360, "refurbishment_line", 3, 7, 14),
                        new RecipeJson("laptop_refurbishment", Map.of("capitalismmod:laptop", 1, "capitalismmod:battery", 1, "capitalismmod:display_panel", 1), Map.of("capitalismmod:refurbished_laptop", 1), 420, "refurbishment_line", 4, 8, 15),
                        new RecipeJson("wireless_router_refurbishment", Map.of("capitalismmod:wireless_router", 1, "capitalismmod:packaged_chip", 1), Map.of("capitalismmod:refurbished_wireless_router", 1), 220, "refurbishment_line", 2, 5, 10),
                        new RecipeJson("refurbished_smartphone_recycling", Map.of("capitalismmod:refurbished_smartphone", 1), Map.of("capitalismmod:copper_wire", 1, "capitalismmod:plastic_pellets", 1), 145, "electronics_recycler", 2, 5, 10),
                        new RecipeJson("refurbished_television_recycling", Map.of("capitalismmod:refurbished_television", 1), Map.of("capitalismmod:copper_wire", 2, "capitalismmod:plastic_pellets", 1), 195, "electronics_recycler", 3, 6, 12),
                        new RecipeJson("refurbished_laptop_recycling", Map.of("capitalismmod:refurbished_laptop", 1), Map.of("capitalismmod:copper_wire", 2, "capitalismmod:plastic_pellets", 1), 220, "electronics_recycler", 3, 6, 13),
                        new RecipeJson("refurbished_wireless_router_recycling", Map.of("capitalismmod:refurbished_wireless_router", 1), Map.of("capitalismmod:copper_wire", 1, "capitalismmod:plastic_pellets", 1), 105, "electronics_recycler", 2, 4, 9)),
                withRecipes(new IndustryJson("petrochemical_refining", Map.of("capitalismmod:crude_oil", 3), Map.of("capitalismmod:naphtha", 2), 160, "oil_refinery", 4, 10, 16),
                        new RecipeJson("atmospheric_distillation", Map.of("capitalismmod:crude_oil", 3), Map.of(
                                "capitalismmod:refinery_gas", 1, "capitalismmod:naphtha", 2,
                                "capitalismmod:kerosene", 1, "capitalismmod:gas_oil", 1,
                                "capitalismmod:fuel_oil", 1), 160, "distillation_column", 4, 10, 16),
                        new RecipeJson("refinery_gas_fractionation", Map.of("capitalismmod:refinery_gas", 2), Map.of("capitalismmod:lpg", 1), 110, "distillation_column", 2, 6, 9),
                        new RecipeJson("gasoline_blending", Map.of("capitalismmod:reformate", 1, "capitalismmod:naphtha", 1), Map.of("capitalismmod:gasoline", 2), 160, "blending_unit", 3, 5, 9),
                        new RecipeJson("syngas_shift_separation", Map.of("capitalismmod:syngas", 1, "minecraft:water_bucket", 1), Map.of("capitalismmod:hydrogen", 2, "capitalismmod:carbon_dioxide", 1), 125, "steam_reformer", 3, 8, 12),
                        new RecipeJson("coal_gasification", Map.of("minecraft:coal", 2, "minecraft:water_bucket", 1, "capitalismmod:oxygen", 1), Map.of("capitalismmod:syngas", 2), 125, "steam_reformer", 3, 9, 13),
                        new RecipeJson("water_electrolysis", Map.of("minecraft:water_bucket", 1), Map.of("capitalismmod:hydrogen", 2, "capitalismmod:oxygen", 1), 110, "chlor_alkali_cell", 2, 12, 10),
                        new RecipeJson("ethylbenzene_alkylation", Map.of("capitalismmod:benzene", 1, "capitalismmod:ethylene", 1), Map.of("capitalismmod:ethylbenzene", 1), 145, "chemical_reactor", 3, 6, 10),
                        new RecipeJson("acrylonitrile_ammoxidation", Map.of("capitalismmod:propylene", 1, "capitalismmod:ammonia", 1, "capitalismmod:oxygen", 1), Map.of("capitalismmod:acrylonitrile", 1), 155, "chemical_reactor", 3, 8, 12),
                        new RecipeJson("bisphenol_a_condensation", Map.of("capitalismmod:phenol", 2, "capitalismmod:acetone", 1), Map.of("capitalismmod:bisphenol_a", 1), 170, "chemical_reactor", 3, 7, 11),
                        new RecipeJson("epichlorohydrin_production", Map.of("capitalismmod:propylene", 1, "capitalismmod:chlorine", 1, "capitalismmod:caustic_soda", 1, "minecraft:water_bucket", 1), Map.of("capitalismmod:epichlorohydrin", 1, "capitalismmod:salt", 1), 175, "chemical_reactor", 3, 8, 12),
                        new RecipeJson("vacuum_distillation", Map.of("capitalismmod:fuel_oil", 2), Map.of(
                                "capitalismmod:vacuum_resid", 1, "capitalismmod:base_oil", 1), 125, "vacuum_distillation_unit", 3, 8, 13),
                        new RecipeJson("resid_to_asphalt", Map.of("capitalismmod:vacuum_resid", 1), Map.of("capitalismmod:asphalt", 2), 95, "vacuum_distillation_unit", 3, 6, 10),
                        new RecipeJson("gas_reforming", Map.of("capitalismmod:refinery_gas", 2, "minecraft:water_bucket", 1), Map.of("capitalismmod:syngas", 2), 118, "steam_reformer", 3, 7, 11),
                        new RecipeJson("naphtha_reforming", Map.of("capitalismmod:naphtha", 2), Map.of("capitalismmod:reformate", 2, "capitalismmod:hydrogen", 1), 155, "catalytic_reformer", 3, 9, 14),
                        new RecipeJson("cumene_alkylation", Map.of("capitalismmod:benzene", 1, "capitalismmod:propylene", 1), Map.of("capitalismmod:cumene", 1), 145, "chemical_reactor", 3, 7, 11),
                        new RecipeJson("cumene_cleavage", Map.of("capitalismmod:cumene", 1, "capitalismmod:oxygen", 1), Map.of("capitalismmod:phenol", 1, "capitalismmod:acetone", 1), 175, "chemical_reactor", 3, 8, 12),
                        new RecipeJson("phenolic_resin_production", Map.of("capitalismmod:phenol", 1, "capitalismmod:formaldehyde", 1), Map.of("capitalismmod:phenolic_resin", 2), 155, "polymer_reactor", 3, 7, 11),
                        new RecipeJson("methanol_synthesis", Map.of("capitalismmod:syngas", 2), Map.of("capitalismmod:methanol", 2), 145, "methanol_synthesis_unit", 3, 7, 11),
                        new RecipeJson("formaldehyde_oxidation", Map.of("capitalismmod:methanol", 1, "capitalismmod:oxygen", 1), Map.of("capitalismmod:formaldehyde", 2), 105, "chemical_reactor", 2, 5, 8),
                        new RecipeJson("phenolic_resin_condensation", Map.of("capitalismmod:phenol", 1, "capitalismmod:formaldehyde", 1), Map.of("capitalismmod:phenolic_resin", 2), 165, "polymer_reactor", 3, 7, 11),
                        new RecipeJson("gas_oil_hydrotreating", Map.of("capitalismmod:gas_oil", 1, "capitalismmod:hydrogen", 1), Map.of("capitalismmod:diesel", 1, "capitalismmod:sulfur", 1), 135, "hydrotreater", 3, 8, 13),
                        new RecipeJson("sulfuric_acid_production", Map.of("capitalismmod:sulfur", 1, "capitalismmod:oxygen", 2, "minecraft:water_bucket", 1), Map.of("capitalismmod:sulfuric_acid", 1), 105, "chemical_reactor", 3, 6, 10),
                        new RecipeJson("steam_cracking_fractionated", Map.of("capitalismmod:naphtha", 2, "minecraft:water_bucket", 1), Map.of("capitalismmod:ethylene", 2, "capitalismmod:propylene", 1, "capitalismmod:c4_fraction", 1, "capitalismmod:refinery_gas", 1), 125, "steam_cracker", 3, 8, 13),
                        new RecipeJson("ethylene_oxide_production", Map.of("capitalismmod:ethylene", 1, "capitalismmod:oxygen", 1), Map.of("capitalismmod:ethylene_oxide", 1), 130, "chemical_reactor", 3, 6, 10),
                        new RecipeJson("propylene_oxide", Map.of("capitalismmod:propylene", 1, "capitalismmod:hydrogen_peroxide", 1), Map.of("capitalismmod:propylene_oxide", 1), 130, "chemical_reactor", 3, 6, 10),
                        new RecipeJson("polyethylene_film_extrusion", Map.of("capitalismmod:polyethylene_pellets", 1), Map.of("capitalismmod:polyethylene_film", 3), 105, "extrusion_line", 2, 5, 8),
                        new RecipeJson("polypropylene_fiber_spinning", Map.of("capitalismmod:polypropylene_pellets", 1), Map.of("capitalismmod:polypropylene_fiber", 3), 112, "extrusion_line", 2, 5, 8),
                        new RecipeJson("salt_extraction", Map.of("minecraft:water_bucket", 1, "minecraft:sand", 1), Map.of("capitalismmod:salt", 2), 48, "chemical_reactor", 1, 2, 4),
                        new RecipeJson("chlor_alkali_electrolysis", Map.of("capitalismmod:brine", 2), Map.of("capitalismmod:chlorine", 1, "capitalismmod:caustic_soda", 1, "capitalismmod:hydrogen", 1), 125, "chlor_alkali_cell", 3, 7, 10),
                        new RecipeJson("ethylene_dichloride_synthesis", Map.of("capitalismmod:ethylene", 1, "capitalismmod:chlorine", 1), Map.of("capitalismmod:ethylene_dichloride", 1), 145, "chemical_reactor", 3, 7, 10),
                        new RecipeJson("ethylene_dichloride_cracking", Map.of("capitalismmod:ethylene_dichloride", 1, "minecraft:water_bucket", 1), Map.of("capitalismmod:vinyl_chloride", 1, "capitalismmod:hydrochloric_acid", 1), 155, "vinyl_chloride_unit", 3, 7, 11),
                        new RecipeJson("vinyl_chloride_synthesis", Map.of("capitalismmod:ethylene_dichloride", 1, "minecraft:water_bucket", 1), Map.of("capitalismmod:vinyl_chloride", 1, "capitalismmod:hydrochloric_acid", 1), 155, "vinyl_chloride_unit", 3, 7, 11),
                        new RecipeJson("pvc_polymerization", Map.of("capitalismmod:vinyl_chloride", 2), Map.of("capitalismmod:pvc_resin", 3), 175, "polymer_reactor", 3, 7, 11),
                        new RecipeJson("pvc_pipe_extrusion", Map.of("capitalismmod:pvc_resin", 2), Map.of("capitalismmod:pvc_pipe", 1), 160, "extrusion_line", 2, 6, 9),
                        new RecipeJson("pvc_pipe_recycling", Map.of("capitalismmod:pvc_pipe", 2), Map.of("capitalismmod:pvc_resin", 1), 115, "plastic_recycler", 3, 6, 10),
                        new RecipeJson("butadiene_extraction", Map.of("capitalismmod:c4_fraction", 2), Map.of("capitalismmod:butadiene", 1), 105, "steam_cracker", 2, 6, 9),
                        new RecipeJson("sbr_rubber_polymerization", Map.of("capitalismmod:butadiene", 1, "capitalismmod:styrene_monomer", 1), Map.of("capitalismmod:synthetic_rubber", 2), 175, "rubber_polymerization_unit", 3, 7, 11),
                        new RecipeJson("polyether_polyol_synthesis", Map.of("capitalismmod:propylene_oxide", 1, "capitalismmod:ethylene_oxide", 1), Map.of("capitalismmod:polyether_polyol", 2), 165, "chemical_reactor", 3, 7, 11),
                        new RecipeJson("polyurethane_foam_production", Map.of("capitalismmod:polyether_polyol", 1, "capitalismmod:plastic_pellets", 1), Map.of("capitalismmod:polyurethane_foam", 2), 145, "polymer_reactor", 3, 6, 10),
                        new RecipeJson("pet_fiber_spinning", Map.of("capitalismmod:pet_resin", 1), Map.of("capitalismmod:pet_fiber", 3), 125, "extrusion_line", 2, 5, 8),
                        new RecipeJson("plastic_reclamation", Map.of("capitalismmod:plastic_container", 2, "capitalismmod:packaging_film", 2), Map.of("capitalismmod:reclaimed_plastic", 2), 105, "plastic_recycler", 3, 6, 10),
                        new RecipeJson("reclaimed_plastic_reprocessing", Map.of("capitalismmod:reclaimed_plastic", 2), Map.of("capitalismmod:plastic_pellets", 1), 88, "plastic_recycler", 2, 5, 8)),
                withRecipes(new IndustryJson("metallurgy", Map.of("minecraft:copper_ore", 2), Map.of("capitalismmod:copper_cathode", 1), 100, "ore_processor", 2, 4, 7),
                        new RecipeJson("copper_concentrate_refining", Map.of("minecraft:raw_copper", 2), Map.of("capitalismmod:copper_concentrate", 1), 42, "ore_processor", 2, 2, 4),
                        new RecipeJson("cathode_wire_drawing", Map.of("capitalismmod:copper_cathode", 1), Map.of("capitalismmod:copper_wire", 2), 65, "wire_mill", 2, 2, 4),
                        new RecipeJson("copper_cathode", Map.of("capitalismmod:copper_concentrate", 2, "minecraft:coal", 1), Map.of("capitalismmod:copper_cathode", 1), 105, "blast_furnace", 3, 5, 8),
                        new RecipeJson("alumina_refining", Map.of("capitalismmod:raw_bauxite", 2, "capitalismmod:caustic_soda", 1, "minecraft:water_bucket", 1), Map.of("capitalismmod:alumina", 1), 76, "ore_processor", 2, 4, 6),
                        new RecipeJson("aluminum_electrolysis", Map.of("capitalismmod:alumina", 2, "capitalismmod:graphite", 1), Map.of("capitalismmod:aluminum_ingot", 1), 115, "electric_arc_furnace", 3, 9, 12),
                        new RecipeJson("nickel_sulfate_refining", Map.of("capitalismmod:nickel_ingot", 1, "capitalismmod:sulfuric_acid", 1), Map.of("capitalismmod:nickel_sulfate", 1), 118, "chemical_reactor", 3, 7, 10),
                        new RecipeJson("cobalt_concentration", Map.of("capitalismmod:cobalt_ore", 2), Map.of("capitalismmod:raw_cobalt", 1), 78, "flotation_cell", 2, 3, 6),
                        new RecipeJson("cobalt_smelting", Map.of("capitalismmod:raw_cobalt", 2, "minecraft:coal", 1), Map.of("capitalismmod:cobalt_ingot", 1), 125, "electric_arc_furnace", 3, 7, 10),
                        new RecipeJson("manganese_concentration", Map.of("capitalismmod:manganese_ore", 2), Map.of("capitalismmod:raw_manganese", 1), 70, "flotation_cell", 2, 3, 6),
                        new RecipeJson("manganese_smelting", Map.of("capitalismmod:raw_manganese", 2, "minecraft:coal", 1), Map.of("capitalismmod:manganese_ingot", 1), 112, "electric_arc_furnace", 3, 7, 10),
                        new RecipeJson("chromium_concentration", Map.of("capitalismmod:chromium_ore", 2), Map.of("capitalismmod:raw_chromium", 1), 72, "flotation_cell", 2, 3, 6),
                        new RecipeJson("chromium_smelting", Map.of("capitalismmod:raw_chromium", 2, "minecraft:coal", 1), Map.of("capitalismmod:chromium_ingot", 1), 120, "electric_arc_furnace", 3, 8, 11),
                        new RecipeJson("titanium_concentration", Map.of("capitalismmod:titanium_ore", 2), Map.of("capitalismmod:raw_titanium", 1), 82, "flotation_cell", 2, 4, 7),
                        new RecipeJson("titanium_smelting", Map.of("capitalismmod:raw_titanium", 2, "capitalismmod:chlorine", 1), Map.of("capitalismmod:titanium_ingot", 1), 145, "electric_arc_furnace", 3, 9, 12),
                        new RecipeJson("tungsten_concentration", Map.of("capitalismmod:tungsten_ore", 2), Map.of("capitalismmod:raw_tungsten", 1), 95, "flotation_cell", 3, 4, 8),
                        new RecipeJson("tungsten_smelting", Map.of("capitalismmod:raw_tungsten", 2, "minecraft:coal", 1), Map.of("capitalismmod:tungsten_ingot", 1), 165, "electric_arc_furnace", 4, 10, 14),
                        new RecipeJson("molybdenum_concentration", Map.of("capitalismmod:molybdenum_ore", 2), Map.of("capitalismmod:raw_molybdenum", 1), 92, "flotation_cell", 3, 4, 8),
                        new RecipeJson("molybdenum_smelting", Map.of("capitalismmod:raw_molybdenum", 2, "minecraft:coal", 1), Map.of("capitalismmod:molybdenum_ingot", 1), 155, "electric_arc_furnace", 4, 9, 13),
                        new RecipeJson("fluorite_concentration", Map.of("capitalismmod:fluorite_ore", 2), Map.of("capitalismmod:fluorite", 2), 68, "flotation_cell", 2, 3, 5),
                        new RecipeJson("rare_earth_concentration", Map.of("capitalismmod:rare_earth_ore", 2), Map.of("capitalismmod:rare_earth_concentrate", 1), 110, "flotation_cell", 3, 5, 9),
                        new RecipeJson("rare_earth_cracking", Map.of("capitalismmod:rare_earth_concentrate", 1, "capitalismmod:sulfuric_acid", 1), Map.of("capitalismmod:mixed_rare_earth_carbonate", 2, "capitalismmod:radioactive_tailings", 1), 175, "chemical_reactor", 3, 8, 12),
                        new RecipeJson("rare_earth_solvent_separation", Map.of("capitalismmod:mixed_rare_earth_carbonate", 2), Map.of("capitalismmod:neodymium_oxide", 1, "capitalismmod:lanthanum_oxide", 1, "capitalismmod:cerium_oxide", 1, "capitalismmod:dysprosium_oxide", 1), 260, "rare_earth_separation_unit", 4, 11, 16),
                        new RecipeJson("rare_earth_magnet_sintering", Map.of("capitalismmod:neodymium_oxide", 1, "minecraft:iron_ingot", 2, "capitalismmod:cobalt_ingot", 1), Map.of("capitalismmod:rare_earth_magnet", 2), 220, "electric_arc_furnace", 3, 9, 13),
                        new RecipeJson("uranium_ore_milling", Map.of("capitalismmod:uranium_ore", 2), Map.of("capitalismmod:raw_uranium", 1, "capitalismmod:radioactive_tailings", 1), 145, "uranium_milling_circuit", 3, 7, 11),
                        new RecipeJson("yellowcake_leaching", Map.of("capitalismmod:raw_uranium", 2, "capitalismmod:sulfuric_acid", 1), Map.of("capitalismmod:yellowcake", 1, "capitalismmod:radioactive_tailings", 1), 185, "chemical_reactor", 3, 8, 13),
                        new RecipeJson("uranium_hexafluoride_conversion", Map.of("capitalismmod:yellowcake", 1, "capitalismmod:fluorite", 1), Map.of("capitalismmod:uranium_hexafluoride", 1), 240, "chemical_reactor", 4, 10, 15),
                        new RecipeJson("nuclear_fuel_pellet_fabrication", Map.of("capitalismmod:uranium_hexafluoride", 1), Map.of("capitalismmod:nuclear_fuel", 2), 280, "nuclear_fuel_fabricator", 4, 12, 18),
                        new RecipeJson("radiation_shielding_fabrication", Map.of("capitalismmod:lead_ingot", 2, "capitalismmod:steel_sheet", 1), Map.of("capitalismmod:radiation_shielding", 2), 150, "radiation_shielding_station", 2, 6, 9)),
                withRecipes(new IndustryJson("semiconductor_fabrication", Map.of("minecraft:quartz", 2), Map.of("capitalismmod:final_chip_test", 1), 240, "semiconductor_fab", 4, 10, 16),
                        new RecipeJson("silicon_ingot", Map.of("capitalismmod:polysilicon", 2), Map.of("capitalismmod:silicon_ingot", 1), 145, "crystal_growth_furnace", 3, 7, 12),
                        new RecipeJson("silicon_wafer_from_ingot", Map.of("capitalismmod:silicon_ingot", 1), Map.of("capitalismmod:silicon_wafer", 2), 185, "wafer_dicing_saw", 3, 8, 14),
                        new RecipeJson("photoresist_coating", Map.of("capitalismmod:plastic_pellets", 1, "minecraft:glass", 1), Map.of("capitalismmod:photoresist", 2), 155, "chemical_reactor", 2, 5, 9),
                        new RecipeJson("wafer_fabrication", Map.of("capitalismmod:silicon_wafer", 1, "capitalismmod:photoresist", 1, "capitalismmod:electronic_etchant", 1, "capitalismmod:electronic_solvent", 1), Map.of("capitalismmod:tested_wafer", 1), 330, "semiconductor_fab", 4, 11, 18),
                        new RecipeJson("wafer_test", Map.of("capitalismmod:tested_wafer", 1), Map.of("capitalismmod:silicon_die", 4), 220, "chip_testing_station", 3, 7, 12),
                        new RecipeJson("final_chip_test", Map.of("capitalismmod:packaged_chip", 1), Map.of("capitalismmod:final_chip_test", 1), 340, "chip_testing_station", 3, 6, 12)),
                withRecipes(new IndustryJson("pcb_manufacturing", Map.of("minecraft:glass", 1, "capitalismmod:epoxy_resin", 1), Map.of("capitalismmod:assembled_pcb", 1), 260, "pcb_assembly_line", 4, 8, 14),
                        new RecipeJson("copper_clad_laminate", Map.of("capitalismmod:pcb_substrate", 1, "capitalismmod:copper_foil", 1), Map.of("capitalismmod:copper_clad_laminate", 1), 145, "lamination_press", 2, 5, 9),
                        new RecipeJson("pcb_drilling_realistic", Map.of("capitalismmod:copper_clad_laminate", 1), Map.of("capitalismmod:drilled_pcb_panel", 1), 145, "pcb_fabrication_line", 3, 5, 10),
                        new RecipeJson("pcb_etching_realistic", Map.of("capitalismmod:drilled_pcb_panel", 1, "capitalismmod:electronic_etchant", 1), Map.of("capitalismmod:etched_pcb", 1), 165, "pcb_fabrication_line", 3, 6, 11),
                        new RecipeJson("pcb_assembly_realistic", Map.of("capitalismmod:solder_masked_pcb", 1, "capitalismmod:final_chip_test", 1, "capitalismmod:smd_components", 1, "capitalismmod:solder", 1), Map.of("capitalismmod:assembled_pcb", 1), 360, "pcb_assembly_line", 4, 8, 15)),
                withRecipes(new IndustryJson("battery_chemistry", Map.of("capitalismmod:lithium_carbonate", 1), Map.of("capitalismmod:battery_cell", 1), 250, "battery_cell_line", 4, 8, 13),
                        new RecipeJson("cathode_precursor", Map.of("capitalismmod:nickel_sulfate", 1, "capitalismmod:cobalt_ingot", 1, "capitalismmod:manganese_ingot", 1, "capitalismmod:sulfuric_acid", 1), Map.of("capitalismmod:cathode_precursor", 1), 145, "battery_materials", 3, 6, 10),
                        new RecipeJson("lfp_cathode", Map.of("capitalismmod:lithium_carbonate", 1, "capitalismmod:phosphoric_acid", 1, "minecraft:iron_ingot", 1), Map.of("capitalismmod:lfp_cathode", 1), 190, "battery_materials", 3, 7, 11),
                        new RecipeJson("nmc_cathode", Map.of("capitalismmod:lithium_carbonate", 1, "capitalismmod:cathode_precursor", 1), Map.of("capitalismmod:nmc_cathode", 1), 210, "battery_materials", 3, 8, 12),
                        new RecipeJson("lfp_cell_assembly", Map.of("capitalismmod:lfp_cathode", 1, "capitalismmod:graphite_anode", 1, "capitalismmod:battery_separator", 1, "capitalismmod:battery_electrolyte", 1, "capitalismmod:copper_foil", 1), Map.of("capitalismmod:battery_cell", 2), 280, "battery_cell_line", 4, 9, 14),
                        new RecipeJson("nmc_cell_assembly", Map.of("capitalismmod:nmc_cathode", 1, "capitalismmod:graphite_anode", 1, "capitalismmod:battery_separator", 1, "capitalismmod:battery_electrolyte", 1, "capitalismmod:copper_foil", 1), Map.of("capitalismmod:battery_cell", 2), 310, "battery_cell_line", 4, 10, 15)),
                withRecipes(new IndustryJson("mineral_processing", Map.of("minecraft:stone", 2), Map.of("minecraft:gravel", 2), 45, "crusher", 2, 2, 4),
                        new RecipeJson("crushed_stone", Map.of("minecraft:stone", 2), Map.of("minecraft:gravel", 2), 45, "crusher", 2, 2, 4),
                        new RecipeJson("sand_screening", Map.of("minecraft:gravel", 2), Map.of("minecraft:sand", 2), 48, "flotation_cell", 2, 3, 5),
                        new RecipeJson("ore_washing", Map.of("minecraft:raw_iron", 2, "minecraft:water_bucket", 1), Map.of("minecraft:iron_ore", 1), 66, "flotation_cell", 3, 4, 7)),
                withRecipes(new IndustryJson("steelmaking", Map.of("minecraft:iron_ingot", 2), Map.of("capitalismmod:steel_sheet", 1), 95, "electric_arc_furnace", 3, 8, 12),
                        new RecipeJson("electric_arc_steel", Map.of("minecraft:iron_ingot", 2, "minecraft:coal", 1), Map.of("capitalismmod:steel_sheet", 2), 110, "electric_arc_furnace", 3, 8, 12),
                        new RecipeJson("steel_casting", Map.of("capitalismmod:steel_sheet", 1), Map.of("minecraft:iron_block", 1), 130, "electric_arc_furnace", 3, 9, 14)),
                withRecipes(new IndustryJson("construction_materials", Map.of("minecraft:stone", 2, "minecraft:sand", 1), Map.of("minecraft:brick", 2), 60, "cement_kiln", 2, 5, 8),
                        new RecipeJson("cement_clinker", Map.of("minecraft:stone", 2, "minecraft:coal", 1), Map.of("minecraft:brick", 2), 60, "cement_kiln", 2, 5, 8),
                        new RecipeJson("glass_sand", Map.of("minecraft:sand", 2, "minecraft:coal", 1), Map.of("minecraft:glass", 2), 64, "cement_kiln", 2, 4, 7)),
                withRecipes(new IndustryJson("textiles", Map.of("minecraft:string", 4), Map.of("minecraft:white_wool", 1), 72, "textile_mill", 2, 3, 6),
                        new RecipeJson("spun_fiber", Map.of("minecraft:string", 4), Map.of("minecraft:white_wool", 1), 72, "textile_mill", 2, 3, 6),
                        new RecipeJson("leather_fabric", Map.of("minecraft:leather", 2, "minecraft:string", 2), Map.of("minecraft:rabbit_hide", 2), 88, "textile_mill", 2, 4, 7)),
                withRecipes(new IndustryJson("paper_products", Map.of("minecraft:oak_log", 1), Map.of("minecraft:paper", 4), 52, "pulp_digester", 2, 3, 5),
                        new RecipeJson("wood_pulp", Map.of("minecraft:oak_log", 1, "minecraft:water_bucket", 1), Map.of("minecraft:paper", 4), 52, "pulp_digester", 2, 3, 5),
                        new RecipeJson("bookbinding", Map.of("minecraft:paper", 3, "minecraft:leather", 1), Map.of("minecraft:book", 1), 95, "paper_mill", 2, 3, 6)),
                withRecipes(new IndustryJson("food_processing", Map.of("minecraft:wheat", 2), Map.of("minecraft:bread", 2), 82, "food_processor", 2, 3, 6),
                        new RecipeJson("flour_milling", Map.of("minecraft:wheat", 2), Map.of("capitalismmod:flour", 2), 62, "milling_machine", 1, 2, 4),
                        new RecipeJson("bread_baking", Map.of("capitalismmod:flour", 2, "minecraft:sugar", 1), Map.of("minecraft:bread", 2), 82, "food_processor", 2, 3, 6),
                        new RecipeJson("pasteurized_milk", Map.of("minecraft:milk_bucket", 1), Map.of("minecraft:honey_bottle", 1), 96, "pasteurizer", 2, 4, 7),
                        new RecipeJson("canned_rations", Map.of("minecraft:cooked_beef", 1, "capitalismmod:metal_can", 1), Map.of("capitalismmod:canned_food", 1), 125, "cannery", 3, 5, 9)),
                withRecipes(new IndustryJson("basic_inorganic_chemicals", Map.of("capitalismmod:salt", 2), Map.of("capitalismmod:chlorine", 1), 120, "chemical_reactor", 2, 5, 8),
                        new RecipeJson("air_compression", Map.of(), Map.of("capitalismmod:compressed_air", 2), 70, "air_compressor", 1, 3, 5),
                        new RecipeJson("cryogenic_air_separation", Map.of("capitalismmod:compressed_air", 2), Map.of("capitalismmod:nitrogen", 2, "capitalismmod:oxygen", 1, "capitalismmod:argon", 1), 180, "cryogenic_air_separation", 3, 9, 13),
                        new RecipeJson("brine_preparation", Map.of("capitalismmod:salt", 2, "minecraft:water_bucket", 1), Map.of("capitalismmod:brine", 2), 55, "chemical_reactor", 1, 2, 4),
                        new RecipeJson("air_separation", Map.of("capitalismmod:compressed_air", 2), Map.of("capitalismmod:nitrogen", 2, "capitalismmod:oxygen", 1, "capitalismmod:argon", 1), 115, "cryogenic_air_separation", 2, 5, 8),
                        new RecipeJson("hydrochloric_acid_synthesis", Map.of("capitalismmod:chlorine", 1, "capitalismmod:hydrogen", 1, "minecraft:water_bucket", 1), Map.of("capitalismmod:hydrochloric_acid", 1), 95, "chemical_reactor", 2, 4, 7),
                        new RecipeJson("nitric_acid_synthesis", Map.of("capitalismmod:ammonia", 1, "capitalismmod:oxygen", 2, "minecraft:water_bucket", 1), Map.of("capitalismmod:nitric_acid", 1), 135, "chemical_reactor", 3, 6, 9),
                        new RecipeJson("sodium_hypochlorite_synthesis", Map.of("capitalismmod:chlorine", 1, "capitalismmod:caustic_soda", 1, "minecraft:water_bucket", 1), Map.of("capitalismmod:sodium_hypochlorite", 2), 105, "chemical_reactor", 2, 5, 8),
                        new RecipeJson("hydrogen_peroxide_synthesis", Map.of("capitalismmod:hydrogen", 1, "capitalismmod:oxygen", 1, "minecraft:water_bucket", 1), Map.of("capitalismmod:hydrogen_peroxide", 1), 125, "chemical_reactor", 2, 6, 9),
                        new RecipeJson("soda_ash_production", Map.of("capitalismmod:salt", 1, "capitalismmod:limestone", 1), Map.of("capitalismmod:soda_ash", 2), 105, "chemical_reactor", 2, 5, 8),
                        new RecipeJson("sodium_bicarbonate_production", Map.of("capitalismmod:soda_ash", 1, "capitalismmod:carbon_dioxide", 1, "minecraft:water_bucket", 1), Map.of("capitalismmod:sodium_bicarbonate", 2), 85, "chemical_reactor", 2, 4, 7),
                        new RecipeJson("calcium_chloride_production", Map.of("capitalismmod:limestone", 1, "capitalismmod:hydrochloric_acid", 1), Map.of("capitalismmod:calcium_chloride", 2), 95, "chemical_reactor", 2, 4, 7)),
                withRecipes(new IndustryJson("fertilizer_chemicals", Map.of("capitalismmod:hydrogen", 1, "capitalismmod:nitrogen", 1), Map.of("capitalismmod:ammonia", 2), 150, "fertilizer_plant", 3, 7, 11),
                        new RecipeJson("ammonia_synthesis", Map.of("capitalismmod:hydrogen", 3, "capitalismmod:nitrogen", 1), Map.of("capitalismmod:ammonia", 2), 150, "fertilizer_plant", 3, 7, 11),
                        new RecipeJson("urea_synthesis", Map.of("capitalismmod:ammonia", 2, "capitalismmod:carbon_dioxide", 1), Map.of("capitalismmod:urea", 2), 135, "fertilizer_plant", 3, 6, 10),
                        new RecipeJson("ammonium_nitrate_synthesis", Map.of("capitalismmod:ammonia", 1, "capitalismmod:nitric_acid", 1), Map.of("capitalismmod:ammonium_nitrate", 2), 125, "fertilizer_plant", 3, 6, 9),
                        new RecipeJson("phosphate_fertilizer_production", Map.of("capitalismmod:phosphate", 2, "capitalismmod:sulfuric_acid", 1), Map.of("capitalismmod:phosphate_fertilizer", 2), 145, "fertilizer_plant", 3, 7, 10),
                        new RecipeJson("phosphoric_acid_wet_process", Map.of("capitalismmod:phosphate", 2, "capitalismmod:sulfuric_acid", 1, "minecraft:water_bucket", 1), Map.of("capitalismmod:phosphoric_acid", 2), 135, "chemical_reactor", 3, 6, 10),
                        new RecipeJson("ammonium_sulfate_production", Map.of("capitalismmod:ammonia", 1, "capitalismmod:sulfuric_acid", 1), Map.of("capitalismmod:ammonium_sulfate", 2), 125, "fertilizer_plant", 3, 6, 9),
                        new RecipeJson("dap_fertilizer_granulation", Map.of("capitalismmod:ammonia", 2, "capitalismmod:phosphoric_acid", 1), Map.of("capitalismmod:dap_fertilizer", 2), 155, "fertilizer_plant", 3, 7, 10)),
                withRecipes(new IndustryJson("water_treatment", Map.of("minecraft:water_bucket", 1, "capitalismmod:activated_carbon", 1), Map.of("capitalismmod:industrial_water", 2), 105, "chemical_reactor", 2, 5, 8),
                        new RecipeJson("activated_carbon", Map.of("minecraft:coal", 2), Map.of("capitalismmod:activated_carbon", 2), 75, "chemical_reactor", 1, 3, 5),
                        new RecipeJson("alum_coagulation_agent", Map.of("capitalismmod:alumina", 1, "capitalismmod:sulfuric_acid", 1), Map.of("capitalismmod:alum", 2), 115, "chemical_reactor", 2, 5, 8),
                        new RecipeJson("industrial_water_purification", Map.of("minecraft:water_bucket", 1, "capitalismmod:alum", 1, "capitalismmod:activated_carbon", 1), Map.of("capitalismmod:industrial_water", 2), 125, "chemical_reactor", 2, 6, 9),
                        new RecipeJson("chlorine_disinfectant", Map.of("capitalismmod:sodium_hypochlorite", 1, "minecraft:water_bucket", 1), Map.of("capitalismmod:disinfectant", 2), 95, "chemical_reactor", 2, 4, 7),
                        new RecipeJson("coagulation_and_settling", Map.of("minecraft:water_bucket", 1, "capitalismmod:alum", 1), Map.of("capitalismmod:wastewater_sludge", 1), 95, "chemical_reactor", 2, 4, 7),
                        new RecipeJson("sludge_carbonization", Map.of("capitalismmod:wastewater_sludge", 2), Map.of("capitalismmod:activated_carbon", 1), 115, "chemical_reactor", 2, 5, 8)),
                withRecipes(new IndustryJson("coatings_and_adhesives", Map.of("capitalismmod:epoxy_resin", 1), Map.of("capitalismmod:industrial_coating", 1), 125, "chemical_reactor", 2, 5, 8),
                        new RecipeJson("industrial_coating", Map.of("capitalismmod:epoxy_resin", 1, "capitalismmod:industrial_dye", 1), Map.of("capitalismmod:industrial_coating", 2), 125, "chemical_reactor", 2, 5, 8),
                        new RecipeJson("industrial_adhesive", Map.of("capitalismmod:phenolic_resin", 1, "capitalismmod:epoxy_resin", 1), Map.of("capitalismmod:industrial_adhesive", 2), 145, "polymer_reactor", 3, 6, 10),
                        new RecipeJson("industrial_dye", Map.of("capitalismmod:benzene", 1, "minecraft:lapis_lazuli", 1), Map.of("capitalismmod:industrial_dye", 2), 115, "chemical_reactor", 2, 5, 8)),
                withRecipes(new IndustryJson("biochemical_industry", Map.of("minecraft:wheat", 2), Map.of("capitalismmod:ethanol", 2), 80, "chemical_reactor", 2, 3, 6),
                        new RecipeJson("culture_medium_preparation", Map.of("capitalismmod:glucose", 1, "minecraft:wheat", 1, "minecraft:water_bucket", 1), Map.of("capitalismmod:culture_medium", 2), 80, "chemical_reactor", 2, 3, 6),
                        new RecipeJson("yeast_culture_propagation", Map.of("capitalismmod:culture_medium", 1), Map.of("capitalismmod:yeast_culture", 2), 90, "fermentation_tank", 2, 4, 7),
                        new RecipeJson("glucose_hydrolysis", Map.of("minecraft:wheat", 2, "minecraft:water_bucket", 1), Map.of("capitalismmod:glucose", 2), 72, "chemical_reactor", 2, 3, 5),
                        new RecipeJson("citric_acid_fermentation", Map.of("capitalismmod:glucose", 2), Map.of("capitalismmod:citric_acid", 2), 95, "chemical_reactor", 2, 4, 7),
                        new RecipeJson("bioethanol_fermentation", Map.of("capitalismmod:glucose", 2, "capitalismmod:yeast_culture", 1), Map.of("capitalismmod:fermentation_broth", 2, "capitalismmod:carbon_dioxide", 1), 120, "fermentation_tank", 2, 5, 8),
                        new RecipeJson("ethanol_distillation", Map.of("capitalismmod:fermentation_broth", 2), Map.of("capitalismmod:ethanol", 2), 105, "chemical_reactor", 2, 4, 7),
                        new RecipeJson("lactic_acid_fermentation", Map.of("capitalismmod:glucose", 2, "capitalismmod:yeast_culture", 1), Map.of("capitalismmod:lactic_acid", 2), 125, "fermentation_tank", 2, 5, 8),
                        new RecipeJson("pla_polymerization", Map.of("capitalismmod:lactic_acid", 2), Map.of("capitalismmod:pla_pellets", 2), 145, "polymer_reactor", 3, 6, 10),
                        new RecipeJson("biodegradable_packaging", Map.of("capitalismmod:pla_pellets", 1), Map.of("capitalismmod:biodegradable_packaging", 3), 105, "extrusion_line", 2, 4, 7)),
                withRecipes(new IndustryJson("pharmaceuticals", Map.of("capitalismmod:phenol", 1, "capitalismmod:sulfuric_acid", 1), Map.of("capitalismmod:active_pharmaceutical", 1), 180, "pharmaceutical_reactor", 3, 7, 11),
                        new RecipeJson("pharmaceutical_excipient_blending", Map.of("minecraft:wheat", 1, "capitalismmod:glucose", 1), Map.of("capitalismmod:pharmaceutical_excipient", 2), 85, "pharmaceutical_reactor", 2, 3, 6),
                        new RecipeJson("salicylic_acid_synthesis", Map.of("capitalismmod:phenol", 1, "capitalismmod:oxygen", 1), Map.of("capitalismmod:salicylic_acid", 1), 145, "pharmaceutical_reactor", 3, 6, 10),
                        new RecipeJson("acetic_acid_bioconversion", Map.of("capitalismmod:ethanol", 1, "capitalismmod:oxygen", 1), Map.of("capitalismmod:acetic_acid", 1), 105, "fermentation_tank", 2, 4, 7),
                        new RecipeJson("aspirin_active_ingredient", Map.of("capitalismmod:salicylic_acid", 1, "capitalismmod:acetic_acid", 1), Map.of("capitalismmod:active_pharmaceutical", 1), 175, "pharmaceutical_reactor", 3, 7, 11),
                        new RecipeJson("aspirin_intermediate_synthesis", Map.of("capitalismmod:salicylic_acid", 1, "capitalismmod:acetone", 1), Map.of("capitalismmod:aspirin_intermediate", 1), 155, "pharmaceutical_reactor", 3, 7, 10),
                        new RecipeJson("painkiller_tablet_forming", Map.of("capitalismmod:active_pharmaceutical", 1, "capitalismmod:pharmaceutical_excipient", 1), Map.of("capitalismmod:painkiller_tablet", 2), 135, "tablet_press", 2, 5, 8),
                        new RecipeJson("tablet_film_coating", Map.of("capitalismmod:painkiller_tablet", 2, "capitalismmod:industrial_coating", 1), Map.of("capitalismmod:coated_tablet", 2), 120, "pharmaceutical_reactor", 2, 5, 8),
                        new RecipeJson("sterile_solution_filling", Map.of("capitalismmod:active_pharmaceutical", 1, "capitalismmod:industrial_water", 1), Map.of("capitalismmod:sterile_solution", 2), 155, "sterile_filling_line", 3, 6, 10),
                        new RecipeJson("active_pharmaceutical_synthesis", Map.of("capitalismmod:phenol", 1, "capitalismmod:sulfuric_acid", 1), Map.of("capitalismmod:active_pharmaceutical", 1), 180, "pharmaceutical_reactor", 3, 7, 11),
                        new RecipeJson("antibiotic_tablet_forming", Map.of("capitalismmod:active_pharmaceutical", 1, "minecraft:paper", 1), Map.of("capitalismmod:antibiotic_tablet", 2), 135, "pharmaceutical_reactor", 2, 5, 8)),
                withRecipes(new IndustryJson("textile_chemistry", Map.of("minecraft:lapis_lazuli", 1), Map.of("capitalismmod:industrial_dye", 2), 85, "chemical_reactor", 2, 3, 6),
                        new RecipeJson("peroxide_bleaching_agent", Map.of("capitalismmod:hydrogen_peroxide", 1, "capitalismmod:caustic_soda", 1), Map.of("capitalismmod:bleaching_agent", 2), 105, "chemical_reactor", 2, 4, 7),
                        new RecipeJson("fiber_bleaching", Map.of("minecraft:string", 2, "capitalismmod:bleaching_agent", 1), Map.of("capitalismmod:dyed_fiber", 2), 95, "textile_mill", 2, 4, 7),
                        new RecipeJson("fiber_dyeing", Map.of("capitalismmod:pet_fiber", 1, "capitalismmod:industrial_dye", 1), Map.of("capitalismmod:dyed_fiber", 2), 95, "textile_mill", 2, 4, 7),
                        new RecipeJson("textile_finishing", Map.of("capitalismmod:dyed_fiber", 2, "capitalismmod:industrial_coating", 1), Map.of("capitalismmod:finished_textile", 2), 115, "textile_mill", 2, 5, 8)),
                withRecipes(new IndustryJson("fine_chemicals_and_solvents", Map.of("capitalismmod:methanol", 1, "capitalismmod:acetic_acid", 1), Map.of("capitalismmod:ethyl_acetate", 2), 120, "chemical_reactor", 2, 5, 8),
                        new RecipeJson("ethyl_acetate_esterification", Map.of("capitalismmod:ethanol", 1, "capitalismmod:acetic_acid", 1), Map.of("capitalismmod:ethyl_acetate", 2), 120, "chemical_reactor", 2, 5, 8),
                        new RecipeJson("solvent_blend_formulation", Map.of("capitalismmod:acetone", 1, "capitalismmod:ethanol", 1, "capitalismmod:ethyl_acetate", 1), Map.of("capitalismmod:solvent_blend", 3), 90, "solvent_recovery_unit", 2, 4, 7)),
                withRecipes(new IndustryJson("crop_protection_chemicals", Map.of("capitalismmod:phenol", 1, "capitalismmod:chlorine", 1), Map.of("capitalismmod:pesticide_active", 1), 165, "pesticide_reactor", 3, 7, 11),
                        new RecipeJson("pesticide_active_synthesis", Map.of("capitalismmod:phenol", 1, "capitalismmod:chlorine", 1), Map.of("capitalismmod:pesticide_active", 1), 165, "pesticide_reactor", 3, 7, 11),
                        new RecipeJson("herbicide_formulation", Map.of("capitalismmod:pesticide_active", 1, "capitalismmod:caustic_soda", 1), Map.of("capitalismmod:herbicide", 2), 115, "pesticide_reactor", 2, 5, 8),
                        new RecipeJson("fungicide_formulation", Map.of("capitalismmod:pesticide_active", 1, "capitalismmod:sulfuric_acid", 1), Map.of("capitalismmod:fungicide", 2), 125, "pesticide_reactor", 2, 6, 9)),
                withRecipes(new IndustryJson("rubber_and_tire_manufacturing", Map.of("capitalismmod:synthetic_rubber", 2, "capitalismmod:carbon_black", 1), Map.of("capitalismmod:tire", 2), 180, "tire_press", 3, 8, 12),
                        new RecipeJson("carbon_black_production", Map.of("capitalismmod:refinery_gas", 2, "minecraft:coal", 1), Map.of("capitalismmod:carbon_black", 2), 115, "chemical_reactor", 2, 5, 8),
                        new RecipeJson("tire_compounding_and_vulcanization", Map.of("capitalismmod:synthetic_rubber", 2, "capitalismmod:carbon_black", 1, "capitalismmod:sulfur", 1), Map.of("capitalismmod:tire", 2), 180, "tire_press", 3, 8, 12),
                        new RecipeJson("rubber_sealant_formulation", Map.of("capitalismmod:synthetic_rubber", 1, "capitalismmod:phenolic_resin", 1), Map.of("capitalismmod:industrial_sealant", 2), 135, "polymer_reactor", 2, 6, 9)),
                withRecipes(new IndustryJson("electronic_chemicals", Map.of("minecraft:quartz", 1, "capitalismmod:sulfuric_acid", 1), Map.of("capitalismmod:hydrofluoric_acid", 1), 155, "electronic_chemical_unit", 3, 7, 11),
                        new RecipeJson("hydrofluoric_acid_preparation", Map.of("capitalismmod:fluorite", 1, "capitalismmod:sulfuric_acid", 1), Map.of("capitalismmod:hydrofluoric_acid", 1), 155, "electronic_chemical_unit", 3, 7, 11),
                        new RecipeJson("electronic_solvent_purification", Map.of("capitalismmod:acetone", 1, "capitalismmod:ethyl_acetate", 1), Map.of("capitalismmod:electronic_solvent", 2), 115, "solvent_recovery_unit", 2, 5, 8),
                        new RecipeJson("electronic_etchant_blending", Map.of("capitalismmod:sulfuric_acid", 1, "capitalismmod:hydrogen_peroxide", 1), Map.of("capitalismmod:electronic_etchant", 2), 135, "electronic_chemical_unit", 3, 6, 10)),
                withRecipes(new IndustryJson("paper_chemicals", Map.of("minecraft:paper", 2, "capitalismmod:paper_coating", 1), Map.of("capitalismmod:coated_paper", 2), 100, "paper_coating_line", 2, 4, 7),
                        new RecipeJson("paper_coating_compound", Map.of("capitalismmod:epoxy_resin", 1, "capitalismmod:alum", 1), Map.of("capitalismmod:paper_coating", 2), 120, "chemical_reactor", 2, 5, 8),
                        new RecipeJson("printing_ink_formulation", Map.of("capitalismmod:industrial_dye", 1, "capitalismmod:solvent_blend", 1), Map.of("capitalismmod:printing_ink", 2), 105, "chemical_reactor", 2, 4, 7),
                        new RecipeJson("paper_printing", Map.of("capitalismmod:coated_paper", 1, "capitalismmod:printing_ink", 1), Map.of("capitalismmod:printed_paper", 2), 90, "paper_coating_line", 2, 4, 6)),
                withRecipes(new IndustryJson("surfactants_and_personal_care", Map.of("capitalismmod:ethylene_oxide", 1, "capitalismmod:sulfuric_acid", 1), Map.of("capitalismmod:surfactant", 2), 140, "surfactant_reactor", 3, 6, 10),
                        new RecipeJson("surfactant_synthesis", Map.of("capitalismmod:ethylene_oxide", 1, "capitalismmod:sulfuric_acid", 1), Map.of("capitalismmod:surfactant", 2), 140, "surfactant_reactor", 3, 6, 10),
                        new RecipeJson("detergent_formulation", Map.of("capitalismmod:surfactant", 1, "capitalismmod:soda_ash", 1), Map.of("capitalismmod:detergent", 2), 105, "surfactant_reactor", 2, 5, 8),
                        new RecipeJson("cleaning_agent_blending", Map.of("capitalismmod:detergent", 1, "capitalismmod:industrial_water", 1), Map.of("capitalismmod:cleaning_agent", 2), 95, "chemical_reactor", 2, 4, 7)),
                withRecipes(new IndustryJson("carbon_circular_chemicals", Map.of("minecraft:coal", 2, "minecraft:water_bucket", 1), Map.of("capitalismmod:carbon_dioxide", 2), 155, "carbon_capture_unit", 3, 7, 11),
                        new RecipeJson("carbon_capture_and_purification", Map.of("minecraft:coal", 2, "minecraft:water_bucket", 1), Map.of("capitalismmod:carbon_dioxide", 2), 155, "carbon_capture_unit", 3, 7, 11),
                        new RecipeJson("co2_hydrogenation_to_methanol", Map.of("capitalismmod:carbon_dioxide", 1, "capitalismmod:hydrogen", 3), Map.of("capitalismmod:methanol", 1), 165, "methanol_synthesis_unit", 3, 8, 12)),
                withRecipes(new IndustryJson("industrial_gases", Map.of("capitalismmod:oxygen", 1), Map.of("capitalismmod:medical_oxygen", 1), 95, "chemical_reactor", 2, 4, 7),
                        new RecipeJson("medical_oxygen_purification", Map.of("capitalismmod:oxygen", 1), Map.of("capitalismmod:medical_oxygen", 1), 95, "chemical_reactor", 2, 4, 7),
                        new RecipeJson("argon_welding_gas", Map.of("capitalismmod:argon", 1, "capitalismmod:oxygen", 1), Map.of("capitalismmod:welding_gas", 2), 105, "chemical_reactor", 2, 4, 7),
                        new RecipeJson("food_grade_co2_purification", Map.of("capitalismmod:carbon_dioxide", 1), Map.of("capitalismmod:food_grade_carbon_dioxide", 1), 85, "chemical_reactor", 2, 3, 6)),
                withRecipes(new IndustryJson("agrochemicals", Map.of("minecraft:bone", 1, "minecraft:coal", 1), Map.of("minecraft:bone_meal", 3), 70, "fertilizer_plant", 2, 5, 8),
                        new RecipeJson("nitrogen_fertilizer", Map.of("minecraft:bone", 1, "minecraft:coal", 1), Map.of("minecraft:bone_meal", 3), 70, "fertilizer_plant", 2, 5, 8),
                        new RecipeJson("soil_conditioner", Map.of("minecraft:bone_meal", 2, "minecraft:rotten_flesh", 1), Map.of("minecraft:bone_meal", 4), 88, "fertilizer_plant", 2, 5, 9)),
                withRecipes(new IndustryJson("precision_machinery", Map.of("minecraft:iron_ingot", 2, "minecraft:diamond", 1), Map.of("minecraft:anvil", 1), 180, "cnc_machining_center", 3, 7, 11),
                        new RecipeJson("precision_part", Map.of("minecraft:iron_ingot", 2, "minecraft:diamond", 1), Map.of("minecraft:anvil", 1), 180, "cnc_machining_center", 3, 7, 11),
                        new RecipeJson("tooling", Map.of("minecraft:iron_ingot", 1, "minecraft:redstone", 1), Map.of("minecraft:iron_nugget", 8), 90, "cnc_machining_center", 2, 5, 8)),
                withRecipes(new IndustryJson("utilities", Map.of("minecraft:coal", 1), Map.of(), 60),
                        new RecipeJson("coal_generation", Map.of("minecraft:coal", 1), Map.of(), 60,
                                "none", 1, 0, 0),
                        new RecipeJson("fuel_generation", Map.of("capitalismmod:fuel_oil", 1), Map.of(), 90,
                                "none", 1, 0, 0),
                        new RecipeJson("diesel_generation", Map.of("capitalismmod:diesel", 1), Map.of(), 100,
                                "none", 1, 0, 0),
                        new RecipeJson("lpg_generation", Map.of("capitalismmod:lpg", 1), Map.of(), 92,
                                "none", 1, 0, 0)),
                new IndustryJson("construction", Map.of("minecraft:rail", 1), Map.of(), 50),
                withRecipes(new IndustryJson("transport", Map.of("minecraft:coal", 1), Map.of(), 45),
                        new RecipeJson("coal_transport", Map.of("minecraft:coal", 1), Map.of(), 45,
                                "none", 1, 0, 0),
                        new RecipeJson("fuel_transport", Map.of("capitalismmod:fuel_oil", 1), Map.of(), 70,
                                "none", 1, 0, 0),
                        new RecipeJson("diesel_transport", Map.of("capitalismmod:diesel", 1), Map.of(), 82,
                                "none", 1, 0, 0),
                        new RecipeJson("gasoline_transport", Map.of("capitalismmod:gasoline", 1), Map.of(), 78,
                                "none", 1, 0, 0),
                        new RecipeJson("lpg_transport", Map.of("capitalismmod:lpg", 1), Map.of(), 74,
                                "none", 1, 0, 0)),
                new IndustryJson("hospitality", Map.of("minecraft:wheat", 1), Map.of(), 35),
                new IndustryJson("retail", Map.of("minecraft:rail", 1, "minecraft:wheat", 1), Map.of(), 45),
                new IndustryJson("it_services", Map.of("minecraft:iron_ingot", 1), Map.of(), 90),
                new IndustryJson("research", Map.of("minecraft:iron_ingot", 1, "minecraft:coal", 1), Map.of(), 75),
                new IndustryJson("healthcare", Map.of("minecraft:wheat", 1), Map.of(), 65),
                new IndustryJson("real_estate", Map.of("minecraft:rail", 1), Map.of(), 70),
                new IndustryJson("business_services", Map.of(), Map.of(), 55),
                new IndustryJson("environment", Map.of(), Map.of(), 30),
                new IndustryJson("consumer_services", Map.of(), Map.of(), 30),
                new IndustryJson("education", Map.of(), Map.of(), 40),
                new IndustryJson("culture", Map.of(), Map.of(), 50),
                new IndustryJson("public_admin", Map.of(), Map.of(), 30),
                new IndustryJson("intl_org", Map.of(), Map.of(), 40),
                new IndustryJson("finance", Map.of(), Map.of(), 0)
        );
    }

    private static IndustryJson withRecipes(IndustryJson industry, RecipeJson... recipes) {
        industry.recipes = List.of(recipes);
        return industry;
    }

    public static class IndustryJson {
        public String id = "mining";
        public Map<String, Integer> inputs = new HashMap<>();
        public Map<String, Integer> outputs = new HashMap<>();
        public long income = 55;
        public String machine_type = "none";
        public int workers_per_cycle = 0;
        public long energy_cost = 0L;
        public long maintenance_cost = 0L;
        public String material_class = "intermediate";
        public String quality_tier = "industrial";
        public Map<String, Integer> byproducts = new HashMap<>();
        public int pollution_score = 0;
        public boolean hazardous = false;
        public List<RecipeJson> recipes = new ArrayList<>();

        public IndustryJson() {
        }

        public IndustryJson(String id, Map<String, Integer> inputs, Map<String, Integer> outputs, long income) {
            this.id = id;
            this.inputs = new HashMap<>(inputs);
            this.outputs = new HashMap<>(outputs);
            this.income = income;
            this.workers_per_cycle = outputs.isEmpty() ? (income > 0 ? 1 : 0) : 1;
        }

        public IndustryJson(String id, Map<String, Integer> inputs, Map<String, Integer> outputs, long income,
                            String machineType, int workers, long energy, long maintenance) {
            this(id, inputs, outputs, income);
            this.machine_type = machineType;
            this.workers_per_cycle = workers;
            this.energy_cost = energy;
            this.maintenance_cost = maintenance;
        }
    }

    public static class RecipeJson {
        public String id = "default";
        public Map<String, Integer> inputs = new HashMap<>();
        public Map<String, Integer> outputs = new HashMap<>();
        public long income = 0L;
        public String machine_type = "none";
        public int workers_per_cycle = 0;
        public long energy_cost = 0L;
        public long maintenance_cost = 0L;
        public String material_class = "intermediate";
        public String quality_tier = "industrial";
        public Map<String, Integer> byproducts = new HashMap<>();
        public int pollution_score = 0;
        public boolean hazardous = false;

        public RecipeJson() {
        }

        public RecipeJson(String id, Map<String, Integer> inputs, Map<String, Integer> outputs, long income,
                          String machineType, int workers, long energy, long maintenance) {
            this.id = id;
            this.inputs = new HashMap<>(inputs);
            this.outputs = new HashMap<>(outputs);
            this.income = income;
            this.machine_type = machineType;
            this.workers_per_cycle = workers;
            this.energy_cost = energy;
            this.maintenance_cost = maintenance;
        }
    }
}
