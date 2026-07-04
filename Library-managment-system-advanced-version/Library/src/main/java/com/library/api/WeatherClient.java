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
 * Client for Open-Meteo weather API (free, no API key required).
 * Cached for 30 minutes. Thread-safe via double-checked locking.
 */
public final class WeatherClient {

    private static final Logger LOG = LoggerFactory.getLogger(WeatherClient.class);

    private static final String GEO_URL     = "https://geocoding-api.open-meteo.com/v1/search?name=%s&count=1&language=en&format=json";
    private static final String WEATHER_URL = "https://api.open-meteo.com/v1/forecast?latitude=%s&longitude=%s&current_weather=true&temperature_unit=celsius";
    private static final Duration TIMEOUT   = Duration.ofSeconds(6);
    private static final long CACHE_TTL_MS  = 30 * 60 * 1000L;

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Object CACHE_LOCK = new Object();

    private static volatile WeatherInfo cachedWeather = null;
    private static volatile long        lastFetchMs   = 0;

    private WeatherClient() {}

    public static Optional<WeatherInfo> getWeather(String city) {
        long now = System.currentTimeMillis();
        if (cachedWeather != null && (now - lastFetchMs) < CACHE_TTL_MS)
            return Optional.of(cachedWeather);

        synchronized (CACHE_LOCK) {
            // double-checked locking
            if (cachedWeather != null && (System.currentTimeMillis() - lastFetchMs) < CACHE_TTL_MS)
                return Optional.of(cachedWeather);
            return fetch(city);
        }
    }

    private static Optional<WeatherInfo> fetch(String city) {
        try {
            // Step 1: geocode city
            String geoUrl = String.format(GEO_URL, city.replace(" ", "+"));
            HttpResponse<String> geoResp = HTTP.send(
                HttpRequest.newBuilder().uri(URI.create(geoUrl)).timeout(TIMEOUT).GET().build(),
                HttpResponse.BodyHandlers.ofString());
            if (geoResp.statusCode() != 200) return Optional.ofNullable(cachedWeather);

            JsonNode results = JSON.readTree(geoResp.body()).path("results");
            if (!results.isArray() || results.isEmpty()) {
                LOG.warn("WeatherClient: city '{}' not found", city);
                return Optional.ofNullable(cachedWeather);
            }

            String lat  = results.get(0).path("latitude").asText();
            String lon  = results.get(0).path("longitude").asText();
            String name = results.get(0).path("name").asText(city);

            // Step 2: fetch weather
            String weatherUrl = String.format(WEATHER_URL, lat, lon);
            HttpResponse<String> wResp = HTTP.send(
                HttpRequest.newBuilder().uri(URI.create(weatherUrl)).timeout(TIMEOUT).GET().build(),
                HttpResponse.BodyHandlers.ofString());
            if (wResp.statusCode() != 200) return Optional.ofNullable(cachedWeather);

            JsonNode current = JSON.readTree(wResp.body()).path("current_weather");

            WeatherInfo info = new WeatherInfo();
            info.setCity(name);
            info.setTemperature(current.path("temperature").asDouble());
            info.setWindspeed(current.path("windspeed").asDouble());
            info.setWeathercode(current.path("weathercode").asInt());
            info.setDescription(decodeWeatherCode(info.getWeathercode()));

            cachedWeather = info;
            lastFetchMs   = System.currentTimeMillis();
            LOG.info("WeatherClient: {} - {}C, {}", name, info.getTemperature(), info.getDescription());
            return Optional.of(info);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.ofNullable(cachedWeather);
        } catch (Exception e) {
            LOG.warn("WeatherClient error: {}", e.getMessage());
            return Optional.ofNullable(cachedWeather);
        }
    }

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
