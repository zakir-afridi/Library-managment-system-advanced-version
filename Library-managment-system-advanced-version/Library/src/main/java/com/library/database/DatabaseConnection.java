package com.library.database;

import com.library.security.PasswordUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

/**
 * Central database initialiser for LibraCore Pro v3.
 *
 * The connection pool itself is now managed by {@link HikariConnectionPool}.
 * This class is responsible only for schema creation, migrations, and seeding.
 *
 * Usage:
 *   DatabaseConnection.initialise();            // call once at startup
 *   try (Connection c = DatabaseConnection.getConnection()) { ... }
 */
public class DatabaseConnection {

    private static final Logger LOG = LoggerFactory.getLogger(DatabaseConnection.class);
    private static volatile boolean initialised = false;

    // ── Public API ─────────────────────────────────────────────────────────────

    /** Must be called once at application startup (from SharedModule.initDatabase()). */
    public static synchronized void initialise() {
        if (initialised) return;
        HikariConnectionPool.initialise();
        try (Connection c = HikariConnectionPool.getConnection()) {
            applySchema(c);
        } catch (Exception e) {
            throw new RuntimeException("DB initialisation failed: " + e.getMessage(), e);
        }
        initialised = true;
        LOG.info("Database initialised — {}", HikariConnectionPool.getDatabasePath());
    }

    /**
     * Borrow a connection from the HikariCP pool.
     * Always use try-with-resources — returns connection to pool on close().
     */
    public static Connection getConnection() throws SQLException {
        if (!initialised) initialise();
        return HikariConnectionPool.getConnection();
    }

    // ── Schema ─────────────────────────────────────────────────────────────────

    private static void applySchema(Connection c) throws SQLException {
        try (Statement s = c.createStatement()) {

            // ── users ──────────────────────────────────────────────────────
            s.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    user_id               INTEGER PRIMARY KEY AUTOINCREMENT,
                    username              TEXT    NOT NULL UNIQUE,
                    password_hash         TEXT    NOT NULL,
                    role                  TEXT    NOT NULL DEFAULT 'LIBRARIAN',
                    status                TEXT    NOT NULL DEFAULT 'Active',
                    failed_attempts       INTEGER NOT NULL DEFAULT 0,
                    locked_until          TEXT,
                    force_password_change INTEGER NOT NULL DEFAULT 0,
                    last_login            TEXT,
                    created_at            TEXT    NOT NULL DEFAULT (datetime('now'))
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
                    cover_url        TEXT,
                    book_code        TEXT,
                    serial_no        INTEGER,
                    archived_date    TEXT,
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
                    member_code        TEXT,
                    serial_no          INTEGER,
                    archived_date      TEXT,
                    registration_date  TEXT    NOT NULL DEFAULT (date('now'))
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
                    serial_no        INTEGER,
                    archived_date    TEXT
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
                    expires_at       TEXT,
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

            // ── book_metadata_cache (Open Library — 30-day TTL) ────────────
            s.execute("""
                CREATE TABLE IF NOT EXISTS book_metadata_cache (
                    isbn         TEXT PRIMARY KEY,
                    title        TEXT,
                    author       TEXT,
                    publisher    TEXT,
                    publish_date TEXT,
                    page_count   INTEGER,
                    description  TEXT,
                    cover_url    TEXT,
                    category     TEXT,
                    fetched_at   TEXT NOT NULL DEFAULT (datetime('now'))
                )
            """);

            // ── email_queue ────────────────────────────────────────────────
            s.execute("""
                CREATE TABLE IF NOT EXISTS email_queue (
                    queue_id   INTEGER PRIMARY KEY AUTOINCREMENT,
                    recipient  TEXT    NOT NULL,
                    subject    TEXT    NOT NULL,
                    body       TEXT    NOT NULL,
                    status     TEXT    NOT NULL DEFAULT 'PENDING',
                    attempts   INTEGER NOT NULL DEFAULT 0,
                    created_at TEXT    NOT NULL DEFAULT (datetime('now')),
                    updated_at TEXT
                )
            """);

            // ── Legacy tables (backward compat) ────────────────────────────
            s.execute("""
                CREATE TABLE IF NOT EXISTS admin (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
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

            // ── id_counters ───────────────────────────────────────────────
            s.execute("""
                CREATE TABLE IF NOT EXISTS id_counters (
                    entity  TEXT    PRIMARY KEY,
                    last_id INTEGER NOT NULL DEFAULT 0
                )
            """);
            s.execute("INSERT OR IGNORE INTO id_counters (entity, last_id) VALUES ('BK', 0)");
            s.execute("INSERT OR IGNORE INTO id_counters (entity, last_id) VALUES ('ST', 0)");
            s.execute("INSERT OR IGNORE INTO id_counters (entity, last_id) VALUES ('MB', 0)");
            s.execute("INSERT OR IGNORE INTO id_counters (entity, last_id) VALUES ('EP', 0)");

            // ── Indexes ────────────────────────────────────────────────────
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
            s.execute("CREATE INDEX IF NOT EXISTS idx_cache_isbn     ON book_metadata_cache(isbn)");
            s.execute("CREATE INDEX IF NOT EXISTS idx_email_status   ON email_queue(status, attempts)");
        }

        runMigrations(c);
        seedDefaultAdmin(c);
        seedLegacyAdmin(c);
    }

    // ── Migrations ─────────────────────────────────────────────────────────────

    /** Additive migrations — safe to run on every startup. */
    private static void runMigrations(Connection c) {
        String[] migrations = {
            // v3 additions
            "ALTER TABLE users        ADD COLUMN locked_until TEXT",
            "ALTER TABLE books        ADD COLUMN cover_url TEXT",
            "ALTER TABLE reservations ADD COLUMN expires_at TEXT",
            // v2 legacy safety
            "ALTER TABLE members   ADD COLUMN archived_date TEXT",
            "ALTER TABLE books     ADD COLUMN archived_date TEXT",
            "ALTER TABLE books     ADD COLUMN book_code TEXT",
            "ALTER TABLE members   ADD COLUMN member_code TEXT",
            "ALTER TABLE books     ADD COLUMN serial_no INTEGER",
            "ALTER TABLE members   ADD COLUMN serial_no INTEGER",
            "ALTER TABLE employees ADD COLUMN serial_no INTEGER"
        };
        for (String sql : migrations) {
            try (Statement s = c.createStatement()) {
                s.execute(sql);
            } catch (SQLException ignored) {}
        }
        try (Statement s = c.createStatement()) {
            s.execute("CREATE INDEX IF NOT EXISTS idx_books_code   ON books(book_code)");
            s.execute("CREATE INDEX IF NOT EXISTS idx_members_code ON members(member_code)");
        } catch (SQLException ignored) {}
    }

    // ── Seed ──────────────────────────────────────────────────────────────────

    private static void seedDefaultAdmin(Connection c) throws SQLException {
        try (Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM users WHERE username='admin'")) {
            if (rs.next() && rs.getInt(1) == 0) {
                String hash = PasswordUtil.hash("admin");
                try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO users (username, password_hash, role, force_password_change) " +
                    "VALUES ('admin', ?, 'ADMIN', 0)")) {
                    ps.setString(1, hash);
                    ps.executeUpdate();
                }
                LOG.info("Default admin user seeded.");
            }
        }
    }

    private static void seedLegacyAdmin(Connection c) throws SQLException {
        try (Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM admin")) {
            if (rs.next() && rs.getInt(1) == 0)
                s.execute("INSERT OR IGNORE INTO admin (id,username,password) VALUES (1,'admin','admin')");
        } catch (SQLException ignored) {}
        try (Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM librarydetails")) {
            if (rs.next() && rs.getInt(1) == 0)
                s.execute("INSERT OR IGNORE INTO librarydetails (id,library_title) VALUES (1,'LibraCore Pro')");
        } catch (SQLException ignored) {}
    }
}
