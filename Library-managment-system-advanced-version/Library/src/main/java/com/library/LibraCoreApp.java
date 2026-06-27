package com.library;

import com.library.auth.AuthModule;
import com.library.database.HikariConnectionPool;
import com.library.shared.SharedModule;
import com.library.util.GlobalExceptionHandler;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.io.InputStream;
import java.util.logging.LogManager;

/**
 * MAIN TRUNK — the ONLY entry point for LibraCore Pro v3.0.0.
 * All branches connect through this root via their Module classes.
 *
 * v3 additions:
 *  - Java 21 virtual threads (via AsyncRunner)
 *  - HikariCP connection pool (replaces hand-rolled ArrayBlockingQueue pool)
 *  - SLF4J + Logback structured logging
 *  - Global uncaught exception handler (no more silent crashes)
 */
public class LibraCoreApp extends Application {

    public static final String APP_NAME    = "LibraCore Pro";
    public static final String APP_VERSION = "v3.0.0";

    @Override
    public void init() {
        // ── Bridge java.util.logging to SLF4J ─────────────────────────────
        LogManager.getLogManager().reset();
        SLF4JBridgeHandler.install();

        // ── Global crash handler ──────────────────────────────────────────
        GlobalExceptionHandler.register();

        // ── Database ──────────────────────────────────────────────────────
        SharedModule.initDatabase();   // HikariCP pool + schema + seed

        // ── Background services (virtual threads) ─────────────────────────
        com.library.service.BackupScheduler.getInstance().start();
        com.library.service.OverdueNotificationService.getInstance().start();
    }

    @Override
    public void start(Stage stage) {
        try (InputStream icon = getClass()
                .getResourceAsStream("/com/library/ui/images/login.png")) {
            if (icon != null) stage.getIcons().add(new Image(icon));
        } catch (Exception ignored) {}

        AuthModule.showLogin(stage);   // auth/ branch: first screen
    }

    @Override
    public void stop() {
        SharedModule.saveConfig();     // persist AppConfig → properties file
        com.library.service.BackupScheduler.getInstance().stop();
        com.library.service.OverdueNotificationService.getInstance().stop();
        HikariConnectionPool.close();  // close DB pool gracefully
        Platform.exit();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
