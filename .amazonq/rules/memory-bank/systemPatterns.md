# System Patterns

## Architecture — Branch-Per-Module Tree
Single entry point (`LibraCoreApp`) is the **Main Trunk**. Every feature is an independent **Branch** that exposes a static Module API. Branches only import from `shared/`.

```
LIBRARY-TREE/
│
├── MAIN TRUNK
│   └── com/library/LibraCoreApp.java   ← ONLY main() in entire project
│
├── BRANCH: auth/
│   ├── AuthModule.java       ← static showLogin(), getCurrentUser(), logout()
│   ├── AuthController.java
│   ├── AuthService.java
│   └── resources/auth/
│       ├── login.fxml
│       ├── forgot_password.fxml
│       └── reset_password.fxml
│
├── BRANCH: dashboard/
│   ├── DashboardModule.java  ← static showDashboard(), updateStats()
│   ├── DashboardController.java
│   ├── DashboardService.java
│   ├── ChartFactory.java
│   └── resources/dashboard/dashboard.fxml
│
├── BRANCH: books/
│   ├── BookModule.java       ← static showBookList(), showAddBook(), searchBooks(), getTotalBookCount()
│   ├── BookController.java
│   ├── BookService.java
│   ├── BookRepository.java
│   └── resources/books/
│       ├── book_list.fxml
│       ├── book_form.fxml
│       └── book_archive.fxml
│
├── BRANCH: members/
│   ├── MemberModule.java
│   ├── MemberController.java
│   ├── MemberService.java
│   └── resources/members/
│       ├── member_list.fxml
│       └── member_form.fxml
│
├── BRANCH: students/
│   ├── StudentModule.java
│   ├── StudentController.java
│   ├── StudentService.java
│   └── resources/students/
│       ├── student_list.fxml
│       └── student_form.fxml
│
├── BRANCH: employees/
│   ├── EmployeeModule.java
│   ├── EmployeeController.java
│   ├── EmployeeService.java
│   └── resources/employees/
│       ├── employee_list.fxml
│       └── employee_form.fxml
│
├── BRANCH: issuing/
│   ├── IssueModule.java
│   ├── IssueController.java
│   ├── IssueService.java
│   ├── FineCalculator.java
│   └── resources/issuing/
│       ├── issue_book.fxml
│       ├── return_book.fxml
│       └── issue_history.fxml
│
├── BRANCH: reports/
│   ├── ReportModule.java
│   ├── ReportController.java
│   ├── ReportService.java
│   └── resources/reports/reports.fxml
│
├── BRANCH: shared/             ← CONNECTS ALL BRANCHES
│   ├── SharedModule.java       ← static initDatabase()
│   ├── DatabaseManager.java    ← SQLite connection pool
│   ├── Constants.java          ← Recovery key 03150315 (SHA-256)
│   ├── ValidationUtil.java
│   ├── AlertUtil.java
│   ├── DateUtil.java
│   ├── ChartUtil.java
│   ├── model/
│   │   ├── User.java
│   │   ├── Book.java
│   │   ├── Member.java
│   │   ├── Student.java
│   │   ├── Employee.java
│   │   ├── IssueRecord.java
│   │   └── ArchiveLog.java
│   └── resources/shared/css/
│       ├── main.css
│       ├── forms.css
│       ├── tables.css
│       └── charts.css
│
├── controller/   ← Real JavaFX FXML controllers (UI logic)
├── ui/           ← Add/Update dialog controllers
├── service/      ← Business logic (called by branch *Service delegates)
├── model/        ← Plain Java beans
├── database/     ← DatabaseConnection (pool) + DataSeeder
├── config/       ← AppConfig, ThemeManager
├── security/     ← PasswordUtil (BCrypt), SessionManager
├── cache/        ← DashboardCache, DashboardStats
└── util/         ← IdGenerator, PageRequest, Constants, …
```

## Main Trunk Pattern (LibraCoreApp.java)
```java
public class LibraCoreApp extends Application {
    @Override public void init() {
        SharedModule.initDatabase();   // shared/ first
    }
    @Override public void start(Stage stage) {
        AuthModule.showLogin(stage);   // auth/ branch launches everything
    }
    @Override public void stop() {
        DatabaseManager.close();
    }
    public static void main(String[] args) { launch(args); }
}
```

## Branch Static API Pattern
Each branch exposes ONLY static methods. Other branches never touch controllers directly.
```java
// BookModule.java — example
public class BookModule {
    private static BookController controller;
    public static void showBookList(Pane container) { /* FXMLLoader → container */ }
    public static void showAddBook() { /* modal dialog */ }
    public static void refreshBookData() { if (controller != null) controller.loadBooks(); }
    public static List<Book> searchBooks(String query) { return BookService.search(query); }
    public static int getTotalBookCount() { return BookService.getCount(); }
    public static int getAvailableBookCount() { return BookService.getAvailableCount(); }
}
```

## Navigation Tree (Scene Switching)
```
LibraCoreApp (Root)
└── AuthModule
    └── DashboardModule
        ├── BookModule
        ├── MemberModule
        ├── StudentModule
        ├── EmployeeModule
        ├── IssueModule
        └── ReportModule
```

## Branch Dependency Map
```
auth/       → shared/ (UserService, security/, config/)
dashboard/  → shared/ (DashboardCache, TransactionService)
books/      → shared/ (BookService)
members/    → shared/ (MemberService)
students/   → shared/ (DatabaseConnection direct)
employees/  → shared/ (EmployeeService)
issuing/    → shared/ (TransactionService, AppConfig) + books/ + members/ + students/
reports/    → shared/ + ALL other branches
```

## Branch File Map
| Branch | Module | Service | Controller | Notes |
|--------|--------|---------|------------|-------|
| auth/ | AuthModule | AuthService | AuthController | Real: controller/LoginController |
| books/ | BookModule | BooksService | BookController | Real: controller/BookController |
| members/ | MemberModule | MembersService | MemberController | Real: controller/MemberController |
| students/ | StudentModule | StudentService | StudentController | Full impl (no legacy service) |
| employees/ | EmployeeModule | EmployeesService | EmployeeController | Real: controller/EmployeeController |
| issuing/ | IssueModule | IssueService | IssueController | + FineCalculator |
| dashboard/ | DashboardModule | DashboardService | DashboardController | + ChartFactory |
| reports/ | ReportModule | ReportsService | ReportController | Real: controller/ReportsController |
| shared/ | SharedModule | — | — | + DatabaseManager, ValidationUtil, AlertUtil, DateUtil, ChartUtil |

## AI Scanning Rule — Branch-First Search
When fixing a bug, ONLY scan the relevant branch + `service/` + `model/`. IGNORE all other branches.

| Issue | Scan | Ignore |
|-------|------|--------|
| Book search broken | books/, service/BookService, model/Book | members/, students/, employees/, reports/ |
| Login fails | auth/, service/UserService, security/ | All other branches |
| Dashboard charts wrong | dashboard/, cache/DashboardCache | All other branches |
| Fine calculation wrong | issuing/, service/TransactionService | All other branches |

Token savings vs flat scan: ~82–85% reduction per task.

## AI Prompt Templates

### Fix a Branch Bug
```
BRANCH: books/
ISSUE: Search not filtering by ISBN
SCAN ONLY: BookController.java, BookService.java, BookRepository.java, model/Book.java, DatabaseManager.java
DO NOT SCAN: members/, students/, employees/, reports/, issuing/
```

### Add Feature to Branch
```
BRANCH: issuing/
FEATURE: Overdue email notification
SCAN ONLY: IssueModule.java, IssueController.java, IssueService.java, FineCalculator.java, model/IssueRecord.java
DO NOT MODIFY: Other branches
```

## Key Design Decisions

### Database
- Single SQLite file `library.db`, WAL mode, connection pool size 5
- `DatabaseConnection.getConnection()` → `PooledConnection` wrapper; `close()` returns to pool
- Schema via `applySchema()` at startup; additive migrations via `runMigrations()` (safe to re-run)
- Foreign key constraints enabled on every connection

### Password Recovery
- Universal key: `03150315` in `Constants.java`, compared via SHA-256
- Flow: Login → ForgotPasswordController → key check → ResetPasswordController → DB update

### ID Generation
- `BK00000001` (books), `ST00000001` (members), `EP00000001` (employees)
- Counter in `id_counters` table, managed by `IdGenerator` + `SerialNumberService`
- Member display ID: `LIB-YYYY-NNNN`

### Pagination
- `PageRequest.of(page, pageSize)` — LIMIT/OFFSET on all list queries
- Default: 20 rows/page (tables), 10 (config)

### Caching
- `BookService`: in-memory ISBN → Book map, TTL 5 min
- `DashboardCache` singleton; invalidated on any write; 30s auto-refresh

### Transaction Data Structures
- `PriorityQueue<Transaction>` — overdue sorted by days overdue (max-heap)
- `Deque<Transaction>` — last 50 activities, supports undo
- `HashMap<memberId, List<Transaction>>` — active borrowings per member (O(1))

### Security
- BCrypt via `PasswordUtil`; `SessionManager` auto-logout 30 min
- Roles: `ADMIN` (full), `LIBRARIAN` (read/write, no admin settings)
- `PreparedStatement` everywhere — no SQL injection

### Theme
- `ThemeManager` loads CSS from `src/main/resources/com/library/ui/css/`
- Persisted in `AppConfig` (`ui.theme` = `light` | `dark`)

## FXML ↔ Controller Mapping
| FXML | Controller |
|------|-----------|
| LoginPage.fxml | controller/LoginController |
| forgot_password.fxml | ui/login/ForgotPasswordController |
| reset_password.fxml | ui/login/ResetPasswordController |
| ModernDashboard.fxml | controller/ModernDashboardController |
| Dashboard.fxml | controller/DashboardController |
| AddBookForm.fxml | ui/AddBookController |
| AddMemberForm.fxml | ui/AddMemberController |
| IssueReturnBooksForm.fxml | controller/IssueReturnController |
| ReportsView.fxml | controller/ReportsController |
| ArchiveView.fxml | controller/ArchiveController |
| Settings.fxml | controller/SettingsController |
| EmployeeForm.fxml | controller/EmployeeController |
