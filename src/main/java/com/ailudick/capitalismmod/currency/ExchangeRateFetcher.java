package com.ailudick.capitalismmod.currency;

import com.ailudick.capitalismmod.CapitalismMod;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Fetches live exchange rates from a free API (base CNY) and applies them as anchors.
 * Runs on the server, off the main thread, with the fixed defaults as a fallback.
 */
@EventBusSubscriber(modid = CapitalismMod.MODID)
public final class ExchangeRateFetcher {
    private static final String API_URL = "https://open.er-api.com/v6/latest/CNY";
    private static final int FETCH_INTERVAL_TICKS = 72000; // 1 hour of server uptime
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static int tickCounter = 0;

    private ExchangeRateFetcher() {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        tryFetchAsync();
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        tickCounter++;
        if (tickCounter >= FETCH_INTERVAL_TICKS) {
            tickCounter = 0;
            tryFetchAsync();
        }
    }

    /** Kicks off a non-blocking fetch on the common pool. */
    public static void tryFetchAsync() {
        CompletableFuture.runAsync(ExchangeRateFetcher::fetchAndApply);
    }

    private static void fetchAndApply() {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(API_URL))
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new RuntimeException("HTTP " + response.statusCode());
            }
            JsonObject root = new Gson().fromJson(response.body(), JsonObject.class);
            if (!"success".equals(root.get("result").getAsString())) {
                throw new RuntimeException("API returned an error result");
            }
            JsonObject rates = root.getAsJsonObject("rates");
            for (Currency currency : Currencies.ALL) {
                if (currency.id().equals("cny")) {
                    ExchangeRateProvider.setAnchor("cny", 100L);
                    continue;
                }
                double rate = rates.get(currency.id().toUpperCase()).getAsDouble();
                ExchangeRateProvider.setAnchor(currency.id(), Math.round(100.0 / rate));
            }
            ExchangeRateProvider.setLive(true);
            CapitalismMod.LOGGER.info("Updated live exchange rates (fen): USD={}, EUR={}, RUB={}",
                    ExchangeRateProvider.anchor(Currencies.USD),
                    ExchangeRateProvider.anchor(Currencies.EUR),
                    ExchangeRateProvider.anchor(Currencies.RUB));
        } catch (Exception e) {
            CapitalismMod.LOGGER.warn("Failed to fetch live exchange rates; keeping current rates", e);
        }
    }
}
