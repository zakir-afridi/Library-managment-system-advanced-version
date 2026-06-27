package com.library.model;

/**
 * Current weather snapshot from the Open-Meteo API.
 * Shown on the Dashboard weather card.
 */
public class WeatherInfo {

    private String city;
    private double temperature;
    private double windspeed;
    private int    weathercode;
    private String description;

    public WeatherInfo() {}

    public String getCity()                 { return city; }
    public void   setCity(String v)         { this.city = v; }

    public double getTemperature()          { return temperature; }
    public void   setTemperature(double v)  { this.temperature = v; }

    public double getWindspeed()            { return windspeed; }
    public void   setWindspeed(double v)    { this.windspeed = v; }

    public int    getWeathercode()          { return weathercode; }
    public void   setWeathercode(int v)     { this.weathercode = v; }

    public String getDescription()          { return description; }
    public void   setDescription(String v)  { this.description = v; }

    /** Emoji icon representing the weather condition. */
    public String getIcon() {
        return switch (weathercode) {
            case 0          -> "☀";
            case 1, 2, 3    -> "⛅";
            case 45, 48     -> "🌫";
            case 51,53,55,
                 61,63,65   -> "🌧";
            case 71,73,75   -> "❄";
            case 80,81,82   -> "🌦";
            case 95,96,99   -> "⛈";
            default         -> "🌡";
        };
    }

    @Override
    public String toString() {
        return String.format("%s: %s %.1f°C", city, description, temperature);
    }
}
