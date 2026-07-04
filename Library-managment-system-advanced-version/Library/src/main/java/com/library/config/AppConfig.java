package com.library.config;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Singleton that loads/saves all application settings from/to
 * libra_config.properties next to the running JAR.
 */
public class AppConfig {

    private static final String CONFIG_FILE = "libra_config.properties";
    private static final Logger LOG = Logger.getLogger(AppConfig.class.getName());
    private static AppConfig instance;

    private final Properties props = new Properties();

    // ── Keys ──────────────────────────────────────────────────────────────────
    public static final String KEY_LIBRARY_NAME      = "library.name";
    public static final String KEY_LIBRARY_ADDRESS   = "library.address";
    public static final String KEY_LIBRARY_PHONE     = "library.phone";
    public static final String KEY_LIBRARY_EMAIL     = "library.email";
    public static final String KEY_CURRENCY          = "library.currency";
    public static final String KEY_DATE_FORMAT       = "library.dateFormat";
    public static final String KEY_ITEMS_PER_PAGE    = "library.itemsPerPage";
    public static final String KEY_DEFAULT_LIMIT     = "library.defaultLimit";
    public static final String KEY_LOAN_DAYS         = "library.loanDays";
    public static final String KEY_FINE_RATE         = "library.fineRate";
    public static final String KEY_MAX_BOOKS         = "library.maxBooks";
    public static final String KEY_GRACE_PERIOD      = "library.gracePeriod";
    public static final String KEY_THEME             = "ui.theme";
    public static final String KEY_OVERDUE_ALERT     = "notify.overdueAlert";
    public static final String KEY_DUE_SOON_DAYS     = "notify.dueSoonDays";
    // v3: Email SMTP
    public static final String KEY_EMAIL_HOST        = "email.smtp.host";
    public static final String KEY_EMAIL_PORT        = "email.smtp.port";
    public static final String KEY_EMAIL_USER        = "email.smtp.user";
    public static final String KEY_EMAIL_PASSWORD    = "email.smtp.password";
    public static final String KEY_EMAIL_FROM_NAME   = "email.from.name";
    // v3: Weather
    public static final String KEY_WEATHER_CITY      = "weather.city";
    public static final String KEY_WEATHER_ENABLED   = "weather.enabled";

    private AppConfig() {
        load();
    }

    public static synchronized AppConfig getInstance() {
        if (instance == null) instance = new AppConfig();
        return instance;
    }

    // ── Defaults ──────────────────────────────────────────────────────────────
    private void applyDefaults() {
        props.putIfAbsent(KEY_LIBRARY_NAME,    "LibraCore Pro Library");
        props.putIfAbsent(KEY_LIBRARY_ADDRESS, "123 Library Street");
        props.putIfAbsent(KEY_LIBRARY_PHONE,   "+1-000-000-0000");
        props.putIfAbsent(KEY_LIBRARY_EMAIL,   "library@example.com");
        props.putIfAbsent(KEY_CURRENCY,        "PKR");
        props.putIfAbsent(KEY_DATE_FORMAT,     "dd/MM/yyyy");
        props.putIfAbsent(KEY_ITEMS_PER_PAGE,  "10");
        props.putIfAbsent(KEY_DEFAULT_LIMIT,     "10");
        props.putIfAbsent(KEY_LOAN_DAYS,       "14");
        props.putIfAbsent(KEY_FINE_RATE,       "5.0");
        props.putIfAbsent(KEY_MAX_BOOKS,       "5");
        props.putIfAbsent(KEY_GRACE_PERIOD,    "2");
        props.putIfAbsent(KEY_THEME,           "light");
        props.putIfAbsent(KEY_OVERDUE_ALERT,   "true");
        props.putIfAbsent(KEY_DUE_SOON_DAYS,   "2");
        // v3 defaults
        props.putIfAbsent(KEY_EMAIL_HOST,      "smtp.gmail.com");
        props.putIfAbsent(KEY_EMAIL_PORT,      "587");
        props.putIfAbsent(KEY_EMAIL_FROM_NAME, "LibraCore Pro Library");
        props.putIfAbsent(KEY_WEATHER_CITY,    "Peshawar");
        props.putIfAbsent(KEY_WEATHER_ENABLED, "true");
    }

    // ── I/O ───────────────────────────────────────────────────────────────────
    private void load() {
        Path path = Paths.get(CONFIG_FILE);
        if (Files.exists(path)) {
            try (InputStream in = Files.newInputStream(path)) {
                props.load(in);
            } catch (IOException e) {
                LOG.warning("Config load error: " + e.getMessage());
            }
        }
        applyDefaults();
    }

    public void save() {
        try (OutputStream out = Files.newOutputStream(Paths.get(CONFIG_FILE))) {
            props.store(out, "LibraCore Pro Configuration");
        } catch (IOException e) {
            LOG.warning("Config save error: " + e.getMessage());
        }
    }

    // ── Accessors ─────────────────────────────────────────────────────────────
    public String get(String key)                    { return props.getProperty(key); }
    public void   set(String key, String value)      { props.setProperty(key, value); }

    public int    getInt(String key, int def) {
        try { return Integer.parseInt(props.getProperty(key)); }
        catch (Exception e) { return def; }
    }

    public double getDouble(String key, double def) {
        try { return Double.parseDouble(props.getProperty(key)); }
        catch (Exception e) { return def; }
    }

    public boolean getBoolean(String key, boolean def) {
        String v = props.getProperty(key);
        return v == null ? def : Boolean.parseBoolean(v);
    }

    // ── Convenience getters ───────────────────────────────────────────────────
    public String  getLibraryName()   { return get(KEY_LIBRARY_NAME); }
    public String  getTheme()         { return get(KEY_THEME); }
    public int     getLoanDays()      { return getInt(KEY_LOAN_DAYS, 14); }
    public double  getFineRate()      { return getDouble(KEY_FINE_RATE, 5.0); }
    public int     getGracePeriod()   { return getInt(KEY_GRACE_PERIOD, 2); }
    public int     getMaxBooks()      { return getInt(KEY_MAX_BOOKS, 5); }
    public int     getItemsPerPage()  { return getInt(KEY_ITEMS_PER_PAGE, 10); }
    public int     getDefaultLimit()  { return getInt(KEY_DEFAULT_LIMIT, 10); }
    public String  getCurrency()      { return get(KEY_CURRENCY); }
    public String  getDateFormat()    { return get(KEY_DATE_FORMAT); }
    public boolean isOverdueAlert()   { return getBoolean(KEY_OVERDUE_ALERT, true); }
    public int     getDueSoonDays()   { return getInt(KEY_DUE_SOON_DAYS, 2); }
}
