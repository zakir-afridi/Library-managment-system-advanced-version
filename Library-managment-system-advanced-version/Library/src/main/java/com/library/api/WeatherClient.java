package com.library.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.model.WeatherInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

/**
 * Client for Open-Meteo weather API (100% free, no API key required).
 *
 * API: https://api.open-meteo.com/v1/forecast
 * Uses city → lat/lon lookup via the Open-Meteo geocoding API.
 *
 * Cached for 30 minutes to avoid hammering the free API.
 */
public final class WeatherClient {

    private static final Logger LOG = LoggerFactory.getLogger(WeatherClient.class);

    private static final String GEO_URL     = "https://geocoding-api.open-meteo.com/v1/search?name=%s&count=1&language=en&format=json";
    private static final String WEATHER_URL = "https://api.open-meteo.com/v1/forecast?latitude=%s&longitude=%s&current_weather=true&temperature_unit=celsius";

    private static final Duration TIMEOUT = Duration.ofSeconds(6);

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static final ObjectMapper JSON = new ObjectMapper();

    private static WeatherInfo cachedWeather = null;
    private static long lastFetchMs = 0;
    private static final long CACHE_TTL_MS = 30 * 60 * 1000L; // 30 minutes

    private WeatherClient() {}

    /**
     * Fetch current weather for the given city name.
     * Uses a two-step process: geocode city → fetch weather.
     * Returns cached data if within 30-minute TTL.
     */
    public static Optional<WeatherInfo> getWeather(String city) {
        long now = System.currentTimeMillis();
        if (cachedWeather != null && (now - lastFetchMs) < CACHE_TTL_MS) {
            return Optional.of(cachedWeather);
        }

        try {
            // Step 1: Geocode city name
            String geoUrl = String.format(GEO_URL, city.replace(" ", "+"));
            HttpRequest geoReq = HttpRequest.newBuilder()
                    .uri(URI.create(geoUrl))
                    .timeout(TIMEOUT)
                    .GET().build();
            HttpResponse<String> geoResp = HTTP.send(geoReq, HttpResponse.BodyHandlers.ofString());
            if (geoResp.statusCode() != 200) return Optional.empty();

            JsonNode geoRoot = JSON.readTree(geoResp.body());
            JsonNode results = geoRoot.path("results");
            if (!results.isArray() || results.size() == 0) {
                LOG.warn("WeatherClient: city '{}' not found in geocoding API", city);
                return Optional.empty();
            }

            String lat  = results.get(0).path("latitude").asText();
            String lon  = results.get(0).path("longitude").asText();
            String name = results.get(0).path("name").asText(city);

            // Step 2: Fetch weather
            String weatherUrl = String.format(WEATHER_URL, lat, lon);
            HttpRequest wReq = HttpRequest.newBuilder()
                    .uri(URI.create(weatherUrl))
                    .timeout(TIMEOUT)
                    .GET().build();
            HttpResponse<String> wResp = HTTP.send(wReq, HttpResponse.BodyHandlers.ofString());
            if (wResp.statusCode() != 200) return Optional.empty();

            JsonNode wRoot   = JSON.readTree(wResp.body());
            JsonNode current = wRoot.path("current_weather");

            WeatherInfo info = new WeatherInfo();
            info.setCity(name);
            info.setTemperature(current.path("temperature").asDouble());
            info.setWindspeed(current.path("windspeed").asDouble());
            info.setWeathercode(current.path("weathercode").asInt());
            info.setDescription(decodeWeatherCode(info.getWeathercode()));

            cachedWeather = info;
            lastFetchMs   = now;
            LOG.info("WeatherClient: {} — {}°C, {}", name, info.getTemperature(), info.getDescription());
            return Optional.of(info);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.ofNullable(cachedWeather);
        } catch (Exception e) {
            LOG.warn("WeatherClient error: {}", e.getMessage());
            return Optional.ofNullable(cachedWeather);
        }
    }

    /** Map WMO weather code to a human-readable description. */
    private static String decodeWeatherCode(int code) {
        return switch (code) {
            case 0          -> "Clear sky";
            case 1, 2, 3    -> "Partly cloudy";
            case 45, 48     -> "Foggy";
            case 51, 53, 55 -> "Drizzle";
            case 61, 63, 65 -> "Rain";
            case 71, 73, 75 -> "Snow";
            case 80, 81, 82 -> "Rain showers";
            case 95         -> "Thunderstorm";
            case 96, 99     -> "Thunderstorm with hail";
            default         -> "Unknown";
        };
    }
}
