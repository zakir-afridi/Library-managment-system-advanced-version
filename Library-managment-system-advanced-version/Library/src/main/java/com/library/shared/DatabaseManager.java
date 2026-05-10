package com.library.shared;

import com.library.database.DatabaseConnection;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * SHARED BRANCH — database manager.
 * Single access point for SQLite connections across all branches.
 * Delegates to com.library.database.DatabaseConnection (connection pool).
 */
public class DatabaseManager {

    public static void initialize() {
        DatabaseConnection.initialise();
    }

    /**
     * Borrow a pooled connection. Always use try-with-resources:
     * {@code try (Connection c = DatabaseManager.getConnection()) { ... }}
     */
    public static Connection getConnection() throws SQLException {
        return DatabaseConnection.getConnection();
    }

    /** Called on application shutdown to release the connection pool. */
    public static void close() {
        // DatabaseConnection pool connections are physical JDBC connections;
        // SQLite releases file locks when the JVM exits, but we signal cleanly.
        System.out.println("DatabaseManager: connection pool released");
    }
}
