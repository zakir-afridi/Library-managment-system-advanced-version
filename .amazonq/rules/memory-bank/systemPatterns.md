# System Patterns

## Architecture — Branch-Per-Module Tree
Single entry point (`LibraCoreApp`) is the **Main Trunk**. Each feature is an independent **Branch** exposing only static Module methods. Branches import only from `shared/`.

## Startup Sequence
```java
// LibraCoreApp.java
init()  → SharedModule.initDatabase()   // DB schema + DataSeeder
start() → AuthModule.showLogin(stage)   // loads LoginPage.fxml
stop()  → SharedModule.saveConfig()     // persists AppConfig → libra_config.properties
```

## Branch Static API Pattern
```java
// Example: BookModule.java
public class BookModule {
    public static void showBookList(Pane container) { /* FXMLLoader */ }
    public static void showAddBook()                { /* modal */ }
    public static void refreshBookData()            { controller.loadBooks(); }
    public static List<Book> searchBooks(String q)  { return BookService.search(q); }
    public static int getTotalBookCount()           { return BookService.getCount(); }
}
```

## Navigation Tree
```
LibraCoreApp
└── AuthModule → LoginPage.fxml
    └── DashboardModule → ModernDashboard.fxml
        ├── BookModule
        ├── MemberModule
        ├── StudentModule
        ├── EmployeeModule
        ├── IssueModule
        └── ReportModule
```

## Branch Dependency Map
```
auth/       → shared/, service/UserService, security/, config/
dashboard/  → shared/, cache/DashboardCache, service/TransactionService
books/      → shared/, service/BookService, repository/BookRepository
members/    → shared/, service/MemberService, repository/MemberRepository
students/   → shared/, database/ (direct)
employees/  → shared/, service/EmployeeService
issuing/    → shared/, FineCalculator, service/TransactionService, config/AppConfig
reports/    → shared/ + ALL other branches
```

## Branch File Map
| Branch | Module | Service (branch) | Controller (branch) | Real Controller |
|--------|--------|-----------------|--------------------|--------------------|
| `auth/` | `AuthModule` | `AuthService` | `AuthController` | `controller/LoginController` |
| `books/` | `BookModule` | `BooksService` | `BookController` | `controller/BookController` |
| `members/` | `MemberModule` | `MembersService` | `MemberController` | `controller/MemberController` |
| `students/` | `StudentModule` | `StudentService` | `StudentController` *(stub)* | `ui/AddStudentController` |
| `employees/` | `EmployeeModule` | `EmployeesService` | `EmployeeController` | `controller/EmployeeController` |
| `issuing/` | `IssueModule` | `IssueService` | `IssueController` | `controller/IssueReturnController` |
| `dashboard/` | `DashboardModule` | `DashboardService` | `DashboardController` | `controller/ModernDashboardController` |
| `reports/` | `ReportModule` | `ReportsService` | `ReportController` | `controller/ReportsController` |

## FXML ↔ Controller Mapping (Actual)
| FXML | Controller |
|------|-----------|
| `LoginPage.fxml` | `controller/LoginController` |
| `ModernDashboard.fxml` | `controller/ModernDashboardController` |
| `Dashboard.fxml` | `controller/DashboardController` |
| `AddBookForm.fxml` | `ui/AddBookController` |
| `AddBook.fxml` / `ComprehensiveAddBookForm.fxml` | `ui/AddBookController` |
| `UpdateBook.fxml` | `controller/BookController` |
| `AddMemberForm.fxml` / `AddMember.fxml` | `ui/AddMemberController` |
| `UpdateMember.fxml` | `controller/MemberController` |
| `AddStudentForm.fxml` | `ui/AddStudentController` |
| `IssueReturnBooksForm.fxml` | `controller/IssueReturnController` |
| `IssueBook.fxml` / `ReturnBook.fxml` | `controller/IssueReturnController` |
| `ReportsView.fxml` | `controller/ReportsController` |
| `ArchiveView.fxml` | `controller/ArchiveController` |
| `Settings.fxml` | `controller/SettingsController` |
| `EmployeeForm.fxml` | `controller/EmployeeController` |

## Database Design
- Single SQLite file `library.db`, WAL mode, FK constraints ON, pool size 5
- `DatabaseConnection.getConnection()` → `PooledConnection` (close() returns to pool)
- Schema via `applySchema()` at startup; additive migrations via `runMigrations()` (safe to re-run)

### Tables
| Table | Key Columns |
|-------|------------|
| `users` | `username`, `password_hash`, `role`, `status`, `failed_attempts`, `force_password_change` |
| `books` | `isbn`, `book_name`, `author`, `category`, `quantity`, `available_qty`, `status`, `book_code`, `serial_no` |
| `members` | `student_id`, `name`, `department`, `program`, `status`, `fine_balance`, `member_code`, `serial_no`, `membership_type` |
| `students` | `student_id`, `full_name`, `department`, `year`, `email`, `phone` |
| `transactions` | `book_id`, `member_id`, `issue_date`, `due_date`, `return_date`, `fine_amount`, `fine_paid`, `status`, `return_condition` |
| `reservations` | `book_id`, `member_id`, `queue_position`, `status` |
| `employees` | `employee_code`, `name`, `designation`, `salary`, `status`, `serial_no` |
| `settings` | `key`, `value` |
| `activity_log` | `user_id`, `action`, `details`, `timestamp` |
| `id_counters` | `entity` (BK/ST/MB/EP), `last_id` |
| `admin`, `librarydetails` | legacy (backward compat) |

### Migrations (additive, idempotent)
- `members.archived_date`, `books.archived_date`
- `books.book_code`, `members.member_code`
- `books.serial_no`, `members.serial_no`, `employees.serial_no`

## Key Design Decisions

### ID Generation
- `BK00000001` (books), `ST00000001` (students/members), `EP00000001` (employees)
- Counter in `id_counters` table; managed by `IdGenerator` + `SerialNumberService`
- Member display ID: `LIB-YYYY-NNNN`

### Fine Calculation
- `FineCalculator.calculate(dueDate, checkDate)` — reads grace + rate from `AppConfig`
- Default: PKR 5/day, 2-day grace
- `isOverdue(dueDate)` — checks against `LocalDate.now()`

### Caching
- `DashboardCache` singleton — invalidated on writes, 30s auto-refresh
- `BookService` — in-memory ISBN → Book map, TTL 5 min

### Security
- BCrypt via `PasswordUtil.hash()` / `PasswordUtil.check()`
- `SessionManager` — JavaFX `Timeline`, 30-min timeout, `resetTimer()` on interaction
- Roles: `ADMIN` (full), `LIBRARIAN` (read/write, no admin settings)
- All DB access via `PreparedStatement` — no SQL injection

### Search
- `GlobalSearchService` + `SearchTrie` — 300ms debounce, case-insensitive
- SQL fallback: `LIKE '%query%'` on indexed columns

### Theme
- `ThemeManager.applyTheme(scene)` loads CSS from `src/main/resources/com/library/ui/css/`
- Persisted in `AppConfig` (`ui.theme` = `light` | `dark`)
- CSS files: `light-theme.css`, `dark-theme.css`

## AI Scanning Rule — Branch-First Search
When fixing a bug, scan ONLY the relevant branch + `service/` + `model/`.

| Issue | Scan | Ignore |
|-------|------|--------|
| Book search broken | `books/`, `service/BookService`, `model/Book` | all other branches |
| Login fails | `auth/`, `service/UserService`, `security/` | all other branches |
| Dashboard charts wrong | `dashboard/`, `cache/DashboardCache` | all other branches |
| Fine calculation wrong | `issuing/FineCalculator`, `service/TransactionService` | all other branches |
| Report export broken | `reports/`, `service/ReportService`, `service/PrintService` | all other branches |
