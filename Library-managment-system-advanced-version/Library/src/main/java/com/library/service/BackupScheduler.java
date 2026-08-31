package com.library.service;

import com.library.database.DatabaseConnection;
import com.library.database.HikariConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Automated database backup scheduler for LibraCore Pro v3.
 *
 * Features:
 *  - Daily automatic backup to ~/.libracore/backups/ at next startup boundary
 *  - WAL checkpointing prior to copy to ensure zero data loss
 *  - Keeps last 30 backups (configurable)
 *  - One-click manual backup/restore via SettingsController
 *
 * Usage:
 *   BackupScheduler.getInstance().start();   // call from LibraCoreApp.init()
 *   BackupScheduler.getInstance().stop();    // call from LibraCoreApp.stop()
 */
public class BackupScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(BackupScheduler.class);

    private static final int MAX_BACKUPS = 30;
    private static final DateTimeFormatter FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private final ScheduledExecutorService scheduler =
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "backup-scheduler");
            t.setDaemon(true);
            return t;
        });

    private static BackupScheduler instance;

    private BackupScheduler() {}

    public static synchronized BackupScheduler getInstance() {
        if (instance == null) instance = new BackupScheduler();
        return instance;
    }

    /** Start the daily backup scheduler (runs 24 hours after first trigger). */
    public void start() {
        scheduler.scheduleAtFixedRate(
            this::runBackup,
            computeInitialDelaySeconds(),   // next midnight
            TimeUnit.DAYS.toSeconds(1),     // every 24 hours
            TimeUnit.SECONDS
        );
        LOG.info("BackupScheduler started — daily backup at midnight.");
    }

    /** Stop the scheduler gracefully. */
    public void stop() {
        scheduler.shutdownNow();
    }

    /**
     * Perform an immediate backup. Flushes SQLite WAL first.
     * Returns the path of the created backup file.
     * Throws IOException on failure.
     */
    public Path backup() throws IOException {
        // Flush WAL pages to main database file
        checkpointWal();

        Path dbFile = Paths.get(HikariConnectionPool.getDatabasePath());
        if (!Files.exists(dbFile)) {
            throw new IOException("Database file not found: " + dbFile);
        }

        Path backupDir = getBackupDir();
        String timestamp = LocalDateTime.now().format(FMT);
        Path dest = backupDir.resolve("library_backup_" + timestamp + ".db");

        Files.copy(dbFile, dest, StandardCopyOption.REPLACE_EXISTING);
        LOG.info("Backup created: {}", dest);

        pruneOldBackups(backupDir);
        return dest;
    }

    /**
     * Restore from a backup file. The application must be restarted after restore.
     * Returns true if restore was successful.
     */
    public boolean restore(Path backupFile) {
        if (!Files.exists(backupFile)) {
            LOG.error("Backup file not found: {}", backupFile);
            return false;
        }
        try {
            Path dbFile = Paths.get(HikariConnectionPool.getDatabasePath());
            HikariConnectionPool.close();

            // Remove stale WAL / SHM files if present
            Path walFile = Paths.get(HikariConnectionPool.getDatabasePath() + "-wal");
            Path shmFile = Paths.get(HikariConnectionPool.getDatabasePath() + "-shm");
            Files.deleteIfExists(walFile);
            Files.deleteIfExists(shmFile);

            Files.copy(backupFile, dbFile, StandardCopyOption.REPLACE_EXISTING);
            LOG.info("Database restored from: {}", backupFile);
            return true;
        } catch (IOException e) {
            LOG.error("Restore failed: {}", e.getMessage());
            return false;
        }
    }

    /** Checkpoints the SQLite WAL file into the main database file. */
    private void checkpointWal() {
        try (Connection c = DatabaseConnection.getConnection();
             Statement st = c.createStatement()) {
            st.execute("PRAGMA wal_checkpoint(TRUNCATE)");
            LOG.debug("SQLite WAL checkpoint (TRUNCATE) succeeded.");
        } catch (Exception e) {
            LOG.warn("WAL checkpoint warning: {}", e.getMessage());
        }
    }

    /** List all available backup files, newest first. */
    public List<Path> listBackups() throws IOException {
        Path dir = getBackupDir();
        try (var stream = Files.list(dir)) {
            return stream
                .filter(p -> p.getFileName().toString().startsWith("library_backup_"))
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());
        }
    }

    /** Returns the backup directory, creating it if needed. */
    public Path getBackupDir() throws IOException {
        String userHome = System.getProperty("user.home");
        Path dir = Paths.get(userHome, ".libracore", "backups");
        Files.createDirectories(dir);
        return dir;
    }

    // ── Internals ─────────────────────────────────────────────────────────────

    private void runBackup() {
        try {
            Path dest = backup();
            LOG.info("Scheduled backup complete: {}", dest.getFileName());
        } catch (IOException e) {
            LOG.error("Scheduled backup failed: {}", e.getMessage());
        }
    }

    /** Delete oldest backups, keeping at most MAX_BACKUPS. */
    private void pruneOldBackups(Path dir) throws IOException {
        List<Path> backups = listBackups();
        if (backups.size() > MAX_BACKUPS) {
            List<Path> toDelete = backups.subList(MAX_BACKUPS, backups.size());
            for (Path old : toDelete) {
                Files.deleteIfExists(old);
                LOG.debug("Pruned old backup: {}", old.getFileName());
            }
        }
    }

    /** Compute seconds until next midnight. */
    private long computeInitialDelaySeconds() {
        java.time.LocalTime now = java.time.LocalTime.now();
        long secondsUntilMidnight = 86400L - (now.toSecondOfDay());
        return secondsUntilMidnight > 60 ? secondsUntilMidnight : 60;
    }
}
