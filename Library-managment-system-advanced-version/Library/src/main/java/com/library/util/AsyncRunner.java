package com.library.util;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Virtual-thread-based async task runner for LibraCore Pro v3.
 *
 * Replaces raw Thread / JavaFX Task patterns with a clean, reusable API.
 * Uses Java 21 virtual threads (Executors.newVirtualThreadPerTaskExecutor)
 * so thousands of concurrent lightweight tasks are possible.
 *
 * Example usage:
 *   AsyncRunner.run(
 *       () -> bookService.getAllBooks(page, 50),     // background supplier
 *       books -> booksTable.setItems(FXCollections.observableArrayList(books)),  // UI callback
 *       err   -> ToastNotification.error(scene, "Load failed: " + err.getMessage())
 *   );
 */
public final class AsyncRunner {

    private static final Logger LOG = LoggerFactory.getLogger(AsyncRunner.class);

    /** Shared virtual-thread executor — no thread pool overhead. */
    private static final ExecutorService EXECUTOR =
        Executors.newVirtualThreadPerTaskExecutor();

    private AsyncRunner() {}

    /**
     * Run a blocking computation off the JavaFX thread, then deliver the result
     * back to the JavaFX Application Thread.
     *
     * @param computation  Runs on a virtual thread (must not touch JavaFX nodes)
     * @param onSuccess    Runs on FX thread with the computed value
     * @param onError      Runs on FX thread with the exception (nullable)
     */
    public static <T> void run(Supplier<T> computation,
                               Consumer<T> onSuccess,
                               Consumer<Throwable> onError) {
        EXECUTOR.submit(() -> {
            try {
                T result = computation.get();
                Platform.runLater(() -> {
                    try {
                        onSuccess.accept(result);
                    } catch (Exception e) {
                        LOG.error("AsyncRunner onSuccess error", e);
                    }
                });
            } catch (Throwable e) {
                LOG.error("AsyncRunner task error", e);
                Platform.runLater(() -> {
                    if (onError != null) {
                        try { onError.accept(e); }
                        catch (Exception ex) { LOG.error("AsyncRunner onError error", ex); }
                    }
                });
            }
        });
    }

    /**
     * Fire-and-forget version — no result or error handling.
     * Use for side-effect only tasks (e.g., saving to DB).
     */
    public static void fire(Runnable task) {
        EXECUTOR.submit(() -> {
            try { task.run(); }
            catch (Exception e) { LOG.error("AsyncRunner fire error", e); }
        });
    }

    /**
     * Run a computation with an optional busy cursor on the given scene.
     * Cursor is reset to DEFAULT on completion (success or error).
     */
    public static <T> void runWithCursor(Scene scene,
                                         Supplier<T> computation,
                                         Consumer<T> onSuccess,
                                         Consumer<Throwable> onError) {
        if (scene != null) Platform.runLater(() -> scene.setCursor(Cursor.WAIT));
        run(
            computation,
            result -> {
                if (scene != null) scene.setCursor(Cursor.DEFAULT);
                onSuccess.accept(result);
            },
            err -> {
                if (scene != null) scene.setCursor(Cursor.DEFAULT);
                if (onError != null) onError.accept(err);
            }
        );
    }
}
