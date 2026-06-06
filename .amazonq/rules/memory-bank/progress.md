# Progress

## What Works
- [x] Branch-Per-Module tree architecture (8 branches + shared)
- [x] `LibraCoreApp` as sole entry point; `SharedModule` initialises DB + seeds data
- [x] SQLite connection pool (size 5, WAL mode, FK ON) via `PooledConnection`
- [x] Schema creation + additive migrations (idempotent, safe to re-run)
- [x] BCrypt auth (`PasswordUtil`) + 30-min session timeout (`SessionManager`)
- [x] Roles: ADMIN / LIBRARIAN with `canWrite()` check
- [x] Recovery key `03150315` via `Constants.isValidRecoveryKey()` (SHA-256)
- [x] Book CRUD — add, edit, delete, search (title/author/ISBN), paginate, filter
- [x] Member CRUD — add, edit, search, paginate, membership types
- [x] Employee CRUD — add, edit, search, archive, EP code generation
- [x] Issue book (validates member status, fine balance, book limit from `AppConfig`)
- [x] Return book — `FineCalculator.calculate(dueDate, checkDate)`, fine added to member balance
- [x] Overdue detection + `PriorityQueue` sorted by days overdue
- [x] Reservation queue (`ReservationService`)
- [x] Archive / Unarchive (books, members, employees) with `archive_log` reason tracking
- [x] Structured ID generation (`BK`, `ST`, `EP` via `id_counters` + `IdGenerator`)
- [x] Serial number resequencing (`SerialNumberService`)
- [x] Dashboard stats — `DashboardCache` (5-min TTL), 30s auto-refresh
- [x] Charts — PieChart (categories), BarChart (monthly issues), LineChart (trends)
- [x] PDF export (OpenPDF via `ReportService` + `PrintService`)
- [x] Excel export (Apache POI via `ReportService`)
- [x] Global search — `GlobalSearchService` + `SearchTrie`, 300ms debounce
- [x] Settings persistence — `AppConfig` ↔ `libra_config.properties`
- [x] Theme switching — `ThemeManager`, `light-theme.css` / `dark-theme.css`
- [x] Toast notifications (`ToastNotification`)
- [x] Audit trail (`activity_log` table)
- [x] Sample data seeder (`DataSeeder.seedIfNeeded()`)

## Incomplete / Planned
- [ ] Student CRUD UI — `AddStudentForm.fxml` exists but not wired to dashboard nav
- [ ] Forgot/Reset password FXML not wired to controllers
- [ ] Archive students/employees from UI
- [ ] Barcode scanner integration (P1)
- [ ] Email overdue notifications via SMTP (P1)
- [ ] Multi-language / i18n (P2)
- [ ] Scheduled database backups (P2)
- [ ] Remove legacy dead-code entry points (`LibraryApp`, `ModernLibraryApp`, etc.)
- [ ] Full test coverage

## Token Optimization — Branch-First Scan
| Task | Branches to Scan |
|------|-----------------|
| Fix book search | `books/`, `service/BookService`, `model/Book`, `repository/BookRepository` |
| Fix login | `auth/`, `service/UserService`, `security/` |
| Fix fine calc | `issuing/FineCalculator`, `service/TransactionService` |
| Fix dashboard | `dashboard/`, `cache/DashboardCache` |
| Fix report export | `reports/`, `service/ReportService`, `service/PrintService` |
| Add student CRUD | `students/`, `ui/AddStudentController`, `service/StudentService` |
