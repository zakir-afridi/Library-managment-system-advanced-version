package com.library.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * HikariCP-backed SQLite connection pool for LibraCore Pro v3.
 *
 * Replaces the hand-rolled ArrayBlockingQueue pool from v2.
 * Pool size: 10 (configurable via system property libracore.pool.size).
 * WAL mode and FK constraints are set per-connection via connectionInitSql.
 *
 * Usage (same as before):
 *   try (Connection c = HikariConnectionPool.getConnection()) { ... }
 */
public final class HikariConnectionPool {

    private static final Logger LOG = LoggerFactory.getLogger(HikariConnectionPool.class);

    /** DB file is placed next to the running JAR in a ~/.libracore/ user directory. */
    private static final String DB_FILE;

    static {
        String userHome = System.getProperty("user.home");
        Path dir = Paths.get(userHome, ".libracore");
        dir.toFile().mkdirs();
        DB_FILE = dir.resolve("library.db").toString();
    }

    private static volatile HikariDataSource dataSource;

    private HikariConnectionPool() {}

    /** Initialise the pool (idempotent — safe to call multiple times). */
    public static synchronized void initialise() {
        if (dataSource != null && !dataSource.isClosed()) return;

        HikariConfig cfg = new HikariConfig();
        cfg.setDriverClassName("org.sqlite.JDBC");
        cfg.setJdbcUrl("jdbc:sqlite:" + DB_FILE);
        cfg.setPoolName("LibraCorePool");

        // SQLite is file-based; a pool of 10 handles concurrent reads well.
        int poolSize = Integer.getInteger("libracore.pool.size", 10);
        cfg.setMaximumPoolSize(poolSize);
        cfg.setMinimumIdle(2);
        cfg.setConnectionTimeout(10_000);    // 10 s
        cfg.setIdleTimeout(300_000);          // 5 min
        cfg.setMaxLifetime(600_000);          // 10 min
        cfg.setConnectionTestQuery("SELECT 1");

        // Enable WAL mode and FK constraints on every connection
        cfg.setConnectionInitSql(
            "PRAGMA journal_mode=WAL; PRAGMA foreign_keys=ON; " +
            "PRAGMA synchronous=NORMAL; PRAGMA cache_size=-16000;"
        );

        // SQLite-specific: serialize writes; allow concurrent reads
        cfg.addDataSourceProperty("open_mode", "6");  // SQLITE_OPEN_READWRITE | SQLITE_OPEN_CREATE

        dataSource = new HikariDataSource(cfg);
        LOG.info("HikariCP pool initialised — DB: {} | pool size: {}", DB_FILE, poolSize);
    }

    /** Borrow a connection from the pool. Always use try-with-resources. */
    public static Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) initialise();
        return dataSource.getConnection();
    }

    /** Returns the absolute path to the SQLite database file. */
    public static String getDatabasePath() {
        return DB_FILE;
    }

    /** Gracefully close the pool on application shutdown. */
    public static synchronized void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            LOG.info("HikariCP pool closed.");
        }
    }
}
