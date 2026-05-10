package com.library.util;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Legacy DatabaseConnection kept for backward compatibility.
 * All calls now delegate to com.library.database.DatabaseConnection.
 */
public class DatabaseConnection {

    public static Connection getConnection() throws SQLException {
        return com.library.database.DatabaseConnection.getConnection();
    }
}