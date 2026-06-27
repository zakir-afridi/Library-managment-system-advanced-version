package com.library.util;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Global uncaught exception handler for LibraCore Pro.
 *
 * Catches any Throwable that escapes all other handlers (including
 * javafx.application.Application.start()) and shows a user-friendly
 * crash dialog instead of silently killing the app.
 *
 * Registered once at startup in LibraCoreApp.init().
 */
public final class GlobalExceptionHandler implements Thread.UncaughtExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final GlobalExceptionHandler INSTANCE = new GlobalExceptionHandler();

    private GlobalExceptionHandler() {}

    /** Register as the default handler for all threads. */
    public static void register() {
        Thread.setDefaultUncaughtExceptionHandler(INSTANCE);
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        LOG.error("UNCAUGHT EXCEPTION on thread [{}]", thread.getName(), ex);

        // Ensure the dialog appears on the JavaFX Application Thread
        if (Platform.isFxApplicationThread()) {
            showDialog(thread, ex);
        } else {
            Platform.runLater(() -> showDialog(thread, ex));
        }
    }

    private void showDialog(Thread thread, Throwable ex) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("LibraCore Pro — Unexpected Error");
        alert.setHeaderText("An unexpected error has occurred.\n\n"
                + ex.getClass().getSimpleName() + ": " + ex.getMessage());

        // Stack trace in an expandable text area
        StringWriter sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));

        TextArea ta = new TextArea(sw.toString());
        ta.setEditable(false);
        ta.setWrapText(false);
        ta.setMaxWidth(Double.MAX_VALUE);
        ta.setMaxHeight(Double.MAX_VALUE);
        ta.setStyle("-fx-font-family: 'Courier New', monospace; -fx-font-size: 11px;");

        alert.getDialogPane().setExpandableContent(ta);
        alert.getDialogPane().setExpanded(false);
        alert.showAndWait();
    }
}
