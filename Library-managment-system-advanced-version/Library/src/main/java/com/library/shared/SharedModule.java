package com.library.shared;

import com.library.config.AppConfig;
import com.library.database.DatabaseConnection;
import com.library.database.DataSeeder;

/**
 * SHARED BRANCH — public API.
 * Initializes the database and exposes common config.
 * Called by LibraryApp before any other module.
 */
public class SharedModule {

    /** Must be called once at startup before any branch is loaded. */
    public static void initDatabase() {
        DatabaseConnection.initialise();
        new DataSeeder().seedIfNeeded();
    }

    /** Persist app config to disk (call on shutdown). */
    public static void saveConfig() {
        AppConfig.getInstance().save();
    }

    public static String getCurrency()  { return AppConfig.getInstance().getCurrency(); }
    public static String getTheme()     { return AppConfig.getInstance().getTheme(); }
    public static int    getLoanDays()  { return AppConfig.getInstance().getLoanDays(); }
    public static double getFineRate()  { return AppConfig.getInstance().getFineRate(); }
}
