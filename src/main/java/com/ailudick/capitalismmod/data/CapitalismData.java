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
                            recipe.maintenance_cost));
                }
            }
            result.add(recipes.isEmpty()
                    ? new IndustrySpec(j.id, j.inputs, j.outputs, j.income, j.machine_type,
                    j.workers_per_cycle, j.energy_cost, j.maintenance_cost)
                    : new IndustrySpec(j.id, j.inputs, j.outputs, j.income, j.machine_type,
                    j.workers_per_cycle, j.energy_cost, j.maintenance_cost, recipes));
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
                new CommodityJson("capitalismmod:phone_casing", 75),
                new CommodityJson("capitalismmod:camera_module", 180),
                new CommodityJson("capitalismmod:speaker_module", 85),
                new CommodityJson("capitalismmod:microphone_module", 70),
                new CommodityJson("capitalismmod:charging_port", 90),
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
                        new RecipeJson("gold_smelting", Map.of("minecraft:raw_gold", 1, "minecraft:coal", 1), Map.of("minecraft:gold_ingot", 1), 62, "blast_furnace", 2, 3, 5)),
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
                        new RecipeJson("refining", Map.of("capitalismmod:crude_oil", 2), Map.of(
                                "capitalismmod:naphtha", 1, "capitalismmod:fuel_oil", 1,
                                "capitalismmod:diesel", 1, "capitalismmod:gasoline", 1,
                                "capitalismmod:lpg", 1, "capitalismmod:base_oil", 1,
                                "capitalismmod:asphalt", 1), 130, "oil_refinery", 4, 8, 14),
                        new RecipeJson("steam_cracking", Map.of("capitalismmod:naphtha", 1), Map.of("capitalismmod:ethylene", 1, "capitalismmod:propylene", 1), 115, "steam_cracker", 3, 7, 12),
                        new RecipeJson("polyethylene_pellets", Map.of("capitalismmod:ethylene", 1), Map.of("capitalismmod:plastic_pellets", 3), 78, "polymer_reactor", 2, 4, 7),
                        new RecipeJson("polypropylene_pellets", Map.of("capitalismmod:propylene", 1), Map.of("capitalismmod:plastic_pellets", 3), 82, "polymer_reactor", 2, 4, 7),
                        new RecipeJson("polyethylene_grade", Map.of("capitalismmod:ethylene", 1), Map.of("capitalismmod:polyethylene_pellets", 3), 82, "polymer_reactor", 2, 4, 7),
                        new RecipeJson("polypropylene_grade", Map.of("capitalismmod:propylene", 1), Map.of("capitalismmod:polypropylene_pellets", 3), 86, "polymer_reactor", 2, 4, 7),
                        new RecipeJson("synthetic_rubber", Map.of("capitalismmod:propylene", 1, "capitalismmod:naphtha", 1), Map.of("capitalismmod:synthetic_rubber", 2), 105, "polymer_reactor", 3, 5, 9),
                        new RecipeJson("ethylene_glycol", Map.of("capitalismmod:ethylene", 1, "minecraft:water_bucket", 1), Map.of("capitalismmod:ethylene_glycol", 2), 115, "chemical_reactor", 3, 5, 9),
                        new RecipeJson("epoxy_resin", Map.of("capitalismmod:ethylene_glycol", 1, "capitalismmod:plastic_pellets", 1), Map.of("capitalismmod:epoxy_resin", 2), 145, "polymer_reactor", 3, 5, 10),
                        new RecipeJson("benzene_reforming", Map.of("capitalismmod:naphtha", 1), Map.of("capitalismmod:benzene", 1), 108, "oil_refinery", 3, 6, 11),
                        new RecipeJson("styrene_monomer", Map.of("capitalismmod:benzene", 1, "capitalismmod:ethylene", 1), Map.of("capitalismmod:styrene_monomer", 1), 145, "chemical_reactor", 3, 6, 11),
                        new RecipeJson("abs_resin", Map.of("capitalismmod:styrene_monomer", 1, "capitalismmod:synthetic_rubber", 1, "capitalismmod:plastic_pellets", 1), Map.of("capitalismmod:abs_resin", 2), 210, "polymer_reactor", 3, 6, 11),
                        new RecipeJson("lubricant_blending", Map.of("capitalismmod:base_oil", 1, "capitalismmod:fuel_oil", 1), Map.of("capitalismmod:lubricant", 2), 95, "blending_unit", 2, 3, 7),
                        new RecipeJson("plastic_pellets", Map.of("capitalismmod:naphtha", 1, "minecraft:coal", 1), Map.of("capitalismmod:plastic_pellets", 3), 70, "polymer_reactor", 2, 4, 7),
                        new RecipeJson("fan_blades", Map.of("capitalismmod:plastic_pellets", 2, "capitalismmod:steel_sheet", 1), Map.of("capitalismmod:fan_blades", 1), 105, "injection_molder", 2, 3, 8),
                        new RecipeJson("fan_control", Map.of("capitalismmod:copper_wire", 1, "minecraft:redstone", 1, "minecraft:iron_ingot", 1), Map.of("capitalismmod:fan_control", 1), 95, "assembly_line", 2, 2, 6),
                        new RecipeJson("polysilicon", Map.of("minecraft:quartz", 2, "minecraft:coal", 1), Map.of("capitalismmod:polysilicon", 2), 210, "silicon_refiner", 3, 8, 15),
                        new RecipeJson("silicon_wafer", Map.of("minecraft:quartz", 2, "minecraft:coal", 1), Map.of("capitalismmod:silicon_wafer", 1), 180, "semiconductor_fab", 3, 8, 16),
                        new RecipeJson("silicon_wafer_from_polysilicon", Map.of("capitalismmod:polysilicon", 2), Map.of("capitalismmod:silicon_wafer", 1), 230, "crystal_growth_furnace", 3, 9, 16),
                        new RecipeJson("wafer_dicing", Map.of("capitalismmod:silicon_wafer", 1), Map.of("capitalismmod:silicon_die", 4), 190, "wafer_dicing_saw", 3, 6, 12),
                        new RecipeJson("die_testing", Map.of("capitalismmod:silicon_die", 2), Map.of("capitalismmod:tested_die", 2), 225, "chip_testing_station", 3, 6, 13),
                        new RecipeJson("lead_frame_stamping", Map.of("capitalismmod:copper_foil", 1, "capitalismmod:steel_sheet", 1), Map.of("capitalismmod:lead_frame", 2), 105, "stamping_press", 2, 4, 8),
                        new RecipeJson("mold_compound", Map.of("capitalismmod:epoxy_resin", 1, "capitalismmod:plastic_pellets", 1), Map.of("capitalismmod:mold_compound", 2), 125, "molding_compound_unit", 2, 4, 8),
                        new RecipeJson("chip_packaging", Map.of("capitalismmod:silicon_wafer", 1, "capitalismmod:copper_wire", 1, "capitalismmod:plastic_pellets", 1), Map.of("capitalismmod:packaged_chip", 1), 300, "chip_packaging_line", 3, 7, 12),
                        new RecipeJson("tested_die_packaging", Map.of("capitalismmod:tested_die", 1, "capitalismmod:lead_frame", 1, "capitalismmod:copper_wire", 1, "capitalismmod:mold_compound", 1), Map.of("capitalismmod:packaged_chip", 1), 360, "chip_packaging_line", 4, 8, 15),
                        new RecipeJson("circuit_board", Map.of("capitalismmod:silicon_wafer", 1, "capitalismmod:copper_wire", 2, "capitalismmod:plastic_pellets", 1), Map.of("capitalismmod:circuit_board", 1), 260, "electronics_assembly", 3, 5, 10),
                        new RecipeJson("circuit_board_with_packaged_chip", Map.of("capitalismmod:packaged_chip", 1, "capitalismmod:copper_wire", 2, "capitalismmod:plastic_pellets", 1), Map.of("capitalismmod:circuit_board", 1), 280, "electronics_assembly", 3, 5, 10),
                        new RecipeJson("pcb_substrate", Map.of("minecraft:glass", 1, "capitalismmod:epoxy_resin", 1), Map.of("capitalismmod:pcb_substrate", 2), 105, "lamination_press", 2, 4, 8),
                        new RecipeJson("copper_foil", Map.of("minecraft:copper_ingot", 1), Map.of("capitalismmod:copper_foil", 2), 58, "rolling_mill", 2, 2, 5),
                        new RecipeJson("solder", Map.of("minecraft:copper_ingot", 1, "minecraft:iron_ingot", 1), Map.of("capitalismmod:solder", 2), 52, "alloy_furnace", 2, 3, 6),
                        new RecipeJson("pcb_drilling", Map.of("capitalismmod:pcb_substrate", 1, "capitalismmod:copper_foil", 1), Map.of("capitalismmod:drilled_pcb_panel", 1), 130, "pcb_fabrication_line", 3, 5, 10),
                        new RecipeJson("pcb_etching", Map.of("capitalismmod:drilled_pcb_panel", 1, "minecraft:water_bucket", 1), Map.of("capitalismmod:etched_pcb", 1), 155, "pcb_fabrication_line", 3, 6, 11),
                        new RecipeJson("pcb_solder_mask", Map.of("capitalismmod:etched_pcb", 1, "capitalismmod:epoxy_resin", 1), Map.of("capitalismmod:solder_masked_pcb", 1), 180, "pcb_fabrication_line", 3, 6, 11),
                        new RecipeJson("smd_components", Map.of("minecraft:redstone", 1, "capitalismmod:copper_wire", 1, "capitalismmod:plastic_pellets", 1), Map.of("capitalismmod:smd_components", 2), 175, "smt_line", 3, 5, 11),
                        new RecipeJson("passive_components", Map.of("minecraft:redstone", 1, "minecraft:iron_ingot", 1, "capitalismmod:copper_wire", 1), Map.of("capitalismmod:passive_components", 4), 155, "smt_line", 3, 5, 10),
                        new RecipeJson("power_management_ic", Map.of("capitalismmod:packaged_chip", 1, "capitalismmod:passive_components", 1, "capitalismmod:copper_wire", 1), Map.of("capitalismmod:power_management_ic", 1), 330, "smt_line", 3, 6, 12),
                        new RecipeJson("display_driver", Map.of("capitalismmod:packaged_chip", 1, "capitalismmod:passive_components", 1, "capitalismmod:copper_wire", 1), Map.of("capitalismmod:display_driver", 1), 350, "smt_line", 3, 6, 12),
                        new RecipeJson("printed_circuit_board", Map.of("capitalismmod:pcb_substrate", 1, "capitalismmod:copper_foil", 1, "capitalismmod:solder", 1, "capitalismmod:smd_components", 1, "capitalismmod:packaged_chip", 1), Map.of("capitalismmod:circuit_board", 1), 340, "pcb_assembly_line", 4, 7, 14),
                        new RecipeJson("assembled_fabricated_pcb", Map.of("capitalismmod:solder_masked_pcb", 1, "capitalismmod:solder", 1, "capitalismmod:smd_components", 1, "capitalismmod:packaged_chip", 1), Map.of("capitalismmod:circuit_board", 1), 390, "pcb_assembly_line", 4, 8, 15),
                        new RecipeJson("display_panel", Map.of("minecraft:glass", 1, "capitalismmod:silicon_wafer", 1, "capitalismmod:copper_wire", 1, "capitalismmod:plastic_pellets", 1), Map.of("capitalismmod:display_panel", 1), 330, "electronics_assembly", 4, 7, 13),
                        new RecipeJson("phone_casing", Map.of("capitalismmod:abs_resin", 1), Map.of("capitalismmod:phone_casing", 1), 145, "injection_molder", 2, 3, 8),
                        new RecipeJson("camera_module", Map.of("capitalismmod:glass_lens", 1, "capitalismmod:circuit_board", 1, "capitalismmod:plastic_pellets", 1), Map.of("capitalismmod:camera_module", 1), 280, "electronics_assembly", 3, 5, 10),
                        new RecipeJson("speaker_module", Map.of("capitalismmod:copper_wire", 1, "minecraft:iron_ingot", 1, "capitalismmod:plastic_pellets", 1), Map.of("capitalismmod:speaker_module", 1), 145, "electronics_assembly", 2, 3, 8),
                        new RecipeJson("microphone_module", Map.of("capitalismmod:copper_wire", 1, "minecraft:redstone", 1, "capitalismmod:plastic_pellets", 1), Map.of("capitalismmod:microphone_module", 1), 125, "electronics_assembly", 2, 3, 8),
                        new RecipeJson("charging_port", Map.of("capitalismmod:copper_wire", 1, "minecraft:iron_ingot", 1, "capitalismmod:plastic_pellets", 1), Map.of("capitalismmod:charging_port", 1), 135, "electronics_assembly", 2, 3, 8),
                        new RecipeJson("battery_cell", Map.of("minecraft:iron_ingot", 1, "minecraft:copper_ingot", 1, "minecraft:redstone", 1), Map.of("capitalismmod:battery_cell", 2), 80, "electronics_assembly", 2, 3, 7),
                        new RecipeJson("battery", Map.of("capitalismmod:battery_cell", 2, "capitalismmod:copper_wire", 1, "capitalismmod:plastic_pellets", 1), Map.of("capitalismmod:battery", 1), 120, "electronics_assembly", 2, 3, 7),
                        new RecipeJson("lithium_mineral_concentration", Map.of("minecraft:quartz", 2), Map.of("capitalismmod:lithium_mineral", 1), 72, "ore_processor", 2, 2, 5),
                        new RecipeJson("lithium_carbonate_refining", Map.of("capitalismmod:lithium_mineral", 1, "minecraft:coal", 1), Map.of("capitalismmod:lithium_carbonate", 1), 135, "battery_materials", 3, 6, 10),
                        new RecipeJson("graphite_anode_processing", Map.of("minecraft:coal", 2, "capitalismmod:lubricant", 1), Map.of("capitalismmod:graphite_anode", 1), 110, "battery_materials", 3, 5, 9),
                        new RecipeJson("cathode_active_material", Map.of("capitalismmod:lithium_carbonate", 1, "minecraft:iron_ingot", 1, "minecraft:redstone", 1), Map.of("capitalismmod:cathode_active_material", 1), 175, "battery_materials", 3, 7, 11),
                        new RecipeJson("battery_separator", Map.of("capitalismmod:polypropylene_pellets", 1), Map.of("capitalismmod:battery_separator", 2), 68, "polymer_reactor", 2, 4, 7),
                        new RecipeJson("battery_electrolyte", Map.of("capitalismmod:lithium_carbonate", 1, "capitalismmod:lpg", 1), Map.of("capitalismmod:battery_electrolyte", 1), 125, "battery_materials", 3, 6, 10),
                        new RecipeJson("lithium_ion_cell", Map.of("capitalismmod:cathode_active_material", 1, "capitalismmod:graphite_anode", 1, "capitalismmod:battery_separator", 1, "capitalismmod:battery_electrolyte", 1, "capitalismmod:copper_wire", 1), Map.of("capitalismmod:battery_cell", 2), 260, "battery_cell_line", 4, 8, 13),
                        new RecipeJson("lithium_ion_battery", Map.of("capitalismmod:battery_cell", 2, "capitalismmod:copper_wire", 1, "capitalismmod:plastic_pellets", 1), Map.of("capitalismmod:battery", 1), 145, "electronics_assembly", 2, 4, 8),
                        new RecipeJson("battery_pack", Map.of("capitalismmod:battery", 4, "capitalismmod:circuit_board", 1, "capitalismmod:steel_sheet", 1, "capitalismmod:plastic_pellets", 2), Map.of("capitalismmod:battery_pack", 1), 520, "electronics_assembly", 4, 8, 14),
                        new RecipeJson("battery_pack_shredding", Map.of("capitalismmod:battery_pack", 1), Map.of("capitalismmod:black_mass", 2), 210, "battery_recycler", 3, 7, 12),
                        new RecipeJson("black_mass_hydrometallurgy", Map.of("capitalismmod:black_mass", 2), Map.of("capitalismmod:lithium_carbonate", 1, "capitalismmod:graphite_anode", 1, "capitalismmod:cathode_active_material", 1), 330, "battery_recycler", 4, 9, 15),
                        new RecipeJson("smartphone", Map.of("capitalismmod:circuit_board", 1, "capitalismmod:battery", 1, "capitalismmod:display_panel", 1, "capitalismmod:phone_casing", 1, "capitalismmod:camera_module", 1, "capitalismmod:speaker_module", 1, "capitalismmod:microphone_module", 1, "capitalismmod:charging_port", 1, "capitalismmod:power_management_ic", 1, "capitalismmod:display_driver", 1), Map.of("capitalismmod:smartphone", 1), 820, "electronics_assembly", 5, 8, 15),
                        new RecipeJson("electric_fan", Map.of("capitalismmod:electric_motor", 1, "capitalismmod:fan_blades", 1, "capitalismmod:fan_control", 1, "capitalismmod:machine_frame", 1), Map.of("capitalismmod:electric_fan", 1), 240, "assembly_line", 3, 3, 9),
                        new RecipeJson("power_adapter", Map.of("capitalismmod:circuit_board", 1, "capitalismmod:copper_wire", 1, "capitalismmod:plastic_pellets", 1), Map.of("capitalismmod:power_adapter", 1), 180, "smt_line", 2, 4, 9),
                        new RecipeJson("television", Map.of("capitalismmod:display_panel", 1, "capitalismmod:packaged_chip", 1, "capitalismmod:circuit_board", 1, "capitalismmod:speaker_module", 1, "capitalismmod:abs_resin", 1, "capitalismmod:display_driver", 1), Map.of("capitalismmod:television", 1), 980, "electronics_assembly", 5, 8, 16),
                        new RecipeJson("laptop", Map.of("capitalismmod:display_panel", 1, "capitalismmod:packaged_chip", 2, "capitalismmod:circuit_board", 1, "capitalismmod:battery", 1, "capitalismmod:abs_resin", 1, "capitalismmod:power_management_ic", 1, "capitalismmod:display_driver", 1), Map.of("capitalismmod:laptop", 1), 1380, "electronics_assembly", 6, 9, 18)),
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
