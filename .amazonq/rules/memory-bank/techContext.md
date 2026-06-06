# Tech Context

## Stack
| Layer | Technology | Version |
|-------|-----------|---------||
| Language | Java | 17 |
| UI Framework | JavaFX | 21.0.1 |
| Database | SQLite (sqlite-jdbc) | 3.46.1.3 |
| Password Hashing | jBCrypt | 0.4 |
| Excel Export | Apache POI (poi-ooxml) | 5.2.5 |
| PDF Export | OpenPDF (librepdf) | 1.3.43 |
| Build | Maven | 3.x |

## Key Maven Dependencies
```xml
<dependency><groupId>org.openjfx</groupId><artifactId>javafx-controls</artifactId><version>21</version></dependency>
<dependency><groupId>org.openjfx</groupId><artifactId>javafx-fxml</artifactId><version>21</version></dependency>
<dependency><groupId>org.xerial</groupId><artifactId>sqlite-jdbc</artifactId><version>3.46.1.3</version></dependency>
<dependency><groupId>org.mindrot</groupId><artifactId>jbcrypt</artifactId><version>0.4</version></dependency>
<dependency><groupId>org.apache.poi</groupId><artifactId>poi-ooxml</artifactId><version>5.2.5</version></dependency>
<dependency><groupId>com.github.librepdf</groupId><artifactId>openpdf</artifactId><version>1.3.43</version></dependency>
```

## Build & Run
- `pom.xml`: `Library-managment-system-advanced-version/Library/pom.xml`
- Artifact: `library-management-system-2.0.0.jar`
- Main class: `com.library.LibraCoreApp`
- Build: `mvn clean package` | Run: `mvn javafx:run` or `run.bat`
- JavaFX SDK also bundled at `Library/javafx-sdk/` (for non-Maven runs)
- JVM config: `Library/.mvn/jvm.config`

## Resource Paths
- All FXML, CSS, images: `src/main/resources/com/library/ui/`
- FXML loaded via: `getClass().getResource("/com/library/ui/<file>.fxml")`
- CSS files: `light-theme.css`, `dark-theme.css`, `Login.css`, `Dashboard.css`, `ModernDashboard.css`, `ProfessionalDashboard.css`, `ComprehensiveForms.css`, `AddMember.css`
- Images: `src/main/resources/com/library/ui/images/` (login icon: `login.png`)

## Database
- File: `library.db` (next to JAR), WAL mode, FK constraints ON
- Connection pool: `ArrayBlockingQueue<Connection>` size 5 in `DatabaseConnection`
- `PooledConnection` wraps real connection — `close()` returns to pool, never closes
- DB URL: `jdbc:sqlite:library.db`

## Performance Targets
- App startup: < 3 seconds
- DB query: < 500ms for 10,000 records
- Search response: < 200ms (300ms debounce on UI side)
- Chart rendering: < 1 second

## Config File
`libra_config.properties` — next to JAR, loaded/saved by `AppConfig` singleton.
Keys: `library.loanDays` (14), `library.fineRate` (5.0), `library.gracePeriod` (2), `library.maxBooks` (5), `library.itemsPerPage` (10), `library.currency` (PKR), `ui.theme` (light), `notify.overdueAlert` (true), `notify.dueSoonDays` (2)

## Package Structure
```
com.library/
├── LibraCoreApp.java          ← ONLY main()
├── auth/                      ← AuthModule, AuthController, AuthService
├── books/                     ← BookModule, BookController, BooksService
├── members/                   ← MemberModule, MemberController, MembersService
├── students/                  ← StudentModule, StudentController (stub), StudentService
├── employees/                 ← EmployeeModule, EmployeeController, EmployeesService
├── issuing/                   ← IssueModule, IssueController, IssueService, FineCalculator
├── dashboard/                 ← DashboardModule, DashboardController, DashboardService, ChartFactory
├── reports/                   ← ReportModule, ReportController, ReportsService
├── shared/                    ← SharedModule, DatabaseManager, ValidationUtil, AlertUtil, DateUtil, ChartUtil
├── controller/                ← Real JavaFX FXML controllers
├── ui/                        ← AddBookController, AddMemberController, AddStudentController, LoginController
├── service/                   ← BookService, MemberService, EmployeeService, TransactionService,
│                                 ReportService, PrintService, UserService, LibraryService,
│                                 ReservationService, GlobalSearchService, SearchService,
│                                 SearchTrie, SerialNumberService
├── model/                     ← Book, Member, Employee, User, IssueRecord, Transaction,
│                                 Reservation, ActivityRecord
├── repository/                ← BookRepository, MemberRepository
├── database/                  ← DatabaseConnection (pool + schema), DataSeeder
├── config/                    ← AppConfig (singleton), ThemeManager
├── security/                  ← PasswordUtil (BCrypt), SessionManager (30-min timeout)
├── cache/                     ← DashboardCache, DashboardStats
└── util/                      ← Constants (recovery key SHA-256), IdGenerator, PageRequest,
                                  SerialNumberService, ToastNotification, SampleDataGenerator
```

## Legacy / Unused Files
- `LibraryApp.java`, `LibraryManagementSystem.java`, `ModernLibraryApp.java`, `ProfessionalLibraryApp.java` — old entry points, not wired
- `ui/LoginController.java` — duplicates `controller/LoginController`, not wired
- `students/StudentController.java` — empty stub; real impl in `ui/AddStudentController`
