package com.ailudick.capitalismmod.currency;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Registry of all currencies. Add a new currency by adding a constant here
 * and its denomination item(s) in {@code ModItems}.
 * <p>
 * Exchange rates use a common base unit: the Chinese yuan "fen" (1 yuan = 100 fen).
 * Each currency's {@code baseValue} is how many fen one unit of that currency is worth.
 */
public final class Currencies {
    public static final Currency USD = new Currency("usd", "currency.capitalismmod.usd", 720); // 1 USD ≈ 7.2 CNY
    public static final Currency CNY = new Currency("cny", "currency.capitalismmod.cny", 100); // base
    public static final Currency EUR = new Currency("eur", "currency.capitalismmod.eur", 780); // 1 EUR ≈ 7.8 CNY
    public static final Currency RUB = new Currency("rub", "currency.capitalismmod.rub", 8);   // 1 RUB ≈ 0.08 CNY

    public static final List<Currency> ALL = List.of(USD, CNY, EUR, RUB);

    private static final Map<String, Currency> BY_ID = ALL.stream()
            .collect(Collectors.toUnmodifiableMap(Currency::id, Function.identity()));

    private Currencies() {
    }

    public static boolean exists(String id) {
        return BY_ID.containsKey(id);
    }

    public static Currency byId(String id) {
        Currency currency = BY_ID.get(id);
        if (currency == null) {
            throw new IllegalArgumentException("Unknown currency: " + id);
        }
        return currency;
    }
}
