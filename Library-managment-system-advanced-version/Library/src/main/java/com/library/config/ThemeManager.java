package com.library.config;

import javafx.scene.Scene;

/**
 * Singleton that manages light / dark theme switching.
 * CSS files live at /com/library/ui/css/light-theme.css and dark-theme.css.
 */
public class ThemeManager {

    public enum Theme { LIGHT, DARK }

    private static final String LIGHT_CSS =
            "/com/library/ui/css/light-theme.css";
    private static final String DARK_CSS  =
            "/com/library/ui/css/dark-theme.css";

    private static ThemeManager instance;
    private Theme current;

    private ThemeManager() {
        String saved = AppConfig.getInstance().getTheme();
        current = "dark".equalsIgnoreCase(saved) ? Theme.DARK : Theme.LIGHT;
    }

    public static synchronized ThemeManager getInstance() {
        if (instance == null) instance = new ThemeManager();
        return instance;
    }

    public Theme getCurrent() { return current; }

    public boolean isDark() { return current == Theme.DARK; }

    /** Apply the current theme to a scene (removes the other one first). */
    public void applyTheme(Scene scene) {
        String toAdd    = current == Theme.DARK ? DARK_CSS  : LIGHT_CSS;
        String toRemove = current == Theme.DARK ? LIGHT_CSS : DARK_CSS;

        String addUrl = getUrl(toAdd);
        String remUrl = getUrl(toRemove);

        scene.getStylesheets().remove(remUrl);
        if (addUrl != null && !scene.getStylesheets().contains(addUrl)) {
            scene.getStylesheets().add(addUrl);
        }
    }

    /** Toggle between light and dark, apply to scene, and persist. */
    public void toggle(Scene scene) {
        current = (current == Theme.LIGHT) ? Theme.DARK : Theme.LIGHT;
        AppConfig.getInstance().set(AppConfig.KEY_THEME,
                current == Theme.DARK ? "dark" : "light");
        AppConfig.getInstance().save();
        applyTheme(scene);
    }

    private String getUrl(String resource) {
        var url = getClass().getResource(resource);
        return url == null ? null : url.toExternalForm();
    }
}
