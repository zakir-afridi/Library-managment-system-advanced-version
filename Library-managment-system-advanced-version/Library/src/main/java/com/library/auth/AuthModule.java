package com.library.auth;

import com.library.config.ThemeManager;
import com.library.model.User;
import com.library.security.SessionManager;
import com.library.service.UserService;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * AUTH BRANCH — public API.
 * All other branches call these static methods; never import LoginController directly.
 */
public class AuthModule {

    private static final UserService userService = new UserService();

    /** Show the login screen on the given stage. */
    public static void showLogin(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    AuthModule.class.getResource("/com/library/ui/LoginPage.fxml"));
            Scene scene = new Scene(loader.load());
            ThemeManager.getInstance().applyTheme(scene);
            stage.setScene(scene);
            stage.setTitle("LibraCore Pro v2.0.0 — Login");
            stage.setMinWidth(900);
            stage.setMinHeight(600);
            stage.setMaximized(false);
            stage.show();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load login screen", e);
        }
    }

    public static User getCurrentUser()  { return SessionManager.getInstance().getCurrentUser(); }
    public static void logout()          { SessionManager.getInstance().logout(); }
    public static boolean isLoggedIn()   { return SessionManager.getInstance().isLoggedIn(); }

    /** Unlock a locked account using the universal recovery key. */
    public static User unlockByRecoveryKey(String username, String key) {
        return userService.unlockByRecoveryKey(username, key);
    }
}
