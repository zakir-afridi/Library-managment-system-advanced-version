package com.library.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Client for the Frankfurter exchange rate API (100% free, no API key).
 *
 * API: https://api.frankfurter.app/latest?from=PKR
 * Provides live PKR → USD/EUR/GBP/AED/SAR conversion rates.
 *
 * Falls back gracefully if the network is unavailable — callers receive
 * the last cached rates (or an empty map on first failure).
 */
public final class ExchangeRateClient {

    private static final Logger LOG = LoggerFactory.getLogger(ExchangeRateClient.class);

    private static final String API_URL =
        "https://api.frankfurter.app/latest?from=%s";

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static final ObjectMapper JSON = new ObjectMapper();

    /** Simple in-memory cache — refreshed at most once per day. */
    private static Map<String, Double> rateCache = Collections.emptyMap();
    private static long lastFetchMs = 0;
    private static final long CACHE_TTL_MS = 24 * 60 * 60 * 1000L; // 24 hours

    private ExchangeRateClient() {}

    /**
     * Returns exchange rates from the given base currency.
     * Result: Map of target currency code → rate.  e.g. {"USD": 0.0036, "EUR": 0.0033}
     *
     * The result is cached for 24 hours.  Returns empty map on failure.
     */
    public static Map<String, Double> getRates(String baseCurrency) {
        long now = System.currentTimeMillis();
        if (!rateCache.isEmpty() && (now - lastFetchMs) < CACHE_TTL_MS) {
            return Collections.unmodifiableMap(rateCache);
        }

        String url = String.format(API_URL, baseCurrency.toUpperCase());
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(TIMEOUT)
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() != 200) {
                LOG.warn("ExchangeRate API returned HTTP {}", resp.statusCode());
                return Collections.unmodifiableMap(rateCache);
            }

            JsonNode root  = JSON.readTree(resp.body());
            JsonNode rates = root.path("rates");

            Map<String, Double> result = new HashMap<>();
            rates.fields().forEachRemaining(e ->
                result.put(e.getKey(), e.getValue().asDouble()));

            rateCache   = result;
            lastFetchMs = now;
            LOG.info("ExchangeRate: fetched {} rates from base {}", result.size(), baseCurrency);
            return Collections.unmodifiableMap(result);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.warn("ExchangeRate request interrupted");
            return Collections.unmodifiableMap(rateCache);
        } catch (Exception e) {
            LOG.warn("ExchangeRate fetch failed: {}", e.getMessage());
            return Collections.unmodifiableMap(rateCache);
        }
    }

    /**
     * Convert an amount from baseCurrency to targetCurrency.
     * Returns empty if rates are unavailable.
     */
    public static Optional<Double> convert(double amount, String from, String to) {
        Map<String, Double> rates = getRates(from);
        if (rates.containsKey(to.toUpperCase()))
            return Optional.of(amount * rates.get(to.toUpperCase()));
        return Optional.empty();
    }

    /** Returns the cached rates without triggering a network call. */
    public static Map<String, Double> getCachedRates() {
        return Collections.unmodifiableMap(rateCache);
    }
}
