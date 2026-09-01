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

            // ── categories ─────────────────────────────────────────────────
            s.execute("""
                CREATE TABLE IF NOT EXISTS categories (
                    category_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name        TEXT NOT NULL UNIQUE,
                    description TEXT,
                    status      TEXT NOT NULL DEFAULT 'Active'
                )
            """);

            // ── publishers ─────────────────────────────────────────────────
            s.execute("""
                CREATE TABLE IF NOT EXISTS publishers (
                    publisher_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name         TEXT NOT NULL UNIQUE,
                    contact      TEXT,
                    address      TEXT,
                    status       TEXT NOT NULL DEFAULT 'Active'
                )
            """);

            // ── measurement_units ──────────────────────────────────────────
            s.execute("""
                CREATE TABLE IF NOT EXISTS measurement_units (
                    unit_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name    TEXT NOT NULL UNIQUE,
                    symbol  TEXT,
                    status  TEXT NOT NULL DEFAULT 'Active'
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

            // ── book_copies (Physical Copies) ──────────────────────────────
            s.execute("""
                CREATE TABLE IF NOT EXISTS book_copies (
                    copy_id          INTEGER PRIMARY KEY AUTOINCREMENT,
                    book_id          INTEGER NOT NULL,
                    copy_code        TEXT    NOT NULL UNIQUE,
                    barcode          TEXT    UNIQUE,
                    status           TEXT    NOT NULL DEFAULT 'Available',
                    condition        TEXT    DEFAULT 'Good',
                    shelf_location   TEXT,
                    price            REAL    DEFAULT 0.0,
                    acquisition_date TEXT    NOT NULL DEFAULT (date('now')),
                    created_at       TEXT    NOT NULL DEFAULT (datetime('now')),
                    FOREIGN KEY (book_id) REFERENCES books(book_id) ON DELETE CASCADE
                )
            """);

            // ── authors & book_authors ─────────────────────────────────────
            s.execute("""
                CREATE TABLE IF NOT EXISTS authors (
                    author_id   INTEGER PRIMARY KEY AUTOINCREMENT,
                    name        TEXT    NOT NULL UNIQUE,
                    biography   TEXT,
                    status      TEXT    NOT NULL DEFAULT 'Active'
                )
            """);
            s.execute("""
                CREATE TABLE IF NOT EXISTS book_authors (
                    book_id   INTEGER NOT NULL,
                    author_id INTEGER NOT NULL,
                    PRIMARY KEY (book_id, author_id),
                    FOREIGN KEY (book_id)   REFERENCES books(book_id) ON DELETE CASCADE,
                    FOREIGN KEY (author_id) REFERENCES authors(author_id) ON DELETE CASCADE
                )
            """);

            // ── book_categories ────────────────────────────────────────────
            s.execute("""
                CREATE TABLE IF NOT EXISTS book_categories (
                    book_id     INTEGER NOT NULL,
                    category_id INTEGER NOT NULL,
                    PRIMARY KEY (book_id, category_id),
                    FOREIGN KEY (book_id)     REFERENCES books(book_id) ON DELETE CASCADE,
                    FOREIGN KEY (category_id) REFERENCES categories(category_id) ON DELETE CASCADE
                )
            """);

            // ── audit_logs ─────────────────────────────────────────────────
            s.execute("""
                CREATE TABLE IF NOT EXISTS audit_logs (
                    audit_id    INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id     INTEGER,
                    username    TEXT,
                    action      TEXT    NOT NULL,
                    entity_type TEXT    NOT NULL,
                    entity_id   TEXT,
                    details     TEXT,
                    ip_address  TEXT,
                    timestamp   TEXT    NOT NULL DEFAULT (datetime('now'))
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
            s.execute("INSERT OR IGNORE INTO id_counters (entity, last_id) VALUES ('CP', 0)");
            s.execute("INSERT OR IGNORE INTO id_counters (entity, last_id) VALUES ('ST', 0)");
            s.execute("INSERT OR IGNORE INTO id_counters (entity, last_id) VALUES ('MB', 0)");
            s.execute("INSERT OR IGNORE INTO id_counters (entity, last_id) VALUES ('EP', 0)");

            // ── Indexes ────────────────────────────────────────────────────
            s.execute("CREATE INDEX IF NOT EXISTS idx_books_isbn     ON books(isbn)");
            s.execute("CREATE INDEX IF NOT EXISTS idx_books_name     ON books(book_name)");
            s.execute("CREATE INDEX IF NOT EXISTS idx_books_category ON books(category)");
            s.execute("CREATE INDEX IF NOT EXISTS idx_books_author   ON books(author)");
            s.execute("CREATE INDEX IF NOT EXISTS idx_books_status   ON books(status)");
            s.execute("CREATE INDEX IF NOT EXISTS idx_copies_book    ON book_copies(book_id)");
            s.execute("CREATE INDEX IF NOT EXISTS idx_copies_status  ON book_copies(status)");
            s.execute("CREATE INDEX IF NOT EXISTS idx_copies_code    ON book_copies(copy_code)");
            s.execute("CREATE INDEX IF NOT EXISTS idx_copies_barcode ON book_copies(barcode)");
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
            s.execute("CREATE INDEX IF NOT EXISTS idx_categories_name ON categories(name)");
            s.execute("CREATE INDEX IF NOT EXISTS idx_publishers_name ON publishers(name)");
            s.execute("CREATE INDEX IF NOT EXISTS idx_authors_name   ON authors(name)");
            s.execute("CREATE INDEX IF NOT EXISTS idx_audit_action   ON audit_logs(action)");
            s.execute("CREATE INDEX IF NOT EXISTS idx_audit_entity   ON audit_logs(entity_type, entity_id)");
            s.execute("CREATE INDEX IF NOT EXISTS idx_audit_time     ON audit_logs(timestamp)");
        }

        runMigrations(c);
        seedDefaultAdmin(c);
        seedLegacyAdmin(c);
        seedSettingsAndCatalogs(c);
        DataSeeder.seedIfEmpty(c);
    }

    // ── Migrations ─────────────────────────────────────────────────────────────

    /** Additive migrations — safe to run on every startup. */
    private static void runMigrations(Connection c) {
        String[] migrations = {
            // v3 additions
            "ALTER TABLE users        ADD COLUMN locked_until TEXT",
            "ALTER TABLE books        ADD COLUMN cover_url TEXT",
            "ALTER TABLE reservations ADD COLUMN expires_at TEXT",
            "ALTER TABLE transactions ADD COLUMN handled_by TEXT",
            "ALTER TABLE transactions ADD COLUMN copy_id INTEGER",
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
            s.execute("CREATE INDEX IF NOT EXISTS idx_tx_copy      ON transactions(copy_id)");
        } catch (SQLException ignored) {}

        // Auto-generate BookCopy entries for books that don't have copies yet
        migrateBookCopies(c);
    }

    private static void migrateBookCopies(Connection c) {
        String countSql = "SELECT COUNT(*) FROM book_copies";
        try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery(countSql)) {
            if (rs.next() && rs.getInt(1) == 0) {
                // Generate initial copies for existing books
                String bookSql = "SELECT book_id, quantity, available_qty, shelf_location FROM books";
                try (Statement bs = c.createStatement(); ResultSet brs = bs.executeQuery(bookSql)) {
                    String insertCopy = "INSERT INTO book_copies (book_id, copy_code, barcode, status, shelf_location) VALUES (?, ?, ?, ?, ?)";
                    try (PreparedStatement ps = c.prepareStatement(insertCopy)) {
                        int copyIdx = 1;
                        while (brs.next()) {
                            int bookId = brs.getInt("book_id");
                            int qty = Math.max(1, brs.getInt("quantity"));
                            int avail = brs.getInt("available_qty");
                            String shelf = brs.getString("shelf_location");

                            for (int i = 1; i <= qty; i++) {
                                String code = String.format("CP-%06d", copyIdx);
                                String barcode = String.format("BC%08d", copyIdx);
                                String status = (i <= avail) ? "Available" : "Issued";
                                ps.setInt(1, bookId);
                                ps.setString(2, code);
                                ps.setString(3, barcode);
                                ps.setString(4, status);
                                ps.setString(5, shelf);
                                ps.addBatch();
                                copyIdx++;
                            }
                        }
                        ps.executeBatch();
                        LOG.info("Auto-migrated {} physical book copies for inventory.", copyIdx - 1);
                    }
                }
            }
        } catch (SQLException e) {
            LOG.warn("Book copies migration notice: {}", e.getMessage());
        }
    }

    // ── Seed ──────────────────────────────────────────────────────────────────

    private static void seedSettingsAndCatalogs(Connection c) throws SQLException {
        // Seed default library information & settings
        String[][] defaultSettings = {
            {"library.name", "Central Library"},
            {"library.institution", "University of Engineering & Technology Peshawar"},
            {"library.email", "library@uetpeshawar.edu.pk"},
            {"library.phone", "+92 91 9216796"},
            {"library.address", "University Campus, Peshawar, Khyber Pakhtunkhwa"},
            {"library.website", "https://uetpeshawar.edu.pk"},
            {"library.currency", "PKR"},
            {"library.taxRate", "0.00"},
            {"pdf.paperSize", "A4 — 210 × 297 mm"},
            {"pdf.language", "English"}
        };
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT OR IGNORE INTO settings (key, value) VALUES (?, ?)")) {
            for (String[] kv : defaultSettings) {
                ps.setString(1, kv[0]);
                ps.setString(2, kv[1]);
                ps.addBatch();
            }
            ps.executeBatch();
        }

        // Seed default categories
        String[][] defaultCats = {
            {"Computer Science", "Programming, Systems, Software & Computing theory"},
            {"Software Engineering", "Architecture, clean code, design patterns & testing"},
            {"Artificial Intelligence", "Machine learning, neural networks & computer vision"},
            {"Data Science", "Data analysis, statistics, big data & mining"},
            {"Mathematics", "Calculus, linear algebra, discrete math & analysis"},
            {"Physics", "Mechanics, electrodynamics, quantum physics & optics"},
            {"Business", "Startups, entrepreneurship, finance & corporate management"},
            {"Finance", "Personal finance, investment, economics & accounting"},
            {"Literature", "Fiction, classics, poetry & drama"},
            {"History", "World history, archaeological studies & civilisations"},
            {"Philosophy", "Ethics, logic, epistemology & political philosophy"},
            {"Medicine", "Physiology, anatomy, pathology & pharmacology"},
            {"Engineering", "Electrical, mechanical, civil & chemical engineering"}
        };
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT OR IGNORE INTO categories (name, description, status) VALUES (?, ?, 'Active')")) {
            for (String[] cat : defaultCats) {
                ps.setString(1, cat[0]);
                ps.setString(2, cat[1]);
                ps.addBatch();
            }
            ps.executeBatch();
        }

        // Seed default publishers
        String[][] defaultPubs = {
            {"O'Reilly Media", "+1 (707) 827-7000", "Sebastopol, CA, USA"},
            {"Addison-Wesley", "+1 (800) 824-7799", "Boston, MA, USA"},
            {"MIT Press", "+1 (617) 253-5646", "Cambridge, MA, USA"},
            {"Prentice Hall", "+1 (800) 922-0579", "Upper Saddle River, NJ, USA"},
            {"Pearson", "+44 20 7010 2000", "London, United Kingdom"},
            {"Wiley", "+1 (201) 748-6000", "Hoboken, NJ, USA"},
            {"Springer Nature", "+49 6221 487 0", "Heidelberg, Germany"},
            {"Oxford University Press", "+44 1865 556767", "Oxford, United Kingdom"},
            {"Cambridge University Press", "+44 1223 358331", "Cambridge, United Kingdom"},
            {"McGraw-Hill", "+1 (800) 338-3987", "New York, NY, USA"},
            {"Elsevier", "+31 20 485 2222", "Amsterdam, Netherlands"},
            {"No Starch Press", "+1 (415) 863-9900", "San Francisco, CA, USA"},
            {"Penguin Classics", "+44 20 7139 3000", "London, United Kingdom"}
        };
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT OR IGNORE INTO publishers (name, contact, address, status) VALUES (?, ?, ?, 'Active')")) {
            for (String[] pub : defaultPubs) {
                ps.setString(1, pub[0]);
                ps.setString(2, pub[1]);
                ps.setString(3, pub[2]);
                ps.addBatch();
            }
            ps.executeBatch();
        }

        // Seed default measurement units
        String[][] defaultUnits = {
            {"Copy", "cpy"},
            {"Volume", "vol"},
            {"Set", "set"},
            {"Book Unit", "unit"},
            {"Bundle", "bdl"}
        };
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT OR IGNORE INTO measurement_units (name, symbol, status) VALUES (?, ?, 'Active')")) {
            for (String[] u : defaultUnits) {
                ps.setString(1, u[0]);
                ps.setString(2, u[1]);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

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
