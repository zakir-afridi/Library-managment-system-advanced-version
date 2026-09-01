package com.library.repository;

import com.library.database.DatabaseConnection;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Base data access class providing connection management.
 */
public abstract class BaseRepository {

    protected Connection getConnection() throws SQLException {
        return DatabaseConnection.getConnection();
    }
}
