package com.library.util;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;

/**
 * Animated toast notification utility.
 * Shows a slide-in / fade-out message anchored to the bottom-right
 * of the owning window. Supports SUCCESS, ERROR, WARNING, INFO types.
 */
public class ToastNotification {

    public enum Type { SUCCESS, ERROR, WARNING, INFO }

    private static final double WIDTH   = 320;
    private static final double HEIGHT  = 56;
    private static final double PADDING = 20;

    /**
     * Show a toast on the given scene.
     * Safe to call from any thread.
     */
    public static void show(Scene scene, String message, Type type) {
        if (scene == null || scene.getWindow() == null) return;
        Platform.runLater(() -> display(scene.getWindow(), message, type));
    }

    public static void success(Scene scene, String message) { show(scene, message, Type.SUCCESS); }
    public static void error  (Scene scene, String message) { show(scene, message, Type.ERROR);   }
    public static void warning(Scene scene, String message) { show(scene, message, Type.WARNING); }
    public static void info   (Scene scene, String message) { show(scene, message, Type.INFO);    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private static void display(Window owner, String message, Type type) {
        // Build label
        Label label = new Label(icon(type) + "  " + message);
        label.setWrapText(true);
        label.setMaxWidth(WIDTH - 32);
        label.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: 500;");

        // Container
        StackPane container = new StackPane(label);
        container.setPrefWidth(WIDTH);
        container.setMinHeight(HEIGHT);
        container.setPadding(new Insets(12, 16, 12, 16));
        container.setAlignment(Pos.CENTER_LEFT);
        container.setStyle(
            "-fx-background-color: " + bgColor(type) + ";" +
            "-fx-background-radius: 10;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 12, 0, 0, 4);"
        );

        // Popup
        Popup popup = new Popup();
        popup.getContent().add(container);
        popup.setAutoFix(false);

        // Position: bottom-right of owner window
        double x = owner.getX() + owner.getWidth()  - WIDTH  - PADDING;
        double y = owner.getY() + owner.getHeight()  - HEIGHT - PADDING - 40;
        popup.show(owner, x, y);

        // Animate: slide up + fade in
        container.setTranslateY(30);
        container.setOpacity(0);

        TranslateTransition slide = new TranslateTransition(Duration.millis(280), container);
        slide.setFromY(30); slide.setToY(0);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(280), container);
        fadeIn.setFromValue(0); fadeIn.setToValue(1);

        ParallelTransition enter = new ParallelTransition(slide, fadeIn);

        // Auto-dismiss after 3 seconds
        PauseTransition pause = new PauseTransition(Duration.seconds(3));

        FadeTransition fadeOut = new FadeTransition(Duration.millis(350), container);
        fadeOut.setFromValue(1); fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> popup.hide());

        SequentialTransition seq = new SequentialTransition(enter, pause, fadeOut);
        seq.play();
    }

    private static String icon(Type type) {
        return switch (type) {
            case SUCCESS -> "✅";
            case ERROR   -> "❌";
            case WARNING -> "⚠️";
            case INFO    -> "ℹ️";
        };
    }

    private static String bgColor(Type type) {
        return switch (type) {
            case SUCCESS -> "#2e7d32";
            case ERROR   -> "#c62828";
            case WARNING -> "#e65100";
            case INFO    -> "#0288d1";
        };
    }
}
