package com.library.dashboard;

import com.library.cache.DashboardCache;
import com.library.cache.DashboardStats;
import com.library.model.User;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.Pane;

/**
 * DASHBOARD BRANCH — public API.
 * Called by AuthModule after successful login.
 */
public class DashboardModule {

    // ── UI Navigation ─────────────────────────────────────────────────────────

    /** Load the dashboard view into the given container pane. */
    public static void showDashboard(Pane container) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    DashboardModule.class.getResource("/com/library/ui/ProfessionalDashboard.fxml"));
            Pane view = loader.load();
            container.getChildren().setAll(view);
        } catch (Exception e) {
            System.err.println("DashboardModule.showDashboard: " + e.getMessage());
        }
    }

    /**
     * Show the dashboard as the primary scene on a Stage.
     * Called by AuthModule after login succeeds.
     */
    public static void showDashboard(javafx.stage.Stage stage, User user) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    DashboardModule.class.getResource("/com/library/ui/ProfessionalDashboard.fxml"));
            javafx.scene.Scene scene = new javafx.scene.Scene(loader.load());
            com.library.config.ThemeManager.getInstance().applyTheme(scene);
            com.library.controller.DashboardController dc = loader.getController();
            dc.initSession(user);
            stage.setScene(scene);
            stage.setTitle("LibraCore Pro v2.0.0 — Dashboard");
            stage.setMaximized(true);
        } catch (Exception e) {
            System.err.println("DashboardModule.showDashboard(stage): " + e.getMessage());
        }
    }

    /** Force-refresh dashboard stats (invalidates cache). */
    public static void updateStats() {
        DashboardCache.getInstance().invalidate();
    }

    /** Returns cached stats, refreshing from DB if TTL expired (60s). */
    public static DashboardStats getStats() {
        return DashboardCache.getInstance().getStats();
    }
}
