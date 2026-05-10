package com.library.database;

import com.library.security.PasswordUtil;

import java.sql.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Singleton database connection manager.
 *
 * Provides a minimal connection pool (size 5) backed by SQLite.
 * Handles schema creation, migrations, and default seed data.
 *
 * Usage:
 *   try (Connection c = DatabaseConnection.getConnection()) { ... }
 * The try-with-resources returns the connection to the pool automatically.
 */
public class DatabaseConnection {

    private static final String DB_URL    = "jdbc:sqlite:library.db";
    private static final int    POOL_SIZE = 5;

    private static final BlockingQueue<Connection> pool =
            new ArrayBlockingQueue<>(POOL_SIZE);

    private static volatile boolean initialised = false;

    // ── Public API ────────────────────────────────────────────────────────────

    /** Must be called once at application startup (from LibraCoreApp.init()). */
    public static synchronized void initialise() {
        if (initialised) return;
        try {
            for (int i = 0; i < POOL_SIZE; i++) {
                Connection c = createRawConnection();
                pool.offer(c);
            }
            // Run schema on a dedicated connection, NOT from the pool
            try (Connection c = createRawConnection()) {
                applySchema(c);
            }
            initialised = true;
        } catch (Exception e) {
            throw new RuntimeException("DB initialisation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Borrow a connection from the pool.
     * The returned Connection's close() returns it to the pool.
     */
    public static Connection getConnection() throws SQLException {
        if (!initialised) initialise();
        try {
            Connection raw = pool.take();
            // Wrap so close() returns to pool instead of closing
            return new PooledConnection(raw);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("Interrupted waiting for DB connection", e);
        }
    }

    // ── Schema ────────────────────────────────────────────────────────────────

    private static void applySchema(Connection c) throws SQLException {
        try (Statement s = c.createStatement()) {
            s.execute("PRAGMA journal_mode=WAL");
            s.execute("PRAGMA foreign_keys=ON");

            // ── users ──────────────────────────────────────────────────────
            s.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    user_id              INTEGER PRIMARY KEY AUTOINCREMENT,
                    username             TEXT    NOT NULL UNIQUE,
                    password_hash        TEXT    NOT NULL,
                    role                 TEXT    NOT NULL DEFAULT 'LIBRARIAN',
                    status               TEXT    NOT NULL DEFAULT 'Active',
                    failed_attempts      INTEGER NOT NULL DEFAULT 0,
                    force_password_change INTEGER NOT NULL DEFAULT 0,
                    last_login           TEXT,
                    created_at           TEXT    NOT NULL DEFAULT (datetime('now'))
                )
            """);

            // ── books ──────────────────────────────────────────────────────
            s.execute("""
                CREATE TABLE IF NOT EXISTS books (
                    book_id          INTEGER PRIMARY KEY AUTOINCREMENT,
                    isbn             TEXT    UNIQUE,
                    book_name        TEXT    NOT NULL,
                    author           TEXT    NOT NULL,
                    publisher        TEXT,
                    publication_year INTEGER,
                    edition          TEXT,
                    category         TEXT,
                    description      TEXT,
                    quantity         INTEGER NOT NULL DEFAULT 1,
                    available_qty    INTEGER NOT NULL DEFAULT 1,
                    status           TEXT    NOT NULL DEFAULT 'Available',
                    shelf_location   TEXT,
                    cover_image      BLOB,
                    created_at       TEXT    NOT NULL DEFAULT (date('now'))
                )
            """);

            // ── members ────────────────────────────────────────────────────
            s.execute("""
                CREATE TABLE IF NOT EXISTS members (
                    std_id             INTEGER PRIMARY KEY AUTOINCREMENT,
                    student_id         TEXT    UNIQUE,
                    name               TEXT    NOT NULL,
                    fname              TEXT,
                    cnic               TEXT,
                    date_of_birth      TEXT,
                    gender             TEXT,
                    contact            TEXT,
                    email              TEXT,
                    emergency_contact  TEXT,
                    blood_group        TEXT,
                    address            TEXT,
                    city               TEXT,
                    province           TEXT,
                    postal_code        TEXT,
                    country            TEXT    DEFAULT 'Pakistan',
                    department         TEXT,
                    program            TEXT,
                    semester           TEXT,
                    session            TEXT,
                    admission_date     TEXT,
                    status             TEXT    NOT NULL DEFAULT 'Active',
                    library_card_no    TEXT,
                    book_limit         INTEGER NOT NULL DEFAULT 5,
                    membership_type    TEXT    DEFAULT 'Student',
                    membership_expiry  TEXT,
                    fine_balance       REAL    NOT NULL DEFAULT 0.0,
                    notes              TEXT,
                    profile_pic        BLOB,
                    registration_date  TEXT    NOT NULL DEFAULT (date('now'))
                )
            """);

            // ── transactions ───────────────────────────────────────────────
            s.execute("""
                CREATE TABLE IF NOT EXISTS transactions (
                    transaction_id   INTEGER PRIMARY KEY AUTOINCREMENT,
                    book_id          INTEGER NOT NULL,
                    member_id        INTEGER NOT NULL,
                    issue_date       TEXT    NOT NULL,
                    due_date         TEXT    NOT NULL,
                    return_date      TEXT,
                    fine_amount      REAL    NOT NULL DEFAULT 0.0,
                    fine_paid        INTEGER NOT NULL DEFAULT 0,
                    status           TEXT    NOT NULL DEFAULT 'Issued',
                    return_condition TEXT,
                    issued_by        TEXT,
                    FOREIGN KEY (book_id)   REFERENCES books(book_id),
                    FOREIGN KEY (member_id) REFERENCES members(std_id)
                )
            """);

            // ── reservations ───────────────────────────────────────────────
            s.execute("""
                CREATE TABLE IF NOT EXISTS reservations (
                    reservation_id   INTEGER PRIMARY KEY AUTOINCREMENT,
                    book_id          INTEGER NOT NULL,
                    member_id        INTEGER NOT NULL,
                    reservation_date TEXT    NOT NULL DEFAULT (date('now')),
                    status           TEXT    NOT NULL DEFAULT 'Pending',
                    queue_position   INTEGER NOT NULL DEFAULT 1,
                    notified_date    TEXT,
                    FOREIGN KEY (book_id)   REFERENCES books(book_id),
                    FOREIGN KEY (member_id) REFERENCES members(std_id)
                )
            """);

            // ── settings ───────────────────────────────────────────────────
            s.execute("""
                CREATE TABLE IF NOT EXISTS settings (
                    key   TEXT PRIMARY KEY,
                    value TEXT
                )
            """);

            // ── activity_log ───────────────────────────────────────────────
            s.execute("""
                CREATE TABLE IF NOT EXISTS activity_log (
                    log_id    INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id   INTEGER,
                    action    TEXT    NOT NULL,
                    details   TEXT,
                    timestamp TEXT    NOT NULL DEFAULT (datetime('now'))
                )
            """);

            // ── Legacy table kept for backward compatibility ────────────────
            s.execute("""
                CREATE TABLE IF NOT EXISTS admin (
                    id       INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT NOT NULL,
                    password TEXT NOT NULL
                )
            """);
            s.execute("""
                CREATE TABLE IF NOT EXISTS librarydetails (
                    id            INTEGER PRIMARY KEY AUTOINCREMENT,
                    library_title TEXT DEFAULT 'LibraCore Pro',
                    library_logo  BLOB
                )
            """);

            // ── employees ────────────────────────────────────────────────────
            s.execute("""
                CREATE TABLE IF NOT EXISTS employees (
                    emp_id           INTEGER PRIMARY KEY AUTOINCREMENT,
                    employee_code    TEXT    UNIQUE NOT NULL,
                    name             TEXT    NOT NULL,
                    designation      TEXT,
                    department       TEXT,
                    contact          TEXT,
                    email            TEXT,
                    cnic             TEXT,
                    address          TEXT,
                    join_date        TEXT    NOT NULL DEFAULT (date('now')),
                    status           TEXT    NOT NULL DEFAULT 'Active',
                    salary           REAL    DEFAULT 0.0,
                    notes            TEXT,
                    profile_pic      BLOB,
                    archived_date    TEXT
                )
            """);

            // ── id_counters (for structured ID generation) ──────────────────────
            s.execute("""
                CREATE TABLE IF NOT EXISTS id_counters (
                    entity TEXT PRIMARY KEY,
                    last_id INTEGER NOT NULL DEFAULT 0
                )
            """);

            // Seed counter rows — EP for employees (not EM)
            s.execute("INSERT OR IGNORE INTO id_counters (entity, last_id) VALUES ('BK', 0)");
            s.execute("INSERT OR IGNORE INTO id_counters (entity, last_id) VALUES ('ST', 0)");
            s.execute("INSERT OR IGNORE INTO id_counters (entity, last_id) VALUES ('MB', 0)");
            s.execute("INSERT OR IGNORE INTO id_counters (entity, last_id) VALUES ('EP', 0)");

            // ── Indexes for 1M-scale queries ───────────────────────────────────────────────
            s.execute("CREATE INDEX IF NOT EXISTS idx_books_isbn     ON books(isbn)");
            s.execute("CREATE INDEX IF NOT EXISTS idx_books_name     ON books(book_name)");
            s.execute("CREATE INDEX IF NOT EXISTS idx_books_category ON books(category)");
            s.execute("CREATE INDEX IF NOT EXISTS idx_books_author   ON books(author)");
            s.execute("CREATE INDEX IF NOT EXISTS idx_books_status   ON books(status)");
            s.execute("CREATE INDEX IF NOT EXISTS idx_members_sid    ON members(student_id)");
            s.execute("CREATE INDEX IF NOT EXISTS idx_members_name   ON members(name)");
            s.execute("CREATE INDEX IF NOT EXISTS idx_members_email  ON members(email)");
            s.execute("CREATE INDEX IF NOT EXISTS idx_members_status ON members(status)");
            s.execute("CREATE INDEX IF NOT EXISTS idx_tx_member      ON transactions(member_id, status)");
            s.execute("CREATE INDEX IF NOT EXISTS idx_tx_due         ON transactions(due_date)");
            s.execute("CREATE INDEX IF NOT EXISTS idx_tx_book        ON transactions(book_id)");
            s.execute("CREATE INDEX IF NOT EXISTS idx_res_book       ON reservations(book_id, status)");
            s.execute("CREATE INDEX IF NOT EXISTS idx_emp_code       ON employees(employee_code)");
            s.execute("CREATE INDEX IF NOT EXISTS idx_emp_name       ON employees(name)");
            s.execute("CREATE INDEX IF NOT EXISTS idx_emp_status     ON employees(status)");

            // ── Migrations: add columns if they don't exist ────────────────
            runMigrations(c);

            // ── Seed default admin user ────────────────────────────────────
            seedDefaultAdmin(c);
            seedLegacyAdmin(c);
        }
    }

    private static void seedDefaultAdmin(Connection c) throws SQLException {
        String check = "SELECT COUNT(*) FROM users WHERE username = 'admin'";
        try (Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(check)) {
            if (rs.next() && rs.getInt(1) == 0) {
                String hash = PasswordUtil.hash("admin");
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO users (username, password_hash, role, force_password_change) " +
                        "VALUES ('admin', ?, 'ADMIN', 0)")) {
                    ps.setString(1, hash);
                    ps.executeUpdate();
                }
            }
        }
    }

    /** Safe ALTER TABLE — adds column only if it doesn't already exist. */
    private static void runMigrations(Connection c) {
        // Each entry is safe to run multiple times — duplicate column errors are ignored
        String[] migrations = {
            // Archive date columns
            "ALTER TABLE members   ADD COLUMN archived_date TEXT",
            "ALTER TABLE books     ADD COLUMN archived_date TEXT",
            // Structured ID columns
            "ALTER TABLE books     ADD COLUMN book_code   TEXT",
            "ALTER TABLE members   ADD COLUMN member_code TEXT",
            // Serial number columns (display order, resequenced on add/remove)
            "ALTER TABLE books     ADD COLUMN serial_no INTEGER",
            "ALTER TABLE members   ADD COLUMN serial_no INTEGER",
            "ALTER TABLE employees ADD COLUMN serial_no INTEGER"
        };
        for (String sql : migrations) {
            try (Statement s = c.createStatement()) {
                s.execute(sql);
            } catch (SQLException ignored) {
                // Column already exists — safe to ignore
            }
        }
        // Indexes on code columns (safe to re-run)
        try (Statement s = c.createStatement()) {
            s.execute("CREATE INDEX IF NOT EXISTS idx_books_code   ON books(book_code)");
            s.execute("CREATE INDEX IF NOT EXISTS idx_members_code ON members(member_code)");
        } catch (SQLException ignored) {}
    }

    private static void seedLegacyAdmin(Connection c) throws SQLException {
        String check = "SELECT COUNT(*) FROM admin";
        try (Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(check)) {
            if (rs.next() && rs.getInt(1) == 0) {
                s.execute("INSERT OR IGNORE INTO admin (id, username, password) VALUES (1,'admin','admin')");
            }
        }
        String checkLib = "SELECT COUNT(*) FROM librarydetails";
        try (Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(checkLib)) {
            if (rs.next() && rs.getInt(1) == 0) {
                s.execute("INSERT OR IGNORE INTO librarydetails (id, library_title) VALUES (1,'LibraCore Pro')");
            }
        }
    }

    // ── Raw connection factory ────────────────────────────────────────────────

    private static Connection createRawConnection() throws SQLException {
        Connection c = DriverManager.getConnection(DB_URL);
        try (Statement s = c.createStatement()) {
            s.execute("PRAGMA foreign_keys=ON");
            s.execute("PRAGMA journal_mode=WAL");
        }
        return c;
    }

    // ── Pooled connection wrapper ─────────────────────────────────────────────

    /**
     * Wraps a real Connection so that close() returns it to the pool
     * instead of physically closing it.
     */
    private static class PooledConnection implements java.sql.Connection {
        // We delegate everything to the real connection.
        // Only close() is overridden to return to pool.

        private final Connection real;

        PooledConnection(Connection real) { this.real = real; }

        @Override public void close() {
            pool.offer(real);   // return to pool
        }

        // ── All other methods delegate to real ────────────────────────────
        @Override public Statement createStatement() throws SQLException { return real.createStatement(); }
        @Override public PreparedStatement prepareStatement(String sql) throws SQLException { return real.prepareStatement(sql); }
        @Override public CallableStatement prepareCall(String sql) throws SQLException { return real.prepareCall(sql); }
        @Override public String nativeSQL(String sql) throws SQLException { return real.nativeSQL(sql); }
        @Override public void setAutoCommit(boolean b) throws SQLException { real.setAutoCommit(b); }
        @Override public boolean getAutoCommit() throws SQLException { return real.getAutoCommit(); }
        @Override public void commit() throws SQLException { real.commit(); }
        @Override public void rollback() throws SQLException { real.rollback(); }
        @Override public boolean isClosed() throws SQLException { return real.isClosed(); }
        @Override public DatabaseMetaData getMetaData() throws SQLException { return real.getMetaData(); }
        @Override public void setReadOnly(boolean b) throws SQLException { real.setReadOnly(b); }
        @Override public boolean isReadOnly() throws SQLException { return real.isReadOnly(); }
        @Override public void setCatalog(String c) throws SQLException { real.setCatalog(c); }
        @Override public String getCatalog() throws SQLException { return real.getCatalog(); }
        @Override public void setTransactionIsolation(int l) throws SQLException { real.setTransactionIsolation(l); }
        @Override public int getTransactionIsolation() throws SQLException { return real.getTransactionIsolation(); }
        @Override public SQLWarning getWarnings() throws SQLException { return real.getWarnings(); }
        @Override public void clearWarnings() throws SQLException { real.clearWarnings(); }
        @Override public Statement createStatement(int t, int c) throws SQLException { return real.createStatement(t, c); }
        @Override public PreparedStatement prepareStatement(String sql, int t, int c) throws SQLException { return real.prepareStatement(sql, t, c); }
        @Override public CallableStatement prepareCall(String sql, int t, int c) throws SQLException { return real.prepareCall(sql, t, c); }
        @Override public java.util.Map<String, Class<?>> getTypeMap() throws SQLException { return real.getTypeMap(); }
        @Override public void setTypeMap(java.util.Map<String, Class<?>> m) throws SQLException { real.setTypeMap(m); }
        @Override public void setHoldability(int h) throws SQLException { real.setHoldability(h); }
        @Override public int getHoldability() throws SQLException { return real.getHoldability(); }
        @Override public Savepoint setSavepoint() throws SQLException { return real.setSavepoint(); }
        @Override public Savepoint setSavepoint(String n) throws SQLException { return real.setSavepoint(n); }
        @Override public void rollback(Savepoint s) throws SQLException { real.rollback(s); }
        @Override public void releaseSavepoint(Savepoint s) throws SQLException { real.releaseSavepoint(s); }
        @Override public Statement createStatement(int t, int c, int h) throws SQLException { return real.createStatement(t, c, h); }
        @Override public PreparedStatement prepareStatement(String sql, int t, int c, int h) throws SQLException { return real.prepareStatement(sql, t, c, h); }
        @Override public CallableStatement prepareCall(String sql, int t, int c, int h) throws SQLException { return real.prepareCall(sql, t, c, h); }
        @Override public PreparedStatement prepareStatement(String sql, int[] ci) throws SQLException { return real.prepareStatement(sql, ci); }
        @Override public PreparedStatement prepareStatement(String sql, String[] cn) throws SQLException { return real.prepareStatement(sql, cn); }
        @Override public PreparedStatement prepareStatement(String sql, int ag) throws SQLException { return real.prepareStatement(sql, ag); }
        @Override public Clob createClob() throws SQLException { return real.createClob(); }
        @Override public Blob createBlob() throws SQLException { return real.createBlob(); }
        @Override public NClob createNClob() throws SQLException { return real.createNClob(); }
        @Override public SQLXML createSQLXML() throws SQLException { return real.createSQLXML(); }
        @Override public boolean isValid(int t) throws SQLException { return real.isValid(t); }
        @Override public void setClientInfo(String n, String v) throws java.sql.SQLClientInfoException { real.setClientInfo(n, v); }
        @Override public void setClientInfo(java.util.Properties p) throws java.sql.SQLClientInfoException { real.setClientInfo(p); }
        @Override public String getClientInfo(String n) throws SQLException { return real.getClientInfo(n); }
        @Override public java.util.Properties getClientInfo() throws SQLException { return real.getClientInfo(); }
        @Override public Array createArrayOf(String t, Object[] e) throws SQLException { return real.createArrayOf(t, e); }
        @Override public Struct createStruct(String t, Object[] a) throws SQLException { return real.createStruct(t, a); }
        @Override public void setSchema(String s) throws SQLException { real.setSchema(s); }
        @Override public String getSchema() throws SQLException { return real.getSchema(); }
        @Override public void abort(java.util.concurrent.Executor e) throws SQLException { real.abort(e); }
        @Override public void setNetworkTimeout(java.util.concurrent.Executor e, int ms) throws SQLException { real.setNetworkTimeout(e, ms); }
        @Override public int getNetworkTimeout() throws SQLException { return real.getNetworkTimeout(); }
        @Override public <T> T unwrap(Class<T> i) throws SQLException { return real.unwrap(i); }
        @Override public boolean isWrapperFor(Class<?> i) throws SQLException { return real.isWrapperFor(i); }
    }
}
