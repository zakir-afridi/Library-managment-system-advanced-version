package com.library.controller;

import com.library.LibraCoreApp;
import com.library.config.ThemeManager;
import com.library.model.User;
import com.library.security.SessionManager;
import com.library.service.UserService;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;

public class LoginController {

    @FXML private TextField     usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button        loginButton;
    @FXML private Label         errorLabel;
    @FXML private Label         attemptsLabel;
    @FXML private Button        themeToggleBtn;

    private final UserService userService = new UserService();

    @FXML
    public void initialize() {
        usernameField.setOnAction(e -> passwordField.requestFocus());
        passwordField.setOnAction(e -> handleLogin());
        updateThemeButtonLabel();
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter both username and password.");
            return;
        }

        loginButton.setDisable(true);
        loginButton.setText("⏳  Authenticating...");

        // Run auth off the FX thread to avoid blocking UI
        new Thread(() -> {
            User user = userService.authenticate(username, password);
            Platform.runLater(() -> {
                loginButton.setDisable(false);
                loginButton.setText("🔐  LOGIN");

                if (user == null) {
                    handleFailedLogin(username);
                } else {
                    handleSuccessfulLogin(user);
                }
            });
        }, "auth-thread").start();
    }

    private void handleFailedLogin(String username) {
        User user = userService.getUserByUsername(username);
        if (user != null && user.isLocked()) {
            showError("Account locked after 3 failed attempts.\nUse \"Forgot Password?\" below to unlock.");
            attemptsLabel.setText("");
        } else if (user != null) {
            int remaining = 3 - user.getFailedAttempts();
            attemptsLabel.setText("⚠ " + remaining + " attempt(s) remaining");
            showError("Invalid username or password.");
        } else {
            showError("Invalid username or password.");
        }
        passwordField.clear();
        shakeField(passwordField);
    }

    private void handleSuccessfulLogin(User user) {
        attemptsLabel.setText("");
        hideError();

        // Force password change for default admin
        if (user.isForcePasswordChange()) {
            showForceChangeDialog(user);
            return;
        }

        loadDashboard(user);
    }

    // ── Forgot Password / Unlock ───────────────────────────────────────────────

    @FXML
    private void handleForgotPassword() {
        String username = usernameField.getText().trim();

        // Step 1: ask for recovery key
        TextInputDialog keyDialog = new TextInputDialog();
        keyDialog.setTitle("Account Recovery");
        keyDialog.setHeaderText("Enter the universal recovery key to unlock your account.");
        keyDialog.setContentText("Recovery Key:");
        keyDialog.getEditor().setPromptText("Recovery key");

        keyDialog.showAndWait().ifPresent(key -> {
            if (!com.library.util.Constants.isValidRecoveryKey(key)) {
                showError("Invalid recovery key.");
                shakeField(errorLabel);
                return;
            }

            // If username is blank, just ask for it
            String user = username.isEmpty() ? askForUsername() : username;
            if (user == null || user.isBlank()) return;

            // Step 2: unlock + prompt new password
            User unlocked = userService.unlockByRecoveryKey(user, key);
            if (unlocked == null) {
                showError("Username not found.");
                return;
            }

            showResetPasswordDialog(unlocked);
        });
    }

    private String askForUsername() {
        TextInputDialog d = new TextInputDialog();
        d.setTitle("Account Recovery");
        d.setHeaderText("Enter the username to unlock.");
        d.setContentText("Username:");
        return d.showAndWait().orElse(null);
    }

    private void showResetPasswordDialog(User user) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Reset Password");
        dialog.setHeaderText("Account unlocked. Set a new password for: " + user.getUsername());

        PasswordField newPass     = new PasswordField();
        PasswordField confirmPass = new PasswordField();
        newPass.setPromptText("New password (min 8 chars, upper, digit, symbol)");
        confirmPass.setPromptText("Confirm new password");
        Label msg = new Label();
        msg.setStyle("-fx-text-fill: #d32f2f;");

        javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(10,
                new Label("New Password:"), newPass,
                new Label("Confirm Password:"), confirmPass, msg);
        content.setPrefWidth(360);
        dialog.getDialogPane().setContent(content);

        ButtonType saveBtn = new ButtonType("Save & Login", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveBtn);
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            String np = newPass.getText();
            String cp = confirmPass.getText();
            if (!np.equals(cp)) {
                msg.setText("Passwords do not match.");
                e.consume();
            } else if (!com.library.security.PasswordUtil.isStrong(np)) {
                msg.setText("Password too weak. Need uppercase, digit, and symbol.");
                e.consume();
            }
        });

        dialog.setResultConverter(btn -> btn == saveBtn ? newPass.getText() : null);
        dialog.showAndWait().ifPresent(newPassword -> {
            userService.adminResetPassword(user.getUserId(), newPassword);
            hideError();
            attemptsLabel.setText("");
            usernameField.setText(user.getUsername());
            passwordField.clear();
            showError("Password reset. Please log in with your new password.");
            errorLabel.setStyle("-fx-text-fill: #2e7d32; -fx-background-color: #e8f5e9;"
                    + "-fx-background-radius: 6; -fx-padding: 8 12 8 12;");
        });
    }

    // ── Force password change dialog ──────────────────────────────────────────

    private void showForceChangeDialog(User user) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Change Password Required");
        dialog.setHeaderText("You must change your default password before continuing.");

        PasswordField newPass    = new PasswordField();
        PasswordField confirmPass = new PasswordField();
        newPass.setPromptText("New password (min 8 chars, upper, digit, symbol)");
        confirmPass.setPromptText("Confirm new password");
        Label msg = new Label();
        msg.setStyle("-fx-text-fill: #d32f2f;");

        javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(10,
                new Label("New Password:"), newPass,
                new Label("Confirm Password:"), confirmPass, msg);
        content.setPrefWidth(360);
        dialog.getDialogPane().setContent(content);

        ButtonType saveBtn = new ButtonType("Save & Continue", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        // Validate before closing
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveBtn);
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            String np = newPass.getText();
            String cp = confirmPass.getText();
            if (!np.equals(cp)) {
                msg.setText("Passwords do not match.");
                e.consume();
            } else if (!com.library.security.PasswordUtil.isStrong(np)) {
                msg.setText("Password too weak. Need uppercase, digit, and symbol.");
                e.consume();
            }
        });

        dialog.setResultConverter(btn -> btn == saveBtn ? newPass.getText() : null);

        dialog.showAndWait().ifPresent(newPassword -> {
            boolean changed = userService.changePassword(
                    user.getUserId(), passwordField.getText(), newPassword);
            if (changed) {
                loadDashboard(user);
            } else {
                // Fallback: admin reset
                userService.adminResetPassword(user.getUserId(), newPassword);
                loadDashboard(user);
            }
        });
    }

    // ── Load dashboard ────────────────────────────────────────────────────────

    private void loadDashboard(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/library/ui/ProfessionalDashboard.fxml"));
            Stage stage = (Stage) loginButton.getScene().getWindow();
            // Maximize first, then swap root — prevents window jump
            stage.setMaximized(true);
            Scene scene = loginButton.getScene();
            scene.setRoot(loader.load());
            ThemeManager.getInstance().applyTheme(scene);
            DashboardController dc = loader.getController();
            dc.initSession(user);
            stage.setTitle(LibraCoreApp.APP_NAME + " " + LibraCoreApp.APP_VERSION
                    + " — Dashboard");
            SessionManager.getInstance().login(user,
                    () -> Platform.runLater(() -> returnToLogin(stage)));
        } catch (IOException e) {
            showError("Failed to load dashboard: " + e.getMessage());
        }
    }

    private void returnToLogin(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/library/ui/LoginPage.fxml"));
            stage.setMaximized(false);
            stage.setWidth(1000);
            stage.setHeight(650);
            Scene scene = stage.getScene();
            scene.setRoot(loader.load());
            ThemeManager.getInstance().applyTheme(scene);
            stage.setTitle(LibraCoreApp.APP_NAME + " " + LibraCoreApp.APP_VERSION + " — Login");
            stage.centerOnScreen();
        } catch (IOException ignored) {}
    }

    // ── Theme toggle ──────────────────────────────────────────────────────────

    @FXML
    private void toggleTheme() {
        ThemeManager.getInstance().toggle(loginButton.getScene());
        updateThemeButtonLabel();
    }

    private void updateThemeButtonLabel() {
        if (themeToggleBtn == null) return;
        boolean dark = ThemeManager.getInstance().isDark();
        themeToggleBtn.setText(dark ? "☀ Light Mode" : "🌙 Dark Mode");
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
        FadeTransition ft = new FadeTransition(Duration.millis(300), errorLabel);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private void shakeField(javafx.scene.Node node) {
        javafx.animation.TranslateTransition tt =
                new javafx.animation.TranslateTransition(Duration.millis(60), node);
        tt.setFromX(0); tt.setByX(10); tt.setCycleCount(4);
        tt.setAutoReverse(true); tt.play();
    }
}
