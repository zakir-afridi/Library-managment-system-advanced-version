package com.library;

import com.library.auth.AuthModule;
import com.library.shared.DatabaseManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.InputStream;

/**
 * MAIN TRUNK — the ONLY entry point for the entire application.
 * All branches connect through this root via their *Module classes.
 *
 * Navigation tree:
 *   LibraryApp
 *   └── AuthModule        (branch 1 — login)
 *       └── DashboardModule (branch 2 — main shell)
 *           ├── BookModule     (branch 3)
 *           ├── MemberModule   (branch 4)
 *           ├── StudentModule  (branch 5)
 *           ├── EmployeeModule (branch 6)
 *           ├── IssueModule    (branch 7)
 *           └── ReportModule   (branch 8)
 */
public class LibraryApp extends Application {

    public static final String APP_NAME    = "LibraCore Pro";
    public static final String APP_VERSION = "v2.0.0";

    @Override
    public void init() {
        DatabaseManager.initialize();   // shared/ — DB schema + seed
        System.out.println("✅ Database initialized");
    }

    @Override
    public void start(Stage primaryStage) {
        try (InputStream icon = getClass()
                .getResourceAsStream("/com/library/ui/images/login.png")) {
            if (icon != null) primaryStage.getIcons().add(new Image(icon));
        } catch (Exception ignored) {}

        AuthModule.showLogin(primaryStage);  // auth/ branch — first screen
        System.out.println("🚀 " + APP_NAME + " " + APP_VERSION + " started");
    }

    @Override
    public void stop() {
        DatabaseManager.close();            // shared/ — release pool
        System.out.println("👋 Application closed");
        Platform.exit();
    }

    // THE ONLY main() METHOD IN THE ENTIRE PROJECT
    public static void main(String[] args) {
        launch(args);
    }
}
