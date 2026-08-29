package com.ailudick.capitalismmod.data;

import com.ailudick.capitalismmod.CapitalismMod;
import com.ailudick.capitalismmod.company.IndustrySpec;
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

/**
 * Loads data-driven config (shop offers, commodities, stocks) from config/capitalismmod/*.json.
 * Missing files are created with defaults so pack authors can edit them.
 */
public final class CapitalismData {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static List<ShopOffer> shopOffers = new ArrayList<>();
    private static List<ItemStack> commodities = new ArrayList<>();
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
        return commodities;
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
        for (CommodityJson j : raw) {
            Item item = parseItem(j.item);
            if (item != null) {
                result.add(new ItemStack(item));
                prices.put(BuiltInRegistries.ITEM.getKey(item).toString(), j.initial_price);
            }
        }
        commodityPrices = prices;
        return result;
    }

    private static List<Stock> loadStocks(Path dir) {
        List<StockJson> raw = read(dir.resolve("stocks.json"), StockJson[].class, defaultStocks());
        List<Stock> result = new ArrayList<>();
        for (StockJson j : raw) {
            result.add(new Stock(j.id, j.name_key, j.initial_price));
        }
        return result;
    }

    private static List<IndustrySpec> loadIndustries(Path dir) {
        List<IndustryJson> raw = read(dir.resolve("industries.json"), IndustryJson[].class, defaultIndustries());
        List<IndustrySpec> result = new ArrayList<>();
        for (IndustryJson j : raw) {
            result.add(new IndustrySpec(j.id, j.inputs, j.outputs, j.income));
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
                new CommodityJson("minecraft:rail", 30)
        );
    }

    private static List<StockJson> defaultStocks() {
        return List.of(
                new StockJson("mining", "stock.capitalismmod.mining", 100),
                new StockJson("tech", "stock.capitalismmod.tech", 200),
                new StockJson("energy", "stock.capitalismmod.energy", 150),
                new StockJson("agriculture", "stock.capitalismmod.agriculture", 80)
        );
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
                new IndustryJson("mining", Map.of(), Map.of("minecraft:iron_ingot", 1, "minecraft:coal", 1, "minecraft:gold_ingot", 1), 55),
                new IndustryJson("agriculture", Map.of(), Map.of("minecraft:wheat", 1), 35),
                new IndustryJson("manufacturing", Map.of("minecraft:iron_ingot", 1, "minecraft:coal", 1), Map.of("minecraft:rail", 1), 80),
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

    public static class IndustryJson {
        public String id = "mining";
        public Map<String, Integer> inputs = new HashMap<>();
        public Map<String, Integer> outputs = new HashMap<>();
        public long income = 55;

        public IndustryJson() {
        }

        public IndustryJson(String id, Map<String, Integer> inputs, Map<String, Integer> outputs, long income) {
            this.id = id;
            this.inputs = new HashMap<>(inputs);
            this.outputs = new HashMap<>(outputs);
            this.income = income;
        }
    }
}
