package com.library;

import com.library.auth.AuthModule;
import com.library.shared.SharedModule;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.InputStream;

/**
 * MAIN TRUNK — the ONLY entry point for the entire application.
 * All branches connect through this root via their Module classes.
 */
public class LibraCoreApp extends Application {

    public static final String APP_NAME    = "LibraCore Pro";
    public static final String APP_VERSION = "v2.0.0";

    @Override
    public void init() {
        SharedModule.initDatabase();   // shared/ branch: DB schema + seed
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
        SharedModule.saveConfig();     // shared/ branch: persist settings
        Platform.exit();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
