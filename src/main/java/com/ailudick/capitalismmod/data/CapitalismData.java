package com.ailudick.capitalismmod.data;

import com.ailudick.capitalismmod.CapitalismMod;
import com.ailudick.capitalismmod.company.IndustrySpec;
import com.ailudick.capitalismmod.company.ProductionRecipe;
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
        List<ShopOfferJson> raw = read(dir.resolve("shop_offers.json"), ShopOfferJson[].class, defaultShopOffers());
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

    private static List<ItemStack> loadCommodities(Path dir) {
        List<CommodityJson> raw = read(dir.resolve("commodities.json"), CommodityJson[].class, defaultCommodities());
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
        List<StockJson> raw = read(dir.resolve("stocks.json"), StockJson[].class, defaultStocks());
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
                new CommodityJson("capitalismmod:canned_food", 50)
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
                        new RecipeJson("canned_food", Map.of("minecraft:wheat", 1, "capitalismmod:metal_can", 1), Map.of("capitalismmod:canned_food", 1), 90, "assembly_line", 3, 2, 8)),
                new IndustryJson("utilities", Map.of("minecraft:coal", 1), Map.of(), 60),
                new IndustryJson("construction", Map.of("minecraft:rail", 1), Map.of(), 50),
                new IndustryJson("transport", Map.of("minecraft:coal", 1), Map.of(), 45),
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
            this.workers_per_cycle = outputs.isEmpty() ? 0 : 1;
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
